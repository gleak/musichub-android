package com.mediaplayer.android.ui.dj

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.data.dto.DjChatMessageDto
import com.mediaplayer.android.data.dto.DjPreferencesDto
import com.mediaplayer.android.data.dto.DjRunDto
import com.mediaplayer.android.data.dto.DjTasteProfileDto
import com.mediaplayer.android.ui.common.CenteredSpinner
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.EyebrowText
import com.mediaplayer.android.ui.common.formatRefreshedAt
import com.mediaplayer.android.ui.profile.settings.SettingsCard
import com.mediaplayer.android.ui.profile.settings.SettingsToggleRow
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MHGradient
import com.mediaplayer.android.ui.theme.MediaPlayerSpacing

/**
 * La sezione del DJ.
 *
 * Non e' una superficie d'ascolto: e' il posto dove l'utente modella la
 * propria esperienza del DJ. Le playlist che il DJ produce vivono nella
 * schermata delle playlist insieme alle altre, riconoscibili dal loro badge
 * — qui ci sono la conversazione, il profilo che ne viene ricavato e (Task
 * 10) preferenze, cronologia e generazione forzata.
 *
 * Su Android Auto questa schermata non esiste e non deve esistere: in
 * macchina una conversazione a testo e' inutile e pericolosa, e la' il DJ
 * vive solo attraverso le playlist che produce.
 */
