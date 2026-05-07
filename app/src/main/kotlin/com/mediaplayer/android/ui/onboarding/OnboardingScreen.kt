package com.mediaplayer.android.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.android.ui.common.EyebrowText
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors

/**
 * M14e first-run tag picker. Shown on top of [com.mediaplayer.android.MainActivity.AuthGate]
 * when `getMe().onboardingComplete == false` AND the user hasn't dismissed
 * the picker locally. Picks land as `user_taste(GENRE)` rows server-side
 * and unlock the recommender's warm path.
 *
 * Layout per `mockup/mh-auth-states.jsx` (`OnboardingNeedsMore` / `OnboardingSaving` / `OnboardingError`):
 *  - Pill cloud (FlowRow), 12 italian-cased genres.
 *  - Footer row [Salta · counter · CTA] above a thin top divider.
 *  - Below threshold: dashed-look ghost lime CTA "Scegli ancora N".
 *  - Saving: grid dimmed, counter swaps to "SALVATAGGIO…", CTA shows spinner + "Salvo…".
 *  - Error: red band between grid and footer with code `onboarding/seed-genres` + RIPROVA pill.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingScreen(
    saving: Boolean,
    error: String?,
    onContinue: (List<String>) -> Unit,
    onSkip: () -> Unit,
) {
    var picked by rememberSaveable { mutableStateOf(setOf<String>()) }
    val canContinue = picked.size >= MIN_PICKS && !saving
    val needMore = (MIN_PICKS - picked.size).coerceAtLeast(0)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(0f to MHColors.BgTop, 1f to MHColors.BgBottom),
            )
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 64.dp, bottom = 12.dp),
        ) {
            EyebrowText(text = "Passo 1 / 1")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Cosa ascolti?",
                style = MaterialTheme.typography.headlineMedium,
                color = MHColors.TextHi,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Scegli almeno $MIN_PICKS generi. Useremo questo segnale " +
                    "per costruire il tuo Daily Mix.",
                style = MaterialTheme.typography.bodyMedium,
                color = MHColors.TextLo,
            )
        }

        FlowRow(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .alpha(if (saving) 0.55f else 1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            GENRES.forEach { (label, slug) ->
                GenrePill(
                    label = label,
                    selected = slug in picked,
                    enabled = !saving,
                    onToggle = {
                        picked = if (slug in picked) picked - slug else picked + slug
                    },
                )
            }
        }

        if (error != null) {
            ErrorBand(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                onRetry = { onContinue(picked.toList()) },
            )
        }

        HorizontalDivider(thickness = 1.dp, color = Color.White.copy(alpha = 0.06f))
        FooterRow(
            picked = picked.size,
            saving = saving,
            canContinue = canContinue,
            needMore = needMore,
            onSkip = onSkip,
            onContinue = { onContinue(picked.toList()) },
        )
    }
}

@Composable
private fun GenrePill(
    label: String,
    selected: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    val bg = if (selected) MHColors.Lime else Color.White.copy(alpha = 0.06f)
    val fg = if (selected) Color(0xFF0A0A0A) else MHColors.TextHi
    val baseModifier = Modifier
        .clip(shape)
        .background(bg)
        .clickable(enabled = enabled, onClick = onToggle)
    val pillModifier = if (selected) baseModifier
    else baseModifier.border(1.dp, Color.White.copy(alpha = 0.10f), shape)

    Row(
        modifier = pillModifier.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(14.dp),
            )
        }
        Text(
            text = label,
            color = fg,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
        )
    }
}

@Composable
private fun ErrorBand(modifier: Modifier = Modifier, onRetry: () -> Unit) {
    val mono = LocalMHMono.current
    val red = Color(0xFFE14848)
    val redText = Color(0xFFFFB3B3)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(red.copy(alpha = 0.12f))
            .border(1.dp, red.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 6.dp)
                .size(8.dp)
                .clip(CircleShape)
                .background(red),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Salvataggio non riuscito",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = redText,
            )
            Spacer(Modifier.height(2.dp))
            Row {
                Text(
                    text = "Verifica la connessione e riprova. Codice: ",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                    color = redText.copy(alpha = 0.72f),
                )
                Text(
                    text = "onboarding/seed-genres",
                    style = mono.duration.copy(
                        color = redText.copy(alpha = 0.72f),
                        fontSize = 11.5.sp,
                    ),
                )
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .border(1.dp, redText.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
                .clickable(onClick = onRetry)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            Text(
                text = "RIPROVA",
                style = mono.badge.copy(color = redText),
            )
        }
    }
}

@Composable
private fun FooterRow(
    picked: Int,
    saving: Boolean,
    canContinue: Boolean,
    needMore: Int,
    onSkip: () -> Unit,
    onContinue: () -> Unit,
) {
    val mono = LocalMHMono.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TextButton(onClick = onSkip, enabled = !saving) {
            Text(
                text = "Salta",
                color = if (saving) MHColors.TextLo2 else MHColors.TextLo,
            )
        }
        val counterText = when {
            saving -> "SALVATAGGIO…"
            picked < MIN_PICKS -> "$picked / $MIN_PICKS minimo"
            else -> "$picked selezionati"
        }
        Text(
            text = counterText,
            style = mono.duration.copy(
                color = if (saving) MHColors.Lime else MHColors.TextLo2,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp,
            ),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
        )
        ContinuePill(
            saving = saving,
            canContinue = canContinue,
            needMore = needMore,
            onContinue = onContinue,
        )
    }
}

@Composable
private fun ContinuePill(
    saving: Boolean,
    canContinue: Boolean,
    needMore: Int,
    onContinue: () -> Unit,
) {
    val shape = RoundedCornerShape(999.dp)
    val belowThreshold = needMore > 0 && !saving
    val bg = if (belowThreshold) MHColors.Lime.copy(alpha = 0.08f) else MHColors.Lime
    val baseModifier = Modifier.clip(shape).background(bg)
    val pillModifier = if (belowThreshold)
        baseModifier.border(1.dp, MHColors.Lime.copy(alpha = 0.5f), shape)
    else baseModifier
    val clickable = pillModifier.clickable(enabled = canContinue && !saving, onClick = onContinue)

    Row(
        modifier = clickable.padding(horizontal = 22.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        when {
            saving -> {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = Color(0xFF0A0A0A),
                )
                Text(
                    text = "Salvo…",
                    color = Color(0xFF0A0A0A),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                )
            }
            belowThreshold -> Text(
                text = "Scegli ancora $needMore",
                color = MHColors.Lime,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            )
            else -> Text(
                text = "Continua",
                color = Color(0xFF0A0A0A),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

private const val MIN_PICKS = 3

/**
 * 18 italian-cased genres mirroring `SearchScreen.GENRES` and
 * `LibraryTree.GENRES` so phone Search, Android Auto browse, and onboarding
 * surface the same canonical set. Slugs stay english — they're the bucket
 * keys backend `GenreBuckets.bucketOf` expects, and `user_taste(GENRE)`
 * rows must be locale-stable.
 */
private val GENRES = listOf(
    "Indie" to "indie",
    "Elettronica" to "electronic",
    "Hip-hop" to "hip-hop",
    "Jazz" to "jazz",
    "Classica" to "classical",
    "Ambient" to "ambient",
    "Rock" to "rock",
    "Pop" to "pop",
    "Metal" to "metal",
    "Punk" to "punk",
    "R&B" to "r&b",
    "Folk" to "folk",
    "Country" to "country",
    "Blues" to "blues",
    "Reggae" to "reggae",
    "Latina" to "latin",
    "Lo-fi" to "lo-fi",
    "Cantautorato" to "singer-songwriter",
)
