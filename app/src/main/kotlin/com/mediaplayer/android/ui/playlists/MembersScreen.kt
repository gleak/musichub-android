package com.mediaplayer.android.ui.playlists

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mediaplayer.android.data.PlaylistRepository
import com.mediaplayer.android.data.dto.PlaylistMemberDto
import com.mediaplayer.android.ui.common.CenteredSpinner
import com.mediaplayer.android.ui.common.ErrorWithRetry
import com.mediaplayer.android.ui.common.MHCaptionHeader
import com.mediaplayer.android.ui.common.friendlyMessage
import com.mediaplayer.android.ui.theme.LocalMHMono
import com.mediaplayer.android.ui.theme.MHColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface MembersUiState {
    data object Loading : MembersUiState
    data class Success(val members: List<PlaylistMemberDto>) : MembersUiState
    data class Error(val message: String) : MembersUiState
}

class MembersViewModel(
    private val playlistId: Long,
    private val repository: PlaylistRepository = PlaylistRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow<MembersUiState>(MembersUiState.Loading)
    val state: StateFlow<MembersUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = MembersUiState.Loading
            _state.value = try {
                MembersUiState.Success(repository.listMembers(playlistId))
            } catch (t: Throwable) {
                MembersUiState.Error(friendlyMessage(t))
            }
        }
    }

    fun kick(userId: Long, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val ok = try { repository.kickMember(playlistId, userId); true }
                     catch (_: Throwable) { false }
            if (ok) refresh()
            onResult(ok)
        }
    }
}

@Composable
fun MembersScreen(
    playlistId: Long,
    isOwnerView: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: MembersViewModel = viewModel(
        key = "members-$playlistId",
        factory = viewModelFactory { initializer { MembersViewModel(playlistId) } },
    )
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var snackMsg by remember { mutableStateOf<String?>(null) }
    var pendingKick by remember { mutableStateOf<PlaylistMemberDto?>(null) }

    LaunchedEffect(snackMsg) {
        val m = snackMsg ?: return@LaunchedEffect
        snackbar.showSnackbar(m)
        snackMsg = null
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val count = (state as? MembersUiState.Success)?.members?.size ?: 0
            MHCaptionHeader(
                eyebrow = "PLAYLIST · MEMBRI",
                title = "Membri",
                count = count.takeIf { it > 0 },
                onBack = onBack,
            )
            when (val s = state) {
                MembersUiState.Loading -> CenteredSpinner()
                is MembersUiState.Error -> ErrorWithRetry(s.message, viewModel::refresh)
                is MembersUiState.Success -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(items = s.members, key = { it.userId }) { m ->
                        MemberRow(
                            member = m,
                            canKick = isOwnerView && !m.owner,
                            onKick = { pendingKick = m },
                        )
                    }
                }
            }
        }
    }

    val target = pendingKick
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingKick = null },
            title = { Text("Rimuovere ${target.name}?") },
            text = {
                Text(
                    "${target.name} non potrà più aprire la playlist né vedere le tue modifiche. " +
                        "Resta nella tua libreria; può rientrare solo se gli condividi un nuovo link."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingKick = null
                    viewModel.kick(target.userId) { ok ->
                        snackMsg = if (ok) "${target.name} rimosso" else "Impossibile rimuovere ${target.name}"
                    }
                }) { Text("Rimuovi", color = Color(0xFFFF7A7A), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingKick = null }) { Text("Annulla") }
            },
        )
    }
}

@Composable
private fun MemberRow(
    member: PlaylistMemberDto,
    canKick: Boolean,
    onKick: () -> Unit,
) {
    val mono = LocalMHMono.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(avatarColor(member.userId)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = member.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                color = Color(0xFF0A0A0A),
                fontWeight = FontWeight.Bold,
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = member.name,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = if (member.owner) "Proprietario" else "Membro",
                style = mono.duration.copy(
                    color = if (member.owner) MHColors.Lime
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
        if (canKick) {
            IconButton(onClick = onKick) {
                Icon(
                    imageVector = Icons.Filled.PersonRemove,
                    contentDescription = "Rimuovi membro",
                    tint = Color(0xFFFF7A7A),
                )
            }
        }
    }
}

private val AvatarPalette = listOf(
    Color(0xFF5C2D8C), Color(0xFFFF6B5B), Color(0xFF06B6D4), Color(0xFFFFC857),
    Color(0xFFA8E04E), Color(0xFFF72585),
)

private fun avatarColor(seed: Long): Color {
    val idx = (seed.hashCode() and 0x7FFFFFFF) % AvatarPalette.size
    return AvatarPalette[idx]
}