@Composable
fun DjScreen(
    onOpenPlaylist: (Long) -> Unit = {},
    viewModel: DjViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    // Due conferme distinte: la seconda azione distrugge molto piu' della
    // prima, e non deve stare dietro lo stesso "sei sicuro?".
    var confirmEraseChat by remember { mutableStateOf(false) }
    var confirmForgetProfile by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MHGradient.heroBg(Color(0xFF241033))),
    ) {
        when {
            state.loading -> CenteredSpinner()
            state.loadError != null -> ErrorWithRetry(state.loadError!!, viewModel::refresh)
            else -> LazyColumn(modifier = Modifier.fillMaxSize().testTag("dj-screen-list")) {
                item(key = "header") {
                    Column(modifier = Modifier.padding(
                        start = MediaPlayerSpacing.M, end = MediaPlayerSpacing.M,
                        top = 16.dp, bottom = 4.dp,
                    )) {
                        EyebrowText(text = "Il tuo rapporto col DJ")
                        Text(
                            text = "DJ",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MHColors.TextHi,
                        )
                        Text(
                            text = "Le proposte del DJ si rinnovano da sole. Se una ti piace, " +
                                "promuovila dalla schermata della playlist e resta tua.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MHColors.TextLo,
                            modifier = Modifier.padding(top = 6.dp),
                        )
                    }
                }

                if (!state.agentAvailable) {
                    item(key = "no-agent") {
                        DjNotice("Il DJ non e’ configurato su questo server: non puo’ ne’ " +
                            "conversare ne’ proporre playlist.")
                    }
                }

                item(key = "chat-header") { DjSectionTitle("Conversazione", "Il cuore della sezione") }

                if (state.messages.isEmpty()) {
                    item(key = "chat-empty") {
                        DjBody("Non avete ancora parlato. Raccontagli cosa ti va di sentire e " +
                            "quando: «la musica per correre la voglio diversa da quella della " +
                            "sera» dice al DJ piu’ di mille ascolti.")
                    }
                }
                items(state.messages, key = { it.createdAt + it.role }) { message ->
                    ChatBubble(
                        message = message,
                        composing = state.composingMessageId == message.id,
                        canCompose = state.canCompose,
                        onCompose = { viewModel.composePlaylistFromChat(it) },
                    )
                }
                // Esito della composizione, sotto la conversazione e non dentro
                // una bolla: riguarda cio' che e' successo dopo lo scambio, non
                // uno dei due che parlano.
                state.composedPlaylistId?.let { playlistId ->
                    item(key = "composed-ok") {
                        Column(modifier = Modifier.padding(horizontal = MediaPlayerSpacing.M)) {
                            DjBody(
                                (state.composedPlaylistName?.let { "«" + it + "» e' pronta." }
                                    ?: "La playlist e' pronta.") +
                                    " La trovi in libreria: e' tua, il ciclo non la tocca."
                            )
                            Button(
                                onClick = {
                                    viewModel.dismissComposeResult()
                                    onOpenPlaylist(playlistId)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MHColors.Lime,
                                    contentColor = Color(0xFF0A0A0A),
                                ),
                            ) {
                                Text("Aprila", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                state.composeError?.let { error ->
                    item(key = "composed-error") { DjNotice(error) }
                }

                if (!state.chatEnabled) {
                    item(key = "chat-off") { DjNotice("La chat col DJ e’ spenta su questo server.") }
                }
                state.sendError?.let { error ->
                    item(key = "send-error") { DjNotice(error) }
                }
                // Il tetto giornaliero risponde 429 con l'attesa residua: se
                // non la si mostra, il pulsante disabilitato sembra rotto
                // invece che in attesa di qualcosa di preciso. Conta alla
                // rovescia da sola, come quella di "Genera adesso", perche'
                // la ricalcola lo stesso ticker del ViewModel.
                if ((state.chatWaitSeconds ?: 0L) > 0L) {
                    item(key = "chat-wait") {
                        DjBody("Puoi scrivere di nuovo fra " +
                            formatWait(state.chatWaitSeconds ?: 0L) + ".")
                    }
                }

                item(key = "composer") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MediaPlayerSpacing.M, vertical = 8.dp),
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = { if (it.length <= 2000) draft = it },
                            placeholder = { Text("Scrivi al DJ…") },
                            // Solo l'invio si ferma durante il tetto
                            // giornaliero: poter scrivere nel frattempo (senza
                            // poter premere Invia) e' piu' clemente che
                            // bloccare anche la tastiera per l'intera attesa.
                            enabled = state.chatEnabled && !state.sending,
                            maxLines = 4,
                            shape = RoundedCornerShape(16.dp),
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = MHColors.TextHi,
                                unfocusedTextColor = MHColors.TextHi,
                                focusedContainerColor = MHColors.Card,
                                unfocusedContainerColor = MHColors.Card,
                                disabledContainerColor = MHColors.Card,
                            ),
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.size(8.dp))
                        IconButton(
                            onClick = {
                                viewModel.send(draft)
                                draft = ""
                            },
                            enabled = state.canSend && draft.isNotBlank(),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MHColors.Lime),
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Invia",
                                tint = Color(0xFF0A0A0A),
                            )
                        }
                    }
                }

                if (state.sending) {
                    item(key = "sending") {
                        // Due righe e non una stringa fissa: l'attesa puo'
                        // durare minuti, e senza il tempo che scorre e'
                        // indistinguibile da un blocco. La fase dice cosa sta
                        // facendo davvero — arriva dalle chiamate che il DJ fa
                        // al catalogo mentre lavora.
                        DjBody("Il DJ sta pensando…   ${formatElapsed(state.turnElapsedSeconds)}")
                        DjBody(phaseText(state.turnPhase))
                    }
                }

                if (state.agentAvailable) {
                    item(key = "force-header") {
                        // Titolo diverso dal testo del pulsante qui sotto: se
                        // fossero identici "Genera adesso" comparirebbe due
                        // volte nell'albero semantico e un test (o TalkBack)
                        // non saprebbe piu' quale dei due intende.
                        DjSectionTitle("Un giro adesso", "Senza aspettare il ciclo")
                    }
                    item(key = "force") {
                        Column(modifier = Modifier.padding(horizontal = MediaPlayerSpacing.M)) {
                            Text(
                                text = "Fa girare il DJ solo per te, subito. Costa: c’e’ un " +
                                    "tetto di frequenza, e un giro alla volta.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MHColors.TextLo,
                                modifier = Modifier.padding(bottom = 10.dp),
                            )
                            Button(
                                onClick = viewModel::forceRun,
                                enabled = state.canForce,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MHColors.Lime,
                                    contentColor = Color(0xFF0A0A0A),
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Genera adesso", fontWeight = FontWeight.Bold)
                            }
                            when {
                                state.forcing -> DjBody("Il DJ sta scegliendo. Puo’ volerci " +
                                    "qualche minuto: puoi lasciare questa schermata, il giro " +
                                    "va avanti lo stesso.")
                                state.waitSeconds > 0L ->
                                    DjBody("Puoi forzare un altro giro fra " +
                                        formatWait(state.waitSeconds) + ".")
                                state.status?.runInProgress == true ->
                                    DjBody("Un giro e’ gia’ in corso.")
                            }
                        }
                    }
                    state.forceError?.let { error ->
                        item(key = "force-error") { DjNotice(error) }
                    }
                    state.lastRun?.takeIf { !state.forcing }?.let { finished ->
                        item(key = "force-result") {
                            DjNotice(runStatusLabel(finished.status) + ": " + describeRun(finished) + ".")
                        }
                    }
                }

                item(key = "prefs-header") { DjSectionTitle("Preferenze", "Come vuoi il DJ") }
                // `status.cycleEnabled` e' `dj.enabled`, l'interruttore globale
                // del cron — non quello per-utente qui sotto. Spento com'e' in
                // produzione, un utente col proprio interruttore acceso
                // vedrebbe un toggle acceso che pero' non produce mai niente
                // da solo, e senza questo avviso non lo saprebbe: lo stesso
                // silenzio che l'avviso su `agentAvailable` esiste per evitare.
                if (state.status?.cycleEnabled == false && state.preferences?.cycleEnabled == true) {
                    item(key = "cycle-off-globally") {
                        DjNotice("Il ciclo automatico e’ spento su questo server: nessuna " +
                            "proposta arrivera’ da sola finche’ non lo riaccende chi gestisce " +
                            "il server. Il tuo interruttore qui sotto resta acceso e conta per " +
                            "quando il ciclo tornera’ attivo; puoi comunque generare a mano.")
                    }
                }
                state.preferences?.let { prefs ->
                    item(key = "prefs") {
                        Column(modifier = Modifier.padding(horizontal = MediaPlayerSpacing.M)) {
                            if (!prefs.explicit) {
                                Text(
                                    text = "Non le hai ancora impostate: questi sono i valori " +
                                        "predefiniti.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MHColors.TextLo,
                                    modifier = Modifier.padding(bottom = 8.dp),
                                )
                            }
                            SettingsCard {
                                SettingsToggleRow(
                                    label = "Il DJ propone da solo",
                                    detail = "Quando e’ spento il DJ non fa piu’ giri " +
                                        "automatici. Puoi comunque generare a mano.",
                                    checked = prefs.cycleEnabled,
                                    onCheckedChange = { viewModel.savePreferences(cycleEnabled = it) },
                                    showDivider = true,
                                )
                                DjStepperRow(
                                    label = "Proposte",
                                    detail = "Quante playlist il DJ tiene in vita per te. Se ne " +
                                        "togli, quelle in piu’ spariscono.",
                                    value = prefs.slots,
                                    min = prefs.minSlots,
                                    max = prefs.maxSlots,
                                    enabled = !state.savingPreferences,
                                    onChange = { viewModel.savePreferences(slots = it) },
                                    showDivider = true,
                                )
                                DjStepperRow(
                                    label = "Frequenza",
                                    detail = "Ogni quanti giorni, al minimo, il DJ rinnova le " +
                                        "tue proposte.",
                                    value = prefs.cadenceDays,
                                    min = prefs.minCadenceDays,
                                    max = prefs.maxCadenceDays,
                                    unit = "giorni",
                                    enabled = !state.savingPreferences,
                                    onChange = { viewModel.savePreferences(cadenceDays = it) },
                                    showDivider = true,
                                )
                                // I bound non sono minPlaylistSize/maxPlaylistSize (i limiti
                                // assoluti, 5 e 50) ma il valore corrente dell'altro stepper:
                                // cosi' il pulsante "+" del minimo si disabilita quando
                                // raggiunge il massimo scelto, e il "-" del massimo quando
                                // raggiunge il minimo scelto. Impostare min > max dall'interfaccia
                                // e' semplicemente un pulsante che non risponde piu'.
                                DjStepperRow(
                                    label = "Dimensione minima",
                                    detail = "Il DJ non propone playlist piu’ corte di cosi’.",
                                    value = prefs.playlistMinSize,
                                    min = prefs.minPlaylistSize,
                                    max = prefs.playlistMaxSize,
                                    unit = "brani",
                                    enabled = !state.savingPreferences,
                                    onChange = { viewModel.savePreferences(playlistMinSize = it) },
                                    showDivider = true,
                                )
                                DjStepperRow(
                                    label = "Dimensione massima",
                                    detail = "Ne’ piu’ lunghe di cosi’.",
                                    value = prefs.playlistMaxSize,
                                    min = prefs.playlistMinSize,
                                    max = prefs.maxPlaylistSize,
                                    unit = "brani",
                                    enabled = !state.savingPreferences,
                                    onChange = { viewModel.savePreferences(playlistMaxSize = it) },
                                )
                            }
                        }
                    }
                }
                state.preferencesError?.let { error ->
                    item(key = "prefs-error") { DjNotice(error) }
                }

                item(key = "runs-header") {
                    DjSectionTitle("Cosa ha fatto di recente", "Cronologia")
                }
                if (state.runs.isEmpty()) {
                    item(key = "runs-empty") {
                        DjBody("Il DJ non ha ancora fatto nessun giro per te.")
                    }
                }
                // Chiave per indice oltre che per id: i giri di un utente
                // sono quasi sempre distinti, ma un id duplicato (o due
                // giri capitati sulla stessa riga per un bug altrove) non
                // deve far esplodere la LazyColumn con una LazyKey doppia.
                itemsIndexed(state.runs, key = { index, run -> "run-$index-${run.id}" }) { _, run ->
                    RunRow(run)
                }
                if (state.runs.isNotEmpty()) {
                    item(key = "runs-note") {
                        DjBody("Le playlist prodotte stanno nella tua libreria, insieme alle altre.")
                    }
                }

                item(key = "profile-header") {
                    DjSectionTitle("Cosa il DJ ha capito", "Profilo del gusto")
                }
                item(key = "profile-body") {
                    ProfileBody(state.profile)
                }
                item(key = "erase-chat") {
                    DangerBlock(
                        label = "Cancella conversazione",
                        explanation = "Spariscono i messaggi. Il DJ continua a conoscere i " +
                            "tuoi gusti, e le playlist gia’ proposte restano dove sono.",
                        confirmLabel = "Sì, cancella la conversazione",
                        confirming = confirmEraseChat,
                        onAsk = { confirmEraseChat = true },
                        onCancel = { confirmEraseChat = false },
                        onConfirm = {
                            confirmEraseChat = false
                            viewModel.eraseChat()
                        },
                    )
                }
                if (state.profile?.hasProfile == true) {
                    item(key = "forget-profile") {
                        DangerBlock(
                            label = "Dimentica cosa sai di me",
                            explanation = "Sparisce il profilo dei tuoi gusti e tutto il suo " +
                                "storico. La conversazione resta.",
                            confirmLabel = "Sì, dimentica tutto",
                            confirming = confirmForgetProfile,
                            onAsk = { confirmForgetProfile = true },
                            onCancel = { confirmForgetProfile = false },
                            onConfirm = {
                                confirmForgetProfile = false
                                viewModel.forgetProfile()
                            },
                        )
                    }
                }

                item(key = "tail") { Spacer(Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
internal fun DjSectionTitle(title: String, eyebrow: String) {
    Column(modifier = Modifier.padding(
        start = MediaPlayerSpacing.M, end = MediaPlayerSpacing.M, top = 20.dp, bottom = 6.dp,
    )) {
        EyebrowText(text = eyebrow)
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MHColors.TextHi,
        )
    }
}

@Composable
internal fun DjBody(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MHColors.TextLo,
        modifier = Modifier.padding(horizontal = MediaPlayerSpacing.M, vertical = 4.dp),
    )
}

@Composable
internal fun DjNotice(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediaPlayerSpacing.M, vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MHColors.Card)
            .padding(12.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MHColors.TextLo,
        )
    }
}

