package com.mediaplayer.android.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.mediaplayer.android.data.Network
import com.mediaplayer.android.data.dto.AlbumDto
import com.mediaplayer.android.data.dto.ArtistDto
import com.mediaplayer.android.data.dto.SongDto

/**
 * Browse tree exposed to Android Auto (and any other `MediaBrowser`).
 *
 * Tree shape mirrors the phone Spotify-style UI:
 * ```
 * root
 * ├── recents      (last played, list)
 * ├── liked        (heart collection, list)
 * ├── playlists    (grid)
 * │   └── playlist:{id}
 * ├── albums       (grid)
 * │   └── album:{nameEnc}|{artistEnc}
 * ├── artists      (list)
 * │   └── artist:{nameEnc}
 * │       ├── (artist's songs, list)
 * │       └── album:{nameEnc}|{artistEnc}  (cross-link)
 * └── all-songs    (list)
 * ```
 *
 * mediaId scheme (stable — AA caches them between sessions):
 * - `root` / `recents` / `liked` / `playlists` / `albums` / `artists` /
 *   `all-songs` / `lyrics`                              — fixed folders
 * - `song:{songId}`                                     — generic playable leaf
 * - `playlist:{playlistId}`                             — playlist folder
 * - `pl:{playlistId}:{position}:{songId}`               — leaf inside playlist
 * - `album:{nameEnc}|{artistEnc}`                       — album folder
 * - `al:{nameEnc}|{artistEnc}|{position}|{songId}`      — leaf inside album
 * - `artist:{nameEnc}`                                  — artist folder
 * - `ar:{nameEnc}|{position}|{songId}`                  — leaf inside artist
 * - `lk:{position}|{songId}`                            — leaf inside liked
 * - `rc:{position}|{songId}`                            — leaf inside recents
 *
 * Positional compound ids on collection entries are deliberate: the same
 * songId may appear twice in a playlist (Spotify-style duplicates), and
 * `MediaBrowser` requires unique ids per parent. Positional ids also let
 * `onSetMediaItems` expand a single tap into the full queue starting at
 * the chosen position — matches phone behaviour.
 */
internal object LibraryTree {

    const val ROOT_ID = "root"
    const val ALL_SONGS_ID = "all-songs"
    const val PLAYLISTS_ID = "playlists"
    const val LYRICS_ID = "lyrics"
    const val LIKED_ID = "liked"
    const val RECENTS_ID = "recents"
    const val ALBUMS_ID = "albums"
    const val ARTISTS_ID = "artists"

    private const val PLAYLIST_PREFIX = "playlist:"
    private const val SONG_PREFIX = "song:"
    private const val PL_LEAF_PREFIX = "pl:"
    private const val ALBUM_PREFIX = "album:"
    private const val ARTIST_PREFIX = "artist:"
    private const val ALBUM_LEAF_PREFIX = "al:"
    private const val ARTIST_LEAF_PREFIX = "ar:"
    private const val LIKED_LEAF_PREFIX = "lk:"
    private const val RECENT_LEAF_PREFIX = "rc:"

    /** Used by paged catalog fetches. AA typically shows ~50 at a time. */
    private const val PAGE_SIZE = 50
    private const val RECENT_LIMIT = 30
    private const val LIKED_LIMIT = 100

    // --- Android Auto content-style hints -----------------------------------
    //
    // Documented constants from the AA browse spec. Setting CONTENT_STYLE_*
    // on the parent folder's metadata tells AA how to render its children.
    // GRID is the right shape for playlists/albums (covers); LIST for songs.

    private const val CONTENT_STYLE_SUPPORTED =
        "android.media.browse.CONTENT_STYLE_SUPPORTED"
    private const val CONTENT_STYLE_BROWSABLE_HINT =
        "android.media.browse.CONTENT_STYLE_BROWSABLE_HINT"
    private const val CONTENT_STYLE_PLAYABLE_HINT =
        "android.media.browse.CONTENT_STYLE_PLAYABLE_HINT"
    private const val CONTENT_STYLE_LIST = 1
    private const val CONTENT_STYLE_GRID = 2

