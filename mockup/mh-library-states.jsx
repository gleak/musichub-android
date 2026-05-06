// MusicHub — Library impl-only states
// Covers gaps in the library mockup: loading, empty, kebab actions w/ dislike/flag,
// auto-playlist variant, swipe/drag gestures, top-app-bar actions, profile entry,
// SPOTIFY_IMPORT row, Scaricati placeholder, generic overlays.

const { T, I, MHCover, MHScreen, MHPlayingBars, MHPlayerBar, MHBottomNav } = window.MH;

// ── Tiny helpers ────────────────────────────────────────
function Eyebrow({ children, color = T.ACCENT }) {
  return (
    <div style={{
      fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5,
      color, textTransform: 'uppercase',
    }}>{children}</div>
  );
}

function ShimmerBlock({ w = '100%', h = 14, r = 4, style = {} }) {
  return (
    <div style={{
      width: w, height: h, borderRadius: r,
      background: 'linear-gradient(90deg, rgba(255,255,255,0.04) 0%, rgba(255,255,255,0.10) 50%, rgba(255,255,255,0.04) 100%)',
      backgroundSize: '200% 100%',
      animation: 'mhShimmer 1.4s linear infinite',
      ...style,
    }}/>
  );
}

const SHIMMER_KEYS = `@keyframes mhShimmer { 0%{background-position:200% 0} 100%{background-position:-200% 0} }
@keyframes mhSpin { to { transform: rotate(360deg); } }
@keyframes mhPulse { 0%,100%{opacity:0.55} 50%{opacity:1} }`;

function Spinner({ size = 22, color = T.ACCENT, thickness = 2.4 }) {
  return (
    <svg width={size} height={size} viewBox="0 0 24 24" style={{ animation: 'mhSpin 0.9s linear infinite' }}>
      <circle cx="12" cy="12" r="9" stroke="rgba(255,255,255,0.08)" strokeWidth={thickness} fill="none"/>
      <path d="M21 12a9 9 0 00-9-9" stroke={color} strokeWidth={thickness} strokeLinecap="round" fill="none"/>
    </svg>
  );
}

