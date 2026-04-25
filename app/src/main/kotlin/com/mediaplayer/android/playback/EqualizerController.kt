package com.mediaplayer.android.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.Equalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class EqPreset(val label: String) {
    FLAT("Flat"),
    BASS_BOOST("Bass"),
    TREBLE_BOOST("Treble"),
    VOCAL("Vocal"),
    CUSTOM("Custom"),
}

data class BandInfo(
    val index: Int,
    val centerFreqHz: Int,
    val levelMilliBel: Int,
    val minLevel: Int,
    val maxLevel: Int,
) {
    val freqLabel: String
        get() = if (centerFreqHz < 1000) "$centerFreqHz Hz"
                else "${"%.0f".format(centerFreqHz / 1000f)} kHz"
    val dbLabel: String
        get() = "${"%.1f".format(levelMilliBel / 100f)} dB"
}

data class EqState(
    val enabled: Boolean,
    val preset: EqPreset,
    val bands: List<BandInfo>,
)

object EqualizerController {

    private val _state = MutableStateFlow<EqState?>(null)
    val state: StateFlow<EqState?> = _state.asStateFlow()

    private var eq: Equalizer? = null
    private var prefs: SharedPreferences? = null

    fun init(context: Context, audioSessionId: Int) {
        if (audioSessionId == 0) return
        val app = context.applicationContext
        val p = app.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE)
        prefs = p
        try {
            val e = Equalizer(0, audioSessionId)
            e.enabled = p.getBoolean(PREF_ENABLED, true)
            restoreBands(e, p)
            eq = e
            _state.value = snapshot(e, savedPreset(p))
        } catch (_: Exception) {
            // Device doesn't support Equalizer
        }
    }

    fun release() {
        try { eq?.release() } catch (_: Exception) {}
        eq = null
        _state.value = null
    }

    fun setEnabled(enabled: Boolean) {
        val e = eq ?: return
        e.enabled = enabled
        prefs?.edit()?.putBoolean(PREF_ENABLED, enabled)?.apply()
        _state.value = _state.value?.copy(enabled = enabled)
    }

    fun setBandLevel(band: Int, levelMilliBel: Int) {
        val e = eq ?: return
        val range = e.bandLevelRange
        val clamped = levelMilliBel.coerceIn(range[0].toInt(), range[1].toInt())
        e.setBandLevel(band.toShort(), clamped.toShort())
        prefs?.edit()
            ?.putInt(bandKey(band), clamped)
            ?.putString(PREF_PRESET, EqPreset.CUSTOM.name)
            ?.apply()
        _state.value = _state.value?.let { s ->
            s.copy(
                preset = EqPreset.CUSTOM,
                bands = s.bands.map { if (it.index == band) it.copy(levelMilliBel = clamped) else it },
            )
        }
    }

    fun applyPreset(preset: EqPreset) {
        val e = eq ?: return
        val bandCount = e.numberOfBands.toInt()
        val range = e.bandLevelRange
        val min = range[0].toInt()
        val max = range[1].toInt()
        val levels = presetLevels(preset, bandCount, min, max)
        val editor = prefs?.edit()
        repeat(bandCount) { i ->
            e.setBandLevel(i.toShort(), levels[i].toShort())
            editor?.putInt(bandKey(i), levels[i])
        }
        editor?.putString(PREF_PRESET, preset.name)?.apply()
        _state.value = snapshot(e, preset)
    }

    private fun restoreBands(e: Equalizer, p: SharedPreferences) {
        val range = e.bandLevelRange
        repeat(e.numberOfBands.toInt()) { i ->
            val saved = p.getInt(bandKey(i), Int.MIN_VALUE)
            if (saved != Int.MIN_VALUE) {
                val clamped = saved.coerceIn(range[0].toInt(), range[1].toInt())
                e.setBandLevel(i.toShort(), clamped.toShort())
            }
        }
    }

    private fun snapshot(e: Equalizer, preset: EqPreset): EqState {
        val range = e.bandLevelRange
        return EqState(
            enabled = e.enabled,
            preset = preset,
            bands = (0 until e.numberOfBands.toInt()).map { i ->
                BandInfo(
                    index = i,
                    centerFreqHz = e.getCenterFreq(i.toShort()) / 1000,
                    levelMilliBel = e.getBandLevel(i.toShort()).toInt(),
                    minLevel = range[0].toInt(),
                    maxLevel = range[1].toInt(),
                )
            },
        )
    }

    private fun savedPreset(p: SharedPreferences): EqPreset =
        p.getString(PREF_PRESET, EqPreset.FLAT.name)
            ?.let { runCatching { EqPreset.valueOf(it) }.getOrNull() }
            ?: EqPreset.FLAT

    private fun presetLevels(preset: EqPreset, bandCount: Int, min: Int, max: Int): List<Int> {
        val raw = when (preset) {
            EqPreset.FLAT        -> List(bandCount) { 0 }
            EqPreset.BASS_BOOST  -> listOf(700, 400, 0, -100, -200)
            EqPreset.TREBLE_BOOST -> listOf(-200, -100, 0, 400, 700)
            EqPreset.VOCAL       -> listOf(-100, 100, 600, 300, -100)
            EqPreset.CUSTOM      -> List(bandCount) { 0 }
        }
        val sized = raw.take(bandCount) + List((bandCount - raw.size).coerceAtLeast(0)) { 0 }
        return sized.map { it.coerceIn(min, max) }
    }

    private fun bandKey(i: Int) = "band_$i"

    private const val PREF_FILE = "equalizer"
    private const val PREF_ENABLED = "enabled"
    private const val PREF_PRESET = "preset"
}
