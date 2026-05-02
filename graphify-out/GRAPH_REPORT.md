# Graph Report - app/src/main/kotlin  (2026-05-02)

## Corpus Check
- 120 files · ~60,954 words
- Verdict: corpus is large enough that graph structure adds value.

## Summary
- 908 nodes · 788 edges · 85 communities detected
- Extraction: 100% EXTRACTED · 0% INFERRED · 0% AMBIGUOUS
- Token cost: 0 input · 0 output

## Community Hubs (Navigation)
- [[_COMMUNITY_Backend API Interface|Backend API Interface]]
- [[_COMMUNITY_Android Auto Library Tree|Android Auto Library Tree]]
- [[_COMMUNITY_Playback State & Controls|Playback State & Controls]]
- [[_COMMUNITY_Home Screen UI|Home Screen UI]]
- [[_COMMUNITY_Media Playback Service|Media Playback Service]]
- [[_COMMUNITY_Event Sync Queue|Event Sync Queue]]
- [[_COMMUNITY_App Navigation Shell|App Navigation Shell]]
- [[_COMMUNITY_Find  Discover ViewModel|Find / Discover ViewModel]]
- [[_COMMUNITY_Search ViewModel|Search ViewModel]]
- [[_COMMUNITY_Audio Equalizer|Audio Equalizer]]
- [[_COMMUNITY_Playback UI State Types|Playback UI State Types]]
- [[_COMMUNITY_Cover Art Rendering|Cover Art Rendering]]
- [[_COMMUNITY_Artist Screen|Artist Screen]]
- [[_COMMUNITY_Generated Cover Art|Generated Cover Art]]
- [[_COMMUNITY_Playlist Data Layer|Playlist Data Layer]]
- [[_COMMUNITY_Artist List Screen|Artist List Screen]]
- [[_COMMUNITY_Playlists Screen|Playlists Screen]]
- [[_COMMUNITY_Spotify Import Flow|Spotify Import Flow]]
- [[_COMMUNITY_Prefetch Orchestrator|Prefetch Orchestrator]]
- [[_COMMUNITY_Album List Screen|Album List Screen]]
- [[_COMMUNITY_Playlist Detail Screen|Playlist Detail Screen]]
- [[_COMMUNITY_Download Repository|Download Repository]]
- [[_COMMUNITY_Album Screen|Album Screen]]
- [[_COMMUNITY_Auth ViewModel|Auth ViewModel]]
- [[_COMMUNITY_Home ViewModel|Home ViewModel]]
- [[_COMMUNITY_Liked ViewModel|Liked ViewModel]]
- [[_COMMUNITY_AA Lyrics Ticker|AA Lyrics Ticker]]
- [[_COMMUNITY_Playlists ViewModel|Playlists ViewModel]]
- [[_COMMUNITY_For You Genre Grid|For You Genre Grid]]
- [[_COMMUNITY_App Update Checker|App Update Checker]]
- [[_COMMUNITY_Auth Repository|Auth Repository]]
- [[_COMMUNITY_Player Settings|Player Settings]]
- [[_COMMUNITY_Read Cache|Read Cache]]
- [[_COMMUNITY_Song Repository|Song Repository]]
- [[_COMMUNITY_Playback Resumption|Playback Resumption]]
- [[_COMMUNITY_Theme & Colors|Theme & Colors]]
- [[_COMMUNITY_Connectivity Observer|Connectivity Observer]]
- [[_COMMUNITY_Find Repository|Find Repository]]
- [[_COMMUNITY_Network & Auth Token|Network & Auth Token]]
- [[_COMMUNITY_Find Request DTOs|Find Request DTOs]]
- [[_COMMUNITY_Cover Shape & Color|Cover Shape & Color]]
- [[_COMMUNITY_For You ViewModel|For You ViewModel]]
- [[_COMMUNITY_Catalog Repository|Catalog Repository]]
- [[_COMMUNITY_Follow Repository|Follow Repository]]
- [[_COMMUNITY_Liked Repository|Liked Repository]]
- [[_COMMUNITY_App Update Installer|App Update Installer]]
- [[_COMMUNITY_History Repository|History Repository]]
- [[_COMMUNITY_Search History Store|Search History Store]]
- [[_COMMUNITY_Playlist Request DTOs|Playlist Request DTOs]]
- [[_COMMUNITY_Media Download Service|Media Download Service]]
- [[_COMMUNITY_Player Connection|Player Connection]]
- [[_COMMUNITY_Ringtone Exporter|Ringtone Exporter]]
- [[_COMMUNITY_Profile Stats ViewModel|Profile Stats ViewModel]]
- [[_COMMUNITY_App Entry Point|App Entry Point]]
- [[_COMMUNITY_App Version & Changelog|App Version & Changelog]]
- [[_COMMUNITY_Changelog Preferences|Changelog Preferences]]
- [[_COMMUNITY_CSV Playlist Parser|CSV Playlist Parser]]
- [[_COMMUNITY_Onboarding Preferences|Onboarding Preferences]]
- [[_COMMUNITY_Download Cache Root|Download Cache Root]]
- [[_COMMUNITY_Sleep Timer|Sleep Timer]]
- [[_COMMUNITY_Onboarding ViewModel|Onboarding ViewModel]]
- [[_COMMUNITY_Lyrics Repository|Lyrics Repository]]
- [[_COMMUNITY_Album DTOs|Album DTOs]]
- [[_COMMUNITY_Artist DTOs|Artist DTOs]]
- [[_COMMUNITY_Share DTOs|Share DTOs]]
- [[_COMMUNITY_Player Cache|Player Cache]]
- [[_COMMUNITY_User State|User State]]
- [[_COMMUNITY_Cover Shapes|Cover Shapes]]
- [[_COMMUNITY_App Update Repository|App Update Repository]]
- [[_COMMUNITY_Spotify Import Track DTO|Spotify Import Track DTO]]
- [[_COMMUNITY_App Update DTO|App Update DTO]]
- [[_COMMUNITY_App Version Request DTO|App Version Request DTO]]
- [[_COMMUNITY_Genre Seed Request DTO|Genre Seed Request DTO]]
- [[_COMMUNITY_Lyric Line DTO|Lyric Line DTO]]
- [[_COMMUNITY_Page Response DTO|Page Response DTO]]
- [[_COMMUNITY_Playlist Detail DTO|Playlist Detail DTO]]
- [[_COMMUNITY_Playlist DTO|Playlist DTO]]
- [[_COMMUNITY_Playlist Song Entry DTO|Playlist Song Entry DTO]]
- [[_COMMUNITY_Record Play Request DTO|Record Play Request DTO]]
- [[_COMMUNITY_Reinit Status DTO|Reinit Status DTO]]
- [[_COMMUNITY_Song DTO|Song DTO]]
- [[_COMMUNITY_Spotify Import Result DTO|Spotify Import Result DTO]]
- [[_COMMUNITY_Stats DTO|Stats DTO]]
- [[_COMMUNITY_User DTO|User DTO]]
- [[_COMMUNITY_Spacing Tokens|Spacing Tokens]]

