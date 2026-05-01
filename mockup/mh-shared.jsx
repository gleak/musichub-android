// MusicHub shared tokens, icons, and UI primitives
// All screens import from window.MH

const MH_TOKENS = {
  ACCENT: '#A8E04E',
  ACCENT_DIM: '#7FB535',
  BG_TOP: '#1F1F1F',
  BG_BOTTOM: '#080808',
  CARD: '#181818',
  CARD_HOVER: '#222',
  TEXT_HI: '#FFFFFF',
  TEXT_LO: '#9A9A9A',
  TEXT_LO2: '#6A6A6A',
  DIVIDER: 'rgba(255,255,255,0.08)',
  FONT: 'Inter, -apple-system, system-ui, sans-serif',
  MONO: 'JetBrains Mono, ui-monospace, monospace',
};

const T = MH_TOKENS;

// ── Icons ────────────────────────────────────────────────
const I = {
  Bell: ({ size = 20, color = T.TEXT_HI }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M6 8a6 6 0 1112 0c0 5 2 6 2 6H4s2-1 2-6z" stroke={color} strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
      <path d="M10 18a2 2 0 004 0" stroke={color} strokeWidth="1.6" strokeLinecap="round"/>
    </svg>
  ),
  History: ({ size = 20, color = T.TEXT_HI }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M3 12a9 9 0 109-9 9 9 0 00-7 3.5" stroke={color} strokeWidth="1.6" strokeLinecap="round"/>
      <path d="M3 3v4h4" stroke={color} strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
      <path d="M12 7v5l3 2" stroke={color} strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  ),
  Settings: ({ size = 20, color = T.TEXT_HI }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <circle cx="12" cy="12" r="3" stroke={color} strokeWidth="1.6"/>
      <path d="M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 17l2.1 2.1M4.9 19.1L7 17M17 7l2.1-2.1" stroke={color} strokeWidth="1.6" strokeLinecap="round"/>
    </svg>
  ),
  Play: ({ size = 14, color = '#000' }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill={color}><path d="M7 5v14l12-7z"/></svg>
  ),
  Pause: ({ size = 14, color = '#000' }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill={color}><rect x="6" y="5" width="4" height="14" rx="1"/><rect x="14" y="5" width="4" height="14" rx="1"/></svg>
  ),
  Heart: ({ size = 18, color = T.TEXT_LO, filled = false }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill={filled ? color : 'none'}>
      <path d="M12 21s-7-4.5-9.5-9A5 5 0 0112 6.5 5 5 0 0121.5 12c-2.5 4.5-9.5 9-9.5 9z" stroke={color} strokeWidth="1.6" strokeLinejoin="round"/>
    </svg>
  ),
  More: ({ size = 18, color = T.TEXT_LO }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill={color}>
      <circle cx="5" cy="12" r="2"/><circle cx="12" cy="12" r="2"/><circle cx="19" cy="12" r="2"/>
    </svg>
  ),
  SkipNext: ({ size = 18, color = T.TEXT_HI }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill={color}><path d="M5 5v14l10-7zM16 5h2v14h-2z"/></svg>
  ),
  SkipPrev: ({ size = 18, color = T.TEXT_HI }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill={color}><path d="M19 5v14L9 12zM6 5h2v14H6z"/></svg>
  ),
  Home: ({ size = 22, color = T.TEXT_HI, active }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill={active ? color : 'none'}>
      <path d="M3 11l9-7 9 7v9a1 1 0 01-1 1h-5v-7h-6v7H4a1 1 0 01-1-1z" stroke={color} strokeWidth="1.7" strokeLinejoin="round"/>
    </svg>
  ),
  Search: ({ size = 22, color = T.TEXT_LO, active }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <circle cx="11" cy="11" r="7" stroke={color} strokeWidth={active ? 2.2 : 1.7}/>
      <path d="M16.5 16.5L21 21" stroke={color} strokeWidth={active ? 2.2 : 1.7} strokeLinecap="round"/>
    </svg>
  ),
  Library: ({ size = 22, color = T.TEXT_LO, active }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M4 4v16M9 4v16M14 5l5 14" stroke={color} strokeWidth={active ? 2.2 : 1.7} strokeLinecap="round"/>
    </svg>
  ),
  Back: ({ size = 22, color = T.TEXT_HI }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M15 5l-7 7 7 7" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  ),
  Down: ({ size = 22, color = T.TEXT_HI }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M5 9l7 7 7-7" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  ),
  Plus: ({ size = 18, color = T.TEXT_LO }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M12 5v14M5 12h14" stroke={color} strokeWidth="2" strokeLinecap="round"/>
    </svg>
  ),
  Shuffle: ({ size = 22, color = T.TEXT_LO }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M16 3h5v5M4 20l17-17M21 16v5h-5M15 15l6 6M4 4l5 5" stroke={color} strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  ),
  Repeat: ({ size = 20, color = T.TEXT_LO }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M17 1l4 4-4 4M3 11V9a4 4 0 014-4h14M7 23l-4-4 4-4M21 13v2a4 4 0 01-4 4H3" stroke={color} strokeWidth="1.7" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  ),
  Mic: ({ size = 22, color = T.TEXT_HI }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <rect x="9" y="2" width="6" height="12" rx="3" stroke={color} strokeWidth="1.7"/>
      <path d="M5 11a7 7 0 0014 0M12 18v3" stroke={color} strokeWidth="1.7" strokeLinecap="round"/>
    </svg>
  ),
  Filter: ({ size = 18, color = T.TEXT_LO }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M3 6h18M6 12h12M10 18h4" stroke={color} strokeWidth="1.8" strokeLinecap="round"/>
    </svg>
  ),
  Grid: ({ size = 18, color = T.TEXT_LO }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <rect x="3" y="3" width="7" height="7" rx="1" stroke={color} strokeWidth="1.7"/>
      <rect x="14" y="3" width="7" height="7" rx="1" stroke={color} strokeWidth="1.7"/>
      <rect x="3" y="14" width="7" height="7" rx="1" stroke={color} strokeWidth="1.7"/>
      <rect x="14" y="14" width="7" height="7" rx="1" stroke={color} strokeWidth="1.7"/>
    </svg>
  ),
  Check: ({ size = 16, color = T.ACCENT }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M5 12l5 5 9-11" stroke={color} strokeWidth="2.4" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  ),
  Chevron: ({ size = 18, color = T.TEXT_LO }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M9 5l7 7-7 7" stroke={color} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  ),
  Download: ({ size = 18, color = T.TEXT_LO }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M12 4v12M6 12l6 6 6-6M4 21h16" stroke={color} strokeWidth="1.8" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  ),
  Share: ({ size = 18, color = T.TEXT_LO }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <circle cx="6" cy="12" r="3" stroke={color} strokeWidth="1.7"/>
      <circle cx="18" cy="6" r="3" stroke={color} strokeWidth="1.7"/>
      <circle cx="18" cy="18" r="3" stroke={color} strokeWidth="1.7"/>
      <path d="M8.5 10.5l7-3M8.5 13.5l7 3" stroke={color} strokeWidth="1.7"/>
    </svg>
  ),
  Cast: ({ size = 18, color = T.TEXT_LO }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M3 8V6a2 2 0 012-2h14a2 2 0 012 2v12a2 2 0 01-2 2h-6" stroke={color} strokeWidth="1.7" strokeLinecap="round"/>
      <path d="M3 13a7 7 0 017 7M3 17a3 3 0 013 3M3 20.5h.01" stroke={color} strokeWidth="1.7" strokeLinecap="round"/>
    </svg>
  ),
  X: ({ size = 22, color = T.TEXT_HI }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M6 6l12 12M18 6L6 18" stroke={color} strokeWidth="2" strokeLinecap="round"/>
    </svg>
  ),
  Edit: ({ size = 18, color = T.TEXT_LO }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <path d="M14 4l6 6M4 20l4-1 12-12-3-3L5 16l-1 4z" stroke={color} strokeWidth="1.7" strokeLinejoin="round"/>
    </svg>
  ),
  Verified: ({ size = 16, color = T.ACCENT }) => (
    <svg width={size} height={size} viewBox="0 0 24 24" fill={color}>
      <path d="M12 1l2.5 2.4L18 3l1 3.4L22 8l-1.4 3.4L22 15l-3 1.6-1 3.4-3.5-.4L12 23l-2.5-2.4L6 21l-1-3.4L2 16l1.4-3.4L2 9l3-1.6 1-3.4 3.5.4z"/>
      <path d="M8 12l3 3 5-6" stroke="#0a0a0a" strokeWidth="2" fill="none" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  ),
};

// ── Generative covers ────────────────────────────────────
const COVER_KINDS = {
  arc: ({ a = '#FF6B5B', b = '#3A1F8A' }, id) => (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <defs><linearGradient id={`cga${id}`} x1="0" y1="0" x2="1" y2="1"><stop offset="0%" stopColor={a}/><stop offset="100%" stopColor={b}/></linearGradient></defs>
      <rect width="100" height="100" fill={`url(#cga${id})`}/>
      <circle cx="20" cy="80" r="55" fill="none" stroke="rgba(255,255,255,0.22)" strokeWidth="1"/>
      <circle cx="20" cy="80" r="42" fill="none" stroke="rgba(255,255,255,0.16)" strokeWidth="1"/>
      <circle cx="20" cy="80" r="28" fill="none" stroke="rgba(255,255,255,0.10)" strokeWidth="1"/>
    </svg>
  ),
  grid: ({ a = '#0E1F3A', b = '#A8E04E' }) => (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="100" fill={a}/>
      {Array.from({ length: 9 }).map((_, i) => {
        const x = (i % 3) * 33, y = Math.floor(i / 3) * 33;
        const on = [0, 2, 4, 6, 8, 5].includes(i);
        return <rect key={i} x={x + 6} y={y + 6} width="22" height="22" fill={on ? b : 'rgba(255,255,255,0.08)'} rx="2"/>;
      })}
    </svg>
  ),
  moon: ({ bg = '#E8DCC4', fg = '#1A1A1A' }) => (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="100" fill={bg}/>
      <circle cx="50" cy="60" r="32" fill={fg}/>
      <circle cx="62" cy="52" r="28" fill={bg}/>
    </svg>
  ),
  triangles: ({ bg = '#1A1A1A', fg = '#FF4D2E' }) => (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="100" fill={bg}/>
      <polygon points="10,80 50,20 90,80" fill={fg}/>
      <polygon points="50,20 90,80 50,80" fill="rgba(0,0,0,0.25)"/>
    </svg>
  ),
  wave: ({ a = '#5C2D8C', b = '#F0A6B0' }, id) => (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <defs><linearGradient id={`cw${id}`} x1="0" y1="0" x2="0" y2="1"><stop offset="0%" stopColor={a}/><stop offset="100%" stopColor={b}/></linearGradient></defs>
      <rect width="100" height="100" fill={`url(#cw${id})`}/>
      {[30, 45, 60, 75].map((y, i) => (
        <path key={i} d={`M0 ${y} Q 25 ${y - 8} 50 ${y} T 100 ${y}`} fill="none" stroke="rgba(255,255,255,0.5)" strokeWidth="0.8"/>
      ))}
    </svg>
  ),
  dot: ({ bg = '#0B3D2E', fg = T.ACCENT }) => (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="100" fill={bg}/>
      <circle cx="68" cy="32" r="18" fill={fg}/>
      <text x="10" y="88" fontFamily="-apple-system, system-ui" fontSize="9" fontWeight="700" fill="rgba(255,255,255,0.9)" letterSpacing="1">NIGHT MODE</text>
    </svg>
  ),
  stripes: ({ a = '#FFC857', b = '#1A1A1A' }) => (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="100" fill={a}/>
      {[0, 20, 40, 60, 80].map((y, i) => <rect key={i} x="0" y={y} width="100" height="6" fill={b} opacity={0.85}/>)}
      <circle cx="78" cy="22" r="14" fill={b}/>
    </svg>
  ),
  blob: ({ a = '#1E3A8A', b = '#06B6D4' }) => (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="100" fill={a}/>
      <ellipse cx="50" cy="55" rx="38" ry="28" fill={b} opacity="0.85"/>
      <ellipse cx="40" cy="48" rx="20" ry="14" fill="rgba(255,255,255,0.25)"/>
    </svg>
  ),
  type: ({ bg = '#E8E2D5', fg = '#222' }) => (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="100" fill={bg}/>
      <text x="6" y="58" fontFamily="Georgia, serif" fontStyle="italic" fontSize="32" fontWeight="700" fill={fg}>echo</text>
      <line x1="6" y1="68" x2="94" y2="68" stroke={fg} strokeWidth="0.6"/>
      <text x="6" y="80" fontFamily="-apple-system, system-ui" fontSize="6" fontWeight="600" fill={fg} letterSpacing="2">VOL. III · 2026</text>
    </svg>
  ),
  duotone: ({ a = '#3A0CA3', b = '#F72585' }) => (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="50" fill={a}/>
      <rect y="50" width="100" height="50" fill={b}/>
      <circle cx="50" cy="50" r="22" fill={a}/>
    </svg>
  ),
  artist: ({ bg = '#2A1E12', fg = '#E8DCC4' }) => (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="100" fill={bg}/>
      <circle cx="50" cy="42" r="20" fill={fg}/>
      <ellipse cx="50" cy="92" rx="35" ry="22" fill={fg}/>
    </svg>
  ),
};

let coverIdCounter = 0;
function MHCover({ kind, palette, radius = 8, style = {} }) {
  const idRef = React.useRef(++coverIdCounter);
  const Render = COVER_KINDS[kind] || COVER_KINDS.arc;
  return (
    <div style={{
      width: '100%', height: '100%', borderRadius: radius, overflow: 'hidden',
      boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
      ...style,
    }}>
      {Render(palette || {}, idRef.current)}
    </div>
  );
}

// ── Animated playing-bars indicator ──────────────────────
function MHPlayingBars({ color = T.ACCENT }) {
  return (
    <div style={{ display: 'inline-flex', gap: 2, alignItems: 'flex-end', height: 12, marginLeft: 2 }}>
      {[0, 1, 2].map(i => (
        <div key={i} style={{
          width: 2, background: color, borderRadius: 1,
          animation: `mhpb${i} 0.9s ${i * 0.12}s ease-in-out infinite`,
        }}/>
      ))}
      <style>{`
        @keyframes mhpb0 { 0%,100%{height:4px} 50%{height:12px} }
        @keyframes mhpb1 { 0%,100%{height:10px} 50%{height:4px} }
        @keyframes mhpb2 { 0%,100%{height:6px} 50%{height:11px} }
      `}</style>
    </div>
  );
}

// ── Logo ────────────────────────────────────────────────
function MHLogo({ size = 28 }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <div style={{
        width: size, height: size, borderRadius: 8,
        background: T.ACCENT,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        <svg width={size * 0.57} height={size * 0.57} viewBox="0 0 16 16">
          <circle cx="8" cy="8" r="6.5" fill="none" stroke="#0a0a0a" strokeWidth="1.5"/>
          <circle cx="8" cy="8" r="2.5" fill="#0a0a0a"/>
        </svg>
      </div>
      <div style={{ fontWeight: 800, fontSize: 18, letterSpacing: -0.6, color: T.TEXT_HI }}>MusicHub</div>
    </div>
  );
}

// ── Player Bar ──────────────────────────────────────────
function MHPlayerBar({ track }) {
  const t = track || { title: 'Undertow', artist: 'Helena Vorr · Slow Hours', kind: 'blob', palette: { a: '#1E3A8A', b: '#06B6D4' }, progress: 0.38 };
  const [playing, setPlaying] = React.useState(true);
  return (
    <div style={{
      margin: '0 8px',
      borderRadius: 10,
      background: `linear-gradient(135deg, ${t.palette.a || '#1E3A8A'} 0%, ${t.palette.b || '#06B6D4'} 100%)`,
      padding: 1,
      boxShadow: '0 8px 24px rgba(0,0,0,0.4)',
    }}>
      <div style={{ background: '#161616', borderRadius: 9, padding: '8px 10px',
        display: 'flex', alignItems: 'center', gap: 10 }}>
        <div style={{ width: 40, height: 40, flexShrink: 0 }}>
          <MHCover kind={t.kind} palette={t.palette} radius={4}/>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 13, fontWeight: 600, color: T.TEXT_HI, letterSpacing: -0.1, lineHeight: '16px',
            overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.title}</div>
          <div style={{ fontSize: 11.5, color: T.TEXT_LO, lineHeight: '14px', marginTop: 1,
            overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.artist}</div>
          <div style={{ marginTop: 5, height: 2, borderRadius: 1, background: 'rgba(255,255,255,0.15)', position: 'relative' }}>
            <div style={{ position: 'absolute', inset: 0, width: `${(t.progress || 0.38) * 100}%`, background: T.ACCENT, borderRadius: 1 }}/>
          </div>
        </div>
        <button onClick={() => setPlaying(p => !p)} style={{
          width: 36, height: 36, borderRadius: 999, background: T.ACCENT, border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          boxShadow: '0 4px 12px rgba(168,224,78,0.35)',
        }}>{playing ? <I.Pause/> : <I.Play/>}</button>
        <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 4,
          display: 'flex', alignItems: 'center', flexShrink: 0 }}><I.SkipNext/></button>
      </div>
    </div>
  );
}

// ── Bottom Nav ──────────────────────────────────────────
function MHBottomNav({ active = 'home' }) {
  const item = (id, Icon, label) => {
    const isActive = id === active;
    return (
      <div key={id} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3 }}>
        <Icon active={isActive} color={isActive ? T.TEXT_HI : T.TEXT_LO}/>
        <div style={{ fontSize: 10.5, fontWeight: 600, letterSpacing: 0.1,
          color: isActive ? T.TEXT_HI : T.TEXT_LO }}>{label}</div>
      </div>
    );
  };
  return (
    <div style={{ display: 'flex', padding: '8px 12px 4px', background: 'rgba(8,8,8,0.92)',
      backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)' }}>
      {item('home', I.Home, 'Home')}
      {item('search', I.Search, 'Cerca')}
      {item('library', I.Library, 'Libreria')}
    </div>
  );
}

// ── Screen scaffold (gradient bg + bottom dock) ─────────
function MHScreen({ children, gradient, dock = true, navActive = 'home', showPlayer = true, track }) {
  const bg = gradient || `linear-gradient(180deg, ${T.BG_TOP} 0%, ${T.BG_BOTTOM} 360px)`;
  return (
    <div style={{
      width: '100%', height: '100%', background: bg, color: T.TEXT_HI,
      fontFamily: T.FONT, display: 'flex', flexDirection: 'column', overflow: 'hidden', position: 'relative',
    }}>
      <div style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', paddingTop: 56, paddingBottom: dock ? 130 : 36 }}>
        {children}
      </div>
      {dock && (
        <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0 }}>
          {showPlayer && <div style={{ paddingBottom: 6 }}><MHPlayerBar track={track}/></div>}
          <MHBottomNav active={navActive}/>
          <div style={{ height: 28 }}/>
        </div>
      )}
    </div>
  );
}

// ── Section header ──────────────────────────────────────
function MHSectionHeader({ eyebrow, title, action = 'Vedi tutti' }) {
  return (
    <div style={{ padding: '24px 16px 8px', display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
      <div>
        {eyebrow && <div style={{
          fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5,
          color: T.ACCENT, textTransform: 'uppercase', marginBottom: 4,
        }}>{eyebrow}</div>}
        <div style={{ fontSize: 20, fontWeight: 800, letterSpacing: -0.4 }}>{title}</div>
      </div>
      {action && <div style={{ fontSize: 12, color: T.TEXT_LO, fontWeight: 500 }}>{action}</div>}
    </div>
  );
}

window.MH = {
  T, I, MHCover, MHPlayingBars, MHLogo, MHPlayerBar, MHBottomNav, MHScreen, MHSectionHeader,
};
