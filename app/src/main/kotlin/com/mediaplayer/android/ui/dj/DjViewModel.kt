package com.mediaplayer.android.ui.dj

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.DjRefusal
import com.mediaplayer.android.data.DjRepository
import com.mediaplayer.android.data.PlaylistsCache
import com.mediaplayer.android.data.dto.DjChatMessageDto
import com.mediaplayer.android.data.dto.DjPreferencesDto
import com.mediaplayer.android.data.dto.DjRunDto
import com.mediaplayer.android.data.dto.DjStatusDto
import com.mediaplayer.android.data.dto.DjTasteProfileDto
import com.mediaplayer.android.ui.common.friendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** "4 min", "2 min 5 s", "45 s". Mai un numero nudo di secondi. */
fun formatWait(seconds: Long): String {
    val total = seconds.coerceAtLeast(0L)
    if (total < 60) return "$total s"
    val minutes = total / 60
    val rest = total % 60
    return if (rest == 0L) "$minutes min" else "$minutes min $rest s"
}

/**
 * PARTIAL non e' un fallimento: le playlist sono state scritte davvero, e'
 * un passo successivo ad aver ceduto. Chiamarlo "non riuscito" direbbe il
 * falso a chi trova due playlist nuove nella libreria.
 */
fun runStatusLabel(status: String?): String = when (status?.uppercase()) {
    "RUNNING" -> "In corso"
    "OK" -> "Riuscito"
    "PARTIAL" -> "Riuscito in parte"
    "FAILED" -> "Non riuscito"
    else -> status ?: "Sconosciuto"
}

/** "2 playlist scritte, 1 scartata". */
fun describeRun(run: DjRunDto): String {
    val written = if (run.playlistsWritten == 1) "1 playlist scritta"
        else "${run.playlistsWritten} playlist scritte"
    val rejected = when {
        run.playlistsRejected == 1 -> ", 1 scartata"
        run.playlistsRejected > 1 -> ", ${run.playlistsRejected} scartate"
        else -> ""
    }
    return "$written$rejected"
}

/**
 * Tutto quello che la sezione DJ mostra in un colpo solo.
 *
 * Uno stato unico invece di un flow per riquadro: le parti si condizionano a
 * vicenda (senza agente non si conversa e non si genera; una risposta di
 * chat puo' cambiare il profilo), e tenerle separate significherebbe
 * ricomporle a mano dentro la schermata.
 */
data class DjUiState(
    val loading: Boolean = true,
    val loadError: String? = null,
    val status: DjStatusDto? = null,
    val messages: List<DjChatMessageDto> = emptyList(),
    val profile: DjTasteProfileDto? = null,
    val sending: Boolean = false,
    val sendError: String? = null,
    val preferences: DjPreferencesDto? = null,
    val savingPreferences: Boolean = false,
    val preferencesError: String? = null,
    val runs: List<DjRunDto> = emptyList(),
    val forcing: Boolean = false,
    val forceError: String? = null,
    val lastRun: DjRunDto? = null,
    /**
     * L'attesa che il server ha appena rifiutato. Tenuta a parte da
     * `status.cooldownSeconds` perche' quello si aggiorna solo al prossimo
     * `refresh()`, e chi ha appena premuto il pulsante deve sapere subito
     * quanto manca.
     */
    val refusedWaitSeconds: Long? = null,
) {
    val agentAvailable: Boolean get() = status?.agentAvailable == true
    val chatEnabled: Boolean get() = status?.chatEnabled == true
    val waitSeconds: Long get() = refusedWaitSeconds ?: status?.cooldownSeconds ?: 0L
    val canForce: Boolean
        get() = agentAvailable && !forcing && status?.runInProgress != true && waitSeconds <= 0L
}

/**
 * Costruttore senza argomenti obbligatori: in questo progetto la DI e'
 * manuale e i ViewModel nascono da `viewModel()`, che sa creare solo
 * un'istanza no-arg. Il parametro esiste perche' i test possano passare un
 * repository con una `DjApi` finta.
 */
