package com.mediaplayer.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors

/**
 * Members preview row used on collaborative-playlist detail (`mh-library.jsx:253-275`).
 * Shows up to 4 overlapping avatar pills + a primary line and a mono caption,
 * with a trailing `Gestisci` pill (owner) or chevron (member).
 *
 * The backend doesn't yet return per-member identity, so non-owner members
 * are rendered as anonymous color-tinted initials seeded by index — the avatar
 * count is honest (matches `memberCount`) without faking specific names.
 */
@Composable
fun MembersStripCard(
    isOwner: Boolean,
    ownerName: String?,
    memberCount: Int,
    onManage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val mono = LocalMHMono.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onManage)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarsCluster(
            isOwner = isOwner,
            ownerName = ownerName,
            memberCount = memberCount,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = if (isOwner) "Condivisa con ${pluralizePeople(memberCount)}"
                       else "Condivisa da ${ownerName ?: "Sconosciuto"}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
            )
            val sub = if (isOwner) {
                if (memberCount == 1) "1 collaboratore attivo" else "$memberCount collaboratori attivi"
            } else {
                if (memberCount == 1) "1 membro" else "$memberCount membri"
            }
            Text(
                text = sub,
                style = mono.duration.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
            )
        }
        Spacer(Modifier.width(8.dp))
        if (isOwner) {
            Button(
                onClick = onManage,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MHColors.Lime,
                    contentColor = Color(0xFF0A0A0A),
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
            ) {
                Text("Gestisci", style = MaterialTheme.typography.labelLarge)
            }
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun AvatarsCluster(isOwner: Boolean, ownerName: String?, memberCount: Int) {
    val displayCount = (memberCount + if (isOwner) 0 else 1).coerceIn(1, 4)
    Row {
        repeat(displayCount) { idx ->
            val (color, letter) = avatarFor(idx, ownerName, isOwner, memberCount)
            Box(
                modifier = Modifier
                    .offset(x = if (idx == 0) 0.dp else (-8 * idx).dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(2.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = letter,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0A0A0A),
                )
            }
        }
    }
}

private val AvatarPalette = listOf(
    Color(0xFF5C2D8C), Color(0xFFFF6B5B), Color(0xFF06B6D4), Color(0xFFFFC857),
)

private fun avatarFor(
    index: Int,
    ownerName: String?,
    isOwner: Boolean,
    memberCount: Int,
): Pair<Color, String> {
    val color = AvatarPalette[index % AvatarPalette.size]
    val letter = when {
        // First avatar = owner; for owner-pov use ownerName initial,
        // for member-pov also use ownerName since it's the only known name.
        index == 0 && !ownerName.isNullOrBlank() -> ownerName.first().uppercaseChar().toString()
        else -> ('A' + ((index * 7) % 26)).toString()
    }
    return color to letter
}

private fun pluralizePeople(count: Int): String =
    if (count == 1) "1 persona" else "$count persone"
