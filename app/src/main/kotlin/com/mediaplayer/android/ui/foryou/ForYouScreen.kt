package com.mediaplayer.android.ui.foryou

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mediaplayer.android.data.dto.PlaylistDto
import com.mediaplayer.android.ui.common.AutoPlaylistFamily
import com.mediaplayer.android.ui.common.CenteredSpinner
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.EyebrowText
import com.mediaplayer.android.ui.common.GeneratedCover
import com.mediaplayer.android.ui.common.SectionHeader
import com.mediaplayer.android.ui.common.badgeFor
import com.mediaplayer.android.ui.common.familyOf
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import com.mediaplayer.android.ui.theme.MHGradient
import com.mediaplayer.android.ui.theme.MediaPlayerSpacing

@Composable
fun ForYouScreen(
    onPlaylistClick: (PlaylistDto) -> Unit = {},
    viewModel: ForYouViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MHGradient.heroBg(Color(0xFF1A2010))),
    ) {
        when (val s = state) {
            ForYouUiState.Loading -> CenteredSpinner()
            is ForYouUiState.Error -> ErrorWithRetry(s.message, viewModel::refresh)
            is ForYouUiState.Ready -> ForYouContent(s.autoPlaylists, onPlaylistClick)
        }
    }
}

@Composable
private fun ForYouContent(
    autoPlaylists: List<PlaylistDto>,
    onPlaylistClick: (PlaylistDto) -> Unit,
) {
    val mono = LocalMHMono.current
    val rotation = autoPlaylists.firstOrNull { familyOf(it.kind) == AutoPlaylistFamily.Rotation }
    val mixes = autoPlaylists.filter { familyOf(it.kind) == AutoPlaylistFamily.Daily }
    val weekly = autoPlaylists.filter {
        familyOf(it.kind) in setOf(
            AutoPlaylistFamily.Releases,
            AutoPlaylistFamily.Radar,
            AutoPlaylistFamily.Capsule,
        )
    }
    val context = autoPlaylists.filter {
        familyOf(it.kind) in setOf(AutoPlaylistFamily.Mood, AutoPlaylistFamily.Next)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 16.dp, bottom = 24.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = MediaPlayerSpacing.M)) {
                EyebrowText(text = "Generata dal sistema")
                Text(
                    text = "Per te",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MHColors.TextHi,
                )
                Text(
                    text = "${autoPlaylists.size} playlist · aggiornate oggi",
                    style = mono.caption.copy(color = MHColors.TextLo),
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        if (rotation != null) {
            item { RotationHero(rotation, onClick = { onPlaylistClick(rotation) }) }
        }

        if (mixes.isNotEmpty()) {
            item {
                Spacer(Modifier.height(24.dp))
                SectionHeader(eyebrow = "6 mix", title = "I tuoi mix giornalieri")
            }
            item {
                MixGrid(mixes, onClick = onPlaylistClick)
            }
        }

        if (weekly.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader(eyebrow = "Settimanali", title = "Aggiornamenti")
            }
            item {
                LazyRow(
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = MediaPlayerSpacing.M),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    items(weekly, key = { it.id }) { pl ->
                        WeeklyCard(pl, onClick = { onPlaylistClick(pl) })
                    }
                }
            }
        }

        if (context.isNotEmpty()) {
            item {
                Spacer(Modifier.height(20.dp))
                SectionHeader(eyebrow = "Contesto", title = "Adesso e in poi")
            }
            items(context, key = { it.id }) { pl ->
                ContextRow(pl, onClick = { onPlaylistClick(pl) })
            }
        }

        item {
            Spacer(Modifier.height(20.dp))
            HowItWorksCard()
            Spacer(Modifier.height(8.dp))
            Text(
                text = "— FINE —",
                style = mono.duration.copy(color = Color.White.copy(alpha = 0.25f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun RotationHero(pl: PlaylistDto, onClick: () -> Unit) {
    val mono = LocalMHMono.current
    Box(
        modifier = Modifier
            .padding(horizontal = MediaPlayerSpacing.M)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MHColors.Lime.copy(alpha = 0.10f))
            .border(1.5.dp, MHColors.Lime.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GeneratedCover(
                family = AutoPlaylistFamily.Rotation,
                badge = badgeFor(pl.kind),
                modifier = Modifier.size(100.dp),
                cornerRadius = 8.dp,
            )
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "// IN ROTAZIONE",
                    style = mono.eyebrow.copy(color = MHColors.Lime),
                )
                Text(
                    text = pl.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MHColors.TextHi,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Text(
                    text = "${pl.songCount} brani · aggiornata oggi",
                    style = MaterialTheme.typography.bodySmall,
                    color = MHColors.TextLo,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick = onClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MHColors.Lime,
                        contentColor = Color(0xFF0A0A0A),
                    ),
                    shape = RoundedCornerShape(999.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("RIPRODUCI", fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun MixGrid(mixes: List<PlaylistDto>, onClick: (PlaylistDto) -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = MediaPlayerSpacing.M, vertical = 8.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        mixes.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { pl ->
                    MixTile(pl, onClick = { onClick(pl) }, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun MixTile(pl: PlaylistDto, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.clickable(onClick = onClick)) {
        GeneratedCover(
            family = AutoPlaylistFamily.Daily,
            badge = badgeFor(pl.kind),
            subtitle = pl.name,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            cornerRadius = 10.dp,
        )
        Text(
            text = pl.name,
            color = MHColors.TextHi,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${pl.songCount} brani",
            color = MHColors.TextLo,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun WeeklyCard(pl: PlaylistDto, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(170.dp)
            .clickable(onClick = onClick),
    ) {
        GeneratedCover(
            family = familyOf(pl.kind),
            badge = badgeFor(pl.kind),
            modifier = Modifier
                .size(170.dp),
            cornerRadius = 10.dp,
        )
        Text(
            text = pl.name,
            color = MHColors.TextHi,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${pl.songCount} brani",
            color = MHColors.TextLo,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun ContextRow(pl: PlaylistDto, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .padding(horizontal = MediaPlayerSpacing.M, vertical = 4.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.04f))
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GeneratedCover(
            family = familyOf(pl.kind),
            badge = badgeFor(pl.kind),
            modifier = Modifier.size(56.dp),
            cornerRadius = 6.dp,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pl.name,
                color = MHColors.TextHi,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "${pl.songCount} brani",
                color = MHColors.TextLo,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun HowItWorksCard() {
    val mono = LocalMHMono.current
    Column(
        modifier = Modifier
            .padding(horizontal = MediaPlayerSpacing.M)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .border(1.dp, MHColors.Divider, RoundedCornerShape(12.dp))
            .padding(14.dp),
    ) {
        Text(
            text = "// COME FUNZIONA",
            style = mono.eyebrow,
        )
        Text(
            text = "Le playlist sono generate ogni giorno dal motore di MusicHub " +
                "analizzando i tuoi ascolti, gli artisti che segui e il momento " +
                "della giornata. Più ascolti, più diventano accurate.",
            style = MaterialTheme.typography.bodySmall,
            color = MHColors.TextLo,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
