// MusicHub — All screens canvas
const { HomeScreen, SearchScreen, LibraryScreen, AlbumScreen,
  NowPlayingScreen, ArtistScreen, PlaylistScreen, ProfileScreen } = window.MHScreens;
const { LyricsScreen, VideoScreen } = window.MHExtraScreens;
const { ForYouScreen, GeneratedDetailScreen } = window.MHForYou;
const { AAFrame, AAHome, AASearch, AALibrary, AAAlbum, AANowPlaying,
  AALyrics, AAVideo, AAArtist, AAProfile, AAQueue, AAVoice, AARecents, AAForYou } = window.MHAutoScreens;

const W = 390, H = 844;

function Phone({ children }) {
  return <IOSDevice width={W} height={H} dark={true}>{children}</IOSDevice>;
}

function App() {
  return (
    <DesignCanvas>
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
      <DCSection id="foryou" title="Per te · Playlist generate" subtitle="Sistema di playlist algoritmiche di MusicHub">
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
        <DCArtboard id="aa-home" label="AA · Home" width={1280} height={720}>
          <AAFrame label="ANDROID AUTO"><AAHome/></AAFrame>
        </DCArtboard>
        <DCArtboard id="aa-foryou" label="AA · Per te" width={1280} height={720}>
          <AAFrame label="ANDROID AUTO"><AAForYou/></AAFrame>
        </DCArtboard>
        <DCArtboard id="aa-search" label="AA · Cerca" width={1280} height={720}>
          <AAFrame label="ANDROID AUTO"><AASearch/></AAFrame>
        </DCArtboard>
        <DCArtboard id="aa-voice" label="AA · Comando vocale" width={1280} height={720}>
          <AAFrame label="ANDROID AUTO"><AAVoice/></AAFrame>
        </DCArtboard>
        <DCArtboard id="aa-library" label="AA · Libreria" width={1280} height={720}>
          <AAFrame label="ANDROID AUTO"><AALibrary/></AAFrame>
        </DCArtboard>
        <DCArtboard id="aa-recents" label="AA · Recenti" width={1280} height={720}>
          <AAFrame label="ANDROID AUTO"><AARecents/></AAFrame>
        </DCArtboard>
        <DCArtboard id="aa-album" label="AA · Album / Playlist" width={1280} height={720}>
          <AAFrame label="ANDROID AUTO"><AAAlbum/></AAFrame>
        </DCArtboard>
        <DCArtboard id="aa-artist" label="AA · Artista" width={1280} height={720}>
          <AAFrame label="ANDROID AUTO"><AAArtist/></AAFrame>
        </DCArtboard>
        <DCArtboard id="aa-now" label="AA · In riproduzione" width={1280} height={720}>
          <AAFrame label="ANDROID AUTO"><AANowPlaying/></AAFrame>
        </DCArtboard>
        <DCArtboard id="aa-queue" label="AA · Coda" width={1280} height={720}>
          <AAFrame label="ANDROID AUTO"><AAQueue/></AAFrame>
        </DCArtboard>
        <DCArtboard id="aa-lyrics" label="AA · Testo" width={1280} height={720}>
          <AAFrame label="ANDROID AUTO"><AALyrics/></AAFrame>
        </DCArtboard>
        <DCArtboard id="aa-video" label="AA · Video (driver-safe)" width={1280} height={720}>
          <AAFrame label="ANDROID AUTO"><AAVideo/></AAFrame>
        </DCArtboard>
        <DCArtboard id="aa-profile" label="AA · Profilo" width={1280} height={720}>
          <AAFrame label="ANDROID AUTO"><AAProfile/></AAFrame>
        </DCArtboard>
      </DCSection>
    </DesignCanvas>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App/>);
