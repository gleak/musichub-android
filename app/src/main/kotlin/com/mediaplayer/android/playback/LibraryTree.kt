package com.mediaplayer.android.playback

import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.mediaplayer.android.R
import com.mediaplayer.android.data.ConnectivityObserver
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
 * ├── genres       (grid — mirrors phone Search "Sfoglia · Tutti i generi")
 * │   └── genre:{tag}                                   (songs filtered by tag)
 * └── all-songs    (list)
 * ```
 *
 * mediaId scheme (stable — AA caches them between sessions):
 * - `root` / `recents` / `liked` / `playlists` / `albums` / `artists` /
 *   `genres` / `all-songs` / `lyrics`                   — fixed folders
 * - `song:{songId}`                                     — generic playable leaf
 * - `playlist:{playlistId}`                             — playlist folder
 * - `pl:{playlistId}:{position}:{songId}`               — leaf inside playlist
 * - `album:{nameEnc}|{artistEnc}`                       — album folder
 * - `al:{nameEnc}|{artistEnc}|{position}|{songId}`      — leaf inside album
 * - `artist:{nameEnc}`                                  — artist folder
 * - `ar:{nameEnc}|{position}|{songId}`                  — leaf inside artist
 * - `genre:{tag}`                                       — genre folder
 * - `gn:{tagEnc}|{position}|{songId}`                   — leaf inside genre
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
    const val LIKED_ID = "liked"
    const val RECENTS_ID = "recents"
    const val ALBUMS_ID = "albums"
    const val ARTISTS_ID = "artists"
    /** M14f: server-curated auto-playlists (Discover Daily, On Repeat, …). */
    const val MADE_FOR_YOU_ID = "made-for-you"
    /** Mirrors the phone Search "Sfoglia · Tutti i generi" tile in AA. */
    const val GENRES_ID = "genres"
    /**
     * Custom-queue browse folder. AA already exposes a generic queue
     * affordance derived from `Player.timeline`, but it has none of the
     * MusicHub design language and cannot mark the current row. This
     * folder mirrors the mockup §9.2 `Coda` chip's intent (audit
     * `08-auto-extra.md` D9): a browseable list of the current player
     * timeline with the playing item flagged, where a tap jumps to that
     * position without rebuilding the queue.
     */
    const val QUEUE_ID = "queue"

    private const val PLAYLIST_PREFIX = "playlist:"
    private const val SONG_PREFIX = "song:"
    private const val PL_LEAF_PREFIX = "pl:"
    private const val ALBUM_PREFIX = "album:"
    private const val ARTIST_PREFIX = "artist:"
    private const val GENRE_PREFIX = "genre:"
    private const val ALBUM_LEAF_PREFIX = "al:"
    private const val ARTIST_LEAF_PREFIX = "ar:"
    private const val GENRE_LEAF_PREFIX = "gn:"
    private const val LIKED_LEAF_PREFIX = "lk:"
    private const val RECENT_LEAF_PREFIX = "rc:"
    /** `qu:{pos}|{sid}` — leaf inside the custom queue folder. */
    const val QUEUE_LEAF_PREFIX = "qu:"

    /**
     * Genre catalogue exposed in the AA browse tree. Display name + backend
     * tag must stay in sync with `SearchScreen.GENRES` so phone and car
     * surface the same 8 buckets. The tag is what `listSongs(genre=…)`
     * filters on server-side (`song_tags`).
     */
    private val GENRES: List<Pair<String, String>> = listOf(
        "Indie" to "indie",
        "Elettronica" to "electronic",
        "Hip-hop" to "hip-hop",
        "Jazz" to "jazz",
        "Classica" to "classical",
        "Ambient" to "ambient",
        "Rock" to "rock",
        "Pop" to "pop",
    )

    /**
     * Per-genre cover drawable mapping. Each XML drawable is a gradient
     * matching the duotone palette of the corresponding `MHCover` kind in
     * `mockup/mh-auto-extra.jsx`, so AA tiles render with distinct visual
     * identity instead of blank slate (audit `08-auto-extra.md` D1).
     * Exposed as `android.resource://...` URIs so AA fetches them locally
     * without depending on the LAN backend (see [genreArtworkUri]).
     */
    private val GENRE_ART_RES: Map<String, Int> = mapOf(
        "indie" to R.drawable.genre_indie,
        "electronic" to R.drawable.genre_electronic,
        "hip-hop" to R.drawable.genre_hiphop,
        "jazz" to R.drawable.genre_jazz,
        "classical" to R.drawable.genre_classical,
        "ambient" to R.drawable.genre_ambient,
        "rock" to R.drawable.genre_rock,
        "pop" to R.drawable.genre_pop,
    )

    /** App package id — hardcoded so [LibraryTree] stays a context-free
     *  singleton. Matches `applicationId` in `app/build.gradle.kts`. */
    private const val APP_PACKAGE = "com.mediaplayer.android"

    private fun genreArtworkUri(tag: String): Uri? =
        GENRE_ART_RES[tag]?.let { Uri.parse("android.resource://$APP_PACKAGE/$it") }

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
    /**
     * Setting this on the root LibraryParams is what gates AA's search
     * affordance. Without it, AA hides the magnifying-glass icon even when
     * the service implements `onSearch` / `onGetSearchResult`.
     */
    private const val SEARCH_SUPPORTED = "android.media.browse.SEARCH_SUPPORTED"

    /** Bundle returned in LibraryParams root so AA enables per-folder hints + search. */
    fun rootExtras(): Bundle = Bundle().apply {
        putBoolean(CONTENT_STYLE_SUPPORTED, true)
        putInt(CONTENT_STYLE_BROWSABLE_HINT, CONTENT_STYLE_LIST)
        putInt(CONTENT_STYLE_PLAYABLE_HINT, CONTENT_STYLE_LIST)
        putBoolean(SEARCH_SUPPORTED, true)
    }

    // --- public API ----------------------------------------------------------

    /**
     * Browse root for AA / MediaBrowser. Newer Android Auto builds (gearhead
     * 2024+) silently hide a root's children when the root MediaMetadata has
     * no `mediaType` set — the search affordance still appears but the grid
     * stays empty ("Nessun elemento"). Setting `MEDIA_TYPE_FOLDER_MIXED`
     * matches the heterogeneous content of `rootChildren()` (playlists,
     * albums, artists, songs) and unblocks enumeration.
     */
    suspend fun root(): MediaItem =
        MediaItem.Builder()
            .setMediaId(ROOT_ID)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .setMediaType(MediaMetadata.MEDIA_TYPE_FOLDER_MIXED)
                    .setTitle("MusicHub")
                    .setExtras(rootExtras())
                    .build()
            )
            .build()

    /**
     * Children of [parentId], or `null` if the id is unrecognised. Honours AA's
     * pagination: [page] / [pageSize] propagate to network calls so deep lists
     * (all-songs, search, large playlists) don't truncate at PAGE_SIZE.
     */
    suspend fun children(
        parentId: String,
        currentSongId: Long? = null,
        page: Int = 0,
        pageSize: Int = PAGE_SIZE,
    ): List<MediaItem>? = when {
        parentId == ROOT_ID -> rootChildren()
        parentId == ALL_SONGS_ID -> allSongs(page, pageSize)
        parentId == PLAYLISTS_ID -> playlists()
        parentId == MADE_FOR_YOU_ID -> madeForYou()
        parentId == LIKED_ID -> liked()
        parentId == RECENTS_ID -> recents()
        parentId == ALBUMS_ID -> albums(page, pageSize)
        parentId == ARTISTS_ID -> artists(page, pageSize)
        parentId == GENRES_ID -> genreTiles()
        parentId.startsWith(PLAYLIST_PREFIX) ->
            parentId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
                ?.let { playlistSongs(it) }
        parentId.startsWith(ALBUM_PREFIX) ->
            decodeAlbumKey(parentId.removePrefix(ALBUM_PREFIX))
                ?.let { (name, artist) -> albumSongs(name, artist) }
        parentId.startsWith(ARTIST_PREFIX) ->
            artistChildren(decodePart(parentId.removePrefix(ARTIST_PREFIX)))
        parentId.startsWith(GENRE_PREFIX) ->
            genreSongs(parentId.removePrefix(GENRE_PREFIX))
        else -> null
    }

    /** Item lookup for resume / deep-link requests. */
    suspend fun item(mediaId: String): MediaItem? = when {
        mediaId == ROOT_ID -> root()
        mediaId == ALL_SONGS_ID -> sectionFolder(ALL_SONGS_ID, "Tutti i brani", grid = false)
        mediaId == PLAYLISTS_ID -> sectionFolder(PLAYLISTS_ID, "Playlist", grid = true)
        mediaId == MADE_FOR_YOU_ID -> sectionFolder(MADE_FOR_YOU_ID, "Per te", grid = true)
        mediaId == LIKED_ID -> sectionFolder(LIKED_ID, "Brani preferiti", grid = false)
        mediaId == RECENTS_ID -> sectionFolder(RECENTS_ID, "Ascoltati di recente", grid = false)
        mediaId == ALBUMS_ID -> sectionFolder(ALBUMS_ID, "Album", grid = true)
        mediaId == ARTISTS_ID -> sectionFolder(ARTISTS_ID, "Artisti", grid = false)
        mediaId == GENRES_ID -> sectionFolder(GENRES_ID, "Generi", grid = true)
        mediaId == QUEUE_ID -> sectionFolder(QUEUE_ID, "Coda corrente", grid = false)
        mediaId.startsWith(GENRE_PREFIX) -> {
            val tag = mediaId.removePrefix(GENRE_PREFIX)
            val display = GENRES.firstOrNull { it.second == tag }?.first ?: tag
            folderTile(mediaId, display,
                subtitle = null,
                type = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
                artworkSongId = null,
                grid = false)
        }
        mediaId.startsWith(GENRE_LEAF_PREFIX) ->
            parseGenreLeaf(mediaId)?.third?.let { sid -> songLeaf(sid) }
        mediaId.startsWith(SONG_PREFIX) ->
            mediaId.removePrefix(SONG_PREFIX).toLongOrNull()?.let { songLeaf(it) }
        mediaId.startsWith(PLAYLIST_PREFIX) -> {
            val id = mediaId.removePrefix(PLAYLIST_PREFIX).toLongOrNull()
            id?.let {
                val detail = Network.api.getPlaylist(it)
                folderTile(mediaId, detail.name,
                    subtitle = "${detail.songs.size} songs",
                    type = MediaMetadata.MEDIA_TYPE_PLAYLIST,
                    artworkSongId = detail.songs.firstOrNull()?.song?.id,
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

    /**
     * Voice-search proxy against the backend `/api/songs?q=` endpoint.
     *
     * Tiny single-entry cache: AA's `onSearch` then `onGetSearchResult`
     * dance fires page=0 twice in a row for the same query — once to learn
     * the result count, then to fetch the items. Memoising the most recent
     * (query, page=0) skips the second round-trip. Cache invalidates on the
     * next non-matching call; thread-safety is per-coroutine since LibraryTree
     * is a singleton accessed from `serviceScope.future`.
     */
    @Volatile private var lastSearchQuery: String? = null
    @Volatile private var lastSearchPage0: List<MediaItem> = emptyList()

    suspend fun search(query: String, page: Int = 0, pageSize: Int = PAGE_SIZE): List<MediaItem> {
        if (page == 0 && pageSize == PAGE_SIZE && query == lastSearchQuery) {
            return lastSearchPage0
        }
        val resp = Network.api.listSongs(query = query, page = page, size = pageSize)
        val items = resp.items.map { songLeaf(it) }
        if (page == 0 && pageSize == PAGE_SIZE) {
            lastSearchQuery = query
            lastSearchPage0 = items
        }
        return items
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

    /** Parses `gn:{tagEnc}|{pos}|{sid}` into `(genreTag, pos, songId)`. */
    fun parseGenreLeaf(mediaId: String): Triple<String, Int, Long>? {
        if (!mediaId.startsWith(GENRE_LEAF_PREFIX)) return null
        val parts = mediaId.removePrefix(GENRE_LEAF_PREFIX).split("|")
        if (parts.size != 3) return null
        val pos = parts[1].toIntOrNull() ?: return null
        val sid = parts[2].toLongOrNull() ?: return null
        return Triple(decodePart(parts[0]), pos, sid)
    }

    /**
     * Renders the current player timeline as browse leaves under the
     * custom queue folder. The current item is marked with a `▸` prefix
     * so AA's list view shows the playhead at a glance; subtitle keeps
     * the artist + per-row position so the driver can tell duplicates
     * apart. The MediaItems handed back here carry no URIs — playback
     * starts via the `qu:` leaf path in `onSetMediaItems`, which
     * returns the same player timeline + the chosen index (no rebuild).
     */
    fun queueChildren(timeline: List<MediaItem>, currentIndex: Int): List<MediaItem> {
        if (timeline.isEmpty()) {
            return listOf(infoItem("Nessun brano in coda"))
        }
        return timeline.mapIndexedNotNull { index, item ->
            val songId = item.mediaId.removePrefix(SONG_PREFIX).toLongOrNull()
                ?: return@mapIndexedNotNull null
            val rawTitle = item.mediaMetadata.title?.toString() ?: "Brano ${index + 1}"
            val title = if (index == currentIndex) "▸ $rawTitle" else rawTitle
            val artist = item.mediaMetadata.artist?.toString()
            val subtitle = listOfNotNull(
                "${index + 1}",
                artist,
            ).joinToString(" · ")
            val artwork = item.mediaMetadata.artworkUri ?: aaArtworkUri(songId)
            MediaItem.Builder()
                .setMediaId("$QUEUE_LEAF_PREFIX$index|$songId")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
                        .setTitle(title)
                        .setArtist(subtitle)
                        .apply { if (artwork != null) setArtworkUri(artwork) }
                        .build()
                )
                .build()
        }
    }

    /** Parses `qu:{pos}|{sid}` into `(position, songId)`. */
    fun parseQueueLeaf(mediaId: String): Pair<Int, Long>? =
        parseSimpleLeaf(mediaId, QUEUE_LEAF_PREFIX)

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
        return detail.songs.map { playableSong(it.song) }
    }

    suspend fun albumQueue(name: String, artist: String): List<MediaItem> {
        val detail = Network.api.getAlbum(name, artist)
        return detail.songs.map { playableSong(it) }
    }

    suspend fun artistQueue(name: String): List<MediaItem> {
        val detail = Network.api.getArtist(name)
        return detail.songs.map { playableSong(it) }
    }

    /**
     * Queue resolver for a genre tap in AA. Pulls a wider page than the
     * browse list (which is capped at PAGE_SIZE=50) so the user actually
     * gets a long playable session per genre.
     */
    suspend fun genreQueue(tag: String): List<MediaItem> =
        Network.api.listSongs(query = null, genre = tag, page = 0, size = LIKED_LIMIT)
            .items.map { playableSong(it) }

    suspend fun likedQueue(): List<MediaItem> =
        Network.api.getLikedSongs(page = 0, size = LIKED_LIMIT).items.map { playableSong(it) }

    suspend fun recentsQueue(): List<MediaItem> =
        Network.api.recentSongs(limit = RECENT_LIMIT).map { playableSong(it) }

    // --- info placeholder ----------------------------------------------------

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
                    .setArtworkUri(aaArtworkUri(songId))
                    .build()
            )
            .build()

    // --- internals -----------------------------------------------------------

    private fun rootChildren(): List<MediaItem> = listOf(
        // Coda first — driver-glance order: "what's queued right now"
        // is the most time-sensitive entry in a car session.
        sectionFolder(QUEUE_ID, "Coda corrente", grid = false,
            type = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
        sectionFolder(MADE_FOR_YOU_ID, "Per te", grid = true,
            type = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
        sectionFolder(RECENTS_ID, "Ascoltati di recente", grid = false,
            type = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
        sectionFolder(LIKED_ID, "Brani preferiti", grid = false,
            type = MediaMetadata.MEDIA_TYPE_PLAYLIST),
        sectionFolder(PLAYLISTS_ID, "Playlist", grid = true,
            type = MediaMetadata.MEDIA_TYPE_FOLDER_PLAYLISTS),
        sectionFolder(ALBUMS_ID, "Album", grid = true,
            type = MediaMetadata.MEDIA_TYPE_FOLDER_ALBUMS),
        sectionFolder(ARTISTS_ID, "Artisti", grid = false,
            type = MediaMetadata.MEDIA_TYPE_FOLDER_ARTISTS),
        sectionFolder(GENRES_ID, "Generi", grid = true,
            type = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
        sectionFolder(ALL_SONGS_ID, "Tutti i brani", grid = false,
            type = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED),
    )

    private suspend fun allSongs(page: Int, pageSize: Int): List<MediaItem> {
        val resp = Network.api.listSongs(query = null, page = page, size = pageSize)
        return resp.items.map { songLeaf(it) }
    }

    private suspend fun playlists(): List<MediaItem> =
        Network.api.listPlaylists().map { pl ->
            folderTile(
                mediaId = "$PLAYLIST_PREFIX${pl.id}",
                title = pl.name,
                subtitle = "${pl.songCount} bran${if (pl.songCount == 1) "o" else "i"}",
                type = MediaMetadata.MEDIA_TYPE_PLAYLIST,
                artworkSongId = pl.coverSongId,
                grid = true,
            )
        }

    /**
     * M14f: server-curated playlists (Discover Daily, On Repeat). Backend
     * filters by `kind` query param; we render them with the same
     * `playlist:{id}` leaf scheme as user playlists since they are real
     * playlist rows server-side. The "Made for you" subtitle distinguishes
     * them in the AA list.
     */
    private suspend fun madeForYou(): List<MediaItem> {
        val list = Network.api.listPlaylists(kind = "auto")
        if (list.isEmpty()) {
            return listOf(infoItem("Nothing here yet — check back tomorrow"))
        }
        return list.map { pl ->
            folderTile(
                mediaId = "$PLAYLIST_PREFIX${pl.id}",
                title = pl.name,
                subtitle = "Per te · ${pl.songCount} bran${if (pl.songCount == 1) "o" else "i"}",
                type = MediaMetadata.MEDIA_TYPE_PLAYLIST,
                artworkSongId = pl.coverSongId,
                grid = true,
            )
        }
    }

    private suspend fun playlistSongs(playlistId: Long): List<MediaItem> {
        val detail = Network.api.getPlaylist(playlistId)
        return detail.songs.mapIndexed { index, entry ->
            val song = entry.song
            MediaItem.Builder()
                .setMediaId("$PL_LEAF_PREFIX$playlistId:$index:${song.id}")
                .setMediaMetadata(song.asBrowseMetadata(
                    playable = true,
                    trackNumber = index + 1,
                    totalTrackCount = detail.songs.size,
                ))
                .build()
        }
    }

    private suspend fun albums(page: Int, pageSize: Int): List<MediaItem> {
        val resp = Network.api.listAlbums(query = null, page = page, size = pageSize)
        return resp.items.map { it.asTile() }
    }

    private suspend fun albumSongs(name: String, artist: String): List<MediaItem> {
        val detail = Network.api.getAlbum(name, artist)
        val nameEnc = encodePart(detail.name)
        val artistEnc = encodePart(detail.artist)
        return detail.songs.mapIndexed { index, song ->
            MediaItem.Builder()
                .setMediaId("$ALBUM_LEAF_PREFIX$nameEnc|$artistEnc|$index|${song.id}")
                .setMediaMetadata(song.asBrowseMetadata(
                    playable = true,
                    trackNumber = index + 1,
                    totalTrackCount = detail.songs.size,
                ))
                .build()
        }
    }

    private suspend fun artists(page: Int, pageSize: Int): List<MediaItem> {
        val resp = Network.api.listArtists(query = null, page = page, size = pageSize)
        return resp.items.map { it.asTile() }
    }

    private suspend fun artistChildren(name: String): List<MediaItem> {
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
        }
        return list
    }

    /**
     * Genre tile grid for AA. Static list — same 8 buckets as the phone's
     * `Sfoglia · Tutti i generi`. No subtitle: the mockup's grid renders
     * the genre name directly over a coloured cover, so a literal "Genere"
     * caption on every row was redundant noise (audit `08-auto-extra.md` D2).
     * Per-tile artwork is sourced from bundled `genre_*.xml` gradient
     * drawables matching the mockup's MHCover palettes (audit D1).
     */
    private fun genreTiles(): List<MediaItem> = GENRES.map { (display, tag) ->
        folderTile(
            mediaId = "$GENRE_PREFIX$tag",
            title = display,
            subtitle = null,
            type = MediaMetadata.MEDIA_TYPE_FOLDER_MIXED,
            artworkSongId = null,
            grid = false,
            artworkUri = genreArtworkUri(tag),
        )
    }

    private suspend fun genreSongs(tag: String): List<MediaItem> {
        val resp = Network.api.listSongs(
            query = null,
            genre = tag,
            page = 0,
            size = PAGE_SIZE,
        )
        val tagEnc = encodePart(tag)
        return resp.items.mapIndexed { index, song ->
            MediaItem.Builder()
                .setMediaId("$GENRE_LEAF_PREFIX$tagEnc|$index|${song.id}")
                .setMediaMetadata(song.asBrowseMetadata(playable = true))
                .build()
        }.ifEmpty { listOf(infoItem("Nessun brano per questo genere")) }
    }

    private suspend fun liked(): List<MediaItem> {
        val page = Network.api.getLikedSongs(page = 0, size = LIKED_LIMIT)
        val list = page.items.mapIndexed { index, song ->
            MediaItem.Builder()
                .setMediaId("$LIKED_LEAF_PREFIX$index|${song.id}")
                .setMediaMetadata(song.asBrowseMetadata(playable = true))
                .build()
        }
        return list.ifEmpty { listOf(infoItem("Ancora nessun brano preferito")) }
    }

    private suspend fun recents(): List<MediaItem> {
        val songs = Network.api.recentSongs(limit = RECENT_LIMIT)
        val list = songs.mapIndexed { index, song ->
            MediaItem.Builder()
                .setMediaId("$RECENT_LEAF_PREFIX$index|${song.id}")
                .setMediaMetadata(song.asBrowseMetadata(playable = true))
                .build()
        }
        return list.ifEmpty { listOf(infoItem("Ancora nessun ascolto")) }
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
                    .setArtworkUri(aaArtworkUri(songId))
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
        artworkUri: Uri? = null,
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
                        // Explicit URI wins; otherwise resolve from songId
                        // (skipped when the LAN backend is unreachable).
                        val art = artworkUri ?: artworkSongId?.let { aaArtworkUri(it) }
                        if (art != null) setArtworkUri(art)
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
        artworkSongId = coverSongId,
        grid = false,
    )

    private fun ArtistDto.asTile(): MediaItem = folderTile(
        mediaId = "$ARTIST_PREFIX${encodePart(name)}",
        title = name,
        subtitle = "$songCount song${if (songCount == 1) "" else "s"} · $albumCount album${if (albumCount == 1) "" else "s"}",
        type = MediaMetadata.MEDIA_TYPE_ARTIST,
        artworkSongId = coverSongId,
        grid = false,
    )

    private fun SongDto.asBrowseMetadata(
        playable: Boolean,
        trackNumber: Int? = null,
        totalTrackCount: Int? = null,
    ): MediaMetadata =
        MediaMetadata.Builder()
            .setIsBrowsable(false)
            // Disable AA tap when the backend says the audio file is missing —
            // AA renders the row dimmed instead of opaquely failing playback.
            .setIsPlayable(playable && this.playable)
            .setMediaType(MediaMetadata.MEDIA_TYPE_MUSIC)
            .setTitle(title)
            .setArtist(artist)
            .apply {
                if (album != null) setAlbumTitle(album)
                if (trackNumber != null) setTrackNumber(trackNumber)
                if (totalTrackCount != null) setTotalTrackCount(totalTrackCount)
            }
            .setArtworkUri(aaArtworkUri(id))
            .build()

    /**
     * Cover URLs are LAN-only (`http://192.168…/api/songs/{id}/cover`). On a
     * real Android Auto session over a head unit that's not on the LAN, the
     * fetch silently fails and the slot flashes empty. Skip artworkUri while
     * the network signal says the backend is unreachable so AA falls back to
     * its own generic placeholder instead. The check is best-effort —
     * `networkAvailable` flips true on the next successful HTTP call.
     */
    private fun aaArtworkUri(songId: Long): Uri? =
        if (ConnectivityObserver.networkAvailable.value) Uri.parse(Network.coverUrl(songId)) else null

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
