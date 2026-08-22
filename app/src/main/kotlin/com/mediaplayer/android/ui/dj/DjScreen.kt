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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.data.dto.DjChatMessageDto
import com.mediaplayer.android.data.dto.DjTasteProfileDto
import com.mediaplayer.android.ui.common.CenteredSpinner
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.EyebrowText
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
fun DjScreen(viewModel: DjViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    var confirmErase by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MHGradient.heroBg(Color(0xFF241033))),
    ) {
        when {
            state.loading -> CenteredSpinner()
            state.loadError != null -> ErrorWithRetry(state.loadError!!, viewModel::refresh)
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                items(state.messages, key = { it.createdAt + it.role }) { ChatBubble(it) }

                if (!state.chatEnabled) {
                    item(key = "chat-off") { DjNotice("La chat col DJ e’ spenta su questo server.") }
                }
                state.sendError?.let { error ->
                    item(key = "send-error") { DjNotice(error) }
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
                            enabled = state.chatEnabled && !state.sending && draft.isNotBlank(),
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
                    item(key = "sending") { DjBody("Il DJ sta pensando…") }
                }

                item(key = "profile-header") {
                    DjSectionTitle("Cosa il DJ ha capito", "Profilo del gusto")
                }
                item(key = "profile-body") {
                    ProfileBody(state.profile)
                }
                if (state.profile?.hasProfile == true) {
                    item(key = "erase") {
                        EraseBlock(
                            confirming = confirmErase,
                            onAsk = { confirmErase = true },
                            onCancel = { confirmErase = false },
                            onConfirm = {
                                confirmErase = false
                                viewModel.eraseChatAndProfile()
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
private fun ChatBubble(message: DjChatMessageDto) {
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

@Composable
private fun EraseBlock(
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
            DangerPill("Cancella conversazione e profilo", red, onAsk)
        } else {
            Text(
                text = "Spariscono i messaggi, il profilo e tutto il suo storico. Le playlist " +
                    "gia’ proposte restano dove sono.",
                style = MaterialTheme.typography.bodySmall,
                color = MHColors.TextLo,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DangerPill("Sì, cancella tutto", red, onConfirm)
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
