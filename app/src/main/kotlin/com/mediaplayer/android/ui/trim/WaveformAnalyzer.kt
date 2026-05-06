package com.mediaplayer.android.ui.trim

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.mediaplayer.android.MediaPlayerApp
import com.mediaplayer.android.data.Network
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.ShortBuffer

/**
 * Decodes the streamed MP3 of a song to PCM, then collapses it into a fixed
 * number of amplitude bins so the trim editor can render a real waveform and
 * snap-to-silence lands on actual quiet sections rather than a synthetic
 * curve. Result is cached on disk per (songId, bars) so reopening the editor
 * is free.
 *
 * Pipeline:
 *  1. Stream bytes through Network.okHttp (auth + cert pinning reused) into
 *     a tmp file inside cacheDir/waveform/. MediaExtractor over HTTP can be
 *     flaky on partial servers; a local file is bulletproof.
 *  2. MediaExtractor picks the audio track; MediaCodec decodes to 16-bit PCM.
 *  3. Each PCM frame's peak amplitude (max abs sample across channels) feeds
 *     the bin matching its presentation timestamp. Per bin we keep the max
 *     peak — RMS smooths out percussive transients we want to *see* in the
 *     waveform, and we want valleys to mean "actually quiet" for snap.
 *  4. Bins normalized to [0..1]; if the track is mostly silence the result
 *     is left raw rather than scaled to 1.0 — avoids amplifying noise floor.
 */
object WaveformAnalyzer {

    /** Bin count we render. Must match the canvas grid in TrimScreen. */
    const val BARS = 96

    private const val CACHE_DIR = "waveform"
    private const val PEAKS_MAGIC = 0x57415645 // 'WAVE'
    private const val PEAKS_VERSION = 1

    suspend fun analyze(songId: Long, bars: Int = BARS): FloatArray? = withContext(Dispatchers.IO) {
        val cacheRoot = File(MediaPlayerApp.instance.cacheDir, CACHE_DIR).apply { mkdirs() }
        val peaksFile = File(cacheRoot, "$songId-$bars.peaks")
        readPeaksCache(peaksFile, bars)?.let { return@withContext it }

        val tmpFile = File(cacheRoot, "$songId.bin")
        try {
            if (!tmpFile.exists() || tmpFile.length() == 0L) {
                if (!downloadStream(songId, tmpFile)) return@withContext null
            }
            val peaks = decodeToPeaks(tmpFile, bars) ?: return@withContext null
            writePeaksCache(peaksFile, peaks)
            peaks
        } finally {
            // Keep the bin file around for reanalysis with a different bar count;
            // cacheDir gets reaped by Android under storage pressure anyway.
        }
    }

