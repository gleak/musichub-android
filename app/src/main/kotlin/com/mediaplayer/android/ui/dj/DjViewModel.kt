package com.mediaplayer.android.ui.dj

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.DjChatTurnPolling
import com.mediaplayer.android.data.DjRefusal
import com.mediaplayer.android.data.DjRepository
import com.mediaplayer.android.data.PlaylistsCache
import com.mediaplayer.android.data.dto.DjChatMessageDto
import com.mediaplayer.android.data.dto.DjPreferencesDto
import com.mediaplayer.android.data.dto.DjRunDto
import com.mediaplayer.android.data.dto.DjStatusDto
import com.mediaplayer.android.data.dto.DjTasteProfileDto
import com.mediaplayer.android.ui.common.friendlyMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
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
     * L'attesa residua per il pulsante di generazione, gia' scesa a zero
     * quando e' il momento — [DjViewModel] la ricalcola ogni secondo da un
     * orologio iniettabile, non e' un numero congelato al momento del
     * rifiuto o dell'ultimo `refresh()`. Copre sia un 429 appena ricevuto
     * sia il `cooldownSeconds` che lo stato porta gia' al primo caricamento.
     */
    val refusedWaitSeconds: Long? = null,
    /**
     * Lo stesso conto alla rovescia, ma per l'invio in chat: il tetto
     * giornaliero risponde 429 con un `Retry-After`, e senza questo campo
     * quel numero veniva letto e buttato via — il pulsante "Invia" restava
     * disabilitato a vita o abilitato subito, mai coerente con l'attesa
     * vera.
     */
    val chatWaitSeconds: Long? = null,
    /**
     * Il messaggio la cui playlist si sta componendo, o null se nessuna.
     *
     * Un id e non un booleano: la conversazione puo' contenere piu' turni con
     * una proposta, e lo spinner deve stare sotto QUELLO su cui si e' premuto.
     * Con un booleano girerebbero tutti.
     */
    val composingMessageId: Long? = null,
    val composeError: String? = null,
    /**
     * La playlist appena composta. L'interfaccia la usa per offrire di
     * aprirla: dire "fatto" e lasciare la persona a cercarla in libreria
     * sarebbe il modo piu' rapido di rendere inutile la funzione.
     */
    val composedPlaylistId: Long? = null,
    val composedPlaylistName: String? = null,
    /**
     * A che punto e' il DJ mentre risponde. L'etichetta grezza del server
     * (`CATALOG`), non la frase: la traduzione sta in [phaseText], perche' e'
     * nel client che abita la lingua dell'interfaccia.
     */
    val turnPhase: String? = null,
    /**
     * Da quanto sta lavorando, in secondi. E' l'unica differenza visibile fra
     * "sta pensando da otto secondi" e "e' morto da quaranta": prima lo
     * schermo mostrava una stringa fissa, e un'attesa muta lunga due minuti e'
     * indistinguibile da un guasto.
     */
    val turnElapsedSeconds: Long = 0L,
) {
    val agentAvailable: Boolean get() = status?.agentAvailable == true
    val chatEnabled: Boolean get() = status?.chatEnabled == true
    val waitSeconds: Long get() = refusedWaitSeconds ?: 0L
    val canSend: Boolean get() = chatEnabled && !sending && (chatWaitSeconds ?: 0L) <= 0L
    val canForce: Boolean
        get() = agentAvailable && !forcing && status?.runInProgress != true && waitSeconds <= 0L

    /**
     * Comporre dalla chat passa dallo stesso giro del pulsante "genera ora":
     * stesso executor a un thread sul server, stessa guardia "un giro alla
     * volta". Quindi le condizioni sono le stesse — se non si puo' generare,
     * non si puo' nemmeno comporre, e mostrare il pulsante attivo
     * significherebbe promettere un 409.
     */
    val canCompose: Boolean get() = canForce && composingMessageId == null
}