## God Nodes (most connected - your core abstractions)
1. `MediaPlayerApi` - 45 edges
2. `LibraryTree` - 43 edges
3. `PlaybackViewModel` - 39 edges
4. `EventQueue` - 17 edges
5. `PlaylistRepository` - 13 edges
6. `EqualizerController` - 13 edges
7. `MediaPlaybackService` - 13 edges
8. `LibraryCallback` - 12 edges
9. `FindViewModel` - 12 edges
10. `SearchViewModel` - 12 edges

## Surprising Connections (you probably didn't know these)
- None detected - all connections are within the same source files.

## Communities

### Community 0 - "Backend API Interface"
Cohesion: 0.04
Nodes (1): MediaPlayerApi

### Community 1 - "Android Auto Library Tree"
Cohesion: 0.04
Nodes (2): LibraryTree, Quadruple

### Community 2 - "Playback State & Controls"
Cohesion: 0.05
Nodes (1): PlaybackViewModel

### Community 3 - "Home Screen UI"
Cohesion: 0.07
Nodes (5): HomeFilter, Liked, Playlist, ShortcutItem, Song

### Community 4 - "Media Playback Service"
Cohesion: 0.08
Nodes (2): LibraryCallback, MediaPlaybackService

### Community 5 - "Event Sync Queue"
Cohesion: 0.08
Nodes (4): EventQueue, EventType, Row, SyncDb

### Community 6 - "App Navigation Shell"
Cohesion: 0.11
Nodes (3): BottomDestination, MainActivity, Routes

### Community 7 - "Find / Discover ViewModel"
Cohesion: 0.11
Nodes (6): Error, FindUiState, FindViewModel, Idle, Searching, Tracking

