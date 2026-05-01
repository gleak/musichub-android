// MusicHub — Home screen
// Original design language: lime-green accent (#A8E04E), Inter Display + JetBrains Mono.
// No podcasts. Album/artist/playlist focus.

const ACCENT = '#A8E04E';
const BG_TOP = '#1F1F1F';
const BG_BOTTOM = '#080808';
const CARD = '#181818';
const CARD_HOVER = '#222';
const TEXT_HI = '#FFFFFF';
const TEXT_LO = '#9A9A9A';

// ── Icons ────────────────────────────────────────────────
const IconBell = ({ size = 20, color = TEXT_HI }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
    <path d="M6 8a6 6 0 1112 0c0 5 2 6 2 6H4s2-1 2-6z" stroke={color} strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M10 18a2 2 0 004 0" stroke={color} strokeWidth="1.6" strokeLinecap="round"/>
  </svg>
);
const IconHistory = ({ size = 20, color = TEXT_HI }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
    <path d="M3 12a9 9 0 109-9 9 9 0 00-7 3.5" stroke={color} strokeWidth="1.6" strokeLinecap="round"/>
    <path d="M3 3v4h4" stroke={color} strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
    <path d="M12 7v5l3 2" stroke={color} strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
  </svg>
);
const IconSettings = ({ size = 20, color = TEXT_HI }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
    <circle cx="12" cy="12" r="3" stroke={color} strokeWidth="1.6"/>
    <path d="M19.4 15a1.7 1.7 0 00.3 1.8l.1.1a2 2 0 11-2.8 2.8l-.1-.1a1.7 1.7 0 00-1.8-.3 1.7 1.7 0 00-1 1.5V21a2 2 0 11-4 0v-.1a1.7 1.7 0 00-1-1.5 1.7 1.7 0 00-1.8.3l-.1.1a2 2 0 11-2.8-2.8l.1-.1a1.7 1.7 0 00.3-1.8 1.7 1.7 0 00-1.5-1H3a2 2 0 110-4h.1a1.7 1.7 0 001.5-1 1.7 1.7 0 00-.3-1.8l-.1-.1a2 2 0 112.8-2.8l.1.1a1.7 1.7 0 001.8.3h0a1.7 1.7 0 001-1.5V3a2 2 0 114 0v.1a1.7 1.7 0 001 1.5 1.7 1.7 0 001.8-.3l.1-.1a2 2 0 112.8 2.8l-.1.1a1.7 1.7 0 00-.3 1.8v0a1.7 1.7 0 001.5 1H21a2 2 0 110 4h-.1a1.7 1.7 0 00-1.5 1z" stroke={color} strokeWidth="1.4"/>
  </svg>
);
const IconPlayFill = ({ size = 14, color = '#000' }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill={color}>
    <path d="M7 5v14l12-7z"/>
  </svg>
);
const IconPause = ({ size = 14, color = '#000' }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill={color}>
    <rect x="6" y="5" width="4" height="14" rx="1"/>
    <rect x="14" y="5" width="4" height="14" rx="1"/>
  </svg>
);
const IconHeart = ({ size = 18, color = TEXT_LO, filled = false }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill={filled ? color : 'none'}>
    <path d="M12 21s-7-4.5-9.5-9A5 5 0 0112 6.5 5 5 0 0121.5 12c-2.5 4.5-9.5 9-9.5 9z" stroke={color} strokeWidth="1.6" strokeLinejoin="round"/>
  </svg>
);
const IconMore = ({ size = 18, color = TEXT_LO }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill={color}>
    <circle cx="5" cy="12" r="2"/><circle cx="12" cy="12" r="2"/><circle cx="19" cy="12" r="2"/>
  </svg>
);
const IconSkipNext = ({ size = 18, color = TEXT_HI }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill={color}>
    <path d="M5 5v14l10-7zM16 5h2v14h-2z"/>
  </svg>
);
const IconHome = ({ size = 22, color = TEXT_HI, active }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill={active ? color : 'none'}>
    <path d="M3 11l9-7 9 7v9a1 1 0 01-1 1h-5v-7h-6v7H4a1 1 0 01-1-1z" stroke={color} strokeWidth="1.7" strokeLinejoin="round"/>
  </svg>
);
const IconSearch = ({ size = 22, color = TEXT_LO }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
    <circle cx="11" cy="11" r="7" stroke={color} strokeWidth="1.7"/>
    <path d="M16.5 16.5L21 21" stroke={color} strokeWidth="1.7" strokeLinecap="round"/>
  </svg>
);
const IconLibrary = ({ size = 22, color = TEXT_LO }) => (
  <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
    <path d="M4 4v16M9 4v16M14 5l5 14" stroke={color} strokeWidth="1.7" strokeLinecap="round"/>
  </svg>
);

