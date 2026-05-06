package com.mediaplayer.android.ui.profile.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.mediaplayer.android.ui.common.EyebrowText
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MHGradient
import com.mediaplayer.android.ui.theme.MediaPlayerSpacing

/**
 * Shared scaffold for settings sub-screens reached from `ProfileScreen`.
 * MusicHub style: gradient background, lime mono eyebrow above the title
 * (`mh-settings.jsx:5-19`), back chevron, scrollable content column with
 * each section a `SettingsCard`. Optional [footer] slot pins beneath a
 * 1px divider per the mockup contract.
 */
@Composable
fun SettingsSubScreen(
    title: String,
    onBack: () -> Unit,
    eyebrow: String? = "Impostazioni",
    footer: (@Composable () -> Unit)? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MHGradient.screenBg()),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Indietro",
                        tint = MHColors.TextHi,
                    )
                }
                Column(modifier = Modifier.padding(start = 4.dp)) {
                    if (eyebrow != null) EyebrowText(text = eyebrow)
                    Text(
                        text = title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MHColors.TextHi,
                        modifier = Modifier.padding(top = if (eyebrow != null) 2.dp else 0.dp),
                    )
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = MediaPlayerSpacing.M, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                content = content,
            )
            if (footer != null) {
                HorizontalDivider(color = MHColors.Divider, thickness = 1.dp)
                Box(modifier = Modifier.padding(horizontal = MediaPlayerSpacing.M, vertical = 12.dp)) {
                    footer()
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    eyebrow: String? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    if (eyebrow != null) EyebrowText(text = eyebrow)
    Column(
        modifier = Modifier
            .padding(top = if (eyebrow != null) 8.dp else 0.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MHColors.Card),
        content = content,
    )
}

@Composable
fun androidx.compose.foundation.layout.ColumnScope.SettingsToggleRow(
    label: String,
    detail: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
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
        androidx.compose.material3.Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = androidx.compose.material3.SwitchDefaults.colors(
                checkedThumbColor = MHColors.TextHi,
                checkedTrackColor = MHColors.Lime,
                uncheckedThumbColor = MHColors.TextLo,
                uncheckedTrackColor = MHColors.Card,
            ),
        )
    }
    if (showDivider) HorizontalDivider(color = MHColors.Divider, thickness = 0.5.dp)
}

@Composable
fun androidx.compose.foundation.layout.ColumnScope.SettingsRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MHColors.TextHi,
            modifier = Modifier.weight(1f),
        )
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(if (selected) MHColors.Lime else MHColors.Divider),
            contentAlignment = Alignment.Center,
        ) {
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(androidx.compose.foundation.shape.CircleShape)
                        .background(androidx.compose.ui.graphics.Color(0xFF0A0A0A)),
                )
            }
        }
    }
    if (showDivider) HorizontalDivider(color = MHColors.Divider, thickness = 0.5.dp)
}

@Composable
fun androidx.compose.foundation.layout.ColumnScope.SettingsInfoRow(
    label: String,
    value: String,
    showDivider: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MHColors.TextHi,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MHColors.TextLo,
        )
    }
    if (showDivider) HorizontalDivider(color = MHColors.Divider, thickness = 0.5.dp)
}

/**
 * Chevron-suffixed action row used inside a `SettingsCard`. Pairs a label
 * with an explanatory subtitle and routes taps to [onClick]. Used for the
 * `// GESTIONE` triplet on `DownloadOfflineScreen` (`mh-settings.jsx:93-97`).
 */
@Composable
fun androidx.compose.foundation.layout.ColumnScope.SettingsActionRow(
    label: String,
    detail: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
    showDivider: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .let { if (enabled) it.clickable(onClick = onClick) else it }
            .padding(horizontal = 14.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = if (enabled) MHColors.TextHi else MHColors.TextLo,
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
        Spacer(Modifier.size(8.dp))
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MHColors.TextLo,
            modifier = Modifier.size(16.dp),
        )
    }
    if (showDivider) HorizontalDivider(color = MHColors.Divider, thickness = 0.5.dp)
}

/**
 * Pill-shaped destructive button (`mh-settings.jsx:99-101`). Red border +
 * red translucent fill + red label. Used at the bottom of
 * `DownloadOfflineScreen` for "Cancella tutti i download".
 */
@Composable
fun DestructivePillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val red = androidx.compose.ui.graphics.Color(0xFFE14848)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .border(
                width = 1.dp,
                color = red.copy(alpha = 0.55f),
                shape = RoundedCornerShape(999.dp),
            )
            .background(red.copy(alpha = 0.10f))
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = red,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
            ),
        )
    }
}