### Community 8 - "Search ViewModel"
Cohesion: 0.11
Nodes (6): Error, Idle, Loading, SearchUiState, SearchViewModel, Success

### Community 9 - "Audio Equalizer"
Cohesion: 0.12
Nodes (4): BandInfo, EqPreset, EqState, EqualizerController

### Community 10 - "Playback UI State Types"
Cohesion: 0.12
Nodes (6): AlarmExportState, Exporting, Failure, Idle, QueueEntry, Success

### Community 11 - "Cover Art Rendering"
Cohesion: 0.12
Nodes (2): MHCoverKind, MHCoverPalette

### Community 12 - "Artist Screen"
Cohesion: 0.13
Nodes (5): ArtistUiState, ArtistViewModel, Error, Loading, Success

### Community 13 - "Generated Cover Art"
Cohesion: 0.13
Nodes (1): AutoPlaylistFamily

### Community 14 - "Playlist Data Layer"
Cohesion: 0.14
Nodes (1): PlaylistRepository

### Community 15 - "Artist List Screen"
Cohesion: 0.15
Nodes (5): ArtistListUiState, ArtistListViewModel, Error, Loading, Success

### Community 17 - "Playlists Screen"
Cohesion: 0.15
Nodes (1): LibraryFilter

### Community 18 - "Spotify Import Flow"
Cohesion: 0.15
Nodes (8): Confirming, Done, Error, FetchingPlaylist, Idle, Importing, SpotifyImportUiState, SpotifyImportViewModel

### Community 19 - "Prefetch Orchestrator"
Cohesion: 0.17
Nodes (1): PrefetchOrchestrator

### Community 20 - "Album List Screen"
Cohesion: 0.17
Nodes (5): AlbumListUiState, AlbumListViewModel, Error, Loading, Success

### Community 21 - "Playlist Detail Screen"
Cohesion: 0.17
Nodes (5): Error, Loading, PlaylistDetailUiState, PlaylistDetailViewModel, Success

### Community 22 - "Download Repository"
Cohesion: 0.18
Nodes (1): DownloadRepository

### Community 23 - "Album Screen"
Cohesion: 0.18
Nodes (5): AlbumUiState, AlbumViewModel, Error, Loading, Success

### Community 24 - "Auth ViewModel"
Cohesion: 0.18
Nodes (6): AuthViewModel, Error, Loading, NotSignedIn, SignedIn, State

### Community 25 - "Home ViewModel"
Cohesion: 0.18
Nodes (5): Error, HomeUiState, HomeViewModel, Loading, Success

### Community 26 - "Liked ViewModel"
Cohesion: 0.18
Nodes (5): Error, LikedUiState, LikedViewModel, Loading, Success

### Community 27 - "AA Lyrics Ticker"
Cohesion: 0.2
Nodes (1): AALyricsTicker

### Community 29 - "Playlists ViewModel"
Cohesion: 0.2
Nodes (5): Error, Loading, PlaylistsUiState, PlaylistsViewModel, Success

### Community 30 - "For You Genre Grid"
Cohesion: 0.2
Nodes (1): Genre

### Community 31 - "App Update Checker"
Cohesion: 0.2
Nodes (5): AppUpdateChecker, Error, ManualResult, Updated, UpToDate

### Community 32 - "Auth Repository"
Cohesion: 0.22
Nodes (1): AuthRepository

### Community 33 - "Player Settings"
Cohesion: 0.22
Nodes (1): PlayerSettings

### Community 34 - "Read Cache"
Cohesion: 0.22
Nodes (2): Keys, ReadCache

### Community 37 - "Song Repository"
Cohesion: 0.25
Nodes (1): SongRepository

### Community 38 - "Playback Resumption"
Cohesion: 0.25
Nodes (4): PlaybackResumption, Snapshot, SnapshotDto, SongSnapshotDto

### Community 39 - "Theme & Colors"
Cohesion: 0.25
Nodes (4): MHColors, MHGradient, MHMonoTextStyles, MHTheme

### Community 40 - "Connectivity Observer"
Cohesion: 0.29
Nodes (1): ConnectivityObserver

### Community 41 - "Find Repository"
Cohesion: 0.29
Nodes (1): FindRepository

### Community 42 - "Network & Auth Token"
Cohesion: 0.29
Nodes (2): AuthTokenHolder, Network