// ── Data ─────────────────────────────────────────────────
const QUICK_PICKS = [
  { id: 'a', title: 'Echo, Vol. III', sub: 'Marina Vega', kind: 'type', palette: { bg: '#E8E2D5', fg: '#222' }, badge: 'NEW' },
  { id: 'b', title: 'Night Mode', sub: 'Tobi Akin', kind: 'dot', palette: { bg: '#0B3D2E', fg: ACCENT } },
  { id: 'c', title: 'Liked songs', sub: '247 brani', kind: 'duotone', palette: { a: '#3A0CA3', b: '#F72585' }, pinned: true },
  { id: 'd', title: 'Slow Hours', sub: 'Helena Vorr', kind: 'arc', palette: { a: '#FF6B5B', b: '#3A1F8A' } },
];

const RECOMMENDED = [
  { id: 1, title: 'Carbon Mirror', artist: 'Lou Hessler', dur: '3:42', kind: 'grid', palette: { a: '#0E1F3A', b: ACCENT } },
  { id: 2, title: 'Long way home', artist: 'Marina Vega', dur: '4:18', kind: 'moon', palette: { bg: '#E8DCC4', fg: '#1A1A1A' } },
  { id: 3, title: 'Pyre', artist: 'Tobi Akin · Sero', dur: '2:55', kind: 'triangles', palette: { bg: '#1A1A1A', fg: '#FF4D2E' } },
  { id: 4, title: 'Plein soleil', artist: 'Camille Roche', dur: '3:31', kind: 'wave', palette: { a: '#5C2D8C', b: '#F0A6B0' } },
  { id: 5, title: 'Glasshouse', artist: 'The Astral Kit', dur: '5:07', kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
  { id: 6, title: 'Undertow', artist: 'Helena Vorr', dur: '4:02', kind: 'blob', palette: { a: '#1E3A8A', b: '#06B6D4' }, playing: true },
];

// ── Greeting helper ─────────────────────────────────────
function greeting() {
  const h = new Date().getHours();
  if (h < 12) return 'Buongiorno';
  if (h < 18) return 'Buon pomeriggio';
  return 'Buonasera';
}

// ── Quick Pick card (2x2 grid) ──────────────────────────
function QuickPick({ item }) {
  const [hover, setHover] = React.useState(false);
  return (
    <div
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      style={{
        display: 'flex', alignItems: 'center', gap: 10,
        background: hover ? CARD_HOVER : CARD,
        borderRadius: 8, padding: 8, paddingRight: 12,
        position: 'relative', overflow: 'hidden',
        transition: 'background 0.15s',
        cursor: 'pointer',
      }}>
      <div style={{ width: 48, height: 48, flexShrink: 0 }}>
        <Cover kind={item.kind} palette={item.palette} radius={4}/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          color: TEXT_HI, fontSize: 13.5, fontWeight: 600,
          letterSpacing: -0.1,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>{item.title}</div>
        {item.badge && (
          <div style={{
            display: 'inline-block', marginTop: 3,
            fontFamily: 'JetBrains Mono, ui-monospace, monospace',
            fontSize: 9, fontWeight: 600, letterSpacing: 1,
            color: ACCENT,
          }}>{item.badge}</div>
        )}
      </div>
      {/* Play overlay */}
      <div style={{
        width: 32, height: 32, borderRadius: 999,
        background: ACCENT,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        flexShrink: 0,
        opacity: hover ? 1 : 0,
        transform: hover ? 'translateY(0)' : 'translateY(4px)',
        transition: 'all 0.18s',
        boxShadow: '0 6px 14px rgba(168,224,78,0.35)',
      }}>
        <IconPlayFill color="#0a0a0a"/>
      </div>
    </div>
  );
}

// ── Track row ───────────────────────────────────────────
function TrackRow({ track, idx }) {
  const [hover, setHover] = React.useState(false);
  const isPlaying = track.playing;
  const titleColor = isPlaying ? ACCENT : TEXT_HI;
  return (
    <div
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      style={{
        display: 'flex', alignItems: 'center', gap: 12,
        padding: '8px 16px',
        background: hover ? 'rgba(255,255,255,0.04)' : 'transparent',
        cursor: 'pointer', transition: 'background 0.12s',
        borderRadius: 6,
      }}>
      <div style={{
        width: 14, textAlign: 'center', flexShrink: 0,
        fontFamily: 'JetBrains Mono, ui-monospace, monospace',
        fontSize: 11, color: TEXT_LO,
      }}>{String(idx + 1).padStart(2, '0')}</div>
      <div style={{ width: 44, height: 44, flexShrink: 0 }}>
        <Cover kind={track.kind} palette={track.palette} radius={4}/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{
          fontSize: 14, fontWeight: 600, color: titleColor,
          letterSpacing: -0.1,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
          display: 'flex', alignItems: 'center', gap: 6,
        }}>
          {track.title}
          {isPlaying && <PlayingBars/>}
        </div>
        <div style={{
          fontSize: 12, color: TEXT_LO, marginTop: 2,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
        }}>{track.artist}</div>
      </div>
      <IconHeart filled={track.id === 2} color={track.id === 2 ? ACCENT : TEXT_LO}/>
      <div style={{
        fontFamily: 'JetBrains Mono, ui-monospace, monospace',
        fontSize: 11, color: TEXT_LO, width: 32, textAlign: 'right',
      }}>{track.dur}</div>
      <IconMore/>
    </div>
  );
}