@Composable
private fun ChatBubble(
    message: DjChatMessageDto,
    composing: Boolean = false,
    canCompose: Boolean = false,
    onCompose: (Long) -> Unit = {},
) {
    val mine = message.role == "USER"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediaPlayerSpacing.M, vertical = 3.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(if (mine) MHColors.Lime.copy(alpha = 0.16f) else MHColors.Card)
                .border(
                    width = 1.dp,
                    color = if (mine) MHColors.Lime.copy(alpha = 0.30f) else MHColors.Divider,
                    shape = RoundedCornerShape(14.dp),
                )
                .padding(horizontal = 12.dp, vertical = 9.dp),
        ) {
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = MHColors.TextHi,
            )
            if (message.refused) {
                Text(
                    text = "Fuori tema",
                    style = LocalMHMono.current.badge.copy(color = MHColors.TextLo2),
                    modifier = Modifier.padding(top = 5.dp),
                )
            }
            // Il ponte fra il parlare e il fare. In chat il DJ non ha il
            // catalogo in mano, quindi puo' concordare CHE playlist fare ma
            // non farla: questo pulsante apre il giro che la compone davvero,
            // partendo dal briefing che il DJ ha scritto su questo messaggio.
            //
            // Dentro la bolla e non in fondo alla schermata: la conversazione
            // va avanti, e un pulsante staccato dal turno che l'ha prodotto
            // comporrebbe cio' di cui si e' parlato tre messaggi fa senza che
            // si veda quale.
            if (message.hasPlaylistIntent) {
                val id = message.id ?: return@Column
                Spacer(Modifier.height(8.dp))
                Text(
                    text = message.playlistName.orEmpty(),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MHColors.TextHi,
                )
                message.playlistBrief?.takeIf { it.isNotBlank() }?.let { brief ->
                    Text(
                        text = brief,
                        style = MaterialTheme.typography.bodySmall,
                        color = MHColors.TextLo,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onCompose(id) },
                    // Composto e generato passano dallo stesso giro sul
                    // server: se non si puo' generare non si puo' comporre, e
                    // lasciare il pulsante attivo prometterebbe un 409.
                    enabled = canCompose,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MHColors.Lime,
                        contentColor = Color(0xFF0A0A0A),
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = if (composing) "Sto componendo…" else "Crea questa playlist",
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (composing) {
                    Text(
                        text = "Puo' volerci qualche minuto. Puoi lasciare questa schermata.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MHColors.TextLo,
                        modifier = Modifier.padding(top = 6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileBody(profile: DjTasteProfileDto?) {
    if (profile == null || !profile.hasProfile) {
        DjBody("Il DJ non sa ancora niente di te. Il profilo si costruisce parlandogli: " +
            "ogni scambio lo riscrive per intero, e quello che sostituisce resta comunque " +
            "conservato.")
        return
    }
    val sections = listOf(
        "Stati d’animo" to profile.profile.moods,
        "Momenti e contesti" to profile.profile.contexts,
        "Cosa cerchi" to profile.profile.loves,
        "Cosa eviti" to profile.profile.avoids,
        "Artisti" to profile.profile.artists,
        "Vincoli" to profile.profile.constraints,
        "Cosa il DJ vorrebbe chiederti" to profile.profile.openQuestions,
    ).filter { it.second.isNotEmpty() }

    Column(modifier = Modifier.padding(horizontal = MediaPlayerSpacing.M)) {
        Text(
            text = "versione ${profile.version} · da ${profile.sourceMessageCount} messaggi",
            style = LocalMHMono.current.badge.copy(color = MHColors.TextLo2),
            modifier = Modifier.padding(bottom = 8.dp),
        )
        sections.forEach { (label, lines) ->
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MHColors.Lime,
                modifier = Modifier.padding(top = 8.dp, bottom = 3.dp),
            )
            lines.forEach { line ->
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MHColors.TextHi,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
        }
        if (profile.profile.notes.isNotBlank()) {
            Text(
                text = profile.profile.notes,
                style = MaterialTheme.typography.bodySmall,
                color = MHColors.TextLo,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
    }
}

/**
 * Un gesto distruttivo con la sua conferma e la sua spiegazione.
 *
 * Parametrico e non due composable gemelli: le due azioni differiscono solo
 * per le parole, e due copie divergerebbero alla prima modifica di stile —
 * lasciando la piu' pericolosa delle due con la grafica vecchia.
 */
@Composable
private fun DangerBlock(
    label: String,
    explanation: String,
    confirmLabel: String,
    confirming: Boolean,
    onAsk: () -> Unit,
    onCancel: () -> Unit,
    onConfirm: () -> Unit,
) {
    val red = Color(0xFFE14848)
    Column(modifier = Modifier.padding(
        start = MediaPlayerSpacing.M, end = MediaPlayerSpacing.M, top = 16.dp,
    )) {
        if (!confirming) {
            DangerPill(label, red, onAsk)
        } else {
            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MHColors.TextLo,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DangerPill(confirmLabel, red, onConfirm)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(MHColors.Card)
                        .clickable(onClick = onCancel)
                        .padding(horizontal = 18.dp, vertical = 11.dp),
                ) {
                    Text("Annulla", color = MHColors.TextHi, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun DangerPill(text: String, red: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .border(1.dp, red.copy(alpha = 0.55f), RoundedCornerShape(999.dp))
            .background(red.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 11.dp),
    ) {
        Text(text = text, color = red, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

/**
 * Riga con due pulsanti e un numero. Nell'app esiste gia' uno `Slider`
 * (CrossfadeScreen) ma non uno stepper, e per "quante proposte" uno slider
 * e' impreciso al tocco: sbagliare di uno significa una playlist in piu' o
 * in meno.
 *
 * `min`/`max` arrivano dal server (`DjPreferencesDto.minSlots` e compagnia),
 * che fuori intervallo risponde 400: disabilitare il pulsante e' come il
 * controllo dice all'utente cio' che il server gli direbbe comunque.
 */
@Composable
internal fun androidx.compose.foundation.layout.ColumnScope.DjStepperRow(
    label: String,
    detail: String? = null,
    value: Int,
    min: Int,
    max: Int,
    unit: String? = null,
    enabled: Boolean = true,
    onChange: (Int) -> Unit,
    showDivider: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MHColors.TextHi,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MHColors.TextLo,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        IconButton(
            onClick = { onChange(value - 1) },
            enabled = enabled && value > min,
        ) {
            Text("−", color = MHColors.TextHi, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { contentDescription = "Diminuisci $label" })
        }
        Text(
            text = if (unit != null) "$value $unit" else "$value",
            style = LocalMHMono.current.badge.copy(color = MHColors.TextHi),
            modifier = Modifier.width(64.dp),
        )
        IconButton(
            onClick = { onChange(value + 1) },
            enabled = enabled && value < max,
        ) {
            Text("+", color = MHColors.TextHi, fontSize = 18.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.semantics { contentDescription = "Aumenta $label" })
        }
    }
    if (showDivider) HorizontalDivider(color = MHColors.Divider, thickness = 0.5.dp)
}

@Composable
private fun RunRow(run: DjRunDto) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MediaPlayerSpacing.M, vertical = 8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = describeRun(run),
                style = MaterialTheme.typography.bodyMedium,
                color = MHColors.TextHi,
            )
            Text(
                // L'istante grezzo ("2026-08-22T10:00:00Z") non e' per un
                // essere umano: stesso formattatore gia' usato per "For You"
                // ("oggi alle 04:03"), cosi' la cronologia parla la lingua
                // del resto dell'app invece di quella del JSON.
                text = (formatRefreshedAt(run.startedAt) ?: run.startedAt) +
                    (run.model?.let { " · $it" } ?: ""),
                style = LocalMHMono.current.badge.copy(color = MHColors.TextLo2),
                modifier = Modifier.padding(top = 2.dp),
            )
            run.error?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MHColors.TextLo2,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        Text(
            text = runStatusLabel(run.status),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (run.status == "FAILED") Color(0xFFE14848) else MHColors.Lime,
        )
    }
}

/**
 * La frase che descrive a che punto e' il DJ.
 *
 * Il server manda un'etichetta (`CATALOG`), non una frase: la lingua
 * dell'interfaccia vive qui, e cambiarla non deve costare un deploy del
 * backend.
 *
 * Un'etichetta sconosciuta — perche' il backend ne ha aggiunta una che questa
 * versione dell'app non conosce — non produce una riga vuota: ricade sul testo
 * generico. Una riga bianca sotto "sta pensando" sembrerebbe un difetto.
 */
internal fun phaseText(phase: String?): String = when (phase?.uppercase()) {
    "PROFILE" -> "sta guardando i tuoi gusti"
    "CATALOG" -> "sta cercando nella tua libreria"
    "TRACKS" -> "sta controllando i brani"
    "HISTORY" -> "sta rivedendo cosa ti ha già proposto"
    "WRITING" -> "sta scrivendo la risposta"
    else -> "sta leggendo il tuo messaggio"
}

/** "0:47", "2:05". Mai il numero grezzo di secondi. */
internal fun formatElapsed(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    return "${safe / 60}:${(safe % 60).toString().padStart(2, '0')}"
}