### Community 43 - "Find Request DTOs"
Cohesion: 0.29
Nodes (6): CandidateDto, CreateRequestBody, RequestDto, RequestStatus, RequestSummaryDto, SelectCandidateBody

### Community 44 - "Cover Shape & Color"
Cohesion: 0.29
Nodes (1): CoverShape

### Community 45 - "For You ViewModel"
Cohesion: 0.29
Nodes (5): Error, ForYouUiState, ForYouViewModel, Loading, Ready

### Community 47 - "Catalog Repository"
Cohesion: 0.33
Nodes (1): CatalogRepository

### Community 48 - "Follow Repository"
Cohesion: 0.33
Nodes (1): FollowRepository

### Community 49 - "Liked Repository"
Cohesion: 0.33
Nodes (1): LikedRepository

### Community 53 - "App Update Installer"
Cohesion: 0.33
Nodes (1): AppUpdateInstaller

### Community 54 - "History Repository"
Cohesion: 0.4
Nodes (1): HistoryRepository

### Community 55 - "Search History Store"
Cohesion: 0.4
Nodes (1): SearchHistoryStore

### Community 56 - "Playlist Request DTOs"
Cohesion: 0.4
Nodes (4): AddSongRequest, CreatePlaylistRequest, RenamePlaylistRequest, ReorderSongsRequest

### Community 57 - "Media Download Service"
Cohesion: 0.4
Nodes (1): MediaDownloadService

### Community 58 - "Player Connection"
Cohesion: 0.4
Nodes (1): PlayerConnection

### Community 59 - "Ringtone Exporter"
Cohesion: 0.4
Nodes (1): RingtoneExporter

### Community 61 - "Profile Stats ViewModel"
Cohesion: 0.4
Nodes (2): ProfileStats, ProfileStatsViewModel

### Community 62 - "App Entry Point"
Cohesion: 0.5
Nodes (1): MediaPlayerApp

### Community 63 - "App Version & Changelog"
Cohesion: 0.5
Nodes (3): AppVersion, Changelog, ChangelogEntry

### Community 64 - "Changelog Preferences"
Cohesion: 0.5
Nodes (1): ChangelogPreferences

### Community 65 - "CSV Playlist Parser"
Cohesion: 0.5
Nodes (1): CsvPlaylistParser

### Community 66 - "Onboarding Preferences"
Cohesion: 0.5
Nodes (1): OnboardingPreferences

### Community 67 - "Download Cache Root"
Cohesion: 0.5
Nodes (1): DownloadRoot

### Community 68 - "Sleep Timer"
Cohesion: 0.5
Nodes (1): SleepTimer

### Community 71 - "Onboarding ViewModel"
Cohesion: 0.5
Nodes (1): OnboardingViewModel

### Community 75 - "Lyrics Repository"
Cohesion: 0.67
Nodes (1): LyricsRepository

### Community 76 - "Album DTOs"
Cohesion: 0.67
Nodes (2): AlbumDetailDto, AlbumDto

### Community 77 - "Artist DTOs"
Cohesion: 0.67
Nodes (2): ArtistDetailDto, ArtistDto

### Community 78 - "Share DTOs"
Cohesion: 0.67
Nodes (2): ShareLinkDto, SharePreviewDto

### Community 79 - "Player Cache"
Cohesion: 0.67
Nodes (1): PlayerCache

### Community 81 - "User State"
Cohesion: 0.67
Nodes (1): CurrentUser

### Community 89 - "Cover Shapes"
Cohesion: 0.67
Nodes (2): CoverShapes, HeroCoverSize

### Community 90 - "App Update Repository"
Cohesion: 0.67
Nodes (1): AppUpdateRepository

### Community 91 - "Spotify Import Track DTO"
Cohesion: 1.0
Nodes (1): SpotifyImportTrack

### Community 92 - "App Update DTO"
Cohesion: 1.0
Nodes (1): AppUpdateDto

### Community 93 - "App Version Request DTO"
Cohesion: 1.0
Nodes (1): AppVersionRequest

### Community 94 - "Genre Seed Request DTO"
Cohesion: 1.0
Nodes (1): GenreSeedRequest

### Community 95 - "Lyric Line DTO"
Cohesion: 1.0
Nodes (1): LyricLineDto

### Community 96 - "Page Response DTO"
Cohesion: 1.0
Nodes (1): PageResponse