/**
 * Costruttore senza argomenti obbligatori: in questo progetto la DI e'
 * manuale e i ViewModel nascono da `viewModel()`, che sa creare solo
 * un'istanza no-arg. Il parametro `repository` esiste perche' i test
 * possano passare un repository con una `DjApi` finta; [clock] esiste
 * perche' possano far scorrere il tempo del conto alla rovescia senza
 * un `delay()` vero — un test che aspettasse sul serio quattro minuti
 * di cooldown non e' un test che qualcuno lascerebbe nella suite.
 */
class DjViewModel(
    private val repository: DjRepository = DjRepository(),
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _state = MutableStateFlow(DjUiState())
    val state: StateFlow<DjUiState> = _state.asStateFlow()

    /**
     * Istante (epoch ms, secondo [clock]) in cui il pulsante di generazione
     * torna disponibile, o null se non c'e' nessuna attesa in corso. Non e'
     * nello stato pubblico: cio' che la schermata legge e' sempre il
     * risultato gia' ricalcolato, [DjUiState.refusedWaitSeconds].
     */
    private var forceWaitUntilMs: Long? = null

    /** Lo stesso, per il tetto giornaliero di messaggi in chat. */
    private var chatWaitUntilMs: Long? = null

    init {
        refresh()
        // Un solo ticker per tutta la vita del ViewModel invece di uno per
        // rifiuto: costa un `delay` al secondo solo quando c'e' davvero
        // un'attesa da scontare (il controllo e' dentro il loop, non fuori),
        // e si ferma da solo quando `viewModelScope` viene cancellato.
        viewModelScope.launch {
            while (isActive) {
                delay(1_000L)
                if (forceWaitUntilMs != null || chatWaitUntilMs != null) recomputeWaits()
            }
        }
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
            applyStatusCooldown(status.getOrNull())
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
            recomputeWaits()
            // Dopo il caricamento, non prima: serve sapere che la chat e'
            // accesa e che c'e' un agente.
            resumeRunningTurn()
        }
    }

    /**
     * Traduce `status.cooldownSeconds` in una scadenza assoluta, cosi' il
     * ticker puo' scontarla come farebbe con un rifiuto appena ricevuto.
     * Letto una volta sola al caricamento, altrimenti resterebbe congelato
     * al valore del primo `refresh()` per tutta la vita dello schermo — il
     * bug che questo giro di correzioni chiude.
     */
    private fun applyStatusCooldown(status: DjStatusDto?) {
        val seconds = status?.cooldownSeconds ?: return
        if (seconds > 0L) forceWaitUntilMs = clock() + seconds * 1_000L
    }

    /**
     * Ricalcola i due conti alla rovescia dalla scadenza assoluta invece di
     * decrementare un contatore: un `delay(1000)` non e' mai esattamente un
     * secondo, e sommare l'errore ogni giro avrebbe fatto scivolare il
     * numero mostrato rispetto al momento vero in cui il server torna
     * disponibile.
     */
    private fun recomputeWaits() {
        val now = clock()
        val force = forceWaitUntilMs?.let { until -> ((until - now + 999L) / 1_000L).coerceAtLeast(0L) }
        val chat = chatWaitUntilMs?.let { until -> ((until - now + 999L) / 1_000L).coerceAtLeast(0L) }
        if (force != null && force <= 0L) forceWaitUntilMs = null
        if (chat != null && chat <= 0L) chatWaitUntilMs = null
        _state.update {
            it.copy(
                refusedWaitSeconds = force?.takeIf { s -> s > 0L },
                chatWaitSeconds = chat?.takeIf { s -> s > 0L },
            )
        }
    }

    /**
     * Un giro del ticker, chiamabile direttamente. Il ticker vero gira su
     * `delay(1000)` reale: nei test si fa avanzare [clock] e si chiama
     * questo, che e' la stessa funzione che il ticker userebbe, senza
     * aspettare sul serio.
     */
    internal fun tick() = recomputeWaits()

    /**
     * Manda un messaggio e ricarica conversazione e profilo.
     *
     * Il profilo si ricarica sempre perche' un turno di chat lo riscrive per
     * intero: vedere il profilo muoversi dopo uno scambio e' meta' del punto
     * della sezione.
     */
    fun send(message: String) {
        val text = message.trim()
        if (text.isEmpty() || !_state.value.canSend) return
        viewModelScope.launch {
            _state.update {
                it.copy(sending = true, sendError = null, turnPhase = null, turnElapsedSeconds = 0L)
            }
            val result = runCatching { repository.sendMessage(text) }
            if (result.isFailure) {
                val cause = result.exceptionOrNull()
                val refusal = DjRefusal.of(cause!!)
                // Il 429 del tetto giornaliero porta un `Retry-After`: senza
                // tradurlo in una scadenza il pulsante "Invia" restava
                // com'era prima del tentativo, pronto a incassare lo stesso
                // rifiuto un'altra volta.
                chatWaitUntilMs = refusal?.retryAfterSeconds?.takeIf { it > 0L }
                    ?.let { clock() + it * 1_000L }
                _state.update {
                    it.copy(sending = false,
                        // La frase del server quando c'e' (429 del tetto
                        // giornaliero, 503 a chat spenta), altrimenti il
                        // messaggio generico gia' usato altrove nell'app.
                        sendError = refusal?.message ?: friendlyMessage(cause))
                }
                recomputeWaits()
                return@launch
            }
            // Il messaggio dell'utente e' gia' salvato lato server: si
            // ricarica subito la conversazione cosi' compare mentre il DJ
            // pensa, invece che solo alla fine insieme alla risposta.
            runCatching { repository.chat() }.onSuccess { loaded ->
                _state.update { it.copy(messages = loaded) }
            }
            followTurn(result.getOrNull()!!.turnId)
        }
    }

    /**
     * Segue un turno fino alla fine e porta la conversazione a giorno.
     *
     * Separato da [send] perche' serve anche a [resumeRunningTurn]: chi
     * riapre lo schermo mentre il DJ sta ancora rispondendo deve rivedere
     * l'attesa dov'era, non uno schermo fermo su cui la risposta compare dal
     * nulla al ricaricamento successivo.
     */
    private suspend fun followTurn(turnId: Long) {
        val outcome = runCatching {
            DjChatTurnPolling.awaitTerminal(
                turnId = turnId,
                fetch = { repository.chatTurn(it) },
                onUpdate = { turn ->
                    _state.update {
                        it.copy(turnPhase = turn.phase, turnElapsedSeconds = turn.elapsedSeconds)
                    }
                },
            )
        }
        val messages = runCatching { repository.chat() }.getOrDefault(_state.value.messages)
        val profile = runCatching { repository.profile() }.getOrNull() ?: _state.value.profile
        // Il turno fallito porta il motivo scritto dal server; un polling che
        // si arrende no, e li' la frase generica e' l'unica onesta.
        val failure = outcome.getOrNull()
            ?.takeIf { it.status.equals("FAILED", ignoreCase = true) }?.error
            ?: outcome.exceptionOrNull()?.let { friendlyMessage(it) }
        _state.update {
            it.copy(
                sending = false,
                sendError = failure,
                messages = messages,
                profile = profile,
                turnPhase = null,
                turnElapsedSeconds = 0L,
            )
        }
    }

    /**
     * Riaggancia un turno lasciato in volo, se c'e'.
     *
     * Chiamata al caricamento dello schermo: senza, uscire dalla chat e
     * rientrare mentre il DJ risponde farebbe sparire ogni segno che stia
     * succedendo qualcosa.
     */
    private fun resumeRunningTurn() {
        viewModelScope.launch {
            val turn = runCatching { repository.latestChatTurn() }.getOrNull() ?: return@launch
            if (DjChatTurnPolling.isTerminal(turn.status)) return@launch
            _state.update {
                it.copy(sending = true, turnPhase = turn.phase, turnElapsedSeconds = turn.elapsedSeconds)
            }
            followTurn(turn.id)
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
        playlistMinSize: Int? = null,
        playlistMaxSize: Int? = null,
    ) {
        if (_state.value.savingPreferences) return
        viewModelScope.launch {
            _state.update { it.copy(savingPreferences = true, preferencesError = null) }
            val result = runCatching {
                repository.updatePreferences(cycleEnabled, slots, cadenceDays, playlistMinSize, playlistMaxSize)
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
            forceWaitUntilMs = null
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
            // 429: il rifiuto porta l'attesa residua, che diventa una
            // scadenza assoluta cosi' il ticker la puo' scontare.
            forceWaitUntilMs = refusal?.retryAfterSeconds?.takeIf { it > 0L }
                ?.let { clock() + it * 1_000L }
            _state.update {
                it.copy(
                    forcing = false,
                    forceError = refusal?.message ?: friendlyMessage(failure),
                )
            }
            recomputeWaits()
            if (refusal?.status == 409) {
                // 409: un giro e' gia' in corso, ma lo stato locale e'
                // ancora quello di prima del tentativo (altrimenti
                // `canForce` non avrebbe lasciato passare il tap). Senza
                // aggiornarlo il pulsante resterebbe pronto a incassare lo
                // stesso 409 un'altra volta.
                refreshStatusAndRuns()
            }
        }
    }

    /**
     * Fa comporre la playlist concordata in quel turno di chat.
     *
     * <p>Non manda nessun testo: il briefing sta sulla riga del messaggio,
     * lato server. Da qui parte solo l'id di CHE turno.
     */
    fun composePlaylistFromChat(messageId: Long) {
        if (!_state.value.canCompose) return
        viewModelScope.launch {
            forceWaitUntilMs = null
            _state.update {
                it.copy(
                    composingMessageId = messageId,
                    composeError = null,
                    composedPlaylistId = null,
                    composedPlaylistName = null,
                    refusedWaitSeconds = null,
                )
            }
            val result = runCatching { repository.composePlaylistFromChat(messageId) }
            val failure = result.exceptionOrNull()
            if (failure == null) {
                val run = result.getOrNull()
                // Un giro terminale non e' per forza un giro riuscito: la
                // scaletta puo' essere stata scartata dalla validazione. In
                // quel caso created_playlist_id resta nullo, e dire "fatto"
                // manderebbe la persona a cercare qualcosa che non esiste.
                val playlistId = run?.createdPlaylistId
                _state.update {
                    it.copy(
                        composingMessageId = null,
                        composedPlaylistId = playlistId,
                        composedPlaylistName = it.messages
                            .firstOrNull { m -> m.id == messageId }?.playlistName,
                        composeError = if (playlistId != null) null
                        else run?.error ?: "Il DJ non e' riuscito a comporla. Riprova.",
                    )
                }
                runCatching { PlaylistsCache.refresh() }
                refreshStatusAndRuns()
                return@launch
            }
            val refusal = DjRefusal.of(failure)
            forceWaitUntilMs = refusal?.retryAfterSeconds?.takeIf { it > 0L }
                ?.let { clock() + it * 1_000L }
            _state.update {
                it.copy(
                    composingMessageId = null,
                    composeError = refusal?.message ?: friendlyMessage(failure),
                )
            }
            recomputeWaits()
            if (refusal?.status == 409) {
                refreshStatusAndRuns()
            }
        }
    }

    fun dismissComposeResult() {
        _state.update {
            it.copy(composeError = null, composedPlaylistId = null, composedPlaylistName = null)
        }
    }

    private suspend fun refreshStatusAndRuns() {
        val status = runCatching { repository.status() }.getOrNull()
        val runs = runCatching { repository.recentRuns() }.getOrDefault(_state.value.runs)
        applyStatusCooldown(status)
        _state.update { it.copy(status = status ?: it.status, runs = runs) }
        recomputeWaits()
    }
}
