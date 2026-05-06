// MusicHub — All screens canvas
const { HomeScreen, SearchScreen, LibraryScreen, AlbumScreen,
  NowPlayingScreen, ArtistScreen, PlaylistScreen, ProfileScreen } = window.MHScreens;
const { LyricsScreen, VideoScreen } = window.MHExtraScreens;
const { ForYouScreen, GeneratedDetailScreen } = window.MHForYou;
const { AAFrame, AAHome, AASearch, AALibrary, AAAlbum, AANowPlaying,
  AALyrics, AAVideo, AAArtist, AAProfile, AAQueue, AAVoice, AARecents, AAForYou } = window.MHAutoScreens;
const { IconsScreen } = window.MHIcons;
const { LoginScreen, OnboardingScreen: OnboardingTags, OnboardingSheet, AccountSwitchDialog } = window.MHAuth;
const { AuthProbeScreen, OnboardingErrorScreen, OnboardingSavingScreen, OnboardingNeedsMoreScreen,
        LoginSigningInScreen, LoginPickerCancelScreen, OnboardingSheetExplainer } = window.MHAuthStates;
  const { FindScreen, SpotifyImportScreen } = window.MHDiscover;
  const { FindIdleEmpty, FindIdleActive, FindIdlePullRefresh, FindSearching,
          FindCandidatesSelected, FindTerminalScreen, FindLifecycle,
          SpotifyImportIdle, SpotifyImportFetching, SpotifyImportError,
          SpotifyImportConfirming, SpotifyImportDone } = window.MHDiscoverStates;
const { CrossfadeScreen, DownloadOfflineScreen, ThemeScreen, DislikedScreen } = window.MHSettings;
const { QueueSheet, EqualizerSheet: EqualizerScreen, SleepTimerSheet, MiniPlayerSwipe,
        PlaybackErrorDialog, ReportSongDialog: ReportWrongSongSheet,
        AddToPlaylistSheet, AddSongsToPlaylistSheet: AddSongsScreen, TrackActionSheet } = window.MHPlayerSheets;
const { AlbumListScreen, ArtistListScreen, LikedScreen, GenreDetailScreen,
        CollabPlaylistOwner, CollabPlaylistMember, PlaylistShareDialog, PlaylistShareImporter } = window.MHLibrary;
const { LibraryLoadingScreen, LibraryPullRefreshScreen, LibraryErrorRetryScreen, LibraryLoadMoreScreen,
        LikedEmptyScreen, AlbumsEmptyScreen, LibraryTrackKebabSheet,
        AutoPlaylistDetailScreen, PlaylistGesturesScreen, PlaylistDetailWithTopBar,
        LibraryLandingPlusScreen, ScaricatiEmptyScreen,
        AppUpdateDialog, LibraryOfflineBadgeScreen } = window.MHLibraryStates;
const { AppUpdateBannerHome, ChangelogSheet, QueuedEventsScreen } = window.MHUpdate;
const { AAGenres, AANowPlayingTicker, AASleepDriving } = window.MHAutoExtra;
const { TrimTrackScreen } = window.MHTrim;

const W = 390, H = 844;

function Phone({ children }) {
  return <IOSDevice width={W} height={H} dark={true}>{children}</IOSDevice>;
}

