package com.mediaplayer.android.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.ui.common.EyebrowText
import com.mediaplayer.android.ui.theme.CoverShapes
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MHGradient

/**
 * M14e first-run tag picker. Shown on top of [com.mediaplayer.android.MainActivity.AuthGate]
 * when `getMe().onboardingComplete == false` AND the user hasn't dismissed
 * the picker locally. Picks land as `user_taste(GENRE)` rows server-side
 * and unlock the recommender's warm path.
 *
 * UX:
 *  - Pick at least 3 (plan-locked count) before "Continue" enables.
 *  - "Skip" dismisses locally and the user enters with no GENRE seed —
 *    recommender then leans on Last.fm geo-charts as the cold-start signal.
 */
@Composable
fun OnboardingScreen(
    saving: Boolean,
    error: String?,
    onContinue: (List<String>) -> Unit,
    onSkip: () -> Unit,
) {
    var picked by rememberSaveable { mutableStateOf(setOf<String>()) }
    val canContinue = picked.size >= MIN_PICKS && !saving

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MHGradient.screenBg())
            .safeDrawingPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(24.dp))
        EyebrowText(text = "Benvenuto")
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Cosa ascolti?",
            style = MaterialTheme.typography.headlineMedium,
            color = MHColors.TextHi,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Scegli almeno $MIN_PICKS generi per ricevere suggerimenti adatti.",
            style = MaterialTheme.typography.bodyMedium,
            color = MHColors.TextLo,
        )
        Spacer(Modifier.height(20.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(GENRES) { genre ->
                GenreTile(
                    genre = genre,
                    selected = genre in picked,
                    onToggle = {
                        picked = if (genre in picked) picked - genre else picked + genre
                    },
                )
            }
        }

        if (error != null) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        Spacer(Modifier.height(12.dp))
        Button(
            onClick = { onContinue(picked.toList()) },
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth().height(52.dp),
        ) {
            if (saving) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            } else {
                Text(
                    text = if (picked.size < MIN_PICKS)
                        "Scegli ancora ${MIN_PICKS - picked.size}"
                    else "Continua",
                )
            }
        }
        TextButton(
            onClick = onSkip,
            enabled = !saving,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Salta per ora", color = MHColors.TextLo)
        }
        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun GenreTile(genre: String, selected: Boolean, onToggle: () -> Unit) {
    val shape = CoverShapes.Card
    val bg = if (selected) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.surfaceContainerHigh
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary
    else MaterialTheme.colorScheme.onSurface

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(72.dp)
            .clip(shape)
            .background(bg)
            .border(
                width = if (selected) 0.dp else 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = shape,
            )
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = genre.replaceFirstChar { it.uppercase() },
                color = fg,
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}

private const val MIN_PICKS = 3

/**
 * Plan-locked preset of 20 genres. Stored lowercase to match how the
 * backend keys `user_taste(dimension=GENRE, key=...)` rows.
 */
private val GENRES = listOf(
    "rock", "pop", "electronic", "jazz", "hip-hop",
    "classical", "metal", "indie", "r&b", "country",
    "blues", "folk", "punk", "reggae", "soul",
    "techno", "house", "ambient", "latin", "world",
)
