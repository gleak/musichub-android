package com.mediaplayer.android.playback

import android.content.Context
import android.media.audiofx.Equalizer
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

private const val DATASTORE_NAME = "equalizer"

/**
 * Equalizer settings DataStore. [SharedPreferencesMigration] auto-imports the
 * legacy `equalizer` SharedPreferences file on first access so existing user
 * presets and band levels survive the migration without any user action.
 */
private val Context.equalizerDataStore: DataStore<Preferences> by preferencesDataStore(
    name = DATASTORE_NAME,
    produceMigrations = { ctx ->
        listOf(SharedPreferencesMigration(ctx, DATASTORE_NAME))
    },
)

enum class EqPreset(val label: String) {
    FLAT("Lineare"),
    BASS_BOOST("Bassi"),
    TREBLE_BOOST("Alti"),
    VOCAL("Voce"),
    CUSTOM("Personalizzato"),
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
    /**
     * The hardware audio session id this Equalizer instance is bound to.
     * Surfaced in the EQ bottom sheet's `// SESSIONE AUDIO` info card so
     * power users debugging effects routing can confirm which session the
     * EQ is actually applying to. 0 means "unknown / not yet bound".
     */
    val audioSessionId: Int = 0,
)

object EqualizerController {

    private val _state = MutableStateFlow<EqState?>(null)
    val state: StateFlow<EqState?> = _state.asStateFlow()

    private var eq: Equalizer? = null
    private var appContext: Context? = null
    private var boundSessionId: Int = 0

    /**
     * Singleton scope for async DataStore reads/writes. SupervisorJob so a
     * write failure on one band doesn't cancel the rest. Bound to IO since
     * DataStore writes touch disk.
     */
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var restoreJob: Job? = null

    fun init(context: Context, audioSessionId: Int) {
        if (audioSessionId == 0) return
        val app = context.applicationContext
        appContext = app
        boundSessionId = audioSessionId
        try {
            val e = Equalizer(0, audioSessionId)
            eq = e
            // Async restore: hardware stays at flat defaults for the brief
            // window before DataStore's first emit lands (~50 ms cold start
            // / ~5 ms warm). The prior sync read blocked the service
            // looper on a SharedPreferences load — we trade that for a
            // very short visual blip if the user opens the EQ sheet
            // within that window.
            restoreJob?.cancel()
            restoreJob = scope.launch {
                try {
                    val prefs = app.equalizerDataStore.data.first()
                    val enabled = prefs[KEY_ENABLED] ?: true
                    try { e.enabled = enabled } catch (_: Exception) {}
                    restoreBands(e, prefs)
                    _state.value = snapshot(e, savedPreset(prefs))
                } catch (_: Exception) {
                    // Couldn't read DataStore — leave hardware at flat
                    // defaults; _state stays null until a user action.
                }
            }
        } catch (_: Exception) {
            // Device doesn't support Equalizer
        }
    }

    fun release() {
        try { eq?.release() } catch (_: Exception) {}
        eq = null
        restoreJob?.cancel()
        restoreJob = null
        _state.value = null
    }

    fun setEnabled(enabled: Boolean) {
        val e = eq ?: return
        try { e.enabled = enabled } catch (_: Exception) { return }
        editAsync { it[KEY_ENABLED] = enabled }
        _state.value = _state.value?.copy(enabled = enabled)
    }

    fun setBandLevel(band: Int, levelMilliBel: Int) {
        val e = eq ?: return
        val range = e.bandLevelRange
        val clamped = levelMilliBel.coerceIn(range[0].toInt(), range[1].toInt())
        if (!trySetBand(e, band, clamped)) return
        editAsync {
            it[bandKey(band)] = clamped
            it[KEY_PRESET] = EqPreset.CUSTOM.name
        }
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
        // Per-band try/catch: some OEM Equalizer impls throw on a single
        // misbehaving band; without the guard one bad band aborts the
        // whole preset and leaves the EQ in a half-applied state.
        val applied = ArrayList<Pair<Int, Int>>(bandCount)
        repeat(bandCount) { i ->
            if (trySetBand(e, i, levels[i])) {
                applied += i to levels[i]
            }
        }
        editAsync {
            for ((i, lvl) in applied) it[bandKey(i)] = lvl
            it[KEY_PRESET] = preset.name
        }
        _state.value = snapshot(e, preset)
    }

    private fun restoreBands(e: Equalizer, prefs: Preferences) {
        val range = e.bandLevelRange
        repeat(e.numberOfBands.toInt()) { i ->
            val saved = prefs[bandKey(i)]
            if (saved != null) {
                val clamped = saved.coerceIn(range[0].toInt(), range[1].toInt())
                trySetBand(e, i, clamped)
            }
        }
    }

    private fun trySetBand(e: Equalizer, band: Int, levelMilliBel: Int): Boolean =
        try {
            e.setBandLevel(band.toShort(), levelMilliBel.toShort())
            true
        } catch (_: Exception) {
            false
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
            audioSessionId = boundSessionId,
        )
    }

    private fun savedPreset(prefs: Preferences): EqPreset =
        prefs[KEY_PRESET]
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

    private fun bandKey(i: Int) = intPreferencesKey("band_$i")

    private fun editAsync(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        val ctx = appContext ?: return
        scope.launch {
            try {
                ctx.equalizerDataStore.edit { prefs -> block(prefs) }
            } catch (_: Exception) {
                // Best-effort persistence; failures here don't break the
                // hardware EQ which has already been updated.
            }
        }
    }

    private val KEY_ENABLED = booleanPreferencesKey("enabled")
    private val KEY_PRESET = stringPreferencesKey("preset")
}