// Library top app-bar shared by states
function LibraryAppBar({ title = 'Libreria', extra }) {
  return (
    <div style={{ padding: '8px 16px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
      <div style={{ fontSize: 26, fontWeight: 800, letterSpacing: -0.8 }}>{title}</div>
      <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
        {extra}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────
// 1. Loading shimmer · library landing
// ─────────────────────────────────────────────────────────
function LibraryLoadingScreen() {
  return (
    <MHScreen navActive="library">
      <style>{SHIMMER_KEYS}</style>
      <LibraryAppBar extra={<><I.Search size={20} color={T.TEXT_HI}/><I.Plus size={22} color={T.TEXT_HI}/></>}/>
      <div style={{ display: 'flex', gap: 6, padding: '8px 16px 8px' }}>
        {[68, 56, 64, 86].map((w, i) => (
          <ShimmerBlock key={i} w={w} h={28} r={999}/>
        ))}
      </div>
      <div style={{ padding: '6px 16px 4px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <ShimmerBlock w={80} h={14}/>
        <ShimmerBlock w={20} h={20} r={4}/>
      </div>
      {[0,1,2,3,4,5,6,7].map(i => (
        <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 16px' }}>
          <ShimmerBlock w={52} h={52} r={6}/>
          <div style={{ flex: 1 }}>
            <ShimmerBlock w={`${50 + (i*9)%40}%`} h={14}/>
            <div style={{ height: 6 }}/>
            <ShimmerBlock w="32%" h={11}/>
          </div>
        </div>
      ))}
    </MHScreen>
  );
}

// ─────────────────────────────────────────────────────────
// 2a. Empty · liked songs
// ─────────────────────────────────────────────────────────
function LikedEmptyScreen() {
  return (
    <MHScreen navActive="library" gradient={`linear-gradient(180deg, #2A0E48 0%, ${T.BG_BOTTOM} 280px)`}>
      <div style={{ padding: '0 16px 12px', display: 'flex', alignItems: 'center' }}>
        <button style={{ background: 'rgba(0,0,0,0.3)', border: 'none', borderRadius: 999, width: 36, height: 36, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.Back/></button>
      </div>
      <div style={{ padding: '12px 16px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <div style={{ width: 200, height: 200, marginBottom: 16, borderRadius: 10, background: 'rgba(255,255,255,0.04)', border: '1px dashed rgba(255,255,255,0.16)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <I.Heart size={64} color="rgba(255,255,255,0.18)"/>
        </div>
        <Eyebrow>// LIBRERIA · MI PIACE</Eyebrow>
        <div style={{ fontSize: 26, fontWeight: 800, letterSpacing: -0.5, marginTop: 6, textAlign: 'center' }}>Nessun brano che ti piace</div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, marginTop: 10, textAlign: 'center', maxWidth: 280, lineHeight: 1.55 }}>
          Tocca il cuore su qualunque traccia: la troverai qui, sempre offline-ready.
        </div>
        <button style={{ marginTop: 22, padding: '12px 22px', borderRadius: 999, border: 'none', background: T.ACCENT, color: '#0A0A0A', fontWeight: 700, fontSize: 14, fontFamily: T.FONT, cursor: 'pointer' }}>Sfoglia consigli</button>
      </div>
    </MHScreen>
  );
}

// ─────────────────────────────────────────────────────────
// 2b. Empty · albums catalog
// ─────────────────────────────────────────────────────────
function AlbumsEmptyScreen() {
  return (
    <MHScreen navActive="library">
      <div style={{ padding: '0 16px 4px', display: 'flex', alignItems: 'center', gap: 12 }}>
        <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 4, marginLeft: -4 }}><I.Back/></button>
        <div style={{ flex: 1 }}>
          <Eyebrow>// LIBRERIA</Eyebrow>
          <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.5 }}>Album · <span style={{ fontFamily: T.MONO, color: T.TEXT_LO, fontWeight: 600, fontSize: 16 }}>0</span></div>
        </div>
      </div>
      <div style={{ flex: 1, padding: '64px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
        <div style={{ width: 96, height: 96, borderRadius: 12, background: 'rgba(255,255,255,0.04)', border: '1px dashed rgba(255,255,255,0.14)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 18 }}>
          <svg width="40" height="40" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" stroke="rgba(255,255,255,0.22)" strokeWidth="1.6"/>
            <circle cx="12" cy="12" r="2.2" stroke="rgba(255,255,255,0.22)" strokeWidth="1.6"/>
          </svg>
        </div>
        <div style={{ fontSize: 19, fontWeight: 800, letterSpacing: -0.3 }}>Nessun album in catalogo</div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, marginTop: 10, lineHeight: 1.55, maxWidth: 280 }}>
          Importa la tua libreria da Spotify, oppure cerca un brano e MusicHub salverà l'album nella tua collezione.
        </div>
        <div style={{ display: 'flex', gap: 10, marginTop: 22 }}>
          <button style={{ padding: '11px 18px', borderRadius: 999, background: T.ACCENT, color: '#0A0A0A', border: 'none', fontWeight: 700, fontSize: 13, fontFamily: T.FONT, cursor: 'pointer' }}>Importa da Spotify</button>
          <button style={{ padding: '11px 18px', borderRadius: 999, background: 'rgba(255,255,255,0.06)', color: T.TEXT_HI, border: 'none', fontWeight: 600, fontSize: 13, fontFamily: T.FONT, cursor: 'pointer' }}>Cerca</button>
        </div>
      </div>
    </MHScreen>
  );
}

// ─────────────────────────────────────────────────────────
// 3. Track action sheet · with dislike + flag wired in
//    (variant of TrackActionSheet specifically for library rows)
// ─────────────────────────────────────────────────────────
function LibraryTrackKebabSheet() {
  const Track = (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '0 0 14px' }}>
      <div style={{ width: 56, height: 56, flexShrink: 0 }}>
        <MHCover kind="wave" palette={{ a:'#5C2D8C', b:'#F0A6B0' }} radius={6}/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 16, fontWeight: 700, letterSpacing: -0.2 }}>Citrine</div>
        <div style={{ fontSize: 12.5, color: T.TEXT_LO, marginTop: 2 }}>Mira Holt · Strange Mercy</div>
      </div>
    </div>
  );
  const items = [
    { ic: 'plus', l: 'Aggiungi a playlist' },
    { ic: 'queue', l: 'Aggiungi alla coda' },
    { ic: 'next', l: 'Riproduci dopo' },
    { ic: 'heart', l: 'Tolto da Mi piace', muted: true, off: true },
    { divider: true },
    { ic: 'thumbDown', l: 'Non consigliarmi questo brano', danger: false },
    { ic: 'thumbDown', l: 'Non consigliarmi questo artista', danger: false },
    { ic: 'flag', l: 'Segnala brano sbagliato', danger: true },
  ];
  const Icon = ({ k, color = T.TEXT_LO }) => {
    const p = { width: 20, height: 20, viewBox: '0 0 24 24', fill: 'none', stroke: color, strokeWidth: 1.7, strokeLinecap: 'round', strokeLinejoin: 'round' };
    switch (k) {
      case 'plus': return <svg {...p}><path d="M12 5v14M5 12h14"/></svg>;
      case 'queue': return <svg {...p}><path d="M3 6h12M3 12h12M3 18h8M17 14v6M14 17h6"/></svg>;
      case 'next': return <svg {...p}><path d="M5 5v14l8-7zM13 5v14l8-7z"/></svg>;
      case 'heart': return <svg {...p}><path d="M12 21s-7-4.5-9.5-9A5 5 0 0112 6.5 5 5 0 0121.5 12c-2.5 4.5-9.5 9-9.5 9z"/></svg>;
      case 'thumbDown': return <svg {...p}><path d="M10 15v4a3 3 0 003 3l4-9V2H5.7a2 2 0 00-2 1.7L2 13a2 2 0 002 2zM17 2h3a2 2 0 012 2v7a2 2 0 01-2 2h-3"/></svg>;
      case 'flag': return <svg {...p}><path d="M4 21V4M4 5h13l-2 4 2 4H4"/></svg>;
      default: return null;
    }
  };
  return (
    <div style={{ width: '100%', height: '100%', background: 'rgba(0,0,0,0.55)', display: 'flex', alignItems: 'flex-end', fontFamily: T.FONT }}>
      <div style={{ width: '100%', borderRadius: '20px 20px 0 0', background: '#181818', color: T.TEXT_HI, padding: '14px 0 28px' }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 8 }}>
          <div style={{ width: 36, height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.18)' }}/>
        </div>
        <div style={{ padding: '4px 20px 0' }}>
          <Eyebrow>// AZIONI · LIBRERIA</Eyebrow>
          <div style={{ height: 10 }}/>
          {Track}
        </div>
        <div style={{ padding: '0 8px' }}>
          {items.map((it, i) => {
            if (it.divider) return <div key={i} style={{ height: 1, background: 'rgba(255,255,255,0.06)', margin: '8px 12px' }}/>;
            const c = it.danger ? '#FF7A7A' : (it.muted ? T.TEXT_LO : T.TEXT_HI);
            const ic = it.danger ? '#FF7A7A' : (it.muted ? T.TEXT_LO : T.TEXT_LO);
            return (
              <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '12px 16px', borderRadius: 10 }}>
                <Icon k={it.ic} color={ic}/>
                <div style={{ fontSize: 15, fontWeight: 500, color: c }}>{it.l}</div>
                {it.off && <div style={{ marginLeft: 'auto', fontSize: 10, fontFamily: T.MONO, color: T.TEXT_LO2, letterSpacing: 1 }}>OFF</div>}
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────
// 4. Auto-playlist variant of PlaylistDetailScreen
//    (AutoPlaylistMetaStrip · AGGIORNATA · BRANI cards)
// ─────────────────────────────────────────────────────────
function AutoPlaylistDetailScreen() {
  const TRACKS = [
    { title: 'Long way home', artist: 'Marina Vega', dur: '4:18', kind: 'moon', palette: { bg: '#E8DCC4', fg: '#1A1A1A' }, fresh: true },
    { title: 'Carbon Mirror', artist: 'Lou Hessler', dur: '3:42', kind: 'grid', palette: { a: '#0E1F3A', b: T.ACCENT } },
    { title: 'Pyre', artist: 'Tobi Akin', dur: '2:55', kind: 'triangles', palette: { bg: '#1A1A1A', fg: '#FF4D2E' }, fresh: true },
    { title: 'Plein soleil', artist: 'Camille Roche', dur: '3:31', kind: 'wave', palette: { a: '#5C2D8C', b: '#F0A6B0' } },
    { title: 'Glasshouse', artist: 'The Astral Kit', dur: '5:07', kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
    { title: 'Undertow', artist: 'Helena Vorr', dur: '4:02', kind: 'blob', palette: { a: '#1E3A8A', b: '#06B6D4' }, playing: true },
    { title: 'After', artist: 'Marina Vega', dur: '2:18', kind: 'arc', palette: { a: '#FF6B5B', b: '#3A1F8A' } },
  ];
  return (
    <MHScreen
      gradient="linear-gradient(180deg, #1A2E0F 0%, #0A1408 280px, #060309 600px)"
      navActive="library"
    >
      <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 16px' }}>
        <I.Back/>
        <div style={{ display: 'flex', gap: 14 }}>
          <I.More color={T.TEXT_HI}/>
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'center', padding: '8px 16px 16px' }}>
        <div style={{ width: 180, height: 180, boxShadow: '0 20px 50px rgba(0,0,0,0.6)', position: 'relative' }}>
          <MHCover kind="grid" palette={{ a: '#0E1F3A', b: T.ACCENT }} radius={6}/>
          {/* AUTO badge stamped on cover */}
          <div style={{ position: 'absolute', top: 8, left: 8, padding: '4px 8px', background: 'rgba(0,0,0,0.55)', backdropFilter: 'blur(8px)', borderRadius: 6, display: 'flex', alignItems: 'center', gap: 5 }}>
            <svg width="11" height="11" viewBox="0 0 24 24" fill={T.ACCENT}><path d="M12 3l1.8 5.4L19 10l-5.2 1.6L12 17l-1.8-5.4L5 10l5.2-1.6z"/></svg>
            <span style={{ fontFamily: T.MONO, fontSize: 9, fontWeight: 700, letterSpacing: 1.2, color: T.ACCENT }}>AUTO</span>
          </div>
        </div>
      </div>

      <div style={{ padding: '0 16px 12px' }}>
        <Eyebrow>// PER TE · GENERATA</Eyebrow>
        <div style={{ fontSize: 26, fontWeight: 800, letterSpacing: -0.6, lineHeight: 1.1, marginTop: 6 }}>Mix · 02</div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, marginTop: 8, lineHeight: 1.45 }}>
          Costruita dal tuo ascolto delle ultime due settimane. Si rinnova ogni mercoledì.
        </div>
      </div>

      {/* AutoPlaylistMetaStrip — AGGIORNATA · BRANI cards */}
      <div style={{ padding: '4px 16px 14px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
        <div style={{ background: T.CARD, borderRadius: 12, padding: '12px 14px' }}>
          <div style={{ fontFamily: T.MONO, fontSize: 9.5, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase' }}>// AGGIORNATA</div>
          <div style={{ fontSize: 18, fontWeight: 800, letterSpacing: -0.3, marginTop: 6, color: T.TEXT_HI }}>2 giorni fa</div>
          <div style={{ fontSize: 11, color: T.TEXT_LO, marginTop: 4, fontFamily: T.MONO }}>Prossimo: mer 11/05</div>
        </div>
        <div style={{ background: T.CARD, borderRadius: 12, padding: '12px 14px' }}>
          <div style={{ fontFamily: T.MONO, fontSize: 9.5, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase' }}>// BRANI</div>
          <div style={{ fontSize: 18, fontWeight: 800, letterSpacing: -0.3, marginTop: 6, color: T.TEXT_HI }}>32 · 1h 54m</div>
          <div style={{ fontSize: 11, color: T.TEXT_LO, marginTop: 4, fontFamily: T.MONO }}>+8 nuovi questa settimana</div>
        </div>
      </div>

      {/* CTAs — no Edit because auto-playlists aren't user-editable */}
      <div style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '0 16px 12px' }}>
        <I.Heart size={22} color={T.ACCENT} filled/>
        <I.Download size={22}/>
        <I.Share size={20}/>
        <div style={{ flex: 1 }}/>
        <I.Shuffle size={20} color={T.ACCENT}/>
        <button style={{
          width: 52, height: 52, borderRadius: 999, background: T.ACCENT, border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 8px 20px rgba(168,224,78,0.4)',
        }}><I.Play size={20}/></button>
      </div>

      <div style={{ padding: '4px 16px 8px' }}>
        <Eyebrow color={T.TEXT_LO}>// BRANI · ORDINE GENERATO</Eyebrow>
      </div>

      <div style={{ padding: '4px 0 12px' }}>
        {TRACKS.map((t, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px' }}>
            <div style={{ width: 44, height: 44, flexShrink: 0 }}><MHCover kind={t.kind} palette={t.palette} radius={4}/></div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 600, color: t.playing ? T.ACCENT : T.TEXT_HI,
                display: 'flex', alignItems: 'center', gap: 6,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {t.title}
                {t.playing && <MHPlayingBars/>}
                {t.fresh && !t.playing && (
                  <span style={{ fontFamily: T.MONO, fontSize: 8.5, fontWeight: 700, letterSpacing: 1, color: T.ACCENT, padding: '2px 5px', borderRadius: 4, background: 'rgba(168,224,78,0.12)' }}>NEW</span>
                )}
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
// 5. Swipe-to-dismiss + drag-reorder gestures on playlist detail
// ─────────────────────────────────────────────────────────
function PlaylistGesturesScreen() {
  const TRACKS = [
    { title: 'Long way home', artist: 'Marina Vega', dur: '4:18', kind: 'moon', palette: { bg: '#E8DCC4', fg: '#1A1A1A' } },
    { title: 'Carbon Mirror', artist: 'Lou Hessler', dur: '3:42', kind: 'grid', palette: { a: '#0E1F3A', b: T.ACCENT }, swiping: true },
    { title: 'Pyre', artist: 'Tobi Akin', dur: '2:55', kind: 'triangles', palette: { bg: '#1A1A1A', fg: '#FF4D2E' }, dragging: true },
    { title: 'Plein soleil', artist: 'Camille Roche', dur: '3:31', kind: 'wave', palette: { a: '#5C2D8C', b: '#F0A6B0' }, dropTarget: true },
    { title: 'Glasshouse', artist: 'The Astral Kit', dur: '5:07', kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
    { title: 'Undertow', artist: 'Helena Vorr', dur: '4:02', kind: 'blob', palette: { a: '#1E3A8A', b: '#06B6D4' } },
  ];
  return (
    <MHScreen gradient="linear-gradient(180deg, #1F0833 0%, #0F0820 280px, #060309 600px)" navActive="library">
      <div style={{ display: 'flex', justifyContent: 'space-between', padding: '8px 16px' }}>
        <I.Back/>
        <div style={{ display: 'flex', gap: 14, alignItems: 'center' }}>
          <svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M21 12a9 9 0 11-3-6.7M21 4v5h-5" stroke={T.TEXT_HI} strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/></svg>
          <I.Plus size={22}/>
          <I.More color={T.TEXT_HI}/>
        </div>
      </div>

      <div style={{ padding: '0 16px 8px' }}>
        <Eyebrow>// PLAYLIST · MODIFICA</Eyebrow>
        <div style={{ fontSize: 24, fontWeight: 800, letterSpacing: -0.5, marginTop: 6 }}>Slow Hours</div>
        <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 4, fontFamily: T.MONO }}>7 brani · trascina per riordinare · scorri per rimuovere</div>
      </div>

      <div style={{ padding: '12px 0' }}>
        {TRACKS.map((t, i) => {
          if (t.swiping) {
            // Row mid-swipe: pulled left, red 'remove' affordance behind
            return (
              <div key={i} style={{ position: 'relative', height: 60 }}>
                <div style={{ position: 'absolute', inset: '0 0 0 0', display: 'flex', alignItems: 'center', justifyContent: 'flex-end', paddingRight: 24, background: 'linear-gradient(90deg, transparent 30%, rgba(225,72,72,0.12) 60%, rgba(225,72,72,0.85) 100%)' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8, color: '#fff', fontWeight: 700, fontSize: 13, fontFamily: T.FONT }}>
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M3 6h18M8 6V4a2 2 0 012-2h4a2 2 0 012 2v2M19 6l-1 14a2 2 0 01-2 2H8a2 2 0 01-2-2L5 6" stroke="#fff" strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/></svg>
                    Rimuovi
                  </div>
                </div>
                <div style={{ position: 'absolute', inset: 0, transform: 'translateX(-92px)', background: '#0E1F3A', display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px' }}>
                  <div style={{ width: 44, height: 44, flexShrink: 0 }}><MHCover kind={t.kind} palette={t.palette} radius={4}/></div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 14, fontWeight: 600 }}>{t.title}</div>
                    <div style={{ fontSize: 12, color: T.TEXT_LO }}>{t.artist}</div>
                  </div>
                </div>
              </div>
            );
          }
          if (t.dragging) {
            // Lifted/dragged row
            return (
              <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 16px', background: 'linear-gradient(90deg, rgba(168,224,78,0.10) 0%, rgba(168,224,78,0.04) 100%)', boxShadow: '0 12px 28px rgba(0,0,0,0.5), 0 0 0 1px rgba(168,224,78,0.3)', borderRadius: 8, margin: '0 8px', transform: 'translateY(-2px) scale(1.01)' }}>
                <svg width="18" height="18" viewBox="0 0 24 24" fill={T.ACCENT}><circle cx="9" cy="6" r="1.6"/><circle cx="15" cy="6" r="1.6"/><circle cx="9" cy="12" r="1.6"/><circle cx="15" cy="12" r="1.6"/><circle cx="9" cy="18" r="1.6"/><circle cx="15" cy="18" r="1.6"/></svg>
                <div style={{ width: 44, height: 44, flexShrink: 0 }}><MHCover kind={t.kind} palette={t.palette} radius={4}/></div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 14, fontWeight: 600, color: T.ACCENT }}>{t.title}</div>
                  <div style={{ fontSize: 12, color: T.TEXT_LO }}>{t.artist}</div>
                </div>
                <div style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO }}>{t.dur}</div>
              </div>
            );
          }
          if (t.dropTarget) {
            return (
              <div key={i}>
                <div style={{ height: 2, margin: '4px 24px', borderRadius: 2, background: T.ACCENT, boxShadow: `0 0 12px ${T.ACCENT}` }}/>
                <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px', opacity: 0.55 }}>
                  <svg width="18" height="18" viewBox="0 0 24 24" fill={T.TEXT_LO2}><circle cx="9" cy="6" r="1.6"/><circle cx="15" cy="6" r="1.6"/><circle cx="9" cy="12" r="1.6"/><circle cx="15" cy="12" r="1.6"/><circle cx="9" cy="18" r="1.6"/><circle cx="15" cy="18" r="1.6"/></svg>
                  <div style={{ width: 44, height: 44, flexShrink: 0 }}><MHCover kind={t.kind} palette={t.palette} radius={4}/></div>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ fontSize: 14, fontWeight: 600 }}>{t.title}</div>
                    <div style={{ fontSize: 12, color: T.TEXT_LO }}>{t.artist}</div>
                  </div>
                  <div style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO }}>{t.dur}</div>
                </div>
              </div>
            );
          }
          return (
            <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px', opacity: 0.85 }}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill={T.TEXT_LO2}><circle cx="9" cy="6" r="1.6"/><circle cx="15" cy="6" r="1.6"/><circle cx="9" cy="12" r="1.6"/><circle cx="15" cy="12" r="1.6"/><circle cx="9" cy="18" r="1.6"/><circle cx="15" cy="18" r="1.6"/></svg>
              <div style={{ width: 44, height: 44, flexShrink: 0 }}><MHCover kind={t.kind} palette={t.palette} radius={4}/></div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 14, fontWeight: 600 }}>{t.title}</div>
                <div style={{ fontSize: 12, color: T.TEXT_LO }}>{t.artist}</div>
              </div>
              <div style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO }}>{t.dur}</div>
            </div>
          );
        })}
      </div>
    </MHScreen>
  );
}

