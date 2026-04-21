package com.mediaplayer.android.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.SongDto

/**
 * Browse tree exposed to Android Auto (and any other `MediaBrowser`).
 *
 * Tree shape:
 * ```
 * root
 * ├── all-songs      (first page of the catalog)
 * └── playlists
 *     └── playlist:{id}   (ordered songs; duplicates allowed)
 * ```
 *
 * mediaId scheme (stable — AA caches them between sessions):
 * - `root` / `all-songs` / `playlists`                 — fixed folders
 * - `song:{songId}`                                    — song leaf under all-songs
 * - `playlist:{playlistId}`                            — playlist folder
 * - `pl:{playlistId}:{position}:{songId}`              — song leaf inside a playlist
 *
 * The positional compound id on playlist entries is deliberate: the same
 * songId may appear twice in a playlist (Spotify-style duplicates), and
 * `MediaBrowser` requires unique ids per parent.
 */
internal object LibraryTree {

    const val ROOT_ID = "root"
    const val ALL_SONGS_ID = "all-songs"
    const val PLAYLISTS_ID = "playlists"

    private const val PLAYLIST_PREFIX = "playlist:"
    private const val SONG_PREFIX = "song:"
    private const val PL_LEAF_PREFIX = "pl:"

    /** Used by all-songs page fetches. AA typically shows ~50 at a time. */
    private const val PAGE_SIZE = 50

    // --- public API ----------------------------------------------------------

    suspend fun root(): MediaItem = browsable(ROOT_ID, "MediaPlayer")

    /** Children of [parentId], or `null` if the id is unrecognised. */
    suspend fun children(parentId: String): List<MediaItem>? = when {
        parentId == ROOT_ID -> listOf(
            browsable(ALL_SONGS_ID, "All songs"),
            browsable(PLAYLISTS_ID, "Playlists"),
        )
        parentId == ALL_SONGS_ID -> allSongs()
        parentId == PLAYLISTS_ID -> playlists()
        parentId.startsWith(PLAYLIST_PREFIX) ->
            parentId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()?.let { playlistSongs(it) }
        else -> null
    }

    /** Item lookup for resume / deep-link requests. */
    suspend fun item(mediaId: String): MediaItem? = when {
        mediaId == ROOT_ID -> root()
        mediaId == ALL_SONGS_ID -> browsable(ALL_SONGS_ID, "All songs")
        mediaId == PLAYLISTS_ID -> browsable(PLAYLISTS_ID, "Playlists")
        mediaId.startsWith(SONG_PREFIX) ->
            mediaId.removePrefix(SONG_PREFIX).toLongOrNull()?.let { songLeaf(it) }
        mediaId.startsWith(PLAYLIST_PREFIX) -> {
            val id = mediaId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
            id?.let {
                val detail = Network.api.getPlaylist(it)
                browsable(mediaId, detail.name)
            }
        }
        mediaId.startsWith(PL_LEAF_PREFIX) -> parsePlaylistLeaf(mediaId)?.let { (_, _, sid) ->
            songLeaf(sid)
        }
        else -> null
    }

    /** Voice-search proxy against the backend `/api/songs?q=` endpoint. */
    suspend fun search(query: String): List<MediaItem> {
        val page = Network.api.listSongs(query = query, page = 0, size = PAGE_SIZE)
        return page.items.map { songLeaf(it) }
    }

    /**
     * Parses a playlist leaf id and returns `(playlistId, position, songId)`
     * or `null` if it isn't that shape.
     */
    fun parsePlaylistLeaf(mediaId: String): Triple<Long, Int, Long>? {
        if (!mediaId.startsWith(PL_LEAF_PREFIX)) return null
        val parts = mediaId.removePrefix(PL_LEAF_PREFIX).split(":")
        if (parts.size != 3) return null
        val pid = parts[0].toLongOrNull() ?: return null
        val pos = parts[1].toIntOrNull() ?: return null
        val sid = parts[2].toLongOrNull() ?: return null
        return Triple(pid, pos, sid)
    }