function PlayingBars() {
  return (
    <div style={{ display: 'inline-flex', gap: 2, alignItems: 'flex-end', height: 12, marginLeft: 2 }}>
      {[0, 1, 2].map(i => (
        <div key={i} style={{
          width: 2, background: ACCENT, borderRadius: 1,
          animation: `pb${i} 0.9s ${i * 0.12}s ease-in-out infinite`,
        }}/>
      ))}
      <style>{`
        @keyframes pb0 { 0%,100%{height:4px} 50%{height:12px} }
        @keyframes pb1 { 0%,100%{height:10px} 50%{height:4px} }
        @keyframes pb2 { 0%,100%{height:6px} 50%{height:11px} }
      `}</style>
    </div>
  );
}

// ── Filter chips ────────────────────────────────────────
function Chip({ label, active, onClick }) {
  return (
    <button
      onClick={onClick}
      style={{
        border: 'none', cursor: 'pointer',
        padding: '6px 14px', borderRadius: 999,
        background: active ? ACCENT : 'rgba(255,255,255,0.08)',
        color: active ? '#0a0a0a' : TEXT_HI,
        fontSize: 12.5, fontWeight: 600, letterSpacing: -0.1,
        fontFamily: 'inherit',
        whiteSpace: 'nowrap',
      }}>{label}</button>
  );
}

