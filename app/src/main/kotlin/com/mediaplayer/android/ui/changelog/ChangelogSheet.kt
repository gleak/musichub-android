package com.mediaplayer.android.ui.changelog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mediaplayer.android.data.AppVersion
import com.mediaplayer.android.data.Changelog
import com.mediaplayer.android.data.ChangelogEntry
import com.mediaplayer.android.data.ChangelogPreferences
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MonoFamily

/**
 * What's new sheet — mockup `mh-update.jsx:101-152`. Hero with lime
 * gradient + mono eyebrow + version diff, body listing the latest
 * release's highlights as numbered cards (`01`, `02`, …) separated by
 * hairline dividers, sticky `Continua` lime pill at the bottom.
 *
 * Only the LATEST entry is shown — older entries are intentionally
 * hidden so the sheet stays focused on this release. The full history
 * still lives in `Changelog.entries` for any future "tutto lo storico"
 * surface.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogSheet(
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val latest = remember { Changelog.entries.firstOrNull() }
    val previous = remember { Changelog.entries.getOrNull(1)?.version }

    LaunchedEffect(Unit) {
        ChangelogPreferences.instance.markSeen(AppVersion.VERSION)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color(0xFF181818),
        dragHandle = null,
    ) {
        if (latest == null) {
            Spacer(Modifier.height(24.dp))
            return@ModalBottomSheet
        }
        ChangelogContent(
            entry = latest,
            previousVersion = previous,
            onContinue = onDismiss,
        )
    }
}

@Composable
private fun ChangelogContent(
    entry: ChangelogEntry,
    previousVersion: String?,
    onContinue: () -> Unit,
) {
    val mono = LocalMHMono.current
    Column(modifier = Modifier.fillMaxWidth()) {
        // Stock M3 grab handle was suppressed; paint our own slim 36×4 pill.
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .padding(bottom = 6.dp)
                .align(Alignment.CenterHorizontally)
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = 0.18f)),
        )

        // Scrolling body — hero + numbered highlights live in one scroll
        // region so long highlight bodies don't push the footer off-screen.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            // Hero — lime top-down gradient over title + version diff.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            0f to MHColors.Lime.copy(alpha = 0.16f),
                            1f to Color.Transparent,
                        ),
                    )
                    .padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 24.dp),
            ) {
                Text(
                    text = "// NOVITÀ · v${entry.version}",
                    style = mono.eyebrow.copy(color = MHColors.Lime),
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = entry.title,
                    color = MHColors.TextHi,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    lineHeight = 32.sp,
                    letterSpacing = (-0.7).sp,
                )
                if (previousVersion != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = buildAnnotatedString {
                            withStyle(
                                SpanStyle(
                                    color = MHColors.TextLo2,
                                    fontFamily = MonoFamily,
                                ),
                            ) { append("v$previousVersion → ") }
                            withStyle(
                                SpanStyle(
                                    color = MHColors.Lime,
                                    fontFamily = MonoFamily,
                                ),
                            ) { append("v${entry.version}") }
                        },
                        fontSize = 12.sp,
                    )
                }
            }

            // Pager — chunks the highlights into pages of two so a long
            // release fits without an unwieldy scroll. Mockup `mh-update.jsx:139`
            // shows the dot indicator below the pager.
            HighlightsPager(highlights = entry.highlights)
            Spacer(Modifier.height(8.dp))
        }

        HorizontalDivider(
            color = Color.White.copy(alpha = 0.05f),
            thickness = 1.dp,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 36.dp),
        ) {
            ContinuaPill(onClick = onContinue)
        }
    }
}

@Composable
private fun HighlightsPager(highlights: List<String>) {
    if (highlights.isEmpty()) return
    val pages = remember(highlights) { highlights.chunked(2) }
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 180.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp),
            pageSpacing = 0.dp,
        ) { pageIndex ->
            val pageHighlights = pages[pageIndex]
            // Compute the absolute index of each row across the whole list so
            // numbering stays continuous (`01 02 03 04`) even though we
            // paginate in chunks of two.
            val baseIndex = pageIndex * 2
            Column(modifier = Modifier.fillMaxWidth()) {
                pageHighlights.forEachIndexed { localIdx, line ->
                    NumberedRow(index = baseIndex + localIdx + 1, body = line)
                    if (localIdx < pageHighlights.lastIndex) {
                        HorizontalDivider(
                            color = Color.White.copy(alpha = 0.06f),
                            thickness = 1.dp,
                        )
                    }
                }
            }
        }

        if (pages.size > 1) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                val current = pagerState.currentPage
                pages.indices.forEach { i ->
                    val active = i == current
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(if (active) 18.dp else 4.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (active) MHColors.Lime else Color.White.copy(alpha = 0.2f),
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun NumberedRow(index: Int, body: String) {
    val (title, subtitle) = remember(body) { splitHighlight(body) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            text = index.toString().padStart(2, '0'),
            color = MHColors.Lime,
            fontFamily = MonoFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(20.dp).padding(top = 2.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            if (title != null) {
                Text(
                    text = title,
                    color = MHColors.TextHi,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.2).sp,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    color = MHColors.TextLo,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                Text(
                    text = subtitle,
                    color = MHColors.TextHi,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                )
            }
        }
    }
}

@Composable
private fun ContinuaPill(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(999.dp))
            .background(MHColors.Lime)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Continua",
            color = Color(0xFF0A0A0A),
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
    }
}

/**
 * Pulls a leading short title out of a highlight string. Most existing
 * `Changelog.entries` highlights are a single long sentence — that
 * renders fine as a body-only row. A few use the form
 * `"Title: rest of the line"` — we split on the first `:` so the title
 * gets the bold treatment the mockup specifies.
 */
private fun splitHighlight(line: String): Pair<String?, String> {
    val firstColon = line.indexOf(':')
    if (firstColon in 8..40) {
        val title = line.substring(0, firstColon).trim().trimEnd('.', ' ')
        val rest = line.substring(firstColon + 1).trim()
        if (rest.isNotEmpty()) return title to rest
    }
    return null to line
}