    /**
     * Resolves a whole playlist to playable `MediaItem`s (with stream URIs
     * set), indexed by position. Used by `onSetMediaItems` when AA taps a
     * playlist entry — we expand the single tap into the full queue.
     */
    suspend fun playlistQueue(playlistId: Long): List<MediaItem> {
        val detail = Network.api.getPlaylist(playlistId)
        return detail.songs.map { playableSong(it) }
    }

    /** Single playable `MediaItem` for a standalone song tap (all-songs). */
    suspend fun playableForSong(songId: Long): MediaItem {
        // AA tapped a single song under /all-songs — use the stub's tags if
        // the browse cache warmed them; otherwise fetch fresh metadata via a
        // size-1 search by id is impossible (no endpoint), so we return a
        // minimal playable item and the player will still stream correctly.
        // Tags will be replaced by whatever the controller had staged.
        return MediaItem.Builder()
            .setMediaId("$SONG_PREFIX$songId")
            .setUri(Network.streamUrl(songId))
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setArtworkUri(Uri.parse(Network.coverUrl(songId)))
                    .build()
            )
            .build()
    }

    // --- internals -----------------------------------------------------------

    private suspend fun allSongs(): List<MediaItem> {
        val page = Network.api.listSongs(query = null, page = 0, size = PAGE_SIZE)
        return page.items.map { songLeaf(it) }
    }

    private suspend fun playlists(): List<MediaItem> =
        Network.api.listPlaylists().map { pl ->
            browsable(
                mediaId = "$PLAYLIST_PREFIX${pl.id}",
                title = pl.name,
                subtitle = "${pl.songCount} song${if (pl.songCount == 1) "" else "s"}",
            )
        }

    private suspend fun playlistSongs(playlistId: Long): List<MediaItem> {
        val detail = Network.api.getPlaylist(playlistId)
        return detail.songs.mapIndexed { index, song ->
            // Browse-side leaf: NOT playable as-is (no URI). Playback goes
            // through MediaLibrarySession.Callback.onSetMediaItems, which
            // recognises the `pl:` prefix and expands into the full queue
            // starting at this position.
            MediaItem.Builder()
                .setMediaId("$PL_LEAF_PREFIX$playlistId:$index:${song.id}")
                .setMediaMetadata(song.asBrowseMetadata(playable = true))
                .build()
        }
    }

    /** Browse leaf for a song that plays as a single item (under all-songs). */
    private fun songLeaf(song: SongDto): MediaItem =
        MediaItem.Builder()
            .setMediaId("$SONG_PREFIX${song.id}")
            .setMediaMetadata(song.asBrowseMetadata(playable = true))
            .build()

    /** Browse leaf for a song we only know by id (voice-search result click). */
    private fun songLeaf(songId: Long): MediaItem =
        MediaItem.Builder()
            .setMediaId("$SONG_PREFIX$songId")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                    .setArtworkUri(Uri.parse(Network.coverUrl(songId)))
                    .build()
            )
            .build()

    /** Full playable MediaItem including the stream URI — used for queues. */
    private fun playableSong(song: SongDto): MediaItem =
        MediaItem.Builder()
            .setMediaId("$SONG_PREFIX${song.id}")
            .setUri(Network.streamUrl(song.id))
            .setMediaMetadata(song.asBrowseMetadata(playable = true))
            .build()

    private fun browsable(mediaId: String, title: String, subtitle: String? = null): MediaItem =
        MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .setTitle(title)
                    .apply { if (subtitle != null) setSubtitle(subtitle) }
                    .build()
            )
            .build()

    private fun SongDto.asBrowseMetadata(playable: Boolean): MediaMetadata =
        MediaMetadata.Builder()
            .setIsBrowsable(false)
            .setIsPlayable(playable)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setTitle(title)
            .setArtist(artist)
            .apply { if (album != null) setAlbumTitle(album) }
            .setArtworkUri(Uri.parse(Network.coverUrl(id)))
            .build()
}
