package com.mediaplayer.android.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.media3.common.util.UnstableApi
import com.mediaplayer.android.MainActivity
import com.mediaplayer.android.R
import android.content.Context

/**
 * Now-Playing home-screen widget. Renders the active track's cover, title,
 * artist plus prev / play-pause / next transport buttons. State is
 * mirrored from [WidgetState] which the playback service keeps fresh.
 *
 * Tap on cover or text opens [MainActivity] (singleTask launchMode keeps
 * the existing app instance + restores its back stack). Transport buttons
 * fire [NowPlayingActions] callbacks that bind a transient
 * `MediaController` and forward the command to the playback service.
 */
@UnstableApi
class NowPlayingWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Single

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { Content() }
    }

    @Composable
    private fun Content() {
        val s by WidgetState.now.collectAsState()
        val hasSong = s.songId != null && s.title.isNotBlank()
        val open: Action = actionStartActivity<MainActivity>()

        Box(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(Color(0xFF0A0A0A))
                .cornerRadius(16.dp)
                .padding(10.dp)
                .clickable(open),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Cover(s)
                Spacer(GlanceModifier.width(10.dp))
                Column(
                    modifier = GlanceModifier.defaultWeight(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (hasSong) s.title else "Nessun brano",
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(GlanceModifier.height(2.dp))
                    Text(
                        text = if (hasSong) s.artist else "Tocca per aprire",
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFB7B7B7)),
                            fontSize = 12.sp,
                        ),
                    )
                    Spacer(GlanceModifier.height(8.dp))
                    TransportRow(s)
                }
            }
        }
    }

    @Composable
    private fun Cover(s: NowPlayingSnapshot) {
        val provider = s.cover?.let { ImageProvider(it) }
            ?: ImageProvider(R.drawable.ic_widget_cover_placeholder)
        Image(
            provider = provider,
            contentDescription = s.title.ifBlank { "Copertina" },
            contentScale = ContentScale.Crop,
            modifier = GlanceModifier.size(64.dp).cornerRadius(10.dp),
        )
    }

    @Composable
    private fun TransportRow(s: NowPlayingSnapshot) {
        val playPauseRes =
            if (s.isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
        Row(verticalAlignment = Alignment.CenterVertically) {
            TransportButton(
                R.drawable.ic_widget_skip_previous, "Precedente",
                enabled = s.hasPrevious,
                action = actionRunCallback<PreviousAction>(),
            )
            Spacer(GlanceModifier.width(4.dp))
            TransportButton(
                playPauseRes,
                if (s.isPlaying) "Pausa" else "Riproduci",
                enabled = s.songId != null,
                action = actionRunCallback<PlayPauseAction>(),
                accent = true,
            )
            Spacer(GlanceModifier.width(4.dp))
            TransportButton(
                R.drawable.ic_widget_skip_next, "Successivo",
                enabled = s.hasNext,
                action = actionRunCallback<NextAction>(),
            )
        }
    }

    @Composable
    private fun TransportButton(
        iconRes: Int,
        cd: String,
        enabled: Boolean,
        action: Action,
        accent: Boolean = false,
    ) {
        val bg = when {
            !enabled -> Color(0xFF1A1A1A)
            accent -> Color(0xFFD4FF3F)
            else -> Color(0xFF1F1F1F)
        }
        val tint = if (accent) Color(0xFF0A0A0A) else Color.White
        Box(
            modifier = GlanceModifier
                .size(34.dp)
                .background(bg)
                .cornerRadius(17.dp)
                .clickable(action),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                provider = ImageProvider(iconRes),
                contentDescription = cd,
                colorFilter = ColorFilter.tint(ColorProvider(tint)),
                modifier = GlanceModifier.size(18.dp),
            )
        }
    }
}
