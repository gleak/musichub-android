package com.mediaplayer.android.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.action.clickable
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxHeight
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.media3.common.util.UnstableApi

/**
 * Quick-launch home-screen widget. Renders four configurable tiles, each
 * mapped to an auto-playlist (Discover Daily, On Repeat, Liked, Release
 * Radar by default). Tap → launch [com.mediaplayer.android.MainActivity]
 * with a deep-link extra; the activity resolves the playlist and starts
 * playback. No live state — repaint only on user reconfiguration.
 *
 * Tile selection is per-widget-instance (multiple widgets can have
 * different layouts) and persisted in [QuickLaunchPrefs].
 */
@UnstableApi
class QuickLaunchWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val mgr = GlanceAppWidgetManager(context)
        val appWidgetId = mgr.getAppWidgetId(id)
        val tiles = QuickLaunchPrefs.getTiles(context, appWidgetId)
        provideContent { Content(tiles) }
    }

    @Composable
    private fun Content(tiles: List<QuickLaunchKind>) {
        val ctx = LocalContext.current
        val padded = (tiles + QuickLaunchPrefs.DEFAULT_TILES).take(4)
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                .cornerRadius(16.dp)
                .padding(8.dp),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                Tile(padded[0], ctx, GlanceModifier.defaultWeight().fillMaxHeight())
                Spacer(GlanceModifier.width(6.dp))
                Tile(padded[1], ctx, GlanceModifier.defaultWeight().fillMaxHeight())
            }
            Spacer(GlanceModifier.height(6.dp))
            Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                Tile(padded[2], ctx, GlanceModifier.defaultWeight().fillMaxHeight())
                Spacer(GlanceModifier.width(6.dp))
                Tile(padded[3], ctx, GlanceModifier.defaultWeight().fillMaxHeight())
            }
        }
    }

    @Composable
    private fun Tile(kind: QuickLaunchKind, context: Context, modifier: GlanceModifier) {
        val (a, b) = paletteFor(kind)
        Box(
            modifier = modifier
                .background(a)
                .cornerRadius(12.dp)
                .clickable(QuickLaunchActions.launchAction(context, kind))
                .padding(10.dp),
            contentAlignment = Alignment.BottomStart,
        ) {
            Column {
                Text(
                    text = "// ${eyebrow(kind)}",
                    style = TextStyle(
                        color = ColorProvider(b),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
                Spacer(GlanceModifier.height(2.dp))
                Text(
                    text = kind.label,
                    maxLines = 2,
                    style = TextStyle(
                        color = ColorProvider(Color.White),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        }
    }

    private fun eyebrow(kind: QuickLaunchKind): String = when (kind) {
        QuickLaunchKind.DISCOVER_DAILY -> "MIX"
        QuickLaunchKind.ON_REPEAT -> "ROTATION"
        QuickLaunchKind.LIKED -> "LIKED"
        QuickLaunchKind.RELEASE_RADAR -> "DROP"
        QuickLaunchKind.DAILY_MIX_1, QuickLaunchKind.DAILY_MIX_2, QuickLaunchKind.DAILY_MIX_3 -> "MIX"
        QuickLaunchKind.TIME_CAPSULE -> "CAPSULE"
    }

    private fun paletteFor(kind: QuickLaunchKind): Pair<Color, Color> = when (kind) {
        QuickLaunchKind.DISCOVER_DAILY -> Color(0xFF1E3A8A) to Color(0xFF06B6D4)
        QuickLaunchKind.ON_REPEAT -> Color(0xFF1A1A1A) to Color(0xFFD4FF3F)
        QuickLaunchKind.LIKED -> Color(0xFF3A0CA3) to Color(0xFFF72585)
        QuickLaunchKind.RELEASE_RADAR -> Color(0xFFD4FF3F) to Color(0xFF0A0A0A)
        QuickLaunchKind.DAILY_MIX_1 -> Color(0xFF1E3A8A) to Color(0xFF06B6D4)
        QuickLaunchKind.DAILY_MIX_2 -> Color(0xFF06B6D4) to Color(0xFF1E3A8A)
        QuickLaunchKind.DAILY_MIX_3 -> Color(0xFF3A0CA3) to Color(0xFFF72585)
        QuickLaunchKind.TIME_CAPSULE -> Color(0xFFF0A6B0) to Color(0xFF3A1F8A)
    }
}