### Community 97 - "Playlist Detail DTO"
Cohesion: 1.0
Nodes (1): PlaylistDetailDto

### Community 98 - "Playlist DTO"
Cohesion: 1.0
Nodes (1): PlaylistDto

### Community 99 - "Playlist Song Entry DTO"
Cohesion: 1.0
Nodes (1): PlaylistSongEntryDto

### Community 100 - "Record Play Request DTO"
Cohesion: 1.0
Nodes (1): RecordPlayRequest

### Community 101 - "Reinit Status DTO"
Cohesion: 1.0
Nodes (1): ReinitStatusDto

### Community 102 - "Song DTO"
Cohesion: 1.0
Nodes (1): SongDto

### Community 103 - "Spotify Import Result DTO"
Cohesion: 1.0
Nodes (1): SpotifyImportResultDto

### Community 104 - "Stats DTO"
Cohesion: 1.0
Nodes (1): StatsDto

### Community 105 - "User DTO"
Cohesion: 1.0
Nodes (1): UserDto

### Community 117 - "Spacing Tokens"
Cohesion: 1.0
Nodes (1): MediaPlayerSpacing

## Knowledge Gaps
- **131 isolated node(s):** `BottomDestination`, `AppVersion`, `ChangelogEntry`, `Changelog`, `SpotifyImportTrack` (+126 more)
  These have ≤1 connection - possible missing edges or undocumented components.