class DjViewModel(
    private val repository: DjRepository = DjRepository(),
) : ViewModel() {

    private val _state = MutableStateFlow(DjUiState())
    val state: StateFlow<DjUiState> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, loadError = null) }
            // Lo stato prima di tutto: senza agente non ha senso mostrare ne'
            // il composer ne' il pulsante di generazione.
            val status = runCatching { repository.status() }
            if (status.isFailure) {
                _state.update {
                    it.copy(loading = false,
                        loadError = friendlyMessage(status.exceptionOrNull()))
                }
                return@launch
            }
            val messages = runCatching { repository.chat() }.getOrDefault(emptyList())
            val profile = runCatching { repository.profile() }.getOrNull()
            val preferences = runCatching { repository.preferences() }.getOrNull()
            val runs = runCatching { repository.recentRuns() }.getOrDefault(emptyList())
            _state.update {
                it.copy(
                    loading = false,
                    loadError = null,
                    status = status.getOrNull(),
                    messages = messages,
                    profile = profile,
                    preferences = preferences,
                    runs = runs,
                )
            }
        }
    }

    /**
     * Manda un messaggio e ricarica conversazione e profilo.
     *
     * Il profilo si ricarica sempre perche' un turno di chat lo riscrive per
     * intero: vedere il profilo muoversi dopo uno scambio e' meta' del punto
     * della sezione.
     */
    fun send(message: String) {
        val text = message.trim()
        if (text.isEmpty() || _state.value.sending) return
        viewModelScope.launch {
            _state.update { it.copy(sending = true, sendError = null) }
            val result = runCatching { repository.sendMessage(text) }
            if (result.isFailure) {
                val cause = result.exceptionOrNull()
                _state.update {
                    it.copy(sending = false,
                        // La frase del server quando c'e' (429 del tetto
                        // giornaliero, 503 a chat spenta), altrimenti il
                        // messaggio generico gia' usato altrove nell'app.
                        sendError = DjRefusal.of(cause!!)?.message ?: friendlyMessage(cause))
                }
                return@launch
            }
            val messages = runCatching { repository.chat() }.getOrDefault(_state.value.messages)
            val profile = runCatching { repository.profile() }.getOrNull() ?: _state.value.profile
            _state.update {
                it.copy(sending = false, sendError = null, messages = messages, profile = profile)
            }
        }
    }

    /** Cancella conversazione, profilo e storico del profilo, poi ricarica. */
    fun eraseChatAndProfile() {
        viewModelScope.launch {
            runCatching { repository.eraseChat() }
            val messages = runCatching { repository.chat() }.getOrDefault(emptyList())
            val profile = runCatching { repository.profile() }.getOrNull()
            _state.update { it.copy(messages = messages, profile = profile) }
        }
    }

    fun dismissSendError() {
        _state.update { it.copy(sendError = null) }
    }

    /**
     * Salva solo i campi passati: il server conserva gli altri
     * (`explicitNulls = false` fa sparire i null dal JSON). La risposta
     * sostituisce lo stato invece di far ripartire un `refresh()` intero,
     * cosi' il controllo non torna al valore vecchio per un istante.
     */
    fun savePreferences(
        cycleEnabled: Boolean? = null,
        slots: Int? = null,
        cadenceDays: Int? = null,
    ) {
        if (_state.value.savingPreferences) return
        viewModelScope.launch {
            _state.update { it.copy(savingPreferences = true, preferencesError = null) }
            val result = runCatching {
                repository.updatePreferences(cycleEnabled, slots, cadenceDays)
            }
            _state.update {
                it.copy(
                    savingPreferences = false,
                    preferences = result.getOrNull() ?: it.preferences,
                    preferencesError = result.exceptionOrNull()?.let { e ->
                        DjRefusal.of(e)?.message ?: friendlyMessage(e)
                    },
                )
            }
        }
    }

    /**
     * Fa girare il DJ adesso, solo per questo utente, e aspetta l'esito.
     *
     * I tre vincoli della spec li impone il server e questo metodo li
     * riporta: gira solo per chi preme (nessun `userId` nell'endpoint),
     * rifiuta con 409 se un giro e' gia' in corso, rifiuta con 429 dentro il
     * cooldown portandosi dietro l'attesa residua — che finisce in
     * [DjUiState.refusedWaitSeconds] perche' la schermata la mostri invece
     * di limitarsi a fallire.
     */
    fun forceRun() {
        if (!_state.value.canForce) return
        viewModelScope.launch {
            _state.update {
                it.copy(forcing = true, forceError = null, lastRun = null, refusedWaitSeconds = null)
            }
            val result = runCatching {
                repository.forceRun(onProgress = { run ->
                    _state.update { it.copy(lastRun = run) }
                })
            }
            val failure = result.exceptionOrNull()
            if (failure == null) {
                _state.update { it.copy(forcing = false, lastRun = result.getOrNull()) }
                // Le playlist scritte, la cronologia e il cooldown sono tutti
                // cambiati: ricaricare e' piu' onesto che indovinare.
                runCatching { PlaylistsCache.refresh() }
                refreshStatusAndRuns()
                return@launch
            }
            val refusal = DjRefusal.of(failure)
            _state.update {
                it.copy(
                    forcing = false,
                    refusedWaitSeconds = refusal?.retryAfterSeconds,
                    forceError = refusal?.message ?: friendlyMessage(failure),
                )
            }
        }
    }

    private suspend fun refreshStatusAndRuns() {
        val status = runCatching { repository.status() }.getOrNull()
        val runs = runCatching { repository.recentRuns() }.getOrDefault(_state.value.runs)
        _state.update { it.copy(status = status ?: it.status, runs = runs) }
    }
}
