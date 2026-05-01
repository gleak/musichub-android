package com.mediaplayer.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.android.ui.theme.MHColors

/**
 * MusicHub filter / segmented chip. Selected = lime fill + black text.
 * Unselected = `rgba(255,255,255,0.08)` + white text.
 */
@Composable
fun PillChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) MHColors.Lime else Color.White.copy(alpha = 0.08f)
    val fg = if (selected) Color(0xFF0A0A0A) else MHColors.TextHi
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(PaddingValues(horizontal = 14.dp, vertical = 6.dp)),
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 12.5.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}