// ─────────────────────────────────────────────────────────
// 6. Refresh + Add-songs TopAppBar actions on playlist detail
//    (variant of PlaylistScreen with explicit nav-bar action icons)
// ─────────────────────────────────────────────────────────
function PlaylistDetailWithTopBar() {
  const TRACKS = [
    { title: 'Long way home', artist: 'Marina Vega', dur: '4:18', kind: 'moon', palette: { bg: '#E8DCC4', fg: '#1A1A1A' } },
    { title: 'Carbon Mirror', artist: 'Lou Hessler', dur: '3:42', kind: 'grid', palette: { a: '#0E1F3A', b: T.ACCENT } },
    { title: 'Pyre', artist: 'Tobi Akin', dur: '2:55', kind: 'triangles', palette: { bg: '#1A1A1A', fg: '#FF4D2E' } },
    { title: 'Plein soleil', artist: 'Camille Roche', dur: '3:31', kind: 'wave', palette: { a: '#5C2D8C', b: '#F0A6B0' } },
    { title: 'Glasshouse', artist: 'The Astral Kit', dur: '5:07', kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
  ];
  return (
    <MHScreen gradient="linear-gradient(180deg, #1F0833 0%, #0F0820 280px, #060309 600px)" navActive="library">
      <style>{SHIMMER_KEYS}</style>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '8px 16px' }}>
        <I.Back/>
        <div style={{ display: 'flex', alignItems: 'center', gap: 18 }}>
          {/* Refresh — pulsing while syncing */}
          <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
            <div style={{ animation: 'mhSpin 1.4s linear infinite' }}>
              <svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M21 12a9 9 0 11-3-6.7M21 4v5h-5" stroke={T.ACCENT} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/></svg>
            </div>
          </div>
          {/* Add songs */}
          <I.Plus size={22} color={T.TEXT_HI}/>
          <I.More color={T.TEXT_HI}/>
        </div>
      </div>

      {/* Sync banner */}
      <div style={{ margin: '4px 16px 12px', padding: '10px 12px', background: 'rgba(168,224,78,0.08)', border: '1px solid rgba(168,224,78,0.18)', borderRadius: 10, display: 'flex', alignItems: 'center', gap: 10 }}>
        <Spinner size={16} thickness={2}/>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 12.5, fontWeight: 600 }}>Sincronizzo da MusicHub Server…</div>
          <div style={{ fontSize: 11, color: T.TEXT_LO, fontFamily: T.MONO }}>checking 7 tracks · last sync 12m ago</div>
        </div>
      </div>

      <div style={{ display: 'flex', justifyContent: 'center', padding: '4px 16px 16px' }}>
        <div style={{ width: 160, height: 160, boxShadow: '0 20px 50px rgba(0,0,0,0.6)' }}>
          <MHCover kind="duotone" palette={{ a: '#3A0CA3', b: '#F72585' }} radius={6}/>
        </div>
      </div>

      <div style={{ padding: '0 16px 8px' }}>
        <Eyebrow>// PLAYLIST · TOP-BAR ACTIONS</Eyebrow>
        <div style={{ fontSize: 24, fontWeight: 800, letterSpacing: -0.5, marginTop: 6 }}>Slow Hours</div>
        <div style={{ fontSize: 12.5, color: T.TEXT_LO, marginTop: 4, fontFamily: T.MONO }}>5 brani · 19 min · ↻ refresh · ＋ aggiungi</div>
      </div>

      <div style={{ padding: '12px 0' }}>
        {TRACKS.map((t, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px' }}>
            <div style={{ width: 44, height: 44, flexShrink: 0 }}><MHCover kind={t.kind} palette={t.palette} radius={4}/></div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 600 }}>{t.title}</div>
              <div style={{ fontSize: 12, color: T.TEXT_LO }}>{t.artist}</div>
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
// 7. Library landing · with profile/settings entry + Spotify import row
// ─────────────────────────────────────────────────────────
function LibraryLandingPlusScreen() {
  const ITEMS = [
    { type: 'pinned', title: 'Brani che ti piacciono', sub: 'Playlist · 247 brani', kind: 'duotone', palette: { a: '#3A0CA3', b: '#F72585' } },
    { type: 'playlist', title: 'Slow Hours', sub: 'Playlist · 7 brani', kind: 'arc', palette: { a: '#FF6B5B', b: '#3A1F8A' } },
    { type: 'album', title: 'Echo, Vol. III', sub: 'Album · Marina Vega', kind: 'type', palette: { bg: '#E8E2D5', fg: '#222' } },
  ];
  return (
    <MHScreen navActive="library">
      <div style={{ padding: '8px 16px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {/* Profile entry — circular avatar in top-left */}
          <div style={{
            width: 36, height: 36, borderRadius: 999,
            background: `linear-gradient(135deg, ${T.ACCENT} 0%, #3A0CA3 100%)`,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            fontSize: 15, fontWeight: 900, color: '#0a0a0a', letterSpacing: -0.5,
            boxShadow: '0 0 0 2px rgba(168,224,78,0.3)',
          }}>M</div>
          <div style={{ fontSize: 24, fontWeight: 800, letterSpacing: -0.7 }}>Libreria</div>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <I.Search size={20} color={T.TEXT_HI}/>
          <I.Plus size={22} color={T.TEXT_HI}/>
        </div>
      </div>

      <div style={{ display: 'flex', gap: 6, padding: '8px 16px 8px', overflowX: 'auto' }}>
        {['Playlist', 'Album', 'Artisti', 'Scaricati'].map((c, i) => (
          <button key={c} style={{
            border: 'none', cursor: 'pointer', padding: '6px 12px', borderRadius: 999,
            background: i === 0 ? T.ACCENT : 'rgba(255,255,255,0.08)',
            color: i === 0 ? '#0a0a0a' : T.TEXT_HI,
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
            <div style={{ width: 52, height: 52, flexShrink: 0, borderRadius: 6, overflow: 'hidden' }}>
              <MHCover kind={it.kind} palette={it.palette} radius={6}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14.5, fontWeight: 600, color: T.TEXT_HI, display: 'flex', alignItems: 'center', gap: 6 }}>
                {it.type === 'pinned' && (<svg width="11" height="11" viewBox="0 0 24 24" fill={T.ACCENT}><path d="M14 2l8 8-6 1-4 4-2-2-4 4-2-2 4-4-2-2 4-4z"/></svg>)}
                {it.title}
              </div>
              <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 2 }}>{it.sub}</div>
            </div>
            <I.Chevron size={14}/>
          </div>
        ))}

        {/* Spotify import row · Routes.SPOTIFY_IMPORT */}
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 16px', margin: '4px 8px', background: 'rgba(168,224,78,0.06)', border: '1px dashed rgba(168,224,78,0.22)', borderRadius: 10 }}>
          <div style={{ width: 52, height: 52, flexShrink: 0, borderRadius: 6, background: '#0A0A0A', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="26" height="26" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" fill="#1DB954"/>
              <path d="M7 9c3-1 7-1 10 1M7.5 12c2.5-0.7 6-0.5 8.5 1M8 15c2-0.5 4.5-0.3 6.5 0.8" stroke="#000" strokeWidth="1.6" strokeLinecap="round" fill="none"/>
            </svg>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 14, fontWeight: 700, color: T.TEXT_HI }}>Importa da Spotify</div>
            <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 2 }}>Porta le tue playlist nella libreria</div>
          </div>
          <I.Chevron size={16} color={T.ACCENT}/>
        </div>

        {/* extra rows below to keep the list weight believable */}
        {[
          { title: 'Late drives', sub: 'Playlist · 38 brani', kind: 'wave', palette: { a: '#5C2D8C', b: '#F0A6B0' } },
          { title: 'Helena Vorr', sub: 'Artista', kind: 'artist', palette: { bg: '#1E3A8A', fg: '#E8DCC4' }, round: true },
        ].map((it, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px' }}>
            <div style={{ width: 52, height: 52, flexShrink: 0, borderRadius: it.round ? 999 : 6, overflow: 'hidden' }}>
              <MHCover kind={it.kind} palette={it.palette} radius={it.round ? 999 : 6}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14.5, fontWeight: 600 }}>{it.title}</div>
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
// 8. LibraryFilter.Scaricati · placeholder
// ─────────────────────────────────────────────────────────
function ScaricatiEmptyScreen() {
  return (
    <MHScreen navActive="library">
      <LibraryAppBar extra={<><I.Search size={20} color={T.TEXT_HI}/><I.Plus size={22} color={T.TEXT_HI}/></>}/>
      <div style={{ display: 'flex', gap: 6, padding: '8px 16px 8px' }}>
        {['Playlist', 'Album', 'Artisti', 'Scaricati'].map((c, i) => (
          <button key={c} style={{
            border: 'none', padding: '6px 12px', borderRadius: 999,
            background: i === 3 ? T.ACCENT : 'rgba(255,255,255,0.08)',
            color: i === 3 ? '#0a0a0a' : T.TEXT_HI,
            fontSize: 12.5, fontWeight: 600, whiteSpace: 'nowrap', fontFamily: T.FONT,
          }}>{c}</button>
        ))}
      </div>

      <div style={{ flex: 1, padding: '40px 24px 12px', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
        <div style={{ width: 88, height: 88, borderRadius: 999, background: 'rgba(168,224,78,0.08)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 16 }}>
          <I.Download size={40} color={T.ACCENT}/>
        </div>
        <Eyebrow>// LIBRERIA · SCARICATI</Eyebrow>
        <div style={{ fontSize: 19, fontWeight: 800, letterSpacing: -0.3, marginTop: 8 }}>Tutto in streaming, per ora</div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, marginTop: 10, lineHeight: 1.55, maxWidth: 280 }}>
          I brani scaricati per l'ascolto offline appariranno qui. Tocca <I.Download size={13} color={T.ACCENT}/> su qualsiasi album o playlist per iniziare.
        </div>

        <div style={{ marginTop: 22, padding: '12px 14px', background: T.CARD, borderRadius: 12, width: '100%', display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ width: 36, height: 36, borderRadius: 8, background: 'rgba(168,224,78,0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M21 12c0 1-7 8-9 8s-9-7-9-8 7-8 9-8 9 7 9 8z" stroke={T.ACCENT} strokeWidth="1.7"/><circle cx="12" cy="12" r="3" stroke={T.ACCENT} strokeWidth="1.7"/></svg>
          </div>
          <div style={{ flex: 1, textAlign: 'left' }}>
            <div style={{ fontSize: 12.5, fontWeight: 600 }}>Scarica solo via Wi-Fi</div>
            <div style={{ fontSize: 11, color: T.TEXT_LO, fontFamily: T.MONO }}>Impostazioni · Download</div>
          </div>
          <I.Chevron size={16}/>
        </div>
      </div>
    </MHScreen>
  );
}

// ─────────────────────────────────────────────────────────
// 9a. Pull-to-refresh · library
// ─────────────────────────────────────────────────────────
function LibraryPullRefreshScreen() {
  return (
    <MHScreen navActive="library">
      <style>{SHIMMER_KEYS}</style>
      <LibraryAppBar extra={<><I.Search size={20} color={T.TEXT_HI}/><I.Plus size={22} color={T.TEXT_HI}/></>}/>

      {/* Pulled-down indicator */}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, padding: '14px 0 18px', borderBottom: '1px solid rgba(255,255,255,0.04)' }}>
        <Spinner size={20} thickness={2}/>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT }}>// AGGIORNO LIBRERIA…</div>
      </div>

      <div style={{ display: 'flex', gap: 6, padding: '8px 16px 8px' }}>
        {['Playlist', 'Album', 'Artisti', 'Scaricati'].map((c, i) => (
          <button key={c} style={{ border: 'none', padding: '6px 12px', borderRadius: 999, background: i === 0 ? T.ACCENT : 'rgba(255,255,255,0.08)', color: i === 0 ? '#0a0a0a' : T.TEXT_HI, fontSize: 12.5, fontWeight: 600, whiteSpace: 'nowrap' }}>{c}</button>
        ))}
      </div>
      {/* Slightly translated content to imply the pull */}
      <div style={{ transform: 'translateY(4px)', opacity: 0.85 }}>
        {[
          { title: 'Brani che ti piacciono', sub: 'Playlist · 247 brani', kind: 'duotone', palette: { a: '#3A0CA3', b: '#F72585' } },
          { title: 'Slow Hours', sub: 'Playlist · 7 brani', kind: 'arc', palette: { a: '#FF6B5B', b: '#3A1F8A' } },
          { title: 'Echo, Vol. III', sub: 'Album · Marina Vega', kind: 'type', palette: { bg: '#E8E2D5', fg: '#222' } },
          { title: 'Late drives', sub: 'Playlist · 38 brani', kind: 'wave', palette: { a: '#5C2D8C', b: '#F0A6B0' } },
        ].map((it, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px' }}>
            <div style={{ width: 52, height: 52, flexShrink: 0 }}><MHCover kind={it.kind} palette={it.palette} radius={6}/></div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14.5, fontWeight: 600 }}>{it.title}</div>
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
// 9b. Error · retry footer (network failed)
// ─────────────────────────────────────────────────────────
function LibraryErrorRetryScreen() {
  return (
    <MHScreen navActive="library">
      <LibraryAppBar extra={<><I.Search size={20} color={T.TEXT_HI}/><I.Plus size={22} color={T.TEXT_HI}/></>}/>
      <div style={{ display: 'flex', gap: 6, padding: '8px 16px 8px' }}>
        {['Playlist', 'Album', 'Artisti', 'Scaricati'].map((c, i) => (
          <button key={c} style={{ border: 'none', padding: '6px 12px', borderRadius: 999, background: i === 0 ? T.ACCENT : 'rgba(255,255,255,0.08)', color: i === 0 ? '#0a0a0a' : T.TEXT_HI, fontSize: 12.5, fontWeight: 600, whiteSpace: 'nowrap' }}>{c}</button>
        ))}
      </div>

      <div style={{ flex: 1, padding: '64px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
        <div style={{ width: 64, height: 64, borderRadius: 999, background: 'rgba(225,72,72,0.10)', border: '1px solid rgba(225,72,72,0.3)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 18 }}>
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none"><path d="M12 8v5M12 17h.01M12 3l10 18H2L12 3z" stroke="#FF7A7A" strokeWidth="1.8" strokeLinejoin="round"/></svg>
        </div>
        <Eyebrow color="#FF7A7A">// ERRORE · NETWORK</Eyebrow>
        <div style={{ fontSize: 19, fontWeight: 800, letterSpacing: -0.3, marginTop: 8 }}>Server irraggiungibile</div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, marginTop: 10, lineHeight: 1.55, maxWidth: 280 }}>
          mh.duckdns.org non risponde. I brani offline restano disponibili dalla scheda Scaricati.
        </div>
        <div style={{ marginTop: 14, padding: '8px 12px', background: '#0A0A0A', borderRadius: 8, fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO }}>
          ECONNREFUSED · retry 3/5 · backoff 4s
        </div>
        <div style={{ display: 'flex', gap: 10, marginTop: 22 }}>
          <button style={{ padding: '11px 20px', borderRadius: 999, background: T.ACCENT, color: '#0A0A0A', border: 'none', fontWeight: 700, fontSize: 13, fontFamily: T.FONT, cursor: 'pointer' }}>Riprova</button>
          <button style={{ padding: '11px 20px', borderRadius: 999, background: 'rgba(255,255,255,0.06)', color: T.TEXT_HI, border: 'none', fontWeight: 600, fontSize: 13, fontFamily: T.FONT, cursor: 'pointer' }}>Solo offline</button>
        </div>
      </div>
    </MHScreen>
  );
}

// ─────────────────────────────────────────────────────────
// 9c. Paginated load-more footer (inside a long list)
// ─────────────────────────────────────────────────────────
function LibraryLoadMoreScreen() {
  const tracks = [
    { title: 'Undertow', artist: 'Helena Vorr', dur: '3:48', kind: 'blob', palette: { a:'#1E3A8A', b:'#06B6D4' } },
    { title: 'Citrine', artist: 'Mira Holt', dur: '4:12', kind: 'wave', palette: { a:'#5C2D8C', b:'#F0A6B0' } },
    { title: 'Petals', artist: 'Lana Verdier', dur: '2:55', kind: 'arc', palette: { a:'#FF6B5B', b:'#3A1F8A' } },
    { title: 'Halflight', artist: 'Iso Tide', dur: '3:30', kind: 'triangles', palette: { bg:'#1A1A1A', fg:'#FF4D2E' } },
    { title: 'Burnt Letters', artist: 'The Tessera', dur: '5:01', kind: 'grid', palette: { a:'#0E1F3A', b: T.ACCENT } },
    { title: 'Open Window', artist: 'Mira Holt', dur: '2:51', kind: 'dot', palette: { bg:'#0B3D2E', fg: T.ACCENT } },
    { title: 'Slow Driver', artist: 'Iso Tide', dur: '4:20', kind: 'duotone', palette: { a:'#3A0CA3', b:'#F72585' } },
  ];
  return (
    <MHScreen navActive="library">
      <style>{SHIMMER_KEYS}</style>
      <div style={{ padding: '0 16px 4px', display: 'flex', alignItems: 'center', gap: 12 }}>
        <button style={{ background: 'transparent', border: 'none', padding: 4, marginLeft: -4 }}><I.Back/></button>
        <div style={{ flex: 1 }}>
          <Eyebrow>// LIBRERIA · MI PIACE</Eyebrow>
          <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.5 }}>Brani che ti piacciono · <span style={{ fontFamily: T.MONO, color: T.TEXT_LO, fontWeight: 600, fontSize: 16 }}>1284</span></div>
        </div>
      </div>

      <div style={{ padding: '12px 16px 4px', fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase' }}>
        // PAGE 4 OF 26 · 100/1284
      </div>
      <div style={{ padding: '0 16px' }}>
        {tracks.map((t, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 0' }}>
            <div style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO2, width: 28, textAlign: 'right' }}>{93 + i}</div>
            <div style={{ width: 44, height: 44, flexShrink: 0 }}><MHCover kind={t.kind} palette={t.palette} radius={4}/></div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 600 }}>{t.title}</div>
              <div style={{ fontSize: 12, color: T.TEXT_LO }}>{t.artist} · <span style={{ fontFamily: T.MONO }}>{t.dur}</span></div>
            </div>
            <I.Heart filled color={T.ACCENT}/>
            <I.More/>
          </div>
        ))}
      </div>

      {/* Load-more footer */}
      <div style={{ padding: '16px 16px 4px' }}>
        <div style={{ background: T.CARD, borderRadius: 12, padding: '14px 16px', display: 'flex', alignItems: 'center', gap: 12 }}>
          <Spinner size={20} thickness={2.2}/>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 13.5, fontWeight: 600 }}>Carico altri brani…</div>
            <div style={{ fontSize: 11.5, color: T.TEXT_LO, fontFamily: T.MONO }}>page=5 limit=100 · ETA 0.6s</div>
          </div>
          <div style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO2 }}>1184 / 1284</div>
        </div>
      </div>
    </MHScreen>
  );
}