    private fun downloadStream(songId: Long, dst: File): Boolean {
        val req = Request.Builder().url(Network.streamUrl(songId)).get().build()
        return runCatching {
            Network.okHttp.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return false
                val body = resp.body ?: return false
                FileOutputStream(dst).use { out ->
                    body.byteStream().use { input -> input.copyTo(out) }
                }
                true
            }
        }.getOrElse { false }
    }

    private fun decodeToPeaks(file: File, bars: Int): FloatArray? {
        val extractor = MediaExtractor()
        try {
            extractor.setDataSource(file.absolutePath)
        } catch (_: Throwable) {
            return null
        }
        val trackIdx = (0 until extractor.trackCount).firstOrNull { i ->
            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: run { extractor.release(); return null }

        val format = extractor.getTrackFormat(trackIdx)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: run { extractor.release(); return null }
        val durationUs = format.getLong(MediaFormat.KEY_DURATION).coerceAtLeast(1L)
        extractor.selectTrack(trackIdx)

        val codec = try { MediaCodec.createDecoderByType(mime) } catch (_: Throwable) {
            extractor.release(); return null
        }
        codec.configure(format, null, null, 0)
        codec.start()

        val peaks = FloatArray(bars)
        val info = MediaCodec.BufferInfo()
        var sawInputEos = false
        var sawOutputEos = false
        try {
            while (!sawOutputEos) {
                if (!sawInputEos) {
                    val inIdx = codec.dequeueInputBuffer(10_000L)
                    if (inIdx >= 0) {
                        val buf = codec.getInputBuffer(inIdx) ?: continue
                        val sampleSize = extractor.readSampleData(buf, 0)
                        if (sampleSize < 0) {
                            codec.queueInputBuffer(inIdx, 0, 0, 0L, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            sawInputEos = true
                        } else {
                            val pts = extractor.sampleTime
                            codec.queueInputBuffer(inIdx, 0, sampleSize, pts, 0)
                            extractor.advance()
                        }
                    }
                }
                val outIdx = codec.dequeueOutputBuffer(info, 10_000L)
                when {
                    outIdx >= 0 -> {
                        if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) sawOutputEos = true
                        if (info.size > 0) {
                            val out = codec.getOutputBuffer(outIdx)
                            if (out != null) {
                                accumulatePeaks(out, info, durationUs, bars, peaks)
                            }
                        }
                        codec.releaseOutputBuffer(outIdx, false)
                    }
                    outIdx == MediaCodec.INFO_TRY_AGAIN_LATER -> { /* spin */ }
                    outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> { /* ignore */ }
                }
            }
        } catch (_: Throwable) {
            // Partial decode is still useful — fall through and normalize what we have.
        } finally {
            try { codec.stop() } catch (_: Throwable) { }
            try { codec.release() } catch (_: Throwable) { }
            try { extractor.release() } catch (_: Throwable) { }
        }
        return normalize(peaks)
    }

    /**
     * Copy the codec's PCM output into the matching peak bin. Android decoders
     * emit 16-bit little-endian PCM by default; we read shorts, take abs(),
     * and update the bin's running max. PTS lands the sample at the right
     * timeline bucket so VBR MP3s don't drift across the waveform.
     */
    private fun accumulatePeaks(
        buffer: ByteBuffer,
        info: MediaCodec.BufferInfo,
        durationUs: Long,
        bars: Int,
        peaks: FloatArray,
    ) {
        buffer.position(info.offset)
        buffer.limit(info.offset + info.size)
        val shorts: ShortBuffer = buffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
        val sampleCount = shorts.remaining()
        if (sampleCount == 0) return

        val bin = ((info.presentationTimeUs.toFloat() / durationUs) * bars).toInt()
            .coerceIn(0, bars - 1)
        var localMax = 0
        var i = 0
        // Stride to keep the inner loop cheap on long frames; one sample every
        // ~32 is plenty to capture peak amplitude for a 96-bar visualisation.
        val stride = 32
        while (i < sampleCount) {
            val s = shorts.get(i).toInt()
            val abs = if (s < 0) -s else s
            if (abs > localMax) localMax = abs
            i += stride
        }
        val norm = localMax / 32_768f
        if (norm > peaks[bin]) peaks[bin] = norm
    }

    private fun normalize(peaks: FloatArray): FloatArray {
        var maxV = 0f
        for (v in peaks) if (v > maxV) maxV = v
        if (maxV <= 0f) return peaks
        // Soft floor of 0.05 keeps quiet bars visible (matches the synthetic
        // baseline in TrimScreen.generateWaveform) without faking content.
        val scale = 1f / maxV
        for (i in peaks.indices) {
            peaks[i] = (peaks[i] * scale).coerceAtLeast(0.05f)
        }
        return peaks
    }

    private fun readPeaksCache(file: File, expectedBars: Int): FloatArray? {
        if (!file.exists()) return null
        return runCatching {
            DataInputStream(file.inputStream().buffered()).use { input ->
                if (input.readInt() != PEAKS_MAGIC) return null
                if (input.readInt() != PEAKS_VERSION) return null
                val count = input.readInt()
                if (count != expectedBars) return null
                FloatArray(count) { input.readFloat() }
            }
        }.getOrNull()
    }

    private fun writePeaksCache(file: File, peaks: FloatArray) {
        runCatching {
            DataOutputStream(file.outputStream().buffered()).use { out ->
                out.writeInt(PEAKS_MAGIC)
                out.writeInt(PEAKS_VERSION)
                out.writeInt(peaks.size)
                for (v in peaks) out.writeFloat(v)
            }
        }
    }
}
