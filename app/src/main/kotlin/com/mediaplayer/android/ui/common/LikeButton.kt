package com.mediaplayer.android.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mediaplayer.android.data.LikedSongsCache
import com.mediaplayer.android.ui.theme.MHColors

/**
 * Visual variant of [LikeButton]. Picks icon size, padding and the unliked
 * tint to match the surface the button sits on (list rows, mini-player,
 * full-bleed hero).
 */
enum class LikeButtonVariant { Row, Player, Hero }

/**
 * Single shared heart toggle. Reads the current liked state from
 * [LikedSongsCache] so any like performed elsewhere in the app updates
 * here without per-screen plumbing. Tapping calls [LikedSongsCache.toggle]
 * unless [onToggleOverride] is provided (the player surfaces use the
 * override to route the toggle through the playback service so the AA
 * media-style heart and the persistent notification's custom layout
 * stay in sync — the service mirrors back into the cache).
 *
 * Pass [displayLabel] when you want the queued sync event to carry a
 * human-readable label (used by the `QueuedEvents` debug screen).
 */
@Composable
fun LikeButton(
    songId: Long,
    modifier: Modifier = Modifier,
    variant: LikeButtonVariant = LikeButtonVariant.Row,
    displayLabel: String? = null,
    unlikedTint: Color? = null,
    likedTint: Color? = null,
    onToggleOverride: (() -> Unit)? = null,
) {
    val likedIds by LikedSongsCache.likedIds.collectAsStateWithLifecycle()
    val isLiked = songId in likedIds
    LaunchedEffect(songId) { LikedSongsCache.prime(listOf(songId)) }

    val haptics = LocalHapticFeedback.current
    val onClick = {
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        if (onToggleOverride != null) onToggleOverride()
        else LikedSongsCache.toggle(songId, displayLabel)
        Unit
    }
    val resolvedLikedTint = likedTint ?: MaterialTheme.colorScheme.primary
    val resolvedUnlikedTint = unlikedTint ?: when (variant) {
        LikeButtonVariant.Row, LikeButtonVariant.Player -> MaterialTheme.colorScheme.onSurfaceVariant
        LikeButtonVariant.Hero -> MHColors.OnHero
    }
    val tint = if (isLiked) resolvedLikedTint else resolvedUnlikedTint
    val cd = if (isLiked) "Rimuovi mi piace" else "Mi piace"

    when (variant) {
        LikeButtonVariant.Row -> {
            Box(
                modifier = modifier
                    .clip(CircleShape)
                    .clickable(onClick = onClick)
                    .padding(4.dp),
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = cd,
                    tint = tint,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
        LikeButtonVariant.Player, LikeButtonVariant.Hero -> {
            IconButton(onClick = onClick, modifier = modifier) {
                Icon(
                    imageVector = if (isLiked) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = cd,
                    tint = tint,
                )
            }
        }
    }
}