// ─────────────────────────────────────────────────────────
// 10a. App-update dialog · generic overlay
// ─────────────────────────────────────────────────────────
function AppUpdateDialog() {
  return (
    <div style={{ width: '100%', height: '100%', background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: T.FONT, padding: 24 }}>
      <div style={{ width: '100%', maxWidth: 320, borderRadius: 16, background: '#181818', border: '1px solid rgba(168,224,78,0.18)', color: T.TEXT_HI, padding: 22 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
          <div style={{ width: 32, height: 32, borderRadius: 8, background: 'rgba(168,224,78,0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none"><path d="M12 4v12M6 12l6 6 6-6M4 21h16" stroke={T.ACCENT} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/></svg>
          </div>
          <Eyebrow>// AGGIORNAMENTO · v0.13.0</Eyebrow>
        </div>
        <div style={{ fontSize: 20, fontWeight: 800, letterSpacing: -0.3, marginBottom: 8 }}>Nuova versione disponibile</div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, lineHeight: 1.55, marginBottom: 14 }}>
          Drag-reorder, swipe-to-remove e refresh manuale per le playlist. Riavvia per applicare.
        </div>
        <ul style={{ margin: 0, padding: '0 0 14px 0', listStyle: 'none', fontSize: 12.5, color: T.TEXT_HI, lineHeight: 1.7 }}>
          <li style={{ display: 'flex', gap: 8 }}><span style={{ color: T.ACCENT }}>+</span> Gesture su PlaylistDetail</li>
          <li style={{ display: 'flex', gap: 8 }}><span style={{ color: T.ACCENT }}>+</span> Auto-playlist · meta strip</li>
          <li style={{ display: 'flex', gap: 8 }}><span style={{ color: '#FF7A7A' }}>×</span> Crash sleep timer · fix</li>
        </ul>
        <div style={{ padding: '8px 10px', background: '#0A0A0A', borderRadius: 8, marginBottom: 16, fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO, display: 'flex', justifyContent: 'space-between' }}>
          <span>v0.12.6 → v0.13.0</span>
          <span>14.2 MB</span>
        </div>
        <div style={{ display: 'flex', gap: 10 }}>
          <button style={{ flex: 1, padding: '12px 14px', background: 'rgba(255,255,255,0.06)', border: 'none', borderRadius: 12, color: T.TEXT_HI, fontWeight: 600, fontSize: 13, fontFamily: T.FONT, cursor: 'pointer' }}>Più tardi</button>
          <button style={{ flex: 1.4, padding: '12px 14px', background: T.ACCENT, border: 'none', borderRadius: 12, color: '#0A0A0A', fontWeight: 700, fontSize: 13, fontFamily: T.FONT, cursor: 'pointer' }}>Aggiorna ora</button>
        </div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────
// 10b. Offline badge · library landing with global indicator
// ─────────────────────────────────────────────────────────
function LibraryOfflineBadgeScreen() {
  const ITEMS = [
    { title: 'Brani che ti piacciono', sub: 'Playlist · 247 brani · scaricati', kind: 'duotone', palette: { a:'#3A0CA3', b:'#F72585' }, dl: true },
    { title: 'Slow Hours', sub: 'Playlist · 7 brani · scaricati', kind: 'arc', palette: { a:'#FF6B5B', b:'#3A1F8A' }, dl: true },
    { title: 'Echo, Vol. III', sub: 'Album · Marina Vega · streaming', kind: 'type', palette: { bg:'#E8E2D5', fg:'#222' }, dl: false },
    { title: 'Late drives', sub: 'Playlist · 38 brani · scaricati', kind: 'wave', palette: { a:'#5C2D8C', b:'#F0A6B0' }, dl: true },
    { title: 'Carbon Mirror', sub: 'Album · Lou Hessler · streaming', kind: 'grid', palette: { a:'#0E1F3A', b: T.ACCENT }, dl: false },
  ];
  return (
    <MHScreen navActive="library">
      <style>{SHIMMER_KEYS}</style>
      {/* Global offline banner — sticks under iOS status bar area */}
      <div style={{ position: 'absolute', top: 56, left: 0, right: 0, padding: '8px 16px', background: 'rgba(225,160,72,0.12)', borderBottom: '1px solid rgba(225,160,72,0.25)', display: 'flex', alignItems: 'center', gap: 10, zIndex: 5 }}>
        <div style={{ width: 8, height: 8, borderRadius: 4, background: '#E1A048', animation: 'mhPulse 1.6s ease-in-out infinite' }}/>
        <div style={{ flex: 1, minWidth: 0, fontSize: 12.5, fontWeight: 600, color: '#F0C77A' }}>
          Sei offline · solo i brani scaricati sono riproducibili
        </div>
        <div style={{ fontFamily: T.MONO, fontSize: 10, color: '#F0C77A', letterSpacing: 1 }}>OFFLINE</div>
      </div>

      {/* spacer below the banner */}
      <div style={{ height: 36 }}/>

      <LibraryAppBar extra={<><I.Search size={20} color={T.TEXT_HI}/><I.Plus size={22} color={T.TEXT_HI}/></>}/>

      <div style={{ display: 'flex', gap: 6, padding: '8px 16px 8px' }}>
        {['Playlist', 'Album', 'Artisti', 'Scaricati'].map((c, i) => (
          <button key={c} style={{ border: 'none', padding: '6px 12px', borderRadius: 999, background: i === 0 ? T.ACCENT : 'rgba(255,255,255,0.08)', color: i === 0 ? '#0a0a0a' : T.TEXT_HI, fontSize: 12.5, fontWeight: 600, whiteSpace: 'nowrap' }}>{c}</button>
        ))}
      </div>

      <div>
        {ITEMS.map((it, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px', opacity: it.dl ? 1 : 0.45 }}>
            <div style={{ width: 52, height: 52, flexShrink: 0 }}><MHCover kind={it.kind} palette={it.palette} radius={6}/></div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14.5, fontWeight: 600, display: 'flex', alignItems: 'center', gap: 6 }}>
                {it.title}
                {it.dl && <svg width="13" height="13" viewBox="0 0 24 24" fill={T.ACCENT}><circle cx="12" cy="12" r="10"/><path d="M8 12l3 3 5-6" stroke="#0a0a0a" strokeWidth="2.4" fill="none" strokeLinecap="round" strokeLinejoin="round"/></svg>}
              </div>
              <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 2 }}>{it.sub}</div>
            </div>
            {!it.dl && <span style={{ fontFamily: T.MONO, fontSize: 9.5, fontWeight: 700, color: T.TEXT_LO2, letterSpacing: 1, padding: '3px 6px', borderRadius: 4, background: 'rgba(255,255,255,0.04)' }}>N/A</span>}
            <I.Chevron size={14}/>
          </div>
        ))}
      </div>
    </MHScreen>
  );
}

window.MHLibraryStates = {
  LibraryLoadingScreen, LibraryPullRefreshScreen, LibraryErrorRetryScreen, LibraryLoadMoreScreen,
  LikedEmptyScreen, AlbumsEmptyScreen,
  LibraryTrackKebabSheet,
  AutoPlaylistDetailScreen,
  PlaylistGesturesScreen, PlaylistDetailWithTopBar,
  LibraryLandingPlusScreen,
  ScaricatiEmptyScreen,
  AppUpdateDialog, LibraryOfflineBadgeScreen,
};
