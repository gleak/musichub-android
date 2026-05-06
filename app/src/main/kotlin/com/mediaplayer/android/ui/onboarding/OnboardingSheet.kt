package com.mediaplayer.android.ui.onboarding

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
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.android.ui.common.EyebrowText
import com.mediaplayer.android.ui.theme.MHColors

/**
 * One-time onboarding sheet shown on first launch (gated by
 * `ChangelogPreferences.lastSeenVersion() == null`). Sets expectations
 * about what the app does — a personal music library streamed from the user's
 * own backend, with Spotify import and Android Auto support.
 *
 * Layout per `mockup/mh-auth-states.jsx#OnboardingSheetExplainer`:
 * lime-tinted gradient sheet, custom drag handle, italian eyebrow + tagline,
 * three lime-tile feature rows, single "Inizia" pill CTA.
 *
 * Distinct from `ChangelogSheet`: an upgrade shows the changelog,
 * a *first* install shows this sheet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardingSheet(onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sheetShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        contentColor = MHColors.TextHi,
        shape = sheetShape,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(sheetShape)
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFF233A12),
                        0.6f to Color(0xFF131313),
                        1f to Color(0xFF131313),
                    ),
                )
                .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 32.dp),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 18.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.18f)),
            )
            EyebrowText(text = "Benvenuto in MusicHub")
            Spacer(Modifier.height(8.dp))
            Text(
                text = "La tua libreria,\nil tuo ritmo.",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                ),
                color = MHColors.TextHi,
            )
            Spacer(Modifier.height(18.dp))

            FeatureRow(
                icon = Icons.Filled.LibraryMusic,
                title = "La tua libreria, ovunque",
                subtitle = "Importa playlist da Spotify e ritrova tutto in un posto solo.",
            )
            Spacer(Modifier.height(14.dp))
            FeatureRow(
                icon = Icons.Filled.MusicNote,
                title = "Daily Mix che impara",
                subtitle = "Più ascolti, più i suggerimenti diventano tuoi.",
            )
            Spacer(Modifier.height(14.dp))
            FeatureRow(
                icon = Icons.Filled.PlayCircle,
                title = "Offline · senza interruzioni",
                subtitle = "Scarica album e playlist per il viaggio o la metro.",
            )
            Spacer(Modifier.height(26.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MHColors.Lime,
                    contentColor = Color(0xFF0A0A0A),
                ),
            ) {
                Text(
                    text = "Inizia",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun FeatureRow(icon: ImageVector, title: String, subtitle: String) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.verticalGradient(
                        0f to MHColors.Lime,
                        1f to MHColors.LimeDim,
                    ),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF0A0A0A),
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.padding(top = 1.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                ),
                color = MHColors.TextHi,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 12.5.sp,
                    lineHeight = 18.sp,
                ),
                color = MHColors.TextLo,
            )
        }
    }
}