- **Thin community `Backend API Interface`** (46 nodes): `MediaPlayerApi.kt`, `MediaPlayerApi`, `.acceptPlaylistShare()`, `.addSongToPlaylist()`, `.createPlaylist()`, `.createPlaylistShare()`, `.createRequest()`, `.deletePlaylist()`, `.deleteRequest()`, `.downloadVideo()`, `.followArtist()`, `.followStatus()`, `.getAlbum()`, `.getArtist()`, `.getDownloadVideoStatus()`, `.getLikedSongs()`, `.getLikedStatus()`, `.getLyrics()`, `.getMe()`, `.getPlaylist()`, `.getReinitializeStatus()`, `.getRequest()`, `.getStats()`, `.importSpotifyPlaylist()`, `.latestAppUpdate()`, `.likeSong()`, `.listAlbums()`, `.listArtists()`, `.listFollowedArtists()`, `.listPlaylists()`, `.listRequests()`, `.listSongs()`, `.previewPlaylistShare()`, `.recentSongs()`, `.recordPlay()`, `.redownloadSong()`, `.refreshDailyMix()`, `.reinitializeVideo()`, `.removeSongFromPlaylist()`, `.renamePlaylist()`, `.reorderPlaylistSongs()`, `.reportAppVersion()`, `.seedGenres()`, `.selectCandidate()`, `.unfollowArtist()`, `.unlikeSong()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Android Auto Library Tree`** (46 nodes): `LibraryTree.kt`, `LibraryTree`, `.aaArtworkUri()`, `.albumQueue()`, `.albums()`, `.albumSongs()`, `.allSongs()`, `.artistChildren()`, `.artistQueue()`, `.artists()`, `.asBrowseMetadata()`, `.asTile()`, `.browsable()`, `.children()`, `.decodeAlbumKey()`, `.decodePart()`, `.encodePart()`, `.folderTile()`, `.genreQueue()`, `.genreSongs()`, `.genreTiles()`, `.infoItem()`, `.item()`, `.liked()`, `.likedQueue()`, `.madeForYou()`, `.parseAlbumLeaf()`, `.parseArtistLeaf()`, `.parseGenreLeaf()`, `.parsePlaylistLeaf()`, `.parseSimpleLeaf()`, `.playableForSong()`, `.playableSong()`, `.playlistQueue()`, `.playlists()`, `.playlistSongs()`, `.recents()`, `.recentsQueue()`, `.root()`, `.rootChildren()`, `.rootExtras()`, `.search()`, `.sectionFolder()`, `.songLeaf()`, `Quadruple`, `.fourth()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Playback State & Controls`** (39 nodes): `PlaybackViewModel`, `.addToQueue()`, `.cancelSleepTimer()`, `.consumeAlarmExportState()`, `.consumeRedownloadError()`, `.consumeVideoDownloadError()`, `.consumeVideoReinitializeError()`, `.cycleRepeat()`, `.downloadVideoForCurrent()`, `.flushPlayHistoryAwait()`, `.maybeRecordPlay()`, `.onCleared()`, `.pause()`, `.persistShuffle()`, `.play()`, `.playNext()`, `.playPlaylist()`, `.playPlaylistShuffled()`, `.pushDuration()`, `.pushPositionOnce()`, `.pushQueue()`, `.pushQueueAvailability()`, `.rearrangeSourceItems()`, `.redownloadCurrent()`, `.redownloadErrorMessage()`, `.refreshLocalDownload()`, `.reinitializeVideoForCurrent()`, `.removeFromQueue()`, `.saveCurrentAsAlarmSound()`, `.seekTo()`, `.setSleepTimer()`, `.skipNext()`, `.skipPrevious()`, `.skipToQueueItem()`, `.startPositionPoll()`, `.stopPositionPoll()`, `.toggleCurrentLike()`, `.togglePlayPause()`, `.toggleShuffle()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Media Playback Service`** (26 nodes): `MediaPlaybackService.kt`, `LibraryCallback`, `.onConnect()`, `.onCustomCommand()`, `.onDisconnected()`, `.onGetChildren()`, `.onGetItem()`, `.onGetLibraryRoot()`, `.onGetSearchResult()`, `.onPlaybackResumption()`, `.onPostConnect()`, `.onSearch()`, `.onSetMediaItems()`, `MediaPlaybackService`, `.buildCustomLayout()`, `.buildLikeButton()`, `.buildSessionExtras()`, `.buildSleepButton()`, `.fadeInOnAutoTransition()`, `.onCreate()`, `.onDestroy()`, `.onGetSession()`, `.onTaskRemoved()`, `.refreshLikeButtonForCurrent()`, `.songIdOf()`, `.updateCustomLayout()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Cover Art Rendering`** (16 nodes): `MHCover.kt`, `drawArc()`, `drawArtist()`, `drawBlob()`, `drawDot()`, `drawDuotone()`, `drawGrid()`, `drawMoon()`, `drawStripes()`, `drawTriangles()`, `drawType()`, `drawWave()`, `MHCover()`, `mhCoverFor()`, `MHCoverKind`, `MHCoverPalette`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Generated Cover Art`** (15 nodes): `GeneratedCover.kt`, `AutoPlaylistFamily`, `autoPlaylistGradient()`, `badgeFor()`, `drawCapsule()`, `drawDaily()`, `drawMood()`, `drawNext()`, `drawRadar()`, `drawReleases()`, `drawRotation()`, `familyEyebrow()`, `familyOf()`, `GeneratedCover()`, `paletteFor()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Playlist Data Layer`** (14 nodes): `PlaylistRepository.kt`, `PlaylistRepository`, `.acceptShare()`, `.addSong()`, `.create()`, `.createShare()`, `.delete()`, `.detail()`, `.list()`, `.previewShare()`, `.refreshDailyMix()`, `.removeSong()`, `.rename()`, `.reorder()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Playlists Screen`** (13 nodes): `PlaylistsScreen.kt`, `CreatePlaylistDialog()`, `EmptyPlaylistMessage()`, `FilterRow()`, `ImportFromSpotifyRow()`, `LibraryFilter`, `LibraryList()`, `LibraryTopBar()`, `LikedSongsRow()`, `PlaylistsScreen()`, `PlaylistTile()`, `pluralizeSongs()`, `TabPlaceholder()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Prefetch Orchestrator`** (12 nodes): `PrefetchOrchestrator.kt`, `PrefetchOrchestrator`, `.applyGate()`, `.cancelAll()`, `.install()`, `.neighborUris()`, `.onMediaItemTransition()`, `.onTimelineChanged()`, `.postToPlayerLooper()`, `.release()`, `.reschedule()`, `.startPrefetch()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Download Repository`** (11 nodes): `DownloadRepository.kt`, `DownloadRepository`, `.download()`, `.downloadAll()`, `.init()`, `.isDownloaded()`, `.refreshAsync()`, `.remove()`, `.removeAll()`, `onDownloadChanged()`, `onDownloadRemoved()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `AA Lyrics Ticker`** (10 nodes): `AALyricsTicker`, `.applyDescription()`, `.install()`, `.onTrackChanged()`, `.setAaConnected()`, `.startTicker()`, `.stopTicker()`, `.tickOnce()`, `.uninstall()`, `AALyricsTicker.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `For You Genre Grid`** (10 nodes): `SearchScreen.kt`, `Genre`, `GenreFilterPill()`, `GenreGrid()`, `IdleContent()`, `RecentlyPlayedCarousel()`, `RecentRow()`, `RecentSongCard()`, `SearchField()`, `SearchScreen()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Auth Repository`** (9 nodes): `AuthRepository.kt`, `AuthRepository`, `.anonymousId()`, `.extractToken()`, `.hasEverSignedIn()`, `.markSignedIn()`, `.signIn()`, `.signOut()`, `.tryAutoSignIn()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Player Settings`** (9 nodes): `PlayerSettings.kt`, `PlayerSettings`, `.crossfadeSecondsNow()`, `.downloadAutoNow()`, `.downloadWifiOnlyNow()`, `.setCrossfadeSeconds()`, `.setDownloadAuto()`, `.setDownloadWifiOnly()`, `.setTheme()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Read Cache`** (9 nodes): `ReadCache.kt`, `Keys`, `.playlistDetail()`, `ReadCache`, `.delete()`, `.getJson()`, `.getOrNull()`, `.init()`, `.putJson()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Song Repository`** (8 nodes): `SongRepository.kt`, `SongRepository`, `.downloadVideo()`, `.getDownloadVideoStatus()`, `.getReinitializeStatus()`, `.listSongs()`, `.redownload()`, `.reinitializeVideo()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Connectivity Observer`** (7 nodes): `ConnectivityObserver.kt`, `ConnectivityObserver`, `.init()`, `.recompute()`, `.recordBackendFailure()`, `.recordBackendSuccess()`, `.updateFromCm()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Find Repository`** (7 nodes): `FindRepository.kt`, `FindRepository`, `.create()`, `.delete()`, `.detail()`, `.list()`, `.select()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Network & Auth Token`** (7 nodes): `Network.kt`, `AuthTokenHolder`, `Network`, `.coverUrl()`, `.ensureTrailingSlash()`, `.streamUrl()`, `.videoStreamUrl()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Cover Shape & Color`** (7 nodes): `SpotifyHero.kt`, `cacheKey()`, `CoverShape`, `extractDominantColor()`, `HeroCover()`, `rememberCoverDominantColor()`, `SpotifyHero()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Catalog Repository`** (6 nodes): `CatalogRepository.kt`, `CatalogRepository`, `.getAlbum()`, `.getArtist()`, `.listAlbums()`, `.listArtists()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Follow Repository`** (6 nodes): `FollowRepository.kt`, `FollowRepository`, `.follow()`, `.list()`, `.status()`, `.unfollow()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Liked Repository`** (6 nodes): `LikedRepository.kt`, `LikedRepository`, `.like()`, `.likedSongs()`, `.status()`, `.unlike()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `App Update Installer`** (6 nodes): `AppUpdateInstaller.kt`, `AppUpdateInstaller`, `.apkFile()`, `.launchInstall()`, `.sha256()`, `.startDownload()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `History Repository`** (5 nodes): `HistoryRepository.kt`, `HistoryRepository`, `.recent()`, `.record()`, `.recordImmediate()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Search History Store`** (5 nodes): `SearchHistoryStore.kt`, `SearchHistoryStore`, `.add()`, `.clear()`, `.remove()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Media Download Service`** (5 nodes): `MediaDownloadService.kt`, `MediaDownloadService`, `.getDownloadManager()`, `.getForegroundNotification()`, `.getScheduler()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Player Connection`** (5 nodes): `PlayerConnection.kt`, `onExtrasChanged()`, `PlayerConnection`, `.connect()`, `.release()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Ringtone Exporter`** (5 nodes): `RingtoneExporter.kt`, `RingtoneExporter`, `.deleteExisting()`, `.exportAsAlarm()`, `.sanitizeFilename()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Profile Stats ViewModel`** (5 nodes): `ProfileStatsViewModel.kt`, `ProfileStats`, `ProfileStatsViewModel`, `.format()`, `.refresh()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `App Entry Point`** (4 nodes): `MediaPlayerApp.kt`, `MediaPlayerApp`, `.newImageLoader()`, `.onCreate()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Changelog Preferences`** (4 nodes): `ChangelogPreferences.kt`, `ChangelogPreferences`, `.lastSeenVersion()`, `.markSeen()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `CSV Playlist Parser`** (4 nodes): `CsvPlaylistParser.kt`, `CsvPlaylistParser`, `.parse()`, `.parseCsvLine()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Onboarding Preferences`** (4 nodes): `OnboardingPreferences.kt`, `OnboardingPreferences`, `.isDismissed()`, `.markDismissed()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Download Cache Root`** (4 nodes): `DownloadRoot.kt`, `DownloadRoot`, `.getDownloadCache()`, `.getDownloadManager()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Sleep Timer`** (4 nodes): `SleepTimer.kt`, `SleepTimer`, `.cancel()`, `.set()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Onboarding ViewModel`** (4 nodes): `OnboardingViewModel.kt`, `OnboardingViewModel`, `.skip()`, `.submit()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Lyrics Repository`** (3 nodes): `LyricsRepository.kt`, `LyricsRepository`, `.getLyrics()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Album DTOs`** (3 nodes): `AlbumDetailDto`, `AlbumDto`, `AlbumDto.kt`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Artist DTOs`** (3 nodes): `ArtistDto.kt`, `ArtistDetailDto`, `ArtistDto`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Share DTOs`** (3 nodes): `ShareDtos.kt`, `ShareLinkDto`, `SharePreviewDto`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Player Cache`** (3 nodes): `PlayerCache.kt`, `PlayerCache`, `.get()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `User State`** (3 nodes): `UserState.kt`, `CurrentUser`, `displayInitial()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Cover Shapes`** (3 nodes): `Shapes.kt`, `CoverShapes`, `HeroCoverSize`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `App Update Repository`** (3 nodes): `AppUpdateRepository.kt`, `AppUpdateRepository`, `.latest()`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Spotify Import Track DTO`** (2 nodes): `ImportTrack.kt`, `SpotifyImportTrack`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `App Update DTO`** (2 nodes): `AppUpdateDto.kt`, `AppUpdateDto`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `App Version Request DTO`** (2 nodes): `AppVersionRequest.kt`, `AppVersionRequest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Genre Seed Request DTO`** (2 nodes): `GenreSeedRequest.kt`, `GenreSeedRequest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Lyric Line DTO`** (2 nodes): `LyricLineDto.kt`, `LyricLineDto`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Page Response DTO`** (2 nodes): `PageResponse.kt`, `PageResponse`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Playlist Detail DTO`** (2 nodes): `PlaylistDetailDto.kt`, `PlaylistDetailDto`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Playlist DTO`** (2 nodes): `PlaylistDto.kt`, `PlaylistDto`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Playlist Song Entry DTO`** (2 nodes): `PlaylistSongEntryDto.kt`, `PlaylistSongEntryDto`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Record Play Request DTO`** (2 nodes): `RecordPlayRequest.kt`, `RecordPlayRequest`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Reinit Status DTO`** (2 nodes): `ReinitStatusDto.kt`, `ReinitStatusDto`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Song DTO`** (2 nodes): `SongDto.kt`, `SongDto`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Spotify Import Result DTO`** (2 nodes): `SpotifyImportResultDto.kt`, `SpotifyImportResultDto`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Stats DTO`** (2 nodes): `StatsDto.kt`, `StatsDto`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `User DTO`** (2 nodes): `UserDto.kt`, `UserDto`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.
- **Thin community `Spacing Tokens`** (2 nodes): `Spacing.kt`, `MediaPlayerSpacing`
  Too small to be a meaningful cluster - may be noise or needs more connections extracted.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `PlaybackViewModel` connect `Playback State & Controls` to `Playback UI State Types`?**
  _High betweenness centrality (0.003) - this node is a cross-community bridge._
- **What connects `BottomDestination`, `AppVersion`, `ChangelogEntry` to the rest of the system?**
  _131 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `Backend API Interface` be split into smaller, more focused modules?**
  _Cohesion score 0.04 - nodes in this community are weakly interconnected._
- **Should `Android Auto Library Tree` be split into smaller, more focused modules?**
  _Cohesion score 0.04 - nodes in this community are weakly interconnected._
- **Should `Playback State & Controls` be split into smaller, more focused modules?**
  _Cohesion score 0.05 - nodes in this community are weakly interconnected._
- **Should `Home Screen UI` be split into smaller, more focused modules?**
  _Cohesion score 0.07 - nodes in this community are weakly interconnected._
- **Should `Media Playback Service` be split into smaller, more focused modules?**
  _Cohesion score 0.08 - nodes in this community are weakly interconnected._