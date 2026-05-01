// MusicHub — All Screens
// Each screen is a self-contained component that mounts inside an IOSDevice.

const { T, I, MHCover, MHPlayingBars, MHLogo, MHPlayerBar, MHBottomNav, MHScreen, MHSectionHeader } = window.MH;

// ─────────────────────────────────────────────────────────
// 01 · HOME
// ─────────────────────────────────────────────────────────
function HomeScreen() {
  const [chip, setChip] = React.useState('Tutto');
  const QUICK = [
    { id: 'a', title: 'Echo, Vol. III', kind: 'type', palette: { bg: '#E8E2D5', fg: '#222' }, badge: 'NEW' },
    { id: 'b', title: 'Night Mode', kind: 'dot', palette: { bg: '#0B3D2E', fg: T.ACCENT } },
    { id: 'c', title: 'Liked songs', kind: 'duotone', palette: { a: '#3A0CA3', b: '#F72585' } },
    { id: 'd', title: 'Slow Hours', kind: 'arc', palette: { a: '#FF6B5B', b: '#3A1F8A' } },
  ];
  const TRACKS = [
    { id: 1, title: 'Carbon Mirror', artist: 'Lou Hessler', dur: '3:42', kind: 'grid', palette: { a: '#0E1F3A', b: T.ACCENT } },
    { id: 2, title: 'Long way home', artist: 'Marina Vega', dur: '4:18', kind: 'moon', palette: { bg: '#E8DCC4', fg: '#1A1A1A' }, liked: true },
    { id: 3, title: 'Pyre', artist: 'Tobi Akin · Sero', dur: '2:55', kind: 'triangles', palette: { bg: '#1A1A1A', fg: '#FF4D2E' } },
    { id: 4, title: 'Plein soleil', artist: 'Camille Roche', dur: '3:31', kind: 'wave', palette: { a: '#5C2D8C', b: '#F0A6B0' } },
    { id: 5, title: 'Glasshouse', artist: 'The Astral Kit', dur: '5:07', kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
    { id: 6, title: 'Undertow', artist: 'Helena Vorr', dur: '4:02', kind: 'blob', palette: { a: '#1E3A8A', b: '#06B6D4' }, playing: true },
  ];

  const greeting = (() => {
    const h = new Date().getHours();
    if (h < 12) return 'Buongiorno';
    if (h < 18) return 'Buon pomeriggio';
    return 'Buonasera';
  })();

  return (
    <MHScreen navActive="home">
      <div style={{ padding: '8px 16px 14px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <MHLogo/>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <I.Bell/><I.History/><I.Settings/>
        </div>
      </div>
      <div style={{ padding: '4px 16px 14px' }}>
        <div style={{ fontSize: 26, fontWeight: 800, letterSpacing: -0.8, lineHeight: 1.1 }}>{greeting}</div>
        <div style={{ fontSize: 12.5, color: T.TEXT_LO, marginTop: 4, fontFamily: T.MONO, letterSpacing: 0.2 }}>
          Ven 1 Mag · 3 nuove uscite per te
        </div>
      </div>
      <div style={{ display: 'flex', gap: 6, padding: '0 16px 16px', overflowX: 'auto' }}>
        {['Tutto', 'Musica', 'Playlist', 'Artisti'].map(c => (
          <button key={c} onClick={() => setChip(c)} style={{
            border: 'none', cursor: 'pointer', padding: '6px 14px', borderRadius: 999,
            background: c === chip ? T.ACCENT : 'rgba(255,255,255,0.08)',
            color: c === chip ? '#0a0a0a' : T.TEXT_HI,
            fontSize: 12.5, fontWeight: 600, fontFamily: 'inherit', whiteSpace: 'nowrap',
          }}>{c}</button>
        ))}
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8, padding: '0 16px' }}>
        {QUICK.map(q => (
          <div key={q.id} style={{
            display: 'flex', alignItems: 'center', gap: 10,
            background: T.CARD, borderRadius: 8, padding: 8, paddingRight: 12, cursor: 'pointer',
          }}>
            <div style={{ width: 48, height: 48, flexShrink: 0 }}>
              <MHCover kind={q.kind} palette={q.palette} radius={4}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ color: T.TEXT_HI, fontSize: 13.5, fontWeight: 600,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{q.title}</div>
              {q.badge && <div style={{ marginTop: 3, fontFamily: T.MONO, fontSize: 9, fontWeight: 600, letterSpacing: 1, color: T.ACCENT }}>{q.badge}</div>}
            </div>
          </div>
        ))}
      </div>
      <MHSectionHeader eyebrow="// Per te" title="Brani consigliati"/>
      <div style={{ padding: '4px 0 12px' }}>
        {TRACKS.map((t, i) => (
          <div key={t.id} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px' }}>
            <div style={{ width: 14, textAlign: 'center', flexShrink: 0, fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO }}>
              {String(i + 1).padStart(2, '0')}
            </div>
            <div style={{ width: 44, height: 44, flexShrink: 0 }}>
              <MHCover kind={t.kind} palette={t.palette} radius={4}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 600, color: t.playing ? T.ACCENT : T.TEXT_HI,
                display: 'flex', alignItems: 'center', gap: 6,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {t.title}{t.playing && <MHPlayingBars/>}
              </div>
              <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 2,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.artist}</div>
            </div>
            <I.Heart filled={t.liked} color={t.liked ? T.ACCENT : T.TEXT_LO}/>
            <div style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO, width: 32, textAlign: 'right' }}>{t.dur}</div>
            <I.More/>
          </div>
        ))}
      </div>
      <div style={{ textAlign: 'center', padding: '8px 16px 24px',
        fontFamily: T.MONO, fontSize: 10, color: 'rgba(255,255,255,0.25)', letterSpacing: 1 }}>— FINE FEED —</div>
    </MHScreen>
  );
}

// ─────────────────────────────────────────────────────────
// 02 · SEARCH
// ─────────────────────────────────────────────────────────
function SearchScreen() {
  const GENRES = [
    { name: 'Indie', color: '#3A0CA3', kind: 'arc', palette: { a: '#3A0CA3', b: '#F72585' } },
    { name: 'Elettronica', color: '#06B6D4', kind: 'wave', palette: { a: '#1E3A8A', b: '#06B6D4' } },
    { name: 'Hip-hop', color: '#FF4D2E', kind: 'triangles', palette: { bg: '#1A1A1A', fg: '#FF4D2E' } },
    { name: 'Jazz', color: '#FFC857', kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
    { name: 'Classica', color: '#E8DCC4', kind: 'moon', palette: { bg: '#E8DCC4', fg: '#1A1A1A' } },
    { name: 'Ambient', color: '#0B3D2E', kind: 'dot', palette: { bg: '#0B3D2E', fg: T.ACCENT } },
    { name: 'Rock', color: '#5C2D8C', kind: 'grid', palette: { a: '#5C2D8C', b: '#F0A6B0' } },
    { name: 'Pop', color: '#F0A6B0', kind: 'duotone', palette: { a: '#F0A6B0', b: '#3A0CA3' } },
  ];
  const RECENT = ['marina vega', 'long way home', 'tobi akin', 'glasshouse'];

  return (
    <MHScreen navActive="search">
      <div style={{ padding: '8px 16px 4px' }}>
        <div style={{ fontSize: 26, fontWeight: 800, letterSpacing: -0.8 }}>Cerca</div>
      </div>
      <div style={{ padding: '12px 16px 6px' }}>
        <div style={{
          background: 'rgba(255,255,255,0.08)', borderRadius: 10,
          padding: '11px 12px', display: 'flex', alignItems: 'center', gap: 10,
        }}>
          <I.Search size={18} color={T.TEXT_LO}/>
          <div style={{ flex: 1, color: T.TEXT_LO2, fontSize: 14 }}>Brani, artisti, playlist</div>
          <I.Mic size={18} color={T.TEXT_LO}/>
        </div>
      </div>

      <div style={{ padding: '14px 16px 6px', display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT }}>// RECENTI</div>
        <div style={{ fontSize: 12, color: T.TEXT_LO }}>Cancella</div>
      </div>
      <div style={{ padding: '0 16px' }}>
        {RECENT.map(r => (
          <div key={r} style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 0',
            borderBottom: `1px solid ${T.DIVIDER}` }}>
            <I.History size={16} color={T.TEXT_LO}/>
            <div style={{ flex: 1, fontSize: 14, color: T.TEXT_HI }}>{r}</div>
            <I.X size={14} color={T.TEXT_LO2}/>
          </div>
        ))}
      </div>

      <div style={{ padding: '24px 16px 6px' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, marginBottom: 6 }}>// SFOGLIA</div>
        <div style={{ fontSize: 18, fontWeight: 800, letterSpacing: -0.4 }}>Tutti i generi</div>
      </div>
      <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10, padding: '8px 16px' }}>
        {GENRES.map(g => (
          <div key={g.name} style={{
            position: 'relative', height: 90, borderRadius: 10,
            background: g.color, overflow: 'hidden', padding: 12,
          }}>
            <div style={{ fontSize: 16, fontWeight: 800, color: '#fff', letterSpacing: -0.3,
              textShadow: '0 1px 2px rgba(0,0,0,0.3)', position: 'relative', zIndex: 2 }}>{g.name}</div>
            <div style={{ position: 'absolute', right: -10, bottom: -10, width: 60, height: 60,
              transform: 'rotate(20deg)', opacity: 0.85 }}>
              <MHCover kind={g.kind} palette={g.palette} radius={6}/>
            </div>
          </div>
        ))}
      </div>
    </MHScreen>
  );
}

// ─────────────────────────────────────────────────────────
// 03 · LIBRARY
// ─────────────────────────────────────────────────────────
function LibraryScreen() {
  const [tab, setTab] = React.useState('Playlist');
  const ITEMS = [
    { type: 'pinned', title: 'Liked songs', sub: 'Playlist · 247 brani', kind: 'duotone', palette: { a: '#3A0CA3', b: '#F72585' } },
    { type: 'playlist', title: 'Slow Hours', sub: 'Playlist · Marina V.', kind: 'arc', palette: { a: '#FF6B5B', b: '#3A1F8A' } },
    { type: 'album', title: 'Echo, Vol. III', sub: 'Album · Marina Vega', kind: 'type', palette: { bg: '#E8E2D5', fg: '#222' } },
    { type: 'artist', title: 'Tobi Akin', sub: 'Artista', kind: 'artist', palette: { bg: '#2A1E12', fg: '#E8DCC4' }, round: true },
    { type: 'playlist', title: 'Late drives', sub: 'Playlist · 38 brani', kind: 'wave', palette: { a: '#5C2D8C', b: '#F0A6B0' } },
    { type: 'album', title: 'Carbon Mirror', sub: 'Album · Lou Hessler', kind: 'grid', palette: { a: '#0E1F3A', b: T.ACCENT } },
    { type: 'artist', title: 'Helena Vorr', sub: 'Artista', kind: 'artist', palette: { bg: '#1E3A8A', fg: '#E8DCC4' }, round: true },
    { type: 'playlist', title: 'Mattina', sub: 'Playlist · 22 brani', kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
  ];

  return (
    <MHScreen navActive="library">
      <div style={{ padding: '8px 16px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ fontSize: 26, fontWeight: 800, letterSpacing: -0.8 }}>Libreria</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <I.Search size={20} color={T.TEXT_HI}/>
          <I.Plus size={22} color={T.TEXT_HI}/>
        </div>
      </div>
      <div style={{ display: 'flex', gap: 6, padding: '8px 16px 8px', overflowX: 'auto' }}>
        {['Playlist', 'Album', 'Artisti', 'Scaricati'].map(c => (
          <button key={c} onClick={() => setTab(c)} style={{
            border: 'none', cursor: 'pointer', padding: '6px 12px', borderRadius: 999,
            background: c === tab ? T.ACCENT : 'rgba(255,255,255,0.08)',
            color: c === tab ? '#0a0a0a' : T.TEXT_HI,
            fontSize: 12.5, fontWeight: 600, fontFamily: 'inherit', whiteSpace: 'nowrap',
          }}>{c}</button>
        ))}
      </div>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '6px 16px 4px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
          <I.Filter size={14}/>
          <div style={{ fontSize: 12, color: T.TEXT_LO, fontFamily: T.MONO, letterSpacing: 0.5 }}>Recenti</div>
        </div>
        <I.Grid size={16}/>
      </div>
      <div>
        {ITEMS.map((it, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px' }}>
            <div style={{ width: 52, height: 52, flexShrink: 0,
              borderRadius: it.round ? 999 : 6, overflow: 'hidden' }}>
              <MHCover kind={it.kind} palette={it.palette} radius={it.round ? 999 : 6}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14.5, fontWeight: 600, color: T.TEXT_HI,
                display: 'flex', alignItems: 'center', gap: 6 }}>
                {it.type === 'pinned' && (
                  <svg width="11" height="11" viewBox="0 0 24 24" fill={T.ACCENT}>
                    <path d="M14 2l8 8-6 1-4 4-2-2-4 4-2-2 4-4-2-2 4-4z"/>
                  </svg>
                )}
                {it.title}
              </div>
              <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 2 }}>{it.sub}</div>
            </div>
            <I.Chevron size={14}/>
          </div>
        ))}
      </div>
    </MHScreen>
  );
}

// ─────────────────────────────────────────────────────────
// 04 · ALBUM DETAIL
// ─────────────────────────────────────────────────────────
function AlbumScreen() {
  const TRACKS = [
    { n: 1, title: 'Cold lights', dur: '3:14', plays: '1.2M' },
    { n: 2, title: 'Long way home', dur: '4:18', plays: '4.8M', liked: true },
    { n: 3, title: 'Echo (interlude)', dur: '1:42', plays: '820K' },
    { n: 4, title: 'Pyre', dur: '2:55', plays: '2.1M' },
    { n: 5, title: 'Slow hours', dur: '5:07', plays: '3.4M' },
    { n: 6, title: 'Undertow', dur: '4:02', plays: '6.7M', playing: true },
    { n: 7, title: 'Plein soleil', dur: '3:31', plays: '1.9M' },
    { n: 8, title: 'Carbon mirror', dur: '3:42', plays: '2.6M' },
    { n: 9, title: 'Glasshouse', dur: '5:07', plays: '1.1M' },
    { n: 10, title: 'After', dur: '2:18', plays: '740K' },
  ];

  return (
    <MHScreen
      gradient="linear-gradient(180deg, #2A1448 0%, #0F0820 280px, #060309 600px)"
      navActive="home"
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 16px' }}>
        <I.Back/>
        <I.More color={T.TEXT_HI}/>
      </div>

      <div style={{ display: 'flex', justifyContent: 'center', padding: '12px 16px 8px' }}>
        <div style={{ width: 200, height: 200, boxShadow: '0 20px 50px rgba(0,0,0,0.6)' }}>
          <MHCover kind="arc" palette={{ a: '#FF6B5B', b: '#3A1F8A' }} radius={6}/>
        </div>
      </div>

      <div style={{ padding: '12px 16px 4px' }}>
        <div style={{ fontSize: 24, fontWeight: 800, letterSpacing: -0.6, lineHeight: 1.1 }}>Slow Hours</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginTop: 8 }}>
          <div style={{ width: 20, height: 20, borderRadius: 999, overflow: 'hidden' }}>
            <MHCover kind="artist" palette={{ bg: '#2A1E12', fg: '#E8DCC4' }} radius={999}/>
          </div>
          <div style={{ fontSize: 13, fontWeight: 600 }}>Marina Vega</div>
          <I.Verified size={13}/>
        </div>
        <div style={{ fontSize: 11.5, color: T.TEXT_LO, marginTop: 8, fontFamily: T.MONO, letterSpacing: 0.4 }}>
          ALBUM · 2026 · 10 BRANI · 36 MIN
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '14px 16px 12px' }}>
        <I.Heart size={22} color={T.ACCENT} filled/>
        <I.Download size={22}/>
        <I.Share size={20}/>
        <div style={{ flex: 1 }}/>
        <I.Shuffle size={20} color={T.TEXT_HI}/>
        <button style={{
          width: 52, height: 52, borderRadius: 999, background: T.ACCENT, border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 8px 20px rgba(168,224,78,0.4)',
        }}><I.Play size={20}/></button>
      </div>

      <div style={{ padding: '4px 0' }}>
        {TRACKS.map(t => (
          <div key={t.n} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px' }}>
            <div style={{ width: 18, textAlign: 'center', fontFamily: T.MONO, fontSize: 11,
              color: t.playing ? T.ACCENT : T.TEXT_LO }}>
              {t.playing ? <MHPlayingBars/> : String(t.n).padStart(2, '0')}
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 600, color: t.playing ? T.ACCENT : T.TEXT_HI,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.title}</div>
              <div style={{ fontSize: 11, color: T.TEXT_LO, marginTop: 2, fontFamily: T.MONO }}>
                {t.plays} riproduzioni
              </div>
            </div>
            {t.liked && <I.Heart size={16} color={T.ACCENT} filled/>}
            <div style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO, width: 32, textAlign: 'right' }}>{t.dur}</div>
            <I.More/>
          </div>
        ))}
      </div>
    </MHScreen>
  );
}

// ─────────────────────────────────────────────────────────
// 05 · NOW PLAYING (FULL PLAYER)
// ─────────────────────────────────────────────────────────
function NowPlayingScreen() {
  return (
    <div style={{
      width: '100%', height: '100%',
      background: 'linear-gradient(180deg, #1E3A8A 0%, #0E1F3A 50%, #060309 100%)',
      color: T.TEXT_HI, fontFamily: T.FONT,
      display: 'flex', flexDirection: 'column', overflow: 'hidden', paddingTop: 56,
    }}>
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 16px' }}>
        <I.Down/>
        <div style={{ textAlign: 'center' }}>
          <div style={{ fontSize: 11, color: T.TEXT_LO, fontFamily: T.MONO, letterSpacing: 1 }}>IN RIPRODUZIONE DA ALBUM</div>
          <div style={{ fontSize: 13, fontWeight: 600, marginTop: 2 }}>Slow Hours</div>
        </div>
        <I.More color={T.TEXT_HI}/>
      </div>

      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', justifyContent: 'space-between', padding: '24px 24px 32px' }}>
        <div style={{ display: 'flex', justifyContent: 'center' }}>
          <div style={{ width: 300, height: 300, boxShadow: '0 30px 80px rgba(0,0,0,0.7)' }}>
            <MHCover kind="blob" palette={{ a: '#1E3A8A', b: '#06B6D4' }} radius={8}/>
          </div>
        </div>

        <div>
          <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginBottom: 12 }}>
            <div style={{ minWidth: 0, flex: 1 }}>
              <div style={{ fontSize: 24, fontWeight: 800, letterSpacing: -0.5, lineHeight: 1.1 }}>Undertow</div>
              <div style={{ fontSize: 14, color: T.TEXT_LO, marginTop: 6 }}>Helena Vorr</div>
            </div>
            <I.Heart size={26} color={T.ACCENT} filled/>
          </div>

          {/* Progress / scrubber */}
          <div style={{ marginTop: 8 }}>
            <div style={{ height: 3, borderRadius: 2, background: 'rgba(255,255,255,0.18)', position: 'relative' }}>
              <div style={{ position: 'absolute', left: 0, top: 0, bottom: 0, width: '38%', background: T.ACCENT, borderRadius: 2 }}/>
              <div style={{ position: 'absolute', left: '38%', top: '50%', transform: 'translate(-50%,-50%)',
                width: 12, height: 12, borderRadius: 999, background: T.ACCENT, boxShadow: '0 2px 6px rgba(0,0,0,0.5)' }}/>
            </div>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 6,
              fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO }}>
              <span>1:32</span><span>4:02</span>
            </div>
          </div>

          {/* Controls */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 18, padding: '0 6px' }}>
            <I.Shuffle size={22} color={T.ACCENT}/>
            <I.SkipPrev size={32}/>
            <button style={{
              width: 70, height: 70, borderRadius: 999, background: T.ACCENT, border: 'none', cursor: 'pointer',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              boxShadow: '0 10px 28px rgba(168,224,78,0.45)',
            }}><I.Pause size={22}/></button>
            <I.SkipNext size={32}/>
            <I.Repeat size={22}/>
          </div>

          {/* bottom row */}
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 24 }}>
            <I.Cast size={18}/>
            <div style={{ fontSize: 12, color: T.TEXT_LO, fontFamily: T.MONO, letterSpacing: 0.5 }}>iPhone di Marco</div>
            <I.Share size={18}/>
          </div>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────
// 06 · ARTIST PROFILE
// ─────────────────────────────────────────────────────────
function ArtistScreen() {
  const POPULAR = [
    { n: 1, title: 'Long way home', plays: '4.8M', dur: '4:18' },
    { n: 2, title: 'Cold lights', plays: '1.2M', dur: '3:14' },
    { n: 3, title: 'Pyre', plays: '2.1M', dur: '2:55' },
    { n: 4, title: 'Echo', plays: '3.0M', dur: '3:48' },
  ];
  const ALBUMS = [
    { title: 'Slow Hours', year: 2026, kind: 'arc', palette: { a: '#FF6B5B', b: '#3A1F8A' } },
    { title: 'Echo, Vol. III', year: 2024, kind: 'type', palette: { bg: '#E8E2D5', fg: '#222' } },
    { title: 'Mattina', year: 2022, kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
  ];

  return (
    <div style={{
      width: '100%', height: '100%', color: T.TEXT_HI, fontFamily: T.FONT,
      display: 'flex', flexDirection: 'column', overflow: 'hidden', position: 'relative',
      background: '#060309',
    }}>
      <div style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', paddingBottom: 130 }}>
        {/* Hero */}
        <div style={{ position: 'relative', height: 320, marginBottom: -60 }}>
          <div style={{ position: 'absolute', inset: 0 }}>
            <MHCover kind="artist" palette={{ bg: '#2A1E12', fg: '#E8DCC4' }} radius={0}/>
          </div>
          <div style={{ position: 'absolute', inset: 0,
            background: 'linear-gradient(180deg, rgba(0,0,0,0.3) 0%, rgba(6,3,9,0) 50%, rgba(6,3,9,1) 100%)' }}/>
          <div style={{ position: 'relative', zIndex: 2, paddingTop: 56,
            display: 'flex', justifyContent: 'space-between', padding: '56px 16px 8px' }}>
            <I.Back/>
            <I.More color={T.TEXT_HI}/>
          </div>
          <div style={{ position: 'absolute', bottom: 80, left: 16, right: 16, zIndex: 2 }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
              <I.Verified size={14}/>
              <div style={{ fontSize: 11, color: T.TEXT_HI, fontWeight: 600, letterSpacing: 0.3 }}>Artista verificato</div>
            </div>
            <div style={{ fontSize: 36, fontWeight: 900, letterSpacing: -1, lineHeight: 1 }}>Marina Vega</div>
            <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 6, fontFamily: T.MONO, letterSpacing: 0.4 }}>
              2.4M ASCOLTATORI MENSILI
            </div>
          </div>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px 16px' }}>
          <button style={{
            background: 'transparent', border: `1.5px solid ${T.TEXT_HI}`, color: T.TEXT_HI,
            borderRadius: 999, padding: '6px 18px', fontSize: 13, fontWeight: 600, cursor: 'pointer',
          }}>Segui</button>
          <I.Bell size={20} color={T.TEXT_LO}/>
          <I.More size={20}/>
          <div style={{ flex: 1 }}/>
          <I.Shuffle size={20}/>
          <button style={{
            width: 48, height: 48, borderRadius: 999, background: T.ACCENT, border: 'none', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 8px 20px rgba(168,224,78,0.4)',
          }}><I.Play size={18}/></button>
        </div>

        {/* Popular */}
        <MHSectionHeader eyebrow="// POPOLARI" title="Più ascoltati" action={null}/>
        <div>
          {POPULAR.map(t => (
            <div key={t.n} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px' }}>
              <div style={{ width: 18, textAlign: 'center', fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO }}>
                {String(t.n).padStart(2, '0')}
              </div>
              <div style={{ width: 44, height: 44, flexShrink: 0 }}>
                <MHCover kind={['arc','type','triangles','blob'][t.n - 1]} palette={{ a: '#FF6B5B', b: '#3A1F8A' }} radius={4}/>
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 14, fontWeight: 600 }}>{t.title}</div>
                <div style={{ fontSize: 11, color: T.TEXT_LO, marginTop: 2, fontFamily: T.MONO }}>
                  {t.plays} riproduzioni
                </div>
              </div>
              <div style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO }}>{t.dur}</div>
              <I.More/>
            </div>
          ))}
        </div>

        {/* Discography */}
        <MHSectionHeader eyebrow="// DISCOGRAFIA" title="Album" action="Tutti"/>
        <div style={{ display: 'flex', gap: 12, padding: '4px 16px 16px', overflowX: 'auto' }}>
          {ALBUMS.map((a, i) => (
            <div key={i} style={{ width: 140, flexShrink: 0 }}>
              <div style={{ width: 140, height: 140 }}>
                <MHCover kind={a.kind} palette={a.palette} radius={6}/>
              </div>
              <div style={{ fontSize: 13, fontWeight: 600, marginTop: 8,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{a.title}</div>
              <div style={{ fontSize: 11, color: T.TEXT_LO, marginTop: 2, fontFamily: T.MONO }}>
                {a.year} · ALBUM
              </div>
            </div>
          ))}
        </div>

        {/* About */}
        <div style={{ padding: '8px 16px 24px' }}>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, marginBottom: 6 }}>
            // INFO
          </div>
          <div style={{ background: T.CARD, borderRadius: 12, padding: 14 }}>
            <div style={{ fontSize: 13, color: T.TEXT_HI, lineHeight: 1.55, textWrap: 'pretty' }}>
              Cantautrice italiana, voce calda e produzione minimale. Suona regolarmente
              al Blue Note di Milano. Ultimo album, "Slow Hours", uscito a marzo 2026.
            </div>
            <div style={{ display: 'flex', gap: 16, marginTop: 12, fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO }}>
              <div><span style={{ color: T.ACCENT }}>2.4M</span> ascoltatori</div>
              <div><span style={{ color: T.ACCENT }}>184K</span> follower</div>
            </div>
          </div>
        </div>
      </div>

      <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0 }}>
        <div style={{ paddingBottom: 6 }}><MHPlayerBar/></div>
        <MHBottomNav active="home"/>
        <div style={{ height: 28 }}/>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────
// 07 · PLAYLIST DETAIL
// ─────────────────────────────────────────────────────────
function PlaylistScreen() {
  const TRACKS = [
    { title: 'Long way home', artist: 'Marina Vega', dur: '4:18', kind: 'moon', palette: { bg: '#E8DCC4', fg: '#1A1A1A' } },
    { title: 'Carbon Mirror', artist: 'Lou Hessler', dur: '3:42', kind: 'grid', palette: { a: '#0E1F3A', b: T.ACCENT } },
    { title: 'Pyre', artist: 'Tobi Akin', dur: '2:55', kind: 'triangles', palette: { bg: '#1A1A1A', fg: '#FF4D2E' } },
    { title: 'Plein soleil', artist: 'Camille Roche', dur: '3:31', kind: 'wave', palette: { a: '#5C2D8C', b: '#F0A6B0' } },
    { title: 'Glasshouse', artist: 'The Astral Kit', dur: '5:07', kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
    { title: 'Undertow', artist: 'Helena Vorr', dur: '4:02', kind: 'blob', palette: { a: '#1E3A8A', b: '#06B6D4' }, playing: true },
    { title: 'After', artist: 'Marina Vega', dur: '2:18', kind: 'arc', palette: { a: '#FF6B5B', b: '#3A1F8A' } },
  ];

  return (
    <MHScreen
      gradient="linear-gradient(180deg, #1F0833 0%, #0F0820 280px, #060309 600px)"
      navActive="library"
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 16px' }}>
        <I.Back/>
        <I.More color={T.TEXT_HI}/>
      </div>

      <div style={{ display: 'flex', justifyContent: 'center', padding: '8px 16px 16px' }}>
        <div style={{ width: 180, height: 180, boxShadow: '0 20px 50px rgba(0,0,0,0.6)' }}>
          <MHCover kind="duotone" palette={{ a: '#3A0CA3', b: '#F72585' }} radius={6}/>
        </div>
      </div>

      <div style={{ padding: '0 16px 8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 8 }}>
          <div style={{ fontSize: 10.5, fontWeight: 700, letterSpacing: 1.5, color: T.ACCENT, fontFamily: T.MONO }}>
            // PLAYLIST
          </div>
        </div>
        <div style={{ display: 'flex', alignItems: 'flex-start', gap: 8 }}>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 26, fontWeight: 800, letterSpacing: -0.6, lineHeight: 1.1 }}>Slow Hours</div>
            <div style={{ fontSize: 13, color: T.TEXT_LO, marginTop: 8, lineHeight: 1.45, textWrap: 'pretty' }}>
              Ballate e ambient morbidi per le ore lente.
            </div>
          </div>
          <I.Edit size={18}/>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginTop: 14 }}>
          <div style={{ width: 22, height: 22, borderRadius: 999, background: T.ACCENT,
            display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 800, color: '#0a0a0a' }}>M</div>
          <div style={{ fontSize: 12, fontWeight: 600 }}>Marco · 7 brani · 25 min</div>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '14px 16px 12px' }}>
        <I.Heart size={22} color={T.ACCENT} filled/>
        <I.Download size={22}/>
        <I.Plus size={22} color={T.TEXT_LO}/>
        <I.Share size={20}/>
        <div style={{ flex: 1 }}/>
        <I.Shuffle size={20} color={T.ACCENT}/>
        <button style={{
          width: 52, height: 52, borderRadius: 999, background: T.ACCENT, border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 8px 20px rgba(168,224,78,0.4)',
        }}><I.Play size={20}/></button>
      </div>

      <div style={{ padding: '4px 0 12px' }}>
        {TRACKS.map((t, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px' }}>
            <div style={{ width: 44, height: 44, flexShrink: 0 }}>
              <MHCover kind={t.kind} palette={t.palette} radius={4}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 600, color: t.playing ? T.ACCENT : T.TEXT_HI,
                display: 'flex', alignItems: 'center', gap: 6,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {t.title}{t.playing && <MHPlayingBars/>}
              </div>
              <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 2,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.artist}</div>
            </div>
            <div style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO }}>{t.dur}</div>
            <I.More/>
          </div>
        ))}
      </div>
    </MHScreen>
  );
}

// ─────────────────────────────────────────────────────────
// 08 · PROFILE / SETTINGS
// ─────────────────────────────────────────────────────────
function ProfileScreen() {
  const STATS = [
    { label: 'Brani', value: '247' },
    { label: 'Playlist', value: '18' },
    { label: 'Artisti', value: '94' },
  ];
  const SECTIONS = [
    {
      title: 'ACCOUNT',
      items: [
        { label: 'Profilo', detail: '@marco' },
      ],
    },
    {
      title: 'RIPRODUZIONE',
      items: [
        { label: 'Crossfade', detail: '6 sec' },
        { label: 'Download offline', detail: '12 GB' },
      ],
    },
    {
      title: 'APP',
      items: [
        { label: 'Notifiche' },
        { label: 'Lingua', detail: 'Italiano' },
        { label: 'Tema', detail: 'Scuro' },
        { label: 'Informazioni', detail: 'v2.4.1' },
      ],
    },
  ];

  return (
    <MHScreen navActive="library">
      <div style={{ padding: '8px 16px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ fontSize: 26, fontWeight: 800, letterSpacing: -0.8 }}>Profilo</div>
        <I.Settings size={22}/>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '12px 16px 16px' }}>
        <div style={{
          width: 76, height: 76, borderRadius: 999,
          background: `linear-gradient(135deg, ${T.ACCENT} 0%, #3A0CA3 100%)`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 30, fontWeight: 900, color: '#0a0a0a', letterSpacing: -1,
        }}>M</div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 20, fontWeight: 800, letterSpacing: -0.4 }}>Marco Bianchi</div>
          <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 4, fontFamily: T.MONO }}>@marco · iscritto nel 2023</div>
          <button style={{
            marginTop: 8, background: 'transparent', border: `1px solid ${T.DIVIDER}`,
            color: T.TEXT_HI, borderRadius: 999, padding: '4px 12px', fontSize: 11.5, fontWeight: 600, cursor: 'pointer',
          }}>Modifica profilo</button>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 8, padding: '0 16px 16px' }}>
        {STATS.map(s => (
          <div key={s.label} style={{ flex: 1, background: T.CARD, borderRadius: 10, padding: '12px 14px' }}>
            <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.4, color: T.ACCENT, fontFamily: T.MONO }}>{s.value}</div>
            <div style={{ fontSize: 11, color: T.TEXT_LO, marginTop: 2, letterSpacing: 0.4, textTransform: 'uppercase' }}>
              {s.label}
            </div>
          </div>
        ))}
      </div>

      {SECTIONS.map(sec => (
        <div key={sec.title} style={{ padding: '16px 16px 0' }}>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600,
            letterSpacing: 1.5, color: T.ACCENT, marginBottom: 8 }}>// {sec.title}</div>
          <div style={{ background: T.CARD, borderRadius: 12, overflow: 'hidden' }}>
            {sec.items.map((it, i) => (
              <div key={it.label} style={{
                display: 'flex', alignItems: 'center', padding: '13px 14px',
                borderBottom: i < sec.items.length - 1 ? `1px solid ${T.DIVIDER}` : 'none',
              }}>
                <div style={{ flex: 1, fontSize: 14, fontWeight: 500 }}>{it.label}</div>
                {it.detail && (
                  <div style={{ fontSize: 12.5, color: it.accent ? T.ACCENT : T.TEXT_LO,
                    fontWeight: it.accent ? 600 : 400, marginRight: 6 }}>{it.detail}</div>
                )}
                <I.Chevron size={14}/>
              </div>
            ))}
          </div>
        </div>
      ))}

      <div style={{ padding: '24px 16px 16px', textAlign: 'center' }}>
        <div style={{ display: 'inline-block', fontSize: 13, color: '#FF4D2E', fontWeight: 600, cursor: 'pointer' }}>
          Disconnetti
        </div>
      </div>
    </MHScreen>
  );
}

window.MHScreens = {
  HomeScreen, SearchScreen, LibraryScreen, AlbumScreen,
  NowPlayingScreen, ArtistScreen, PlaylistScreen, ProfileScreen,
};