function App() {
  return (
    <DesignCanvas>
      <DCSection id="auth" title="Onboarding · Auth" subtitle="Login self-hosted, scelta tag, account switch">
        <DCArtboard id="login" label="00a · Login" width={W} height={H}><Phone><LoginScreen/></Phone></DCArtboard>
        <DCArtboard id="tags" label="00b · Onboarding tag" width={W} height={H}><Phone><OnboardingTags/></Phone></DCArtboard>
        <DCArtboard id="acct" label="00c · Cambio account" width={W} height={H}><Phone><AccountSwitchDialog/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="auth-states" title="Auth · Stati impl-only" subtitle="Stati presenti nel codice ma assenti dal mockup originale (AuthViewModel · LoginScreen · OnboardingScreen)">
        <DCArtboard id="auth-probe-token" label="State.Loading · token refresh" width={W} height={H}><Phone><AuthProbeScreen stage="token"/></Phone></DCArtboard>
        <DCArtboard id="auth-probe-me" label="State.Loading · refreshMe()" width={W} height={H}><Phone><AuthProbeScreen stage="me"/></Phone></DCArtboard>
        <DCArtboard id="auth-probe-rejected" label="Token rejected · silent clear" width={W} height={H}><Phone><AuthProbeScreen stage="rejected-silent"/></Phone></DCArtboard>
        <DCArtboard id="login-signing" label="Login · signing-in" width={W} height={H}><Phone><LoginSigningInScreen/></Phone></DCArtboard>
        <DCArtboard id="login-cancel" label="Login · picker cancel" width={W} height={H}><Phone><LoginPickerCancelScreen/></Phone></DCArtboard>
        <DCArtboard id="onb-needs-more" label="Onboarding · Scegli ancora N" width={W} height={H}><Phone><OnboardingNeedsMoreScreen/></Phone></DCArtboard>
        <DCArtboard id="onb-saving" label="Onboarding · saving spinner" width={W} height={H}><Phone><OnboardingSavingScreen/></Phone></DCArtboard>
        <DCArtboard id="onb-error" label="Onboarding · errore seedGenres" width={W} height={H}><Phone><OnboardingErrorScreen/></Phone></DCArtboard>
        <DCArtboard id="sheet-explainer" label="OnboardingSheet · 3-feature explainer" width={W} height={H}><Phone><OnboardingSheetExplainer/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="mobile" title="Mobile · iOS / Android" subtitle="Schermate principali del client mobile">
        <DCArtboard id="home" label="01 · Home" width={W} height={H}><Phone><HomeScreen/></Phone></DCArtboard>
        <DCArtboard id="search" label="02 · Cerca" width={W} height={H}><Phone><SearchScreen/></Phone></DCArtboard>
        <DCArtboard id="library" label="03 · Libreria" width={W} height={H}><Phone><LibraryScreen/></Phone></DCArtboard>
        <DCArtboard id="album" label="04 · Album" width={W} height={H}><Phone><AlbumScreen/></Phone></DCArtboard>
        <DCArtboard id="now" label="05 · In riproduzione" width={W} height={H}><Phone><NowPlayingScreen/></Phone></DCArtboard>
        <DCArtboard id="artist" label="06 · Artista" width={W} height={H}><Phone><ArtistScreen/></Phone></DCArtboard>
        <DCArtboard id="playlist" label="07 · Playlist" width={W} height={H}><Phone><PlaylistScreen/></Phone></DCArtboard>
        <DCArtboard id="profile" label="08 · Profilo" width={W} height={H}><Phone><ProfileScreen/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="library-deep" title="Libreria · Drill-down" subtitle="Liste dedicate, mi piace, generi">
        <DCArtboard id="lib-albums" label="03a · Album list" width={W} height={H}><Phone><AlbumListScreen/></Phone></DCArtboard>
        <DCArtboard id="lib-artists" label="03b · Artist list" width={W} height={H}><Phone><ArtistListScreen/></Phone></DCArtboard>
        <DCArtboard id="lib-liked" label="03c · Mi piace" width={W} height={H}><Phone><LikedScreen/></Phone></DCArtboard>
        <DCArtboard id="lib-genre" label="03d · Genere · Indie" width={W} height={H}><Phone><GenreDetailScreen/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="library-states" title="Libreria · Stati impl-only" subtitle="Loading, empty, errore/retry, paginazione, kebab w/ dislike, auto-playlist, gesture, top-bar, profilo, Spotify row, Scaricati, overlay generici">
        <DCArtboard id="lib-loading" label="Loading · shimmer" width={W} height={H}><Phone><LibraryLoadingScreen/></Phone></DCArtboard>
        <DCArtboard id="lib-pull" label="Pull-to-refresh" width={W} height={H}><Phone><LibraryPullRefreshScreen/></Phone></DCArtboard>
        <DCArtboard id="lib-error" label="Errore · retry" width={W} height={H}><Phone><LibraryErrorRetryScreen/></Phone></DCArtboard>
        <DCArtboard id="lib-loadmore" label="Paginazione · load-more" width={W} height={H}><Phone><LibraryLoadMoreScreen/></Phone></DCArtboard>
        <DCArtboard id="lib-empty-liked" label="Empty · Mi piace" width={W} height={H}><Phone><LikedEmptyScreen/></Phone></DCArtboard>
        <DCArtboard id="lib-empty-albums" label="Empty · Album" width={W} height={H}><Phone><AlbumsEmptyScreen/></Phone></DCArtboard>
        <DCArtboard id="lib-kebab" label="Kebab · dislike + flag" width={W} height={H}><Phone><LibraryTrackKebabSheet/></Phone></DCArtboard>
        <DCArtboard id="lib-auto" label="Playlist · auto variant" width={W} height={H}><Phone><AutoPlaylistDetailScreen/></Phone></DCArtboard>
        <DCArtboard id="lib-gestures" label="Playlist · swipe + drag" width={W} height={H}><Phone><PlaylistGesturesScreen/></Phone></DCArtboard>
        <DCArtboard id="lib-topbar" label="Playlist · refresh + add (top-bar)" width={W} height={H}><Phone><PlaylistDetailWithTopBar/></Phone></DCArtboard>
        <DCArtboard id="lib-landing-plus" label="Landing · profilo + Spotify row" width={W} height={H}><Phone><LibraryLandingPlusScreen/></Phone></DCArtboard>
        <DCArtboard id="lib-scaricati" label="Scaricati · placeholder" width={W} height={H}><Phone><ScaricatiEmptyScreen/></Phone></DCArtboard>
        <DCArtboard id="lib-update" label="Overlay · app-update dialog" width={W} height={H}><Phone><AppUpdateDialog/></Phone></DCArtboard>
        <DCArtboard id="lib-offline" label="Overlay · offline badge" width={W} height={H}><Phone><LibraryOfflineBadgeScreen/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="discover" title="Scoperta · Spotify import" subtitle="Find + flow di import Spotify in 5 step">
        <DCArtboard id="find" label="11 · Find" width={W} height={H}><Phone><FindScreen/></Phone></DCArtboard>
        <DCArtboard id="sp" label="Spotify · Importazione" width={W} height={H}><Phone><SpotifyImportScreen/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="discover-states" title="Find · Stati impl-only" subtitle="Background tracking, terminal status, lifecycle (FindViewModel · ActiveRequestRow · IdleBody · StatusHeader)">
        <DCArtboard id="find-idle-empty" label="Idle · vuoto" width={W} height={H}><Phone><FindIdleEmpty/></Phone></DCArtboard>
        <DCArtboard id="find-idle-active" label="Idle · richieste in background" width={W} height={H}><Phone><FindIdleActive/></Phone></DCArtboard>
        <DCArtboard id="find-pull" label="Idle · pull-to-refresh" width={W} height={H}><Phone><FindIdlePullRefresh/></Phone></DCArtboard>
        <DCArtboard id="find-searching" label="Searching · pure (no candidates)" width={W} height={H}><Phone><FindSearching/></Phone></DCArtboard>
        <DCArtboard id="find-unlocking" label="StatusHeader · UNLOCKING + selezione" width={W} height={H}><Phone><FindCandidatesSelected phase="unlocking"/></Phone></DCArtboard>
        <DCArtboard id="find-downloading" label="StatusHeader · DOWNLOADING + selezione" width={W} height={H}><Phone><FindCandidatesSelected phase="downloading"/></Phone></DCArtboard>
        <DCArtboard id="find-imported" label="Terminal · IMPORTED" width={W} height={H}><Phone><FindTerminalScreen kind="IMPORTED"/></Phone></DCArtboard>
        <DCArtboard id="find-imp-partial" label="Terminal · IMPORTED_PARTIAL" width={W} height={H}><Phone><FindTerminalScreen kind="IMPORTED_PARTIAL"/></Phone></DCArtboard>
        <DCArtboard id="find-failed" label="Terminal · FAILED" width={W} height={H}><Phone><FindTerminalScreen kind="FAILED"/></Phone></DCArtboard>
        <DCArtboard id="find-canceled" label="Terminal · CANCELED" width={W} height={H}><Phone><FindTerminalScreen kind="CANCELED"/></Phone></DCArtboard>
        <DCArtboard id="find-life-pause" label="Lifecycle · onPause (jobs paused)" width={W} height={H}><Phone><FindLifecycle paused/></Phone></DCArtboard>
        <DCArtboard id="find-life-resume" label="Lifecycle · onResume (jobs resumed)" width={W} height={H}><Phone><FindLifecycle paused={false}/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="spotify-states" title="Spotify Import · Stati impl-only" subtitle="CSV parse, errore, rinomina playlist, plurali, CTA mancanti">
        <DCArtboard id="sp-idle" label="Idle · Scegli file CSV" width={W} height={H}><Phone><SpotifyImportIdle/></Phone></DCArtboard>
        <DCArtboard id="sp-fetch" label="FetchingPlaylist · Leggo il file…" width={W} height={H}><Phone><SpotifyImportFetching/></Phone></DCArtboard>
        <DCArtboard id="sp-error" label="Error · header mancanti" width={W} height={H}><Phone><SpotifyImportError/></Phone></DCArtboard>
        <DCArtboard id="sp-confirm" label="Confirming · rinomina + Annulla" width={W} height={H}><Phone><SpotifyImportConfirming/></Phone></DCArtboard>
        <DCArtboard id="sp-done-plural" label="Done · 279 brani aggiunti" width={W} height={H}><Phone><SpotifyImportDone variant="plural"/></Phone></DCArtboard>
        <DCArtboard id="sp-done-singular" label="Done · 1 brano aggiunto" width={W} height={H}><Phone><SpotifyImportDone variant="singular"/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="trim" title="Modalità · Taglia traccia" subtitle="Doppia timeline · ascolto libero in alto, IN/OUT del taglio in basso">
        <DCArtboard id="trim-edit" label="Editing · in attesa" width={W} height={H}><Phone><TrimTrackScreen state="editing"/></Phone></DCArtboard>
        <DCArtboard id="trim-play" label="Preview in riproduzione" width={W} height={H}><Phone><TrimTrackScreen state="playingPreview"/></Phone></DCArtboard>
        <DCArtboard id="trim-saved" label="Salvato · toast" width={W} height={H}><Phone><TrimTrackScreen state="savedToast"/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="player" title="Player · Sheets & stati" subtitle="Coda, EQ, sleep timer, mini-player swipe, errori">
        <DCArtboard id="queue" label="Coda" width={W} height={H}><Phone><QueueSheet/></Phone></DCArtboard>
        <DCArtboard id="eq" label="Equalizzatore" width={W} height={H}><Phone><EqualizerScreen/></Phone></DCArtboard>
        <DCArtboard id="sleep" label="Sleep timer" width={W} height={H}><Phone><SleepTimerSheet/></Phone></DCArtboard>
        <DCArtboard id="mini-swipe" label="Mini-player swipe" width={W} height={H}><Phone><MiniPlayerSwipe/></Phone></DCArtboard>
        <DCArtboard id="err-play" label="Errore di playback" width={W} height={H}><Phone><PlaybackErrorDialog/></Phone></DCArtboard>
        <DCArtboard id="report" label="Brano sbagliato" width={W} height={H}><Phone><ReportWrongSongSheet/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="sheets" title="Sheets · Action menu" subtitle="Aggiungi a playlist, aggiungi brani, azioni traccia">
        <DCArtboard id="add-pl" label="Aggiungi a playlist" width={W} height={H}><Phone><AddToPlaylistSheet/></Phone></DCArtboard>
        <DCArtboard id="add-songs" label="Aggiungi brani" width={W} height={H}><Phone><AddSongsScreen/></Phone></DCArtboard>
        <DCArtboard id="track-act" label="Azioni traccia" width={W} height={H}><Phone><TrackActionSheet/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="collab" title="Playlist collaborative" subtitle="Owner/member, share-link, importer">
        <DCArtboard id="collab-own" label="Collab · Owner" width={W} height={H}><Phone><CollabPlaylistOwner/></Phone></DCArtboard>
        <DCArtboard id="collab-mem" label="Collab · Member" width={W} height={H}><Phone><CollabPlaylistMember/></Phone></DCArtboard>
        <DCArtboard id="collab-share" label="Share dialog" width={W} height={H}><Phone><PlaylistShareDialog/></Phone></DCArtboard>
        <DCArtboard id="collab-imp-1" label="Importer · prima volta" width={W} height={H}><Phone><PlaylistShareImporter state="first"/></Phone></DCArtboard>
        <DCArtboard id="collab-imp-2" label="Importer · già membro" width={W} height={H}><Phone><PlaylistShareImporter state="member"/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="settings-deep" title="Impostazioni · Sub-pagine" subtitle="Crossfade, download offline, tema, non consigliarmi">
        <DCArtboard id="set-cross" label="Crossfade & gapless" width={W} height={H}><Phone><CrossfadeScreen/></Phone></DCArtboard>
        <DCArtboard id="set-dl" label="Download & offline" width={W} height={H}><Phone><DownloadOfflineScreen/></Phone></DCArtboard>
        <DCArtboard id="set-theme" label="Tema & accent" width={W} height={H}><Phone><ThemeScreen/></Phone></DCArtboard>
        <DCArtboard id="set-disliked" label="Non consigliarmi" width={W} height={H}><Phone><DislikedScreen/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="update" title="Aggiornamenti · App update channel" subtitle="Banner home in 3 stati, changelog sheet, eventi in coda">
        <DCArtboard id="upd-avail" label="Banner · Disponibile" width={W} height={H}><Phone><AppUpdateBannerHome state="available"/></Phone></DCArtboard>
        <DCArtboard id="upd-prog" label="Banner · Download" width={W} height={H}><Phone><AppUpdateBannerHome state="progress"/></Phone></DCArtboard>
        <DCArtboard id="upd-fail" label="Banner · Fallito" width={W} height={H}><Phone><AppUpdateBannerHome state="failed"/></Phone></DCArtboard>
        <DCArtboard id="upd-cl" label="Changelog sheet" width={W} height={H}><Phone><ChangelogSheet/></Phone></DCArtboard>
        <DCArtboard id="upd-q" label="Eventi in coda" width={W} height={H}><Phone><QueuedEventsScreen/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="foryou" title="Per te · Playlist generate" subtitle="Sistema di playlist algoritmiche">
        <DCArtboard id="foryou-hub" label="Per te · Hub" width={W} height={H}><Phone><ForYouScreen/></Phone></DCArtboard>
        <DCArtboard id="foryou-mix" label="Dettaglio · Mix 02" width={W} height={H}><Phone><GeneratedDetailScreen playlistId="mix-2"/></Phone></DCArtboard>
        <DCArtboard id="foryou-rotation" label="Dettaglio · Rotation" width={W} height={H}><Phone><GeneratedDetailScreen playlistId="rotation"/></Phone></DCArtboard>
        <DCArtboard id="foryou-releases" label="Dettaglio · Nuove uscite" width={W} height={H}><Phone><GeneratedDetailScreen playlistId="releases"/></Phone></DCArtboard>
        <DCArtboard id="foryou-radar" label="Dettaglio · Radar" width={W} height={H}><Phone><GeneratedDetailScreen playlistId="radar"/></Phone></DCArtboard>
        <DCArtboard id="foryou-capsule" label="Dettaglio · Time capsule" width={W} height={H}><Phone><GeneratedDetailScreen playlistId="capsule"/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="media" title="Media · Lyrics & Video" subtitle="Esperienze full-screen mobile">
        <DCArtboard id="lyrics" label="09 · Lyrics sincronizzato" width={W} height={H}><Phone><LyricsScreen/></Phone></DCArtboard>
        <DCArtboard id="video" label="10 · Video player" width={W} height={H}><Phone><VideoScreen/></Phone></DCArtboard>
      </DCSection>

      <DCSection id="auto" title="Android Auto · 1280×720" subtitle="Stesse funzioni del mobile, ottimizzate per uso in auto">
        <DCArtboard id="aa-home" label="AA · Home" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AAHome/></AAFrame></DCArtboard>
        <DCArtboard id="aa-foryou" label="AA · Per te" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AAForYou/></AAFrame></DCArtboard>
        <DCArtboard id="aa-search" label="AA · Cerca" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AASearch/></AAFrame></DCArtboard>
        <DCArtboard id="aa-genres" label="AA · Tutti i generi" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AAGenres/></AAFrame></DCArtboard>
        <DCArtboard id="aa-voice" label="AA · Comando vocale" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AAVoice/></AAFrame></DCArtboard>
        <DCArtboard id="aa-library" label="AA · Libreria" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AALibrary/></AAFrame></DCArtboard>
        <DCArtboard id="aa-recents" label="AA · Recenti" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AARecents/></AAFrame></DCArtboard>
        <DCArtboard id="aa-album" label="AA · Album / Playlist" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AAAlbum/></AAFrame></DCArtboard>
        <DCArtboard id="aa-artist" label="AA · Artista" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AAArtist/></AAFrame></DCArtboard>
        <DCArtboard id="aa-now" label="AA · In riproduzione" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AANowPlaying/></AAFrame></DCArtboard>
        <DCArtboard id="aa-now-tick" label="AA · NP · lyric ticker" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AANowPlayingTicker/></AAFrame></DCArtboard>
        <DCArtboard id="aa-queue" label="AA · Coda" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AAQueue/></AAFrame></DCArtboard>
        <DCArtboard id="aa-lyrics" label="AA · Testo" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AALyrics/></AAFrame></DCArtboard>
        <DCArtboard id="aa-video" label="AA · Video (driver-safe)" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AAVideo/></AAFrame></DCArtboard>
        <DCArtboard id="aa-sleep" label="AA · Sleep timer driver" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AASleepDriving/></AAFrame></DCArtboard>
        <DCArtboard id="aa-profile" label="AA · Profilo" width={1280} height={720}><AAFrame label="ANDROID AUTO"><AAProfile/></AAFrame></DCArtboard>
      </DCSection>

      <DCSection id="brand" title="Brand · Launcher icons" subtitle="Adaptive icon Android — 3 varianti × 2 temi + esporti">
        <DCArtboard id="icons-all" label="Launcher icons · review board" width={1320} height={2400}>
          <IconsScreen/>
        </DCArtboard>
      </DCSection>
    </DesignCanvas>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App/>);
