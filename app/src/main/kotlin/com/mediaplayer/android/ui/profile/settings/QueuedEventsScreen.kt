package com.mediaplayer.android.ui.profile.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediaplayer.android.data.ConnectivityObserver
import com.mediaplayer.android.data.sync.EventQueue
import com.mediaplayer.android.data.sync.EventType
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MonoFamily
import kotlinx.coroutines.delay

/**
 * Profile → App → Eventi in coda. Mockup `mh-update.jsx:155-215`. Hero
 * card with the total queued count in big mono lime numerals + caption,
 * `// DETTAGLIO` eyebrow, list of typed events with squircle icons +
 * ×N pills, and a status footer that ticks down to the next retry when
 * a backoff window is active.
 */
@Composable
fun QueuedEventsScreen(onBack: () -> Unit) {
    val total by EventQueue.pending.collectAsStateWithLifecycle()
    val rows by EventQueue.rows.collectAsStateWithLifecycle()
    val online by ConnectivityObserver.networkAvailable.collectAsStateWithLifecycle()
    val nextRetryAt by EventQueue.nextRetryAt.collectAsStateWithLifecycle()
    val mono = LocalMHMono.current

    SettingsSubScreen(
        title = "Eventi in coda",
        onBack = onBack,
        eyebrow = "Profilo · Diagnostica",
    ) {
        // Hero card — big mono lime count + caption.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(MHColors.Card)
                .padding(18.dp),
        ) {
            Column {
                Text(
                    text = "// IN ATTESA DI SYNC",
                    style = mono.eyebrow.copy(color = MHColors.TextLo),
                )
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = total.toString(),
                        color = MHColors.Lime,
                        fontFamily = MonoFamily,
                        fontSize = 56.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-2).sp,
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = if (total == 1) "evento pronto" else "eventi pronti",
                        color = MHColors.TextLo,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Le azioni offline (mi piace, follow, riproduzioni) si svuotano " +
                        "da sole quando torni online. Nessun dato perso.",
                    color = MHColors.TextLo,
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        }

        if (rows.isEmpty()) {
            EmptyDetail()
        } else {
            DetailList(rows = rows)
        }

        StatusFooter(
            online = online,
            total = total,
            nextRetryAt = nextRetryAt,
        )
    }
}

@Composable
private fun EmptyDetail() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            imageVector = Icons.Filled.CheckCircle,
            contentDescription = null,
            tint = MHColors.Lime,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = "Tutto sincronizzato — nessun evento in attesa.",
            color = MHColors.TextHi,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun DetailList(rows: List<EventQueue.UiRow>) {
    val mono = LocalMHMono.current
    val display = remember(rows) { collapseRows(rows) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "// DETTAGLIO",
            style = mono.eyebrow.copy(color = MHColors.TextLo),
            modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 8.dp),
        )
        display.forEach { item ->
            DetailRow(item = item)
        }
    }
}

@Composable
private fun DetailRow(item: DisplayItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MHColors.Lime.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = MHColors.Lime,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.label,
                color = MHColors.TextHi,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
            if (item.subtitle != null) {
                Text(
                    text = item.subtitle,
                    color = MHColors.TextLo,
                    fontSize = 11.5.sp,
                    modifier = Modifier.padding(top = 1.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MHColors.Lime.copy(alpha = 0.12f))
                .padding(horizontal = 9.dp, vertical = 3.dp),
        ) {
            Text(
                text = "×${item.count}",
                color = MHColors.Lime,
                fontFamily = MonoFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
    androidx.compose.material3.HorizontalDivider(
        color = Color.White.copy(alpha = 0.06f),
        thickness = 1.dp,
    )
}

@Composable
private fun StatusFooter(
    online: Boolean,
    total: Int,
    nextRetryAt: Long?,
) {
    val mono = LocalMHMono.current

    // Live clock — re-reads System.currentTimeMillis once per second so
    // the countdown text refreshes without the queue needing to push.
    var nowTick by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(nextRetryAt) {
        if (nextRetryAt == null) return@LaunchedEffect
        while (true) {
            nowTick = System.currentTimeMillis()
            if (nowTick >= nextRetryAt) break
            delay(1_000L)
        }
    }

    val countdownLeftMs = nextRetryAt?.let { (it - nowTick).coerceAtLeast(0L) }

    val (text, useSpinner, useError) = when {
        total == 0 -> Triple("Niente da inviare", false, false)
        countdownLeftMs != null && countdownLeftMs > 0L -> Triple(
            "Prossimo flush automatico tra ${formatMmSs(countdownLeftMs)}",
            false,
            false,
        )
        online -> Triple("Sincronizzazione in corso…", true, false)
        else -> Triple("In attesa di rete — sync appena torni online", false, true)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (useSpinner) {
            CircularProgressIndicator(
                color = MHColors.Lime,
                trackColor = MHColors.Lime.copy(alpha = 0.25f),
                strokeWidth = 2.4.dp,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Icon(
                imageVector = when {
                    total == 0 -> Icons.Filled.CheckCircle
                    useError -> Icons.Filled.CloudOff
                    else -> Icons.Filled.CheckCircle
                },
                contentDescription = null,
                tint = MHColors.TextLo,
                modifier = Modifier.size(18.dp),
            )
        }
        // Compose the line so the MM:SS portion paints lime + monospace
        // even though the rest is muted body text.
        if (countdownLeftMs != null && countdownLeftMs > 0L && total > 0) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Prossimo flush automatico tra ",
                    color = MHColors.TextLo,
                    fontSize = 12.sp,
                    style = mono.duration.copy(fontFamily = MonoFamily),
                )
                Text(
                    text = formatMmSs(countdownLeftMs),
                    color = MHColors.Lime,
                    fontSize = 12.sp,
                    fontFamily = MonoFamily,
                    fontWeight = FontWeight.Bold,
                )
            }
        } else {
            Text(
                text = text,
                color = MHColors.TextLo,
                fontSize = 12.sp,
                style = mono.duration.copy(fontFamily = MonoFamily),
            )
        }
    }
}

private fun formatMmSs(ms: Long): String {
    val totalSeconds = (ms / 1_000L).coerceAtLeast(0L)
    val mm = totalSeconds / 60L
    val ss = totalSeconds % 60L
    return "%02d:%02d".format(mm, ss)
}

private data class DisplayItem(
    val label: String,
    val subtitle: String?,
    val count: Int,
    val icon: ImageVector,
)

/**
 * Collapses raw queue rows into UI-friendly display items.
 *
 * - When a row carries a `displayLabel` (set by the producer at enqueue
 *   time — e.g. `"Citrine — Mira Holt"`), it gets its own line with the
 *   title in the row label and the rest as subtitle. This matches the
 *   mockup's per-event detail.
 * - Rows without a displayLabel (legacy entries from before v3 of the
 *   sync DB) collapse into a single per-type bucket with a generic
 *   "N brani aggiunti ai preferiti" subtitle so we still surface them.
 * - PLAYs without a label still collapse into one "Riproduzioni" row,
 *   since per-play rows would dominate the list.
 */
private fun collapseRows(rows: List<EventQueue.UiRow>): List<DisplayItem> {
    val out = ArrayList<DisplayItem>(rows.size)

    val songTypes = setOf(
        EventType.LIKE,
        EventType.UNLIKE,
        EventType.DISLIKE_SONG,
        EventType.UNDISLIKE_SONG,
    )
    val artistTypes = setOf(
        EventType.FOLLOW,
        EventType.UNFOLLOW,
        EventType.DISLIKE_ARTIST,
        EventType.UNDISLIKE_ARTIST,
    )

    // PLAY: per-row when labeled, else one collapsed bucket. We dedupe
    // labeled PLAYs by label so a song listened five times in a row
    // shows up once as "× 5" rather than as five identical rows.
    val playRows = rows.filter { it.type == EventType.PLAY }
    val (labeledPlays, unlabeledPlays) = playRows.partition { !it.displayLabel.isNullOrBlank() }
    labeledPlays.groupBy { it.displayLabel!! }.forEach { (label, group) ->
        out += titledItem(EventType.PLAY, label, count = group.size)
    }
    if (unlabeledPlays.isNotEmpty()) {
        out += DisplayItem(
            label = "Riproduzioni",
            subtitle = if (unlabeledPlays.size == 1) "Una traccia ascoltata offline"
            else "${unlabeledPlays.size} tracce ascoltate offline",
            count = unlabeledPlays.size,
            icon = Icons.Filled.PlayArrow,
        )
    }

    // Song-id types: one row per labeled event (deduped by label),
    // plus one collapsed bucket per type for unlabeled rows.
    songTypes.forEach { type ->
        val matches = rows.filter { it.type == type }
        if (matches.isEmpty()) return@forEach
        val (labeled, unlabeled) = matches.partition { !it.displayLabel.isNullOrBlank() }
        labeled.groupBy { it.displayLabel!! }.forEach { (label, group) ->
            out += titledItem(type, label, count = group.size)
        }
        if (unlabeled.isNotEmpty()) {
            out += DisplayItem(
                label = labelFor(type),
                subtitle = subtitleFor(type, count = unlabeled.size),
                count = unlabeled.size,
                icon = iconFor(type),
            )
        }
    }

    // Artist types — payload IS the artist; one row per distinct artist.
    // displayLabel is set to the artist name as well, so we prefer it.
    artistTypes.forEach { type ->
        rows.filter { it.type == type }
            .groupBy { it.displayLabel?.takeIf { it.isNotBlank() } ?: it.payload }
            .forEach { (artist, group) ->
                out += DisplayItem(
                    label = labelFor(type),
                    subtitle = artist.ifBlank { null },
                    count = group.size,
                    icon = iconFor(type),
                )
            }
    }

    return out
}

/**
 * Per-row display item with the labeled title in the main slot and the
 * trailing artist (if any) as subtitle. Mockup format:
 * `Mi piace · Citrine` / `Mira Holt`.
 */
private fun titledItem(type: EventType, label: String, count: Int): DisplayItem {
    val (title, artist) = splitTitleArtist(label)
    val prefix = labelFor(type)
    val composed = if (title.isNotBlank()) "$prefix · $title" else prefix
    return DisplayItem(
        label = composed,
        subtitle = artist.takeIf { it.isNotBlank() },
        count = count,
        icon = iconFor(type),
    )
}

private fun splitTitleArtist(label: String): Pair<String, String> {
    val sepIdx = label.indexOf(" — ")
    if (sepIdx > 0) {
        return label.substring(0, sepIdx).trim() to label.substring(sepIdx + 3).trim()
    }
    return label.trim() to ""
}

private fun labelFor(type: EventType): String = when (type) {
    EventType.PLAY -> "Riproduzioni"
    EventType.LIKE -> "Mi piace"
    EventType.UNLIKE -> "Mi piace rimosso"
    EventType.FOLLOW -> "Segui artista"
    EventType.UNFOLLOW -> "Smetti di seguire"
    EventType.DISLIKE_SONG -> "Non consigliarmi · brano"
    EventType.UNDISLIKE_SONG -> "Ripristina brano"
    EventType.DISLIKE_ARTIST -> "Non consigliarmi · artista"
    EventType.UNDISLIKE_ARTIST -> "Ripristina artista"
}

private fun subtitleFor(type: EventType, count: Int): String? = when (type) {
    EventType.LIKE -> if (count == 1) "Un brano aggiunto ai preferiti"
    else "$count brani aggiunti ai preferiti"
    EventType.UNLIKE -> if (count == 1) "Un brano tolto dai preferiti"
    else "$count brani tolti dai preferiti"
    EventType.DISLIKE_SONG -> if (count == 1) "Un brano escluso dai consigli"
    else "$count brani esclusi dai consigli"
    EventType.UNDISLIKE_SONG -> if (count == 1) "Un brano ripristinato"
    else "$count brani ripristinati"
    else -> null
}

private fun iconFor(type: EventType): ImageVector = when (type) {
    EventType.PLAY -> Icons.Filled.PlayArrow
    EventType.LIKE, EventType.UNLIKE -> Icons.Filled.Favorite
    EventType.FOLLOW -> Icons.Filled.Person
    EventType.UNFOLLOW -> Icons.Filled.PersonOff
    EventType.DISLIKE_SONG, EventType.DISLIKE_ARTIST -> Icons.Filled.ThumbDown
    EventType.UNDISLIKE_SONG, EventType.UNDISLIKE_ARTIST -> Icons.Filled.ThumbUp
}

