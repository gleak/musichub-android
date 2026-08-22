package com.mediaplayer.android.ui.dj

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mediaplayer.android.data.DjRefusal
import com.mediaplayer.android.data.DjRepository
import com.mediaplayer.android.data.dto.DjChatMessageDto
import com.mediaplayer.android.data.dto.DjStatusDto
import com.mediaplayer.android.data.dto.DjTasteProfileDto
import com.mediaplayer.android.ui.common.friendlyMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
) {
    val agentAvailable: Boolean get() = status?.agentAvailable == true
    val chatEnabled: Boolean get() = status?.chatEnabled == true
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
            _state.update {
                it.copy(
                    loading = false,
                    loadError = null,
                    status = status.getOrNull(),
                    messages = messages,
                    profile = profile,
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
}
