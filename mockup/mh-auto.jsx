// MusicHub — Android Auto screens (12 schermate parallele al mobile)
const { T, I, MHCover, MHPlayingBars } = window.MH;

// ─────────────────────────────────────────────────────────
// AA Frame — landscape 1280x720 with status bar + left rail
// ─────────────────────────────────────────────────────────
function AAFrame({ children, label }) {
  return (
    <div style={{
      width: 1280, height: 720, position: 'relative',
      background: '#000', borderRadius: 14, overflow: 'hidden',
      boxShadow: '0 20px 60px rgba(0,0,0,0.4)',
      fontFamily: T.FONT, color: T.TEXT_HI,
    }}>
      <div style={{
        position: 'absolute', top: 0, left: 0, right: 0, height: 36, zIndex: 5,
        display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        padding: '0 18px', background: 'rgba(0,0,0,0.55)',
        backdropFilter: 'blur(12px)', WebkitBackdropFilter: 'blur(12px)',
        fontFamily: T.MONO, fontSize: 12,
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <span style={{ color: '#fff', fontWeight: 600 }}>14:32</span>
          <span style={{ color: T.TEXT_LO }}>22°C</span>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14, color: T.TEXT_LO }}>
          <span style={{ color: T.ACCENT }}>● MUSICHUB</span>
          <span>BT</span><span>4G</span><span>92%</span>
        </div>
      </div>
      <div style={{
        position: 'absolute', top: 36, bottom: 0, left: 0, width: 72, zIndex: 5,
        background: 'rgba(8,8,10,0.95)',
        display: 'flex', flexDirection: 'column', alignItems: 'center',
        padding: '14px 0', gap: 8,
      }}>
        <div style={{ width: 44, height: 44, borderRadius: 12, background: 'rgba(255,255,255,0.08)',
          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M3 6h18M3 12h18M3 18h18" stroke={T.TEXT_HI} strokeWidth="2" strokeLinecap="round"/>
          </svg>
        </div>
        <div style={{ flex: 1 }}/>
        {[
          { icon: <svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M12 2l9 5v10l-9 5-9-5V7l9-5z" stroke={T.TEXT_LO} strokeWidth="1.7"/><path d="M9 9h6v6H9z" fill={T.TEXT_LO}/></svg> },
          { active: true, icon: <svg width="22" height="22" viewBox="0 0 24 24" fill="none"><circle cx="8" cy="18" r="3" stroke={T.ACCENT} strokeWidth="2"/><circle cx="18" cy="16" r="3" stroke={T.ACCENT} strokeWidth="2"/><path d="M11 18V6l10-2v12" stroke={T.ACCENT} strokeWidth="2" strokeLinecap="round"/></svg> },
          { icon: <svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M3 5h18v14H3z" stroke={T.TEXT_LO} strokeWidth="1.7"/><path d="M3 9h18M7 5v14" stroke={T.TEXT_LO} strokeWidth="1.7"/></svg> },
        ].map((a, i) => (
          <div key={i} style={{
            width: 56, height: 56, borderRadius: 14,
            background: a.active ? 'rgba(168,224,78,0.15)' : 'transparent',
            border: a.active ? `1.5px solid ${T.ACCENT}` : '1.5px solid transparent',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>{a.icon}</div>
        ))}
        <div style={{ flex: 1 }}/>
        <div style={{ width: 44, height: 44, borderRadius: 999, background: 'rgba(255,255,255,0.1)',
          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <circle cx="12" cy="12" r="9" fill="none" stroke={T.TEXT_HI} strokeWidth="1.7"/>
            <circle cx="12" cy="12" r="3" fill={T.TEXT_HI}/>
          </svg>
        </div>
      </div>
      <div style={{ position: 'absolute', top: 36, left: 72, right: 0, bottom: 0 }}>{children}</div>
      {label && (
        <div style={{ position: 'absolute', top: 8, left: 86, zIndex: 6,
          fontFamily: T.MONO, fontSize: 10, fontWeight: 700, letterSpacing: 1,
          color: T.ACCENT, opacity: 0.7 }}>{label}</div>
      )}
    </div>
  );
}

// Shared bottom mini-player for AA browse-style screens
function AAMiniPlayer() {
  return (
    <div style={{
      display: 'flex', alignItems: 'center', gap: 14, padding: 14,
      background: 'rgba(168,224,78,0.08)', border: `1.5px solid rgba(168,224,78,0.25)`,
      borderRadius: 14,
    }}>
      <div style={{ width: 60, height: 60 }}>
        <MHCover kind="blob" palette={{ a: '#1E3A8A', b: '#06B6D4' }} radius={8}/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 18, fontWeight: 800, letterSpacing: -0.3 }}>Undertow</div>
        <div style={{ fontSize: 13, color: T.TEXT_LO }}>Helena Vorr</div>
      </div>
      <MHPlayingBars/>
      <button style={{ width: 52, height: 52, borderRadius: 999, background: T.ACCENT, border: 'none',
        display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
        <I.Pause size={18}/>
      </button>
    </div>
  );
}

// AA tab strip
function AATabs({ active }) {
  const tabs = ['Casa', 'Cerca', 'Libreria', 'Recenti'];
  return (
    <div style={{ display: 'flex', gap: 8, marginBottom: 18 }}>
      {tabs.map(t => (
        <div key={t} style={{
          padding: '10px 20px', borderRadius: 999,
          background: t === active ? T.ACCENT : 'rgba(255,255,255,0.08)',
          color: t === active ? '#0a0a0a' : T.TEXT_HI,
          fontSize: 16, fontWeight: 700, letterSpacing: -0.2,
        }}>{t}</div>
      ))}
    </div>
  );
}

const AABG = 'linear-gradient(180deg, #1F1F1F 0%, #080808 100%)';
const AAPad = { width: '100%', height: '100%', padding: 24,
  background: AABG, color: T.TEXT_HI, fontFamily: T.FONT,
  display: 'flex', flexDirection: 'column' };

// 1 · AA Home
function AAHome() {
  const QUICK = [
    { title: 'Echo, Vol. III', sub: 'Marina Vega', kind: 'type', palette: { bg: '#E8E2D5', fg: '#222' } },
    { title: 'Liked songs', sub: '247 brani', kind: 'duotone', palette: { a: '#3A0CA3', b: '#F72585' } },
    { title: 'Slow Hours', sub: 'Marina Vega', kind: 'arc', palette: { a: '#FF6B5B', b: '#3A1F8A' } },
    { title: 'Night Mode', sub: 'Tobi Akin', kind: 'dot', palette: { bg: '#0B3D2E', fg: T.ACCENT } },
  ];
  const FOR_YOU = [
    { title: 'Late drives', kind: 'wave', palette: { a: '#5C2D8C', b: '#F0A6B0' } },
    { title: 'Mattina', kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
    { title: 'Carbon Mirror', kind: 'grid', palette: { a: '#0E1F3A', b: T.ACCENT } },
    { title: 'Plein soleil', kind: 'wave', palette: { a: '#1E3A8A', b: '#06B6D4' } },
  ];
  return (
    <div style={AAPad}>
      <AATabs active="Casa"/>
      <div style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5, marginBottom: 8 }}>// BUONGIORNO MARCO</div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2,1fr)', gap: 12, marginBottom: 18 }}>
        {QUICK.map((q, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: 12,
            background: T.CARD, borderRadius: 12 }}>
            <div style={{ width: 72, height: 72, flexShrink: 0 }}>
              <MHCover kind={q.kind} palette={q.palette} radius={8}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 17, fontWeight: 700, letterSpacing: -0.2,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{q.title}</div>
              <div style={{ fontSize: 13, color: T.TEXT_LO, marginTop: 4 }}>{q.sub}</div>
            </div>
            <div style={{ width: 44, height: 44, borderRadius: 999, background: T.ACCENT,
              display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <I.Play size={16}/>
            </div>
          </div>
        ))}
      </div>
      <div style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5, marginBottom: 10 }}>// PER TE</div>
      <div style={{ display: 'flex', gap: 12, flex: 1 }}>
        {FOR_YOU.map((f, i) => (
          <div key={i} style={{ flex: 1, minWidth: 0 }}>
            <div style={{ aspectRatio: '1', width: '100%' }}>
              <MHCover kind={f.kind} palette={f.palette} radius={10}/>
            </div>
            <div style={{ fontSize: 15, fontWeight: 700, marginTop: 8,
              overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{f.title}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

// 2 · AA Search
function AASearch() {
  const GENRES = [
    { name: 'Indie', color: '#3A0CA3' },
    { name: 'Elettronica', color: '#06B6D4' },
    { name: 'Hip-hop', color: '#FF4D2E' },
    { name: 'Jazz', color: '#FFC857' },
    { name: 'Classica', color: '#5C2D8C' },
    { name: 'Ambient', color: '#0B3D2E' },
    { name: 'Rock', color: '#7C2D12' },
    { name: 'Pop', color: '#F0A6B0' },
  ];
  return (
    <div style={AAPad}>
      <AATabs active="Cerca"/>
      <div style={{ background: 'rgba(255,255,255,0.08)', borderRadius: 14, padding: '18px 20px',
        display: 'flex', alignItems: 'center', gap: 14, marginBottom: 18 }}>
        <I.Search size={24}/>
        <div style={{ flex: 1, fontSize: 18, color: T.TEXT_LO2 }}>Brani, artisti, playlist</div>
        <div style={{ width: 52, height: 52, borderRadius: 999, background: T.ACCENT,
          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <I.Mic size={22} color="#0a0a0a"/>
        </div>
      </div>
      <div style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5, marginBottom: 10 }}>// SFOGLIA</div>
      <div style={{ flex: 1, display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gridAutoRows: '1fr', gap: 12 }}>
        {GENRES.map(g => (
          <div key={g.name} style={{ background: g.color, borderRadius: 14, padding: 18,
            display: 'flex', alignItems: 'flex-end', fontSize: 22, fontWeight: 800,
            letterSpacing: -0.4, color: '#fff', textShadow: '0 2px 6px rgba(0,0,0,0.4)' }}>
            {g.name}
          </div>
        ))}
      </div>
    </div>
  );
}

// 3 · AA Library
function AALibrary() {
  const ITEMS = [
    { title: 'Liked songs', sub: '247 brani', kind: 'duotone', palette: { a: '#3A0CA3', b: '#F72585' } },
    { title: 'Slow Hours', sub: 'Marina Vega', kind: 'arc', palette: { a: '#FF6B5B', b: '#3A1F8A' } },
    { title: 'Late drives', sub: '38 brani', kind: 'wave', palette: { a: '#5C2D8C', b: '#F0A6B0' } },
    { title: 'Echo, Vol. III', sub: 'Marina Vega', kind: 'type', palette: { bg: '#E8E2D5', fg: '#222' } },
    { title: 'Mattina', sub: '22 brani', kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
    { title: 'Carbon Mirror', sub: 'Lou Hessler', kind: 'grid', palette: { a: '#0E1F3A', b: T.ACCENT } },
  ];
  return (
    <div style={AAPad}>
      <AATabs active="Libreria"/>
      <div style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
        {['Tutto', 'Playlist', 'Album', 'Artisti', 'Scaricati'].map((c, i) => (
          <div key={c} style={{ padding: '8px 14px', borderRadius: 999,
            background: i === 0 ? 'rgba(168,224,78,0.18)' : 'rgba(255,255,255,0.06)',
            color: i === 0 ? T.ACCENT : T.TEXT_HI,
            fontSize: 13, fontWeight: 600 }}>{c}</div>
        ))}
      </div>
      <div style={{ flex: 1, display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 12 }}>
        {ITEMS.map((it, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: 12,
            background: T.CARD, borderRadius: 12 }}>
            <div style={{ width: 72, height: 72, flexShrink: 0 }}>
              <MHCover kind={it.kind} palette={it.palette} radius={8}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 16, fontWeight: 700,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{it.title}</div>
              <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 4 }}>{it.sub}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// 4 · AA Album / Playlist detail
function AAAlbum() {
  const TRACKS = [
    { n: 1, title: 'Cold lights', dur: '3:14' },
    { n: 2, title: 'Long way home', dur: '4:18' },
    { n: 3, title: 'Echo (interlude)', dur: '1:42' },
    { n: 4, title: 'Pyre', dur: '2:55' },
    { n: 5, title: 'Slow hours', dur: '5:07' },
    { n: 6, title: 'Undertow', dur: '4:02', playing: true },
    { n: 7, title: 'Plein soleil', dur: '3:31' },
  ];
  return (
    <div style={{ ...AAPad, padding: 0, flexDirection: 'row',
      background: 'linear-gradient(135deg, #2A1448 0%, #0F0820 60%, #060309 100%)' }}>
      <div style={{ width: 360, padding: 32, display: 'flex', flexDirection: 'column' }}>
        <div style={{ width: 296, height: 296, marginBottom: 22, boxShadow: '0 25px 60px rgba(0,0,0,0.6)' }}>
          <MHCover kind="arc" palette={{ a: '#FF6B5B', b: '#3A1F8A' }} radius={10}/>
        </div>
        <div style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5, marginBottom: 4 }}>// ALBUM</div>
        <div style={{ fontSize: 30, fontWeight: 900, letterSpacing: -0.6, lineHeight: 1.05 }}>Slow Hours</div>
        <div style={{ fontSize: 16, color: T.TEXT_LO, marginTop: 6 }}>Marina Vega · 2026</div>
        <div style={{ flex: 1 }}/>
        <div style={{ display: 'flex', gap: 12 }}>
          <button style={{ flex: 1, height: 56, borderRadius: 999, background: T.ACCENT, border: 'none',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10,
            fontSize: 16, fontWeight: 800, color: '#0a0a0a', cursor: 'pointer' }}>
            <I.Play size={16}/> RIPRODUCI
          </button>
          <button style={{ width: 56, height: 56, borderRadius: 999,
            background: 'rgba(255,255,255,0.08)', border: '1.5px solid rgba(255,255,255,0.12)',
            display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <I.Shuffle size={20} color={T.ACCENT}/>
          </button>
          <button style={{ width: 56, height: 56, borderRadius: 999,
            background: 'rgba(255,255,255,0.08)', border: '1.5px solid rgba(255,255,255,0.12)',
            display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <I.Heart size={20} color={T.ACCENT} filled/>
          </button>
        </div>
      </div>
      <div style={{ flex: 1, padding: '32px 28px 28px 0', overflowY: 'auto' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 14 }}>
          <div style={{ fontSize: 18, fontWeight: 800 }}>10 brani</div>
          <div style={{ fontSize: 14, color: T.TEXT_LO, fontFamily: T.MONO }}>· 36 min</div>
        </div>
        {TRACKS.map(t => (
          <div key={t.n} style={{ display: 'flex', alignItems: 'center', gap: 16,
            padding: '14px 0', borderBottom: `1px solid ${T.DIVIDER}` }}>
            <div style={{ width: 28, fontFamily: T.MONO, fontSize: 14,
              color: t.playing ? T.ACCENT : T.TEXT_LO, textAlign: 'center' }}>
              {t.playing ? <MHPlayingBars/> : String(t.n).padStart(2, '0')}
            </div>
            <div style={{ flex: 1, fontSize: 18, fontWeight: 600,
              color: t.playing ? T.ACCENT : T.TEXT_HI }}>{t.title}</div>
            <div style={{ fontFamily: T.MONO, fontSize: 13, color: T.TEXT_LO }}>{t.dur}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

// 5 · AA Now Playing
function AANowPlaying() {
  return (
    <div style={{ width: '100%', height: '100%', display: 'flex',
      background: 'linear-gradient(135deg, #1E3A8A 0%, #0E1F3A 50%, #060309 100%)',
      color: T.TEXT_HI, fontFamily: T.FONT }}>
      <div style={{ width: 380, padding: 36, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ width: 308, height: 308, boxShadow: '0 25px 60px rgba(0,0,0,0.6)' }}>
          <MHCover kind="blob" palette={{ a: '#1E3A8A', b: '#06B6D4' }} radius={10}/>
        </div>
      </div>
      <div style={{ flex: 1, padding: '36px 36px 28px 0', display: 'flex', flexDirection: 'column' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 12, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5, marginBottom: 8 }}>// IN RIPRODUZIONE</div>
        <div style={{ fontSize: 44, fontWeight: 900, letterSpacing: -1, lineHeight: 1.05 }}>Undertow</div>
        <div style={{ fontSize: 22, color: T.TEXT_LO, marginTop: 8, fontWeight: 500 }}>Helena Vorr · Slow Hours</div>
        <div style={{ marginTop: 30 }}>
          <div style={{ height: 5, borderRadius: 3, background: 'rgba(255,255,255,0.15)', position: 'relative' }}>
            <div style={{ position: 'absolute', inset: 0, width: '38%', background: T.ACCENT, borderRadius: 3 }}/>
            <div style={{ position: 'absolute', left: '38%', top: '50%', transform: 'translate(-50%,-50%)',
              width: 18, height: 18, borderRadius: 999, background: T.ACCENT }}/>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 10,
            fontFamily: T.MONO, fontSize: 16, color: T.TEXT_LO }}>
            <span>1:32</span><span>4:02</span>
          </div>
        </div>
        <div style={{ flex: 1 }}/>
        <div style={{ display: 'flex', alignItems: 'center', gap: 18, marginBottom: 14 }}>
          {['Coda', 'Testo', 'Video'].map((c, i) => (
            <div key={c} style={{ padding: '8px 16px', borderRadius: 999,
              background: i === 1 ? 'rgba(168,224,78,0.18)' : 'rgba(255,255,255,0.06)',
              color: i === 1 ? T.ACCENT : T.TEXT_HI, fontSize: 13, fontWeight: 600 }}>{c}</div>
          ))}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
          <button style={{ width: 72, height: 72, borderRadius: 999, background: 'rgba(255,255,255,0.08)',
            border: '1.5px solid rgba(255,255,255,0.12)',
            display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <I.Shuffle size={28} color={T.ACCENT}/></button>
          <button style={{ width: 72, height: 72, borderRadius: 999, background: 'rgba(255,255,255,0.08)',
            border: '1.5px solid rgba(255,255,255,0.12)',
            display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <I.SkipPrev size={32}/></button>
          <button style={{ width: 96, height: 96, borderRadius: 999, background: T.ACCENT, border: 'none',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 12px 32px rgba(168,224,78,0.5)' }}>
            <I.Pause size={32}/></button>
          <button style={{ width: 72, height: 72, borderRadius: 999, background: 'rgba(255,255,255,0.08)',
            border: '1.5px solid rgba(255,255,255,0.12)',
            display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <I.SkipNext size={32}/></button>
          <button style={{ width: 72, height: 72, borderRadius: 999, background: 'rgba(255,255,255,0.08)',
            border: '1.5px solid rgba(255,255,255,0.12)',
            display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <I.Heart size={28} color={T.ACCENT} filled/></button>
        </div>
      </div>
    </div>
  );
}

// 6 · AA Lyrics — split: art + scroll lyrics
function AALyrics() {
  const LINES = [
    { text: 'Slow water through the broken glass', past: true },
    { text: 'Soft hum from a passing car', past: true },
    { text: 'I keep your name in lower case', past: true },
    { text: 'Quiet rooms remember everything', past: true },
    { text: 'Undertow, undertow', active: true },
    { text: 'Drag me to the morning light' },
    { text: 'Hold the silence like a book' },
    { text: 'Counting hours we never spent' },
  ];
  return (
    <div style={{ width: '100%', height: '100%', display: 'flex',
      background: 'linear-gradient(135deg, #1E3A8A 0%, #0E1F3A 60%, #060309 100%)',
      fontFamily: T.FONT, color: T.TEXT_HI }}>
      <div style={{ width: 360, padding: 36, display: 'flex', flexDirection: 'column' }}>
        <div style={{ width: 288, height: 288, boxShadow: '0 25px 60px rgba(0,0,0,0.6)', marginBottom: 20 }}>
          <MHCover kind="blob" palette={{ a: '#1E3A8A', b: '#06B6D4' }} radius={10}/>
        </div>
        <div style={{ fontSize: 26, fontWeight: 800, letterSpacing: -0.5 }}>Undertow</div>
        <div style={{ fontSize: 15, color: T.TEXT_LO, marginTop: 4 }}>Helena Vorr</div>
        <div style={{ flex: 1 }}/>
        <button style={{ height: 56, borderRadius: 999, background: T.ACCENT, border: 'none',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 8px 24px rgba(168,224,78,0.4)' }}>
          <I.Pause size={20}/></button>
      </div>
      <div style={{ flex: 1, padding: '36px 36px 36px 0', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 22 }}>
          <div style={{ fontFamily: T.MONO, fontSize: 12, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5 }}>// TESTO SINCRONIZZATO</div>
          <div style={{ background: 'rgba(168,224,78,0.15)', color: T.ACCENT,
            fontSize: 11, fontFamily: T.MONO, fontWeight: 700, padding: '4px 8px', borderRadius: 6 }}>● LIVE</div>
        </div>
        <div style={{ flex: 1, overflowY: 'auto', paddingRight: 12 }}>
          {LINES.map((l, i) => (
            <div key={i} style={{
              fontSize: l.active ? 32 : 24, fontWeight: l.active ? 800 : 600,
              letterSpacing: -0.5, lineHeight: 1.3, marginBottom: 22,
              color: l.active ? T.TEXT_HI : (l.past ? 'rgba(255,255,255,0.32)' : 'rgba(255,255,255,0.55)'),
            }}>{l.text}</div>
          ))}
        </div>
      </div>
    </div>
  );
}

// 7 · AA Video — driver-safety: only audio while driving
function AAVideo() {
  return (
    <div style={{ width: '100%', height: '100%',
      background: '#000', fontFamily: T.FONT, color: T.TEXT_HI,
      display: 'flex', flexDirection: 'column' }}>
      <div style={{ position: 'relative', flex: 1 }}>
        <div style={{ position: 'absolute', inset: 0, filter: 'blur(8px)' }}>
          <MHCover kind="blob" palette={{ a: '#1E3A8A', b: '#06B6D4' }} radius={0}/>
        </div>
        <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.55)' }}/>
        <div style={{ position: 'relative', zIndex: 2, height: '100%',
          display: 'flex', alignItems: 'center', justifyContent: 'center', padding: 36 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 30, maxWidth: 900 }}>
            <div style={{ width: 240, height: 240, flexShrink: 0, boxShadow: '0 25px 60px rgba(0,0,0,0.6)' }}>
              <MHCover kind="blob" palette={{ a: '#1E3A8A', b: '#06B6D4' }} radius={10}/>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6,
                background: 'rgba(255,77,46,0.18)', border: '1.5px solid #FF4D2E',
                padding: '6px 12px', borderRadius: 999, marginBottom: 14 }}>
                <div style={{ width: 8, height: 8, borderRadius: 999, background: '#FF4D2E' }}/>
                <div style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700, letterSpacing: 1, color: '#fff' }}>VIDEO IN PAUSA</div>
              </div>
              <div style={{ fontSize: 30, fontWeight: 900, letterSpacing: -0.6, lineHeight: 1.1 }}>Undertow — official video</div>
              <div style={{ fontSize: 16, color: T.TEXT_LO, marginTop: 8 }}>Helena Vorr</div>
              <div style={{ fontSize: 14, color: T.TEXT_LO, marginTop: 18, lineHeight: 1.5, maxWidth: 480 }}>
                Per la tua sicurezza, il video è disabilitato durante la guida.
                Continua ad ascoltare l'audio o riprendi il video da fermo.
              </div>
              <div style={{ display: 'flex', gap: 12, marginTop: 22 }}>
                <button style={{ height: 56, padding: '0 24px', borderRadius: 999, background: T.ACCENT, border: 'none',
                  display: 'flex', alignItems: 'center', gap: 10, fontSize: 15, fontWeight: 800,
                  color: '#0a0a0a', cursor: 'pointer' }}>
                  <I.Play size={14}/> CONTINUA AUDIO
                </button>
                <button style={{ height: 56, padding: '0 24px', borderRadius: 999,
                  background: 'rgba(255,255,255,0.08)', border: '1.5px solid rgba(255,255,255,0.18)',
                  fontSize: 15, fontWeight: 600, color: T.TEXT_HI, cursor: 'pointer' }}>
                  Salva per dopo
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div style={{ padding: 18 }}><AAMiniPlayer/></div>
    </div>
  );
}

// 8 · AA Artist
function AAArtist() {
  const POPULAR = [
    { n: 1, title: 'Long way home', dur: '4:18' },
    { n: 2, title: 'Cold lights', dur: '3:14' },
    { n: 3, title: 'Pyre', dur: '2:55' },
    { n: 4, title: 'Echo', dur: '3:48' },
  ];
  return (
    <div style={{ width: '100%', height: '100%', display: 'flex',
      background: 'linear-gradient(135deg, #2A1E12 0%, #060309 70%)',
      fontFamily: T.FONT, color: T.TEXT_HI }}>
      <div style={{ width: 360, padding: 36, display: 'flex', flexDirection: 'column' }}>
        <div style={{ width: 220, height: 220, borderRadius: 999, overflow: 'hidden',
          boxShadow: '0 25px 60px rgba(0,0,0,0.6)', marginBottom: 22 }}>
          <MHCover kind="artist" palette={{ bg: '#2A1E12', fg: '#E8DCC4' }} radius={999}/>
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, marginBottom: 6 }}>
          <I.Verified size={14}/>
          <div style={{ fontSize: 12, fontWeight: 600 }}>Verificato</div>
        </div>
        <div style={{ fontSize: 36, fontWeight: 900, letterSpacing: -0.8, lineHeight: 1 }}>Marina Vega</div>
        <div style={{ fontSize: 14, color: T.TEXT_LO, marginTop: 8, fontFamily: T.MONO, letterSpacing: 0.4 }}>2.4M ASCOLTATORI</div>
        <div style={{ flex: 1 }}/>
        <button style={{ height: 56, borderRadius: 999, background: T.ACCENT, border: 'none',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10,
          fontSize: 16, fontWeight: 800, color: '#0a0a0a', cursor: 'pointer' }}>
          <I.Play size={14}/> RIPRODUCI
        </button>
      </div>
      <div style={{ flex: 1, padding: '36px 28px 28px 0', overflowY: 'auto' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5, marginBottom: 14 }}>// PIÙ ASCOLTATI</div>
        {POPULAR.map(t => (
          <div key={t.n} style={{ display: 'flex', alignItems: 'center', gap: 16,
            padding: '14px 0', borderBottom: `1px solid ${T.DIVIDER}` }}>
            <div style={{ width: 28, fontFamily: T.MONO, fontSize: 14, color: T.TEXT_LO, textAlign: 'center' }}>
              {String(t.n).padStart(2, '0')}
            </div>
            <div style={{ flex: 1, fontSize: 18, fontWeight: 600 }}>{t.title}</div>
            <div style={{ fontFamily: T.MONO, fontSize: 13, color: T.TEXT_LO }}>{t.dur}</div>
          </div>
        ))}
        <div style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5, marginTop: 24, marginBottom: 14 }}>// ALBUM</div>
        <div style={{ display: 'flex', gap: 12 }}>
          {[
            { kind: 'arc', palette: { a: '#FF6B5B', b: '#3A1F8A' } },
            { kind: 'type', palette: { bg: '#E8E2D5', fg: '#222' } },
            { kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
          ].map((a, i) => (
            <div key={i} style={{ width: 120, height: 120 }}>
              <MHCover kind={a.kind} palette={a.palette} radius={8}/>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

// 9 · AA Profile / Settings (free account — minimal)
function AAProfile() {
  return (
    <div style={AAPad}>
      <AATabs active="Casa"/>
      <div style={{ display: 'flex', alignItems: 'center', gap: 22, marginBottom: 22 }}>
        <div style={{ width: 80, height: 80, borderRadius: 999,
          background: `linear-gradient(135deg, ${T.ACCENT} 0%, #3A0CA3 100%)`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontSize: 32, fontWeight: 900, color: '#0a0a0a' }}>M</div>
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 26, fontWeight: 800, letterSpacing: -0.4 }}>Marco Bianchi</div>
          <div style={{ fontSize: 14, color: T.TEXT_LO, marginTop: 4, fontFamily: T.MONO }}>@marco</div>
        </div>
      </div>
      <div style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5, marginBottom: 10 }}>// IMPOSTAZIONI RIPRODUZIONE</div>
      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2,1fr)', gap: 12, marginBottom: 18 }}>
        {[
          { l: 'Crossfade', d: '6 sec' },
          { l: 'Download offline', d: '12 GB' },
          { l: 'Notifiche', d: 'Attive' },
          { l: 'Lingua', d: 'Italiano' },
        ].map((it, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', padding: 18,
            background: T.CARD, borderRadius: 12 }}>
            <div style={{ flex: 1, fontSize: 16, fontWeight: 600 }}>{it.l}</div>
            <div style={{ fontSize: 14, color: T.TEXT_LO, marginRight: 8 }}>{it.d}</div>
            <I.Chevron/>
          </div>
        ))}
      </div>
      <div style={{ flex: 1 }}/>
      <AAMiniPlayer/>
    </div>
  );
}

// 10 · AA Queue
function AAQueue() {
  const QUEUE = [
    { title: 'Undertow', artist: 'Helena Vorr', dur: '4:02', kind: 'blob', palette: { a: '#1E3A8A', b: '#06B6D4' }, playing: true },
    { title: 'Plein soleil', artist: 'Camille Roche', dur: '3:31', kind: 'wave', palette: { a: '#5C2D8C', b: '#F0A6B0' } },
    { title: 'Glasshouse', artist: 'The Astral Kit', dur: '5:07', kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
    { title: 'Carbon Mirror', artist: 'Lou Hessler', dur: '3:42', kind: 'grid', palette: { a: '#0E1F3A', b: T.ACCENT } },
    { title: 'Pyre', artist: 'Tobi Akin', dur: '2:55', kind: 'triangles', palette: { bg: '#1A1A1A', fg: '#FF4D2E' } },
    { title: 'After', artist: 'Marina Vega', dur: '2:18', kind: 'arc', palette: { a: '#FF6B5B', b: '#3A1F8A' } },
  ];
  return (
    <div style={AAPad}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 14, marginBottom: 18 }}>
        <div style={{ fontSize: 30, fontWeight: 900, letterSpacing: -0.6 }}>Coda</div>
        <div style={{ fontSize: 14, color: T.TEXT_LO, fontFamily: T.MONO }}>{QUEUE.length} brani</div>
        <div style={{ flex: 1 }}/>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 14px',
          background: 'rgba(255,255,255,0.06)', borderRadius: 999, fontSize: 13, fontWeight: 600 }}>
          <I.Shuffle size={16} color={T.ACCENT}/> Shuffle
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 14px',
          background: 'rgba(255,255,255,0.06)', borderRadius: 999, fontSize: 13, fontWeight: 600 }}>
          <I.Repeat size={16}/> Ripeti
        </div>
      </div>
      <div style={{ flex: 1, overflowY: 'auto' }}>
        {QUEUE.map((t, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 16,
            padding: '12px 14px', borderRadius: 12,
            background: t.playing ? 'rgba(168,224,78,0.08)' : 'transparent',
            border: t.playing ? `1.5px solid rgba(168,224,78,0.25)` : '1.5px solid transparent',
            marginBottom: 6 }}>
            <div style={{ width: 60, height: 60 }}>
              <MHCover kind={t.kind} palette={t.palette} radius={6}/>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: 18, fontWeight: 700, color: t.playing ? T.ACCENT : T.TEXT_HI,
                display: 'flex', alignItems: 'center', gap: 8 }}>
                {t.title}{t.playing && <MHPlayingBars/>}
              </div>
              <div style={{ fontSize: 13, color: T.TEXT_LO, marginTop: 2 }}>{t.artist}</div>
            </div>
            <div style={{ fontFamily: T.MONO, fontSize: 14, color: T.TEXT_LO }}>{t.dur}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

// 11 · AA Voice search
function AAVoice() {
  return (
    <div style={{ ...AAPad, alignItems: 'center', justifyContent: 'center',
      background: 'radial-gradient(ellipse at center, #1E3A8A 0%, #060309 70%)' }}>
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <div style={{ position: 'relative', width: 200, height: 200, marginBottom: 28 }}>
          <div style={{ position: 'absolute', inset: 0, borderRadius: 999,
            background: T.ACCENT, opacity: 0.15, animation: 'aap1 2s ease-out infinite' }}/>
          <div style={{ position: 'absolute', inset: 30, borderRadius: 999,
            background: T.ACCENT, opacity: 0.25, animation: 'aap1 2s ease-out 0.4s infinite' }}/>
          <div style={{ position: 'absolute', inset: 60, borderRadius: 999,
            background: T.ACCENT, display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 12px 32px rgba(168,224,78,0.5)' }}>
            <I.Mic size={36} color="#0a0a0a"/>
          </div>
          <style>{`@keyframes aap1 { 0%{transform:scale(0.85);opacity:0.4} 100%{transform:scale(1.2);opacity:0} }`}</style>
        </div>
        <div style={{ fontFamily: T.MONO, fontSize: 12, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5, marginBottom: 12 }}>// IN ASCOLTO</div>
        <div style={{ fontSize: 38, fontWeight: 900, letterSpacing: -0.6, textAlign: 'center', maxWidth: 700 }}>
          "Riproduci Slow Hours di Marina Vega"
        </div>
        <div style={{ fontSize: 16, color: T.TEXT_LO, marginTop: 18 }}>Prova: brano · artista · playlist · genere</div>
      </div>
    </div>
  );
}

// 12 · AA Recents
function AARecents() {
  const RECENTS = [
    { title: 'Slow Hours', sub: 'Album · Marina Vega', kind: 'arc', palette: { a: '#FF6B5B', b: '#3A1F8A' } },
    { title: 'Late drives', sub: 'Playlist', kind: 'wave', palette: { a: '#5C2D8C', b: '#F0A6B0' } },
    { title: 'Helena Vorr', sub: 'Artista', kind: 'artist', palette: { bg: '#1E3A8A', fg: '#E8DCC4' }, round: true },
    { title: 'Liked songs', sub: 'Playlist', kind: 'duotone', palette: { a: '#3A0CA3', b: '#F72585' } },
    { title: 'Echo, Vol. III', sub: 'Album', kind: 'type', palette: { bg: '#E8E2D5', fg: '#222' } },
    { title: 'Carbon Mirror', sub: 'Album', kind: 'grid', palette: { a: '#0E1F3A', b: T.ACCENT } },
  ];
  return (
    <div style={AAPad}>
      <AATabs active="Recenti"/>
      <div style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5, marginBottom: 10 }}>// ASCOLTATI DI RECENTE</div>
      <div style={{ flex: 1, display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gridAutoRows: 'min-content', gap: 14 }}>
        {RECENTS.map((r, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: 12,
            background: T.CARD, borderRadius: 12 }}>
            <div style={{ width: 76, height: 76, flexShrink: 0,
              borderRadius: r.round ? 999 : 8, overflow: 'hidden' }}>
              <MHCover kind={r.kind} palette={r.palette} radius={r.round ? 999 : 8}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 16, fontWeight: 700,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{r.title}</div>
              <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 4 }}>{r.sub}</div>
            </div>
            <div style={{ width: 44, height: 44, borderRadius: 999, background: T.ACCENT,
              display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              <I.Play size={14}/>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// 13 · AA Per te — playlist generate
function AAForYou() {
  const { GEN_PLAYLISTS, GenCover } = window.MHGenerated;
  const rotation = GEN_PLAYLISTS.find(p => p.id === 'rotation');
  const mixes = GEN_PLAYLISTS.filter(p => p.family === 'daily');
  const updates = ['releases', 'radar', 'capsule'].map(id => GEN_PLAYLISTS.find(p => p.id === id));

  return (
    <div style={AAPad}>
      <div style={{ display: 'flex', gap: 8, marginBottom: 14 }}>
        {['Casa', 'Cerca', 'Per te', 'Libreria', 'Recenti'].map(t => (
          <div key={t} style={{
            padding: '10px 20px', borderRadius: 999,
            background: t === 'Per te' ? T.ACCENT : 'rgba(255,255,255,0.08)',
            color: t === 'Per te' ? '#0a0a0a' : T.TEXT_HI,
            fontSize: 16, fontWeight: 700, letterSpacing: -0.2,
          }}>{t}</div>
        ))}
      </div>

      <div style={{ flex: 1, display: 'flex', gap: 16, minHeight: 0 }}>
        {/* Hero rotation */}
        <div style={{ width: 320, flexShrink: 0,
          background: 'linear-gradient(135deg, rgba(168,224,78,0.18) 0%, rgba(168,224,78,0.04) 70%)',
          border: '1.5px solid rgba(168,224,78,0.3)', borderRadius: 16, padding: 18,
          display: 'flex', flexDirection: 'column' }}>
          <div style={{ width: '100%', aspectRatio: '1', marginBottom: 14 }}>
            <GenCover pl={rotation} size={284} radius={10}/>
          </div>
          <div style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700, letterSpacing: 1.4, color: T.ACCENT }}>// IN ROTAZIONE</div>
          <div style={{ fontSize: 24, fontWeight: 900, letterSpacing: -0.5, marginTop: 4 }}>{rotation.title}</div>
          <div style={{ fontSize: 13, color: T.TEXT_LO, marginTop: 6, lineHeight: 1.4 }}>
            {rotation.count} brani · {rotation.duration}
          </div>
          <div style={{ flex: 1 }}/>
          <button style={{ height: 56, borderRadius: 999, background: T.ACCENT, border: 'none',
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10,
            fontSize: 16, fontWeight: 800, color: '#0a0a0a', cursor: 'pointer' }}>
            <I.Play size={14}/> RIPRODUCI
          </button>
        </div>

        {/* Mix grid + updates */}
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', gap: 12, minWidth: 0 }}>
          <div style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700, letterSpacing: 1.4, color: T.ACCENT }}>// 6 MIX OGGI</div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 10 }}>
            {mixes.map(m => (
              <div key={m.id} style={{ display: 'flex', alignItems: 'center', gap: 12,
                background: T.CARD, borderRadius: 12, padding: 10 }}>
                <div style={{ width: 64, height: 64, flexShrink: 0 }}>
                  <GenCover pl={m} size={64} radius={6}/>
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 15, fontWeight: 700,
                    overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{m.title}</div>
                  <div style={{ fontSize: 11.5, color: T.TEXT_LO, marginTop: 2,
                    overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{m.sub}</div>
                </div>
              </div>
            ))}
          </div>
          <div style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700, letterSpacing: 1.4, color: T.ACCENT, marginTop: 8 }}>// SETTIMANALI</div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 10, flex: 1, minHeight: 0 }}>
            {updates.map(u => (
              <div key={u.id} style={{ background: T.CARD, borderRadius: 12, padding: 12,
                display: 'flex', flexDirection: 'column', minHeight: 0 }}>
                <div style={{ width: '100%', aspectRatio: '1', marginBottom: 10, minHeight: 0 }}>
                  <GenCover pl={u} size={120} radius={8}/>
                </div>
                <div style={{ fontSize: 14, fontWeight: 700,
                  overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{u.title}</div>
                <div style={{ fontSize: 11, color: T.TEXT_LO, marginTop: 2,
                  overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{u.sub}</div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
}

window.MHAutoScreens = {
  AAFrame, AAHome, AASearch, AALibrary, AAAlbum, AANowPlaying,
  AALyrics, AAVideo, AAArtist, AAProfile, AAQueue, AAVoice, AARecents, AAForYou,
};
