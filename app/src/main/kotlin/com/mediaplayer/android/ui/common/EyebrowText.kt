package com.mediaplayer.android.ui.common

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import androidx.compose.runtime.CompositionLocalProvider

/**
 * `// SECTION` mono lime label used as a section eyebrow throughout
 * the MusicHub design. Auto-prefixes with `// ` and uppercases.
 */
@Composable
fun EyebrowText(
    text: String,
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = MHColors.Lime,
) {
    val style = LocalMHMono.current.eyebrow.copy(color = color)
    Text(
        text = "// ${text.uppercase()}",
        style = style,
        modifier = modifier,
    )
}