    /** Bundle returned in LibraryParams root so AA enables per-folder hints. */
    fun rootExtras(): Bundle = Bundle().apply {
        putBoolean(CONTENT_STYLE_SUPPORTED, true)
        putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST)
        putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
    }

    // --- public API ----------------------------------------------------------

    suspend fun root(): MediaItem = browsable(ROOT_ID, "MediaPlayer")

    /** Children of [parentId], or `null` if the id is unrecognised. */
    suspend fun children(parentId: String, currentSongId: Long? = null): List<MediaItem>? = when {
        parentId == ROOT_ID -> rootChildren(currentSongId)
        parentId == ALL_SONGS_ID -> allSongs(currentSongId)
        parentId == PLAYLISTS_ID -> playlists()
        parentId == LIKED_ID -> liked(currentSongId)
        parentId == RECENTS_ID -> recents(currentSongId)
        parentId == ALBUMS_ID -> albums()
        parentId == ARTISTS_ID -> artists()
        parentId == LYRICS_ID -> lyricsFor(currentSongId)
        parentId.startsWith(PLAYLIST_PREFIX) ->
            parentId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
                ?.let { playlistSongs(it, currentSongId) }
        parentId.startsWith(ALBUM_PREFIX) ->
            decodeAlbumKey(parentId.removePrefix(ALBUM_PREFIX))
                ?.let { (name, artist) -> albumSongs(name, artist, currentSongId) }
        parentId.startsWith(ARTIST_PREFIX) ->
            artistChildren(decodePart(parentId.removePrefix(ARTIST_PREFIX)), currentSongId)
        else -> null
    }

    /** Item lookup for resume / deep-link requests. */
    suspend fun item(mediaId: String): MediaItem? = when {
        mediaId == ROOT_ID -> root()
        mediaId == ALL_SONGS_ID -> sectionFolder(ALL_SONGS_ID, "All songs", grid = false)
        mediaId == PLAYLISTS_ID -> sectionFolder(PLAYLISTS_ID, "Playlists", grid = true)
        mediaId == LIKED_ID -> sectionFolder(LIKED_ID, "Liked Songs", grid = false)
        mediaId == RECENTS_ID -> sectionFolder(RECENTS_ID, "Recently Played", grid = false)
        mediaId == ALBUMS_ID -> sectionFolder(ALBUMS_ID, "Albums", grid = true)
        mediaId == ARTISTS_ID -> sectionFolder(ARTISTS_ID, "Artists", grid = false)
        mediaId == LYRICS_ID -> browsable(LYRICS_ID, "Lyrics")
        mediaId.startsWith(SONG_PREFIX) ->
            mediaId.removePrefix(SONG_PREFIX).toLongOrNull()?.let { songLeaf(it) }
        mediaId.startsWith(PLAYLIST_PREFIX) -> {
            val id = mediaId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
            id?.let {
                val detail = Network.api.getPlaylist(it)
                folderTile(mediaId, detail.name,
                    subtitle = "${detail.songs.size} songs",
                    type = MediaMetadata.MEDIA_TYPE_PLAYLIST,
                    artworkSongId = detail.songs.firstOrNull()?.id,
                    grid = false)
            }
        }
        mediaId.startsWith(ALBUM_PREFIX) -> {
            decodeAlbumKey(mediaId.removePrefix(ALBUM_PREFIX))?.let { (name, artist) ->
                folderTile(mediaId, name,
                    subtitle = artist,
                    type = MediaMetadata.MEDIA_TYPE_ALBUM,
                    artworkSongId = null,
                    grid = false)
            }
        }
        mediaId.startsWith(ARTIST_PREFIX) -> {
            val name = decodePart(mediaId.removePrefix(ARTIST_PREFIX))
            folderTile(mediaId, name,
                subtitle = null,
                type = MediaMetadata.MEDIA_TYPE_ARTIST,
                artworkSongId = null,
                grid = false)
        }
        mediaId.startsWith(PL_LEAF_PREFIX) ->
            parsePlaylistLeaf(mediaId)?.let { (_, _, sid) -> songLeaf(sid) }
        mediaId.startsWith(ALBUM_LEAF_PREFIX) ->
            parseAlbumLeaf(mediaId)?.fourth()?.let { sid -> songLeaf(sid) }
        mediaId.startsWith(ARTIST_LEAF_PREFIX) ->
            parseArtistLeaf(mediaId)?.third?.let { sid -> songLeaf(sid) }
        mediaId.startsWith(LIKED_LEAF_PREFIX) ->
            parseSimpleLeaf(mediaId, LIKED_LEAF_PREFIX)?.second?.let { sid -> songLeaf(sid) }
        mediaId.startsWith(RECENT_LEAF_PREFIX) ->
            parseSimpleLeaf(mediaId, RECENT_LEAF_PREFIX)?.second?.let { sid -> songLeaf(sid) }
        else -> null
    }

    /** Voice-search proxy against the backend `/api/songs?q=` endpoint. */
    suspend fun search(query: String): List<MediaItem> {
        val page = Network.api.listSongs(query = query, page = 0, size = PAGE_SIZE)
        return page.items.map { songLeaf(it) }
    }

    // --- mediaId parsers -----------------------------------------------------

    /** Parses `pl:{pid}:{pos}:{sid}` into `(playlistId, position, songId)`. */
    fun parsePlaylistLeaf(mediaId: String): Triple<Long, Int, Long>? {
        if (!mediaId.startsWith(PL_LEAF_PREFIX)) return null
        val parts = mediaId.removePrefix(PL_LEAF_PREFIX).split(":")
        if (parts.size != 3) return null
        val pid = parts[0].toLongOrNull() ?: return null
        val pos = parts[1].toIntOrNull() ?: return null
        val sid = parts[2].toLongOrNull() ?: return null
        return Triple(pid, pos, sid)
    }

    /** Parses `al:{nameEnc}|{artistEnc}|{pos}|{sid}`. */
    fun parseAlbumLeaf(mediaId: String): Quadruple<String, String, Int, Long>? {
        if (!mediaId.startsWith(ALBUM_LEAF_PREFIX)) return null
        val parts = mediaId.removePrefix(ALBUM_LEAF_PREFIX).split("|")
        if (parts.size != 4) return null
        val pos = parts[2].toIntOrNull() ?: return null
        val sid = parts[3].toLongOrNull() ?: return null
        return Quadruple(decodePart(parts[0]), decodePart(parts[1]), pos, sid)
    }

    /** Parses `ar:{nameEnc}|{pos}|{sid}` into `(artistName, pos, songId)`. */
    fun parseArtistLeaf(mediaId: String): Triple<String, Int, Long>? {
        if (!mediaId.startsWith(ARTIST_LEAF_PREFIX)) return null
        val parts = mediaId.removePrefix(ARTIST_LEAF_PREFIX).split("|")
        if (parts.size != 3) return null
        val pos = parts[1].toIntOrNull() ?: return null
        val sid = parts[2].toLongOrNull() ?: return null
        return Triple(decodePart(parts[0]), pos, sid)
    }

    /** Parses leaves of shape `{prefix}{pos}|{sid}` (liked, recents). */
    fun parseSimpleLeaf(mediaId: String, prefix: String): Pair<Int, Long>? {
        if (!mediaId.startsWith(prefix)) return null
        val parts = mediaId.removePrefix(prefix).split("|")
        if (parts.size != 2) return null
        val pos = parts[0].toIntOrNull() ?: return null
        val sid = parts[1].toLongOrNull() ?: return null
        return Pair(pos, sid)
    }

    // --- queue resolvers (called by onSetMediaItems) -------------------------

    suspend fun playlistQueue(playlistId: Long): List<MediaItem> {
        val detail = Network.api.getPlaylist(playlistId)
        return detail.songs.map { playableSong(it) }
    }

    suspend fun albumQueue(name: String, artist: String): List<MediaItem> {
        val detail = Network.api.getAlbum(name, artist)
        return detail.songs.map { playableSong(it) }
    }

    suspend fun artistQueue(name: String): List<MediaItem> {
        val detail = Network.api.getArtist(name)
        return detail.songs.map { playableSong(it) }
    }

    suspend fun likedQueue(): List<MediaItem> =
        Network.api.getLikedSongs(page = 0, size = LIKED_LIMIT).items.map { playableSong(it) }

    suspend fun recentsQueue(): List<MediaItem> =
        Network.api.recentSongs(limit = RECENT_LIMIT).map { playableSong(it) }

    // --- lyrics --------------------------------------------------------------

    suspend fun lyrics(songId: Long): List<MediaItem> {
        return try {
            val lines = Network.api.getLyrics(songId)
            if (lines.isEmpty()) {
                listOf(infoItem("No lyrics found for this song"))
            } else {
                lines.mapIndexed { index, line ->
                    MediaItem.Builder()
                        .setMediaId("lyric:$songId:$index")
                        .setMediaMetadata(
                            MediaMetadata.Builder()
                                .setTitle(line.text)
                                .setIsBrowsable(false)
                                .setIsPlayable(false)
                                .build()
                        )
                        .build()
                }
            }
        } catch (e: Exception) {
            listOf(infoItem("Error loading lyrics"))
        }
    }

    private suspend fun lyricsFor(currentSongId: Long?): List<MediaItem> =
        if (currentSongId != null) lyrics(currentSongId)
        else listOf(infoItem("No song currently playing"))

    fun infoItem(message: String): MediaItem =
        MediaItem.Builder()
            .setMediaId("info:${message.hashCode()}")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(message)
                    .setIsBrowsable(false)
                    .setIsPlayable(false)
                    .build()
            )
            .build()

    /** Single playable `MediaItem` for a standalone song tap (search etc). */
    suspend fun playableForSong(songId: Long): MediaItem =
        MediaItem.Builder()
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

    // --- internals -----------------------------------------------------------

    private suspend fun rootChildren(currentSongId: Long?): List<MediaItem> {
        val list = mutableListOf(
            sectionFolder(RECENTS_ID, "Recently Played", grid = false,
                type = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
            sectionFolder(LIKED_ID, "Liked Songs", grid = false,
                type = MediaMetadata.MEDIA_TYPE_PLAYLIST),
            sectionFolder(PLAYLISTS_ID, "Playlists", grid = true,
                type = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
            sectionFolder(ALBUMS_ID, "Albums", grid = true,
                type = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
            sectionFolder(ARTISTS_ID, "Artists", grid = false,
                type = MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
            sectionFolder(ALL_SONGS_ID, "All Songs", grid = false,
                type = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
        )
        if (currentSongId != null) {
            list.add(infoItem("--- Lyrics ---"))
            list.addAll(lyrics(currentSongId))
        }
        return list
    }

    private suspend fun allSongs(currentSongId: Long?): List<MediaItem> {
        val page = Network.api.listSongs(query = null, page = 0, size = PAGE_SIZE)
        val list = mutableListOf<MediaItem>()
        for (song in page.items) {
            list.add(songLeaf(song))
            if (song.id == currentSongId) list.addAll(lyrics(song.id))
        }
        return list
    }

    private suspend fun playlists(): List<MediaItem> =
        Network.api.listPlaylists().map { pl ->
            folderTile(
                mediaId = "$PLAYLIST_PREFIX${pl.id}",
                title = pl.name,
                subtitle = "${pl.songCount} song${if (pl.songCount == 1) "" else "s"}",
                type = MediaMetadata.MEDIA_TYPE_PLAYLIST,
                artworkSongId = null,
                grid = true,
            )
        }

    private suspend fun playlistSongs(playlistId: Long, currentSongId: Long?): List<MediaItem> {
        val detail = Network.api.getPlaylist(playlistId)
        val list = mutableListOf<MediaItem>()
        for ((index, song) in detail.songs.withIndex()) {
            list.add(
                MediaItem.Builder()
                    .setMediaId("$PL_LEAF_PREFIX$playlistId:$index:${song.id}")
                    .setMediaMetadata(song.asBrowseMetadata(playable = true))
                    .build()
            )
            if (song.id == currentSongId) list.addAll(lyrics(song.id))
        }
        return list
    }

    private suspend fun albums(): List<MediaItem> {
        val page = Network.api.listAlbums(query = null, page = 0, size = PAGE_SIZE)
        return page.items.map { it.asTile() }
    }

    private suspend fun albumSongs(name: String, artist: String, currentSongId: Long?): List<MediaItem> {
        val detail = Network.api.getAlbum(name, artist)
        val list = mutableListOf<MediaItem>()
        val nameEnc = encodePart(detail.name)
        val artistEnc = encodePart(detail.artist)
        for ((index, song) in detail.songs.withIndex()) {
            list.add(
                MediaItem.Builder()
                    .setMediaId("$ALBUM_LEAF_PREFIX$nameEnc|$artistEnc|$index|${song.id}")
                    .setMediaMetadata(song.asBrowseMetadata(playable = true))
                    .build()
            )
            if (song.id == currentSongId) list.addAll(lyrics(song.id))
        }
        return list
    }

    private suspend fun artists(): List<MediaItem> {
        val page = Network.api.listArtists(query = null, page = 0, size = PAGE_SIZE)
        return page.items.map { it.asTile() }
    }

    private suspend fun artistChildren(name: String, currentSongId: Long?): List<MediaItem> {
        val detail = Network.api.getArtist(name)
        val list = mutableListOf<MediaItem>()
        // Albums first as grid tiles, then songs as list leaves.
        for (album in detail.albums) list.add(album.asTile())
        val nameEnc = encodePart(detail.name)
        for ((index, song) in detail.songs.withIndex()) {
            list.add(
                MediaItem.Builder()
                    .setMediaId("$ARTIST_LEAF_PREFIX$nameEnc|$index|${song.id}")
                    .setMediaMetadata(song.asBrowseMetadata(playable = true))
                    .build()
            )
            if (song.id == currentSongId) list.addAll(lyrics(song.id))
        }
        return list
    }

    private suspend fun liked(currentSongId: Long?): List<MediaItem> {
        val page = Network.api.getLikedSongs(page = 0, size = LIKED_LIMIT)
        val list = mutableListOf<MediaItem>()
        for ((index, song) in page.items.withIndex()) {
            list.add(
                MediaItem.Builder()
                    .setMediaId("$LIKED_LEAF_PREFIX$index|${song.id}")
                    .setMediaMetadata(song.asBrowseMetadata(playable = true))
                    .build()
            )
            if (song.id == currentSongId) list.addAll(lyrics(song.id))
        }
        if (list.isEmpty()) list.add(infoItem("No liked songs yet"))
        return list
    }

    private suspend fun recents(currentSongId: Long?): List<MediaItem> {
        val songs = Network.api.recentSongs(limit = RECENT_LIMIT)
        val list = mutableListOf<MediaItem>()
        for ((index, song) in songs.withIndex()) {
            list.add(
                MediaItem.Builder()
                    .setMediaId("$RECENT_LEAF_PREFIX$index|${song.id}")
                    .setMediaMetadata(song.asBrowseMetadata(playable = true))
                    .build()
            )
            if (song.id == currentSongId) list.addAll(lyrics(song.id))
        }
        if (list.isEmpty()) list.add(infoItem("Nothing played yet"))
        return list
    }

    /** Browse leaf for a song that plays as a single item (search results). */
    private fun songLeaf(song: SongDto): MediaItem =
        MediaItem.Builder()
            .setMediaId("$SONG_PREFIX${song.id}")
            .setMediaMetadata(song.asBrowseMetadata(playable = true))
            .build()

    /** Browse leaf for a song we only know by id (deep-link lookup). */
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

    private fun browsable(mediaId: String, title: String): MediaItem =
        MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle(title)
                    .build()
            )
            .build()

    /**
     * Top-level section folder. `grid` controls how AA renders this
     * folder's children (covers vs list rows).
     */
    private fun sectionFolder(
        mediaId: String,
        title: String,
        grid: Boolean,
        type: Int? = null,
    ): MediaItem {
        val extras = Bundle().apply {
            val hint = if (grid) CONTENT_STYLE_GRID else CONTENT_STYLE_LIST
            putInt(CONTENT_STYLE_BROWSABLE_HINT, hint)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, hint)
        }
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setTitle(title)
                    .apply { if (type != null) setMediaType(type) }
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    /**
     * Mid-level folder tile (a single playlist / album / artist). Carries
     * its own cover art when we have a representative song id, plus
     * content-style hints for its children.
     */
    private fun folderTile(
        mediaId: String,
        title: String,
        subtitle: String?,
        type: Int,
        artworkSongId: Long?,
        grid: Boolean,
    ): MediaItem {
        val extras = Bundle().apply {
            val hint = if (grid) CONTENT_STYLE_GRID else CONTENT_STYLE_LIST
            putInt(CONTENT_STYLE_BROWSABLE_HINT, hint)
            putInt(CONTENT_STYLE_PLAYABLE_HINT, hint)
        }
        return MediaItem.Builder()
            .setMediaId(mediaId)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(type)
                    .setTitle(title)
                    .apply {
                        if (subtitle != null) setSubtitle(subtitle)
                        if (artworkSongId != null) {
                            setArtworkUri(Uri.parse(Network.coverUrl(artworkSongId)))
                        }
                    }
                    .setExtras(extras)
                    .build()
            )
            .build()
    }

    private fun AlbumDto.asTile(): MediaItem = folderTile(
        mediaId = "$ALBUM_PREFIX${encodePart(name)}|${encodePart(artist)}",
        title = name,
        subtitle = artist,
        type = MediaMetadata.MEDIA_TYPE_ALBUM,
        artworkSongId = null,
        grid = false,
    )

    private fun ArtistDto.asTile(): MediaItem = folderTile(
        mediaId = "$ARTIST_PREFIX${encodePart(name)}",
        title = name,
        subtitle = "$songCount song${if (songCount == 1) "" else "s"} · $albumCount album${if (albumCount == 1) "" else "s"}",
        type = MediaMetadata.MEDIA_TYPE_ARTIST,
        artworkSongId = null,
        grid = false,
    )

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

    // --- encoding helpers ----------------------------------------------------

    private fun encodePart(s: String): String = Uri.encode(s, /* allow */ "")
    private fun decodePart(s: String): String = Uri.decode(s)

    private fun decodeAlbumKey(key: String): Pair<String, String>? {
        val parts = key.split("|")
        if (parts.size != 2) return null
        return Pair(decodePart(parts[0]), decodePart(parts[1]))
    }

    /** Tiny 4-tuple — std lib only ships Triple. */
    data class Quadruple<A, B, C, D>(val a: A, val b: B, val c: C, val d: D) {
        fun fourth(): D = d
    }
}