// ── Player Bar ──────────────────────────────────────────
function PlayerBar() {
  const [playing, setPlaying] = React.useState(true);
  return (
    <div style={{
      margin: '0 8px',
      borderRadius: 10,
      background: 'linear-gradient(135deg, #1E3A8A 0%, #06B6D4 100%)',
      padding: 1,
      boxShadow: '0 8px 24px rgba(0,0,0,0.4)',
    }}>
      <div style={{
        background: '#161616',
        borderRadius: 9,
        padding: '8px 10px',
        display: 'flex', alignItems: 'center', gap: 10,
      }}>
        <div style={{ width: 40, height: 40, flexShrink: 0 }}>
          <Cover kind="blob" palette={{ a: '#1E3A8A', b: '#06B6D4' }} radius={4}/>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{
            fontSize: 13, fontWeight: 600, color: TEXT_HI,
            letterSpacing: -0.1, lineHeight: '16px',
            overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
          }}>Undertow</div>
          <div style={{
            fontSize: 11.5, color: TEXT_LO, lineHeight: '14px', marginTop: 1,
            overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
          }}>Helena Vorr · Slow Hours</div>
          {/* mini progress */}
          <div style={{
            marginTop: 5, height: 2, borderRadius: 1,
            background: 'rgba(255,255,255,0.15)',
            position: 'relative',
          }}>
            <div style={{
              position: 'absolute', inset: 0,
              width: '38%', background: ACCENT, borderRadius: 1,
            }}/>
          </div>
        </div>
        <button
          onClick={() => setPlaying(p => !p)}
          style={{
            width: 36, height: 36, borderRadius: 999,
            background: ACCENT, border: 'none', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            flexShrink: 0,
            boxShadow: '0 4px 12px rgba(168,224,78,0.35)',
          }}>
          {playing ? <IconPause/> : <IconPlayFill/>}
        </button>
        <button style={{
          background: 'transparent', border: 'none', cursor: 'pointer',
          padding: 4, display: 'flex', alignItems: 'center', flexShrink: 0,
        }}>
          <IconSkipNext/>
        </button>
      </div>
    </div>
  );
}

// ── Bottom nav ──────────────────────────────────────────
function BottomNav() {
  const item = (Icon, label, active) => (
    <div style={{
      flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3,
    }}>
      <Icon active={active} color={active ? TEXT_HI : TEXT_LO}/>
      <div style={{
        fontSize: 10.5, fontWeight: 600, letterSpacing: 0.1,
        color: active ? TEXT_HI : TEXT_LO,
      }}>{label}</div>
    </div>
  );
  return (
    <div style={{
      display: 'flex', padding: '8px 12px 4px',
      background: 'rgba(8,8,8,0.92)',
      backdropFilter: 'blur(20px)',
      WebkitBackdropFilter: 'blur(20px)',
    }}>
      {item(IconHome, 'Home', true)}
      {item(IconSearch, 'Cerca', false)}
      {item(IconLibrary, 'Libreria', false)}
    </div>
  );
}

// ── Logo ────────────────────────────────────────────────
function Logo() {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
      <div style={{
        width: 28, height: 28, borderRadius: 8,
        background: ACCENT,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
      }}>
        {/* Concentric rings = "hub" */}
        <svg width="16" height="16" viewBox="0 0 16 16">
          <circle cx="8" cy="8" r="6.5" fill="none" stroke="#0a0a0a" strokeWidth="1.5"/>
          <circle cx="8" cy="8" r="2.5" fill="#0a0a0a"/>
        </svg>
      </div>
      <div style={{
        fontWeight: 800, fontSize: 18, letterSpacing: -0.6, color: TEXT_HI,
      }}>MusicHub</div>
    </div>
  );
}

