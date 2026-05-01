// MusicHub — All screens canvas
const { HomeScreen, SearchScreen, LibraryScreen, AlbumScreen,
  NowPlayingScreen, ArtistScreen, PlaylistScreen, ProfileScreen } = window.MHScreens;

const W = 390, H = 844;

function Phone({ children }) {
  return <IOSDevice width={W} height={H} dark={true}>{children}</IOSDevice>;
}

function App() {
  return (
    <DesignCanvas>
      <DCSection id="core" title="MusicHub · App Mockup" subtitle="Tutte le schermate principali">
        <DCArtboard id="home" label="01 · Home" width={W} height={H}><Phone><HomeScreen/></Phone></DCArtboard>
        <DCArtboard id="search" label="02 · Cerca" width={W} height={H}><Phone><SearchScreen/></Phone></DCArtboard>
        <DCArtboard id="library" label="03 · Libreria" width={W} height={H}><Phone><LibraryScreen/></Phone></DCArtboard>
        <DCArtboard id="album" label="04 · Album" width={W} height={H}><Phone><AlbumScreen/></Phone></DCArtboard>
        <DCArtboard id="now" label="05 · In riproduzione" width={W} height={H}><Phone><NowPlayingScreen/></Phone></DCArtboard>
        <DCArtboard id="artist" label="06 · Artista" width={W} height={H}><Phone><ArtistScreen/></Phone></DCArtboard>
        <DCArtboard id="playlist" label="07 · Playlist" width={W} height={H}><Phone><PlaylistScreen/></Phone></DCArtboard>
        <DCArtboard id="profile" label="08 · Profilo" width={W} height={H}><Phone><ProfileScreen/></Phone></DCArtboard>
      </DCSection>
    </DesignCanvas>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App/>);