// ── Home Screen ─────────────────────────────────────────
function HomeScreen() {
  const [activeChip, setActiveChip] = React.useState('Tutto');
  return (
    <div style={{
      width: '100%', height: '100%',
      background: `linear-gradient(180deg, ${BG_TOP} 0%, ${BG_BOTTOM} 360px)`,
      color: TEXT_HI,
      fontFamily: 'Inter, -apple-system, system-ui, sans-serif',
      display: 'flex', flexDirection: 'column',
      overflow: 'hidden',
    }}>
      {/* Scrollable content */}
      <div style={{ flex: 1, overflowY: 'auto', overflowX: 'hidden', paddingTop: 56 }}>
        {/* Header */}
        <div style={{
          padding: '8px 16px 14px',
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
        }}>
          <Logo/>
          <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
            <IconBell/>
            <IconHistory/>
            <IconSettings/>
          </div>
        </div>

        {/* Greeting */}
        <div style={{ padding: '4px 16px 14px' }}>
          <div style={{
            fontSize: 26, fontWeight: 800, letterSpacing: -0.8, lineHeight: 1.1,
          }}>{greeting()}</div>
          <div style={{
            fontSize: 12.5, color: TEXT_LO, marginTop: 4,
            fontFamily: 'JetBrains Mono, ui-monospace, monospace',
            letterSpacing: 0.2,
          }}>Ven 1 Mag · 3 nuove uscite per te</div>
        </div>

        {/* Filter chips */}
        <div style={{
          display: 'flex', gap: 6, padding: '0 16px 16px',
          overflowX: 'auto', WebkitOverflowScrolling: 'touch',
        }}>
          {['Tutto', 'Musica', 'Playlist', 'Artisti'].map(c => (
            <Chip key={c} label={c} active={c === activeChip} onClick={() => setActiveChip(c)}/>
          ))}
        </div>

        {/* 2x2 quick picks */}
        <div style={{
          display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 8,
          padding: '0 16px',
        }}>
          {QUICK_PICKS.map(q => <QuickPick key={q.id} item={q}/>)}
        </div>

        {/* Recommended section */}
        <div style={{ padding: '28px 16px 8px', display: 'flex', alignItems: 'baseline', justifyContent: 'space-between' }}>
          <div>
            <div style={{
              fontFamily: 'JetBrains Mono, ui-monospace, monospace',
              fontSize: 10, fontWeight: 600, letterSpacing: 1.5,
              color: ACCENT, textTransform: 'uppercase', marginBottom: 4,
            }}>// Per te</div>
            <div style={{
              fontSize: 20, fontWeight: 800, letterSpacing: -0.4,
            }}>Brani consigliati</div>
          </div>
          <div style={{ fontSize: 12, color: TEXT_LO, fontWeight: 500 }}>Vedi tutti</div>
        </div>

        {/* Track list */}
        <div style={{ padding: '4px 0 24px' }}>
          {RECOMMENDED.map((t, i) => <TrackRow key={t.id} track={t} idx={i}/>)}
        </div>

        {/* Footer caption */}
        <div style={{
          textAlign: 'center', padding: '8px 16px 24px',
          fontFamily: 'JetBrains Mono, ui-monospace, monospace',
          fontSize: 10, color: 'rgba(255,255,255,0.25)', letterSpacing: 1,
        }}>— FINE FEED —</div>

        {/* Spacer for player bar + nav */}
        <div style={{ height: 120 }}/>
      </div>

      {/* Player bar + bottom nav (fixed) */}
      <div style={{
        position: 'absolute', left: 0, right: 0, bottom: 0,
        paddingBottom: 0,
      }}>
        <div style={{ paddingBottom: 6 }}>
          <PlayerBar/>
        </div>
        <BottomNav/>
        <div style={{ height: 28 }}/>{/* home indicator gutter */}
      </div>
    </div>
  );
}

// ── Mount ───────────────────────────────────────────────
function App() {
  return (
    <div data-screen-label="01 Home" style={{
      minHeight: '100vh',
      background: 'radial-gradient(ellipse at top, #1a1a1a 0%, #050505 60%)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      padding: '40px 20px',
      fontFamily: 'Inter, -apple-system, system-ui, sans-serif',
    }}>
      <IOSDevice width={390} height={844} dark={true}>
        <HomeScreen/>
      </IOSDevice>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById('root')).render(<App/>);
