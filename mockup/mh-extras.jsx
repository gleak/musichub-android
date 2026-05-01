// MusicHub — Mobile extras (Lyrics, Video) + Android Auto screens
const { T, I, MHCover, MHPlayingBars, MHLogo, MHPlayerBar, MHBottomNav, MHScreen } = window.MH;

// ─────────────────────────────────────────────────────────
// 09 · LYRICS (full screen)
// ─────────────────────────────────────────────────────────
function LyricsScreen() {
  const LINES = [
    { t: '0:48', text: 'Slow water through the broken glass', past: true },
    { t: '0:54', text: 'Soft hum from a passing car', past: true },
    { t: '1:02', text: 'I keep your name in lower case', past: true },
    { t: '1:12', text: 'Quiet rooms remember everything', past: true },
    { t: '1:24', text: 'Undertow, undertow', active: true },
    { t: '1:36', text: 'Drag me to the morning light' },
    { t: '1:46', text: 'Hold the silence like a book' },
    { t: '1:58', text: 'Counting hours we never spent' },
    { t: '2:08', text: 'Soft hum from a passing car' },
    { t: '2:18', text: 'I keep your name in lower case' },
  ];
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
          <div style={{ fontSize: 11, color: T.TEXT_LO, fontFamily: T.MONO, letterSpacing: 1 }}>TESTO</div>
          <div style={{ fontSize: 13, fontWeight: 600, marginTop: 2 }}>Undertow</div>
        </div>
        <I.More color={T.TEXT_HI}/>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '12px 16px 4px' }}>
        <div style={{ width: 44, height: 44 }}>
          <MHCover kind="blob" palette={{ a: '#1E3A8A', b: '#06B6D4' }} radius={4}/>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 14, fontWeight: 700 }}>Undertow</div>
          <div style={{ fontSize: 12, color: T.TEXT_LO }}>Helena Vorr · Slow Hours</div>
        </div>
        <div style={{
          background: 'rgba(168,224,78,0.15)', color: T.ACCENT,
          fontSize: 10, fontFamily: T.MONO, fontWeight: 700, letterSpacing: 1,
          padding: '4px 8px', borderRadius: 6,
        }}>SINCRONIZZATO</div>
      </div>

      <div style={{ flex: 1, overflowY: 'auto', padding: '24px 22px 20px' }}>
        {LINES.map((l, i) => (
          <div key={i} style={{
            fontSize: l.active ? 26 : 22, fontWeight: l.active ? 800 : 600,
            letterSpacing: -0.4, lineHeight: 1.3, marginBottom: 18,
            color: l.active ? T.TEXT_HI : (l.past ? 'rgba(255,255,255,0.35)' : 'rgba(255,255,255,0.5)'),
            transition: 'color 0.3s',
          }}>{l.text}</div>
        ))}
      </div>

      <div style={{ padding: '12px 16px 28px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <I.Share size={20} color={T.TEXT_LO}/>
        <button style={{
          width: 56, height: 56, borderRadius: 999, background: T.ACCENT, border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 8px 22px rgba(168,224,78,0.4)',
        }}><I.Pause size={18}/></button>
        <div style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO }}>1:32 / 4:02</div>
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────
// 10 · VIDEO PLAYER
// ─────────────────────────────────────────────────────────
function VideoScreen() {
  const RELATED = [
    { title: 'Long way home — Live at Blue Note', artist: 'Marina Vega', dur: '5:12', kind: 'moon', palette: { bg: '#E8DCC4', fg: '#1A1A1A' } },
    { title: 'Pyre (official video)', artist: 'Tobi Akin', dur: '3:08', kind: 'triangles', palette: { bg: '#1A1A1A', fg: '#FF4D2E' } },
    { title: 'Carbon Mirror (visualizer)', artist: 'Lou Hessler', dur: '3:42', kind: 'grid', palette: { a: '#0E1F3A', b: T.ACCENT } },
    { title: 'Glasshouse — studio session', artist: 'The Astral Kit', dur: '6:20', kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
  ];
  return (
    <div style={{
      width: '100%', height: '100%', background: '#000', color: T.TEXT_HI, fontFamily: T.FONT,
      display: 'flex', flexDirection: 'column', overflow: 'hidden', paddingTop: 56,
    }}>
      {/* Hero video */}
      <div style={{ position: 'relative', width: '100%', aspectRatio: '16/9', background: '#0a0a0a' }}>
        <div style={{ position: 'absolute', inset: 0 }}>
          <MHCover kind="blob" palette={{ a: '#1E3A8A', b: '#06B6D4' }} radius={0}/>
        </div>
        {/* gradient overlay */}
        <div style={{ position: 'absolute', inset: 0,
          background: 'linear-gradient(180deg, rgba(0,0,0,0.5) 0%, transparent 25%, transparent 60%, rgba(0,0,0,0.85) 100%)' }}/>
        {/* top bar */}
        <div style={{ position: 'absolute', top: 8, left: 0, right: 0,
          padding: '0 14px', display: 'flex', justifyContent: 'space-between', alignItems: 'center', zIndex: 2 }}>
          <I.Down color="#fff"/>
          <div style={{
            background: 'rgba(0,0,0,0.5)', backdropFilter: 'blur(10px)',
            padding: '4px 10px', borderRadius: 999, fontSize: 11, fontFamily: T.MONO,
            color: '#fff', letterSpacing: 0.6, display: 'flex', alignItems: 'center', gap: 6,
          }}>
            <div style={{ width: 6, height: 6, borderRadius: 999, background: '#FF4D2E' }}/>
            VIDEO 4K
          </div>
          <I.Cast color="#fff"/>
        </div>
        {/* center play */}
        <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 2 }}>
          <div style={{
            width: 64, height: 64, borderRadius: 999, background: 'rgba(255,255,255,0.18)',
            backdropFilter: 'blur(20px)', border: '1.5px solid rgba(255,255,255,0.4)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <I.Pause size={22} color="#fff"/>
          </div>
        </div>
        {/* bottom progress */}
        <div style={{ position: 'absolute', left: 12, right: 12, bottom: 10, zIndex: 2 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
            <div style={{ fontFamily: T.MONO, fontSize: 11, color: '#fff' }}>1:32</div>
            <div style={{ flex: 1, height: 3, borderRadius: 2, background: 'rgba(255,255,255,0.25)', position: 'relative' }}>
              <div style={{ position: 'absolute', inset: 0, width: '38%', background: T.ACCENT, borderRadius: 2 }}/>
              <div style={{ position: 'absolute', left: '38%', top: '50%', transform: 'translate(-50%,-50%)',
                width: 11, height: 11, borderRadius: 999, background: T.ACCENT }}/>
            </div>
            <div style={{ fontFamily: T.MONO, fontSize: 11, color: '#fff' }}>4:02</div>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M3 3h6M3 3v6M21 3h-6M21 3v6M3 21h6M3 21v-6M21 21h-6M21 21v-6" stroke="#fff" strokeWidth="2" strokeLinecap="round"/>
            </svg>
          </div>
        </div>
      </div>

      {/* Title block */}
      <div style={{ padding: '14px 16px 8px' }}>
        <div style={{ fontSize: 18, fontWeight: 800, letterSpacing: -0.4, lineHeight: 1.2 }}>Undertow — official video</div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 6,
          fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO }}>
          <span>2.4M visualizzazioni</span><span>·</span><span>3 mesi fa</span>
        </div>
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '8px 16px 12px',
        borderBottom: `1px solid ${T.DIVIDER}` }}>
        <div style={{ width: 32, height: 32, borderRadius: 999, overflow: 'hidden' }}>
          <MHCover kind="artist" palette={{ bg: '#1E3A8A', fg: '#E8DCC4' }} radius={999}/>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 13, fontWeight: 600, display: 'flex', alignItems: 'center', gap: 4 }}>
            Helena Vorr <I.Verified size={12}/>
          </div>
          <div style={{ fontSize: 11, color: T.TEXT_LO, fontFamily: T.MONO }}>184K follower</div>
        </div>
        <button style={{
          background: T.ACCENT, color: '#0a0a0a', border: 'none', borderRadius: 999,
          padding: '6px 14px', fontSize: 12, fontWeight: 700, cursor: 'pointer',
        }}>Segui</button>
      </div>

      {/* Action chips */}
      <div style={{ display: 'flex', gap: 8, padding: '10px 16px', overflowX: 'auto' }}>
        {[
          { i: <I.Heart size={14} color={T.ACCENT} filled/>, l: '24K' },
          { i: <I.Share size={14}/>, l: 'Condividi' },
          { i: <I.Download size={14}/>, l: 'Salva' },
          { i: <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="M4 12l4-4 4 4 4-8 4 12" stroke={T.TEXT_LO} strokeWidth="1.7" strokeLinejoin="round"/></svg>, l: 'Audio only' },
        ].map((c, i) => (
          <div key={i} style={{
            display: 'flex', alignItems: 'center', gap: 6, padding: '7px 12px',
            background: 'rgba(255,255,255,0.08)', borderRadius: 999, whiteSpace: 'nowrap',
            fontSize: 12, fontWeight: 600,
          }}>{c.i}<span>{c.l}</span></div>
        ))}
      </div>

      {/* Related videos */}
      <div style={{ padding: '12px 16px 6px' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, marginBottom: 6 }}>// VIDEO CORRELATI</div>
      </div>
      <div style={{ overflowY: 'auto', flex: 1, paddingBottom: 16 }}>
        {RELATED.map((v, i) => (
          <div key={i} style={{ display: 'flex', gap: 10, padding: '8px 16px' }}>
            <div style={{ position: 'relative', width: 110, aspectRatio: '16/9', flexShrink: 0 }}>
              <MHCover kind={v.kind} palette={v.palette} radius={6}/>
              <div style={{ position: 'absolute', right: 4, bottom: 4,
                background: 'rgba(0,0,0,0.75)', color: '#fff', fontSize: 10,
                fontFamily: T.MONO, padding: '2px 5px', borderRadius: 3 }}>{v.dur}</div>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 600, lineHeight: 1.3 }}>{v.title}</div>
              <div style={{ fontSize: 11, color: T.TEXT_LO, marginTop: 4, fontFamily: T.MONO }}>{v.artist}</div>
            </div>
            <I.More/>
          </div>
        ))}
      </div>
    </div>
  );
}

window.MHExtraScreens = { LyricsScreen, VideoScreen };

// ─────────────────────────────────────────────────────────
// ANDROID AUTO — landscape device frame (1280×720)
// ─────────────────────────────────────────────────────────
function AAFrame({ children, label }) {
  return (
    <div style={{
      width: 1280, height: 720, position: 'relative',
      background: '#000', borderRadius: 14, overflow: 'hidden',
      boxShadow: '0 20px 60px rgba(0,0,0,0.4)',
      fontFamily: T.FONT, color: T.TEXT_HI,
    }}>
      {/* Status bar (Android Auto top) */}
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
          <span>BT</span>
          <span>4G</span>
          <span>92%</span>
        </div>
      </div>

      {/* Left rail (Android Auto launcher) */}
      <div style={{
        position: 'absolute', top: 36, bottom: 0, left: 0, width: 72, zIndex: 5,
        background: 'rgba(8,8,10,0.95)', backdropFilter: 'blur(8px)',
        display: 'flex', flexDirection: 'column', alignItems: 'center',
        padding: '14px 0', gap: 8,
      }}>
        {/* App switcher */}
        <div style={{
          width: 44, height: 44, borderRadius: 12, background: 'rgba(255,255,255,0.08)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
            <path d="M3 6h18M3 12h18M3 18h18" stroke={T.TEXT_HI} strokeWidth="2" strokeLinecap="round"/>
          </svg>
        </div>
        <div style={{ flex: 1 }}/>
        {/* Nav app */}
        {[
          { icon: <svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M12 2l9 5v10l-9 5-9-5V7l9-5z" stroke={T.TEXT_LO} strokeWidth="1.7"/><path d="M9 9h6v6H9z" fill={T.TEXT_LO}/></svg>, l: 'Maps' },
          { icon: <svg width="22" height="22" viewBox="0 0 24 24" fill="none"><circle cx="8" cy="18" r="3" stroke={T.ACCENT} strokeWidth="2"/><circle cx="18" cy="16" r="3" stroke={T.ACCENT} strokeWidth="2"/><path d="M11 18V6l10-2v12" stroke={T.ACCENT} strokeWidth="2" strokeLinecap="round"/></svg>, l: 'MusicHub', active: true },
          { icon: <svg width="22" height="22" viewBox="0 0 24 24" fill="none"><path d="M3 5h18v14H3z" stroke={T.TEXT_LO} strokeWidth="1.7"/><path d="M3 9h18M7 5v14" stroke={T.TEXT_LO} strokeWidth="1.7"/></svg>, l: 'Phone' },
        ].map((a, i) => (
          <div key={i} style={{
            width: 56, height: 56, borderRadius: 14,
            background: a.active ? 'rgba(168,224,78,0.15)' : 'transparent',
            border: a.active ? `1.5px solid ${T.ACCENT}` : '1.5px solid transparent',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>{a.icon}</div>
        ))}
        <div style={{ flex: 1 }}/>
        {/* Home */}
        <div style={{
          width: 44, height: 44, borderRadius: 999, background: 'rgba(255,255,255,0.1)',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill={T.TEXT_HI}>
            <circle cx="12" cy="12" r="9" fill="none" stroke={T.TEXT_HI} strokeWidth="1.7"/>
            <circle cx="12" cy="12" r="3" fill={T.TEXT_HI}/>
          </svg>
        </div>
      </div>

      <div style={{ position: 'absolute', top: 36, left: 72, right: 0, bottom: 0 }}>
        {children}
      </div>

      {/* Label tag */}
      {label && (
        <div style={{
          position: 'absolute', top: 8, left: 86, zIndex: 6,
          fontFamily: T.MONO, fontSize: 10, fontWeight: 700, letterSpacing: 1,
          color: T.ACCENT, opacity: 0.7,
        }}>{label}</div>
      )}
    </div>
  );
}

// 11 · ANDROID AUTO — Now Playing
function AANowPlaying() {
  return (
    <div style={{
      width: '100%', height: '100%', display: 'flex',
      background: 'linear-gradient(135deg, #1E3A8A 0%, #0E1F3A 50%, #060309 100%)',
      color: T.TEXT_HI, fontFamily: T.FONT,
    }}>
      {/* Album art panel */}
      <div style={{ width: 380, padding: 36, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ width: 308, height: 308, boxShadow: '0 25px 60px rgba(0,0,0,0.6)' }}>
          <MHCover kind="blob" palette={{ a: '#1E3A8A', b: '#06B6D4' }} radius={10}/>
        </div>
      </div>

      {/* Info + controls */}
      <div style={{ flex: 1, padding: '36px 36px 28px 0', display: 'flex', flexDirection: 'column' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 12, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5, marginBottom: 8 }}>// IN RIPRODUZIONE</div>
        <div style={{ fontSize: 44, fontWeight: 900, letterSpacing: -1, lineHeight: 1.05 }}>Undertow</div>
        <div style={{ fontSize: 22, color: T.TEXT_LO, marginTop: 8, fontWeight: 500 }}>Helena Vorr · Slow Hours</div>

        {/* Progress */}
        <div style={{ marginTop: 30 }}>
          <div style={{ height: 5, borderRadius: 3, background: 'rgba(255,255,255,0.15)', position: 'relative' }}>
            <div style={{ position: 'absolute', inset: 0, width: '38%', background: T.ACCENT, borderRadius: 3 }}/>
            <div style={{ position: 'absolute', left: '38%', top: '50%', transform: 'translate(-50%,-50%)',
              width: 18, height: 18, borderRadius: 999, background: T.ACCENT, boxShadow: '0 4px 10px rgba(0,0,0,0.5)' }}/>
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 10,
            fontFamily: T.MONO, fontSize: 16, color: T.TEXT_LO }}>
            <span>1:32</span><span>4:02</span>
          </div>
        </div>

        {/* Controls — large for car use */}
        <div style={{ flex: 1 }}/>
        <div style={{ display: 'flex', alignItems: 'center', gap: 24 }}>
          <button style={{
            width: 72, height: 72, borderRadius: 999, background: 'rgba(255,255,255,0.08)',
            border: '1.5px solid rgba(255,255,255,0.12)', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}><I.Shuffle size={28} color={T.ACCENT}/></button>
          <button style={{
            width: 72, height: 72, borderRadius: 999, background: 'rgba(255,255,255,0.08)',
            border: '1.5px solid rgba(255,255,255,0.12)', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}><I.SkipPrev size={32} color={T.TEXT_HI}/></button>
          <button style={{
            width: 96, height: 96, borderRadius: 999, background: T.ACCENT, border: 'none', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: '0 12px 32px rgba(168,224,78,0.5)',
          }}><I.Pause size={32}/></button>
          <button style={{
            width: 72, height: 72, borderRadius: 999, background: 'rgba(255,255,255,0.08)',
            border: '1.5px solid rgba(255,255,255,0.12)', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}><I.SkipNext size={32} color={T.TEXT_HI}/></button>
          <button style={{
            width: 72, height: 72, borderRadius: 999, background: 'rgba(255,255,255,0.08)',
            border: '1.5px solid rgba(255,255,255,0.12)', cursor: 'pointer',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}><I.Heart size={28} color={T.ACCENT} filled/></button>
        </div>
      </div>
    </div>
  );
}

// 12 · ANDROID AUTO — Browse / Library
function AABrowse() {
  const TILES = [
    { title: 'Slow Hours', sub: 'Marina Vega', kind: 'arc', palette: { a: '#FF6B5B', b: '#3A1F8A' } },
    { title: 'Liked songs', sub: 'Playlist', kind: 'duotone', palette: { a: '#3A0CA3', b: '#F72585' } },
    { title: 'Late drives', sub: 'Playlist', kind: 'wave', palette: { a: '#5C2D8C', b: '#F0A6B0' } },
    { title: 'Echo, Vol. III', sub: 'Album', kind: 'type', palette: { bg: '#E8E2D5', fg: '#222' } },
    { title: 'Mattina', sub: 'Playlist', kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
    { title: 'Carbon Mirror', sub: 'Album', kind: 'grid', palette: { a: '#0E1F3A', b: T.ACCENT } },
  ];
  return (
    <div style={{
      width: '100%', height: '100%', padding: 28,
      background: 'linear-gradient(180deg, #1F1F1F 0%, #080808 100%)',
      color: T.TEXT_HI, fontFamily: T.FONT, display: 'flex', flexDirection: 'column',
    }}>
      {/* Tabs */}
      <div style={{ display: 'flex', gap: 8, marginBottom: 22 }}>
        {[
          { l: 'Casa', active: true },
          { l: 'Libreria' },
          { l: 'Recenti' },
          { l: 'Cerca' },
        ].map((t, i) => (
          <div key={i} style={{
            padding: '10px 20px', borderRadius: 999,
            background: t.active ? T.ACCENT : 'rgba(255,255,255,0.08)',
            color: t.active ? '#0a0a0a' : T.TEXT_HI,
            fontSize: 16, fontWeight: 700, letterSpacing: -0.2,
          }}>{t.l}</div>
        ))}
      </div>

      {/* Now playing strip */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: 16, padding: 14,
        background: 'rgba(168,224,78,0.08)', border: `1.5px solid rgba(168,224,78,0.25)`,
        borderRadius: 14, marginBottom: 22,
      }}>
        <div style={{ width: 72, height: 72 }}>
          <MHCover kind="blob" palette={{ a: '#1E3A8A', b: '#06B6D4' }} radius={8}/>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 11, fontFamily: T.MONO, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5 }}>// IN RIPRODUZIONE</div>
          <div style={{ fontSize: 22, fontWeight: 800, marginTop: 4, letterSpacing: -0.4 }}>Undertow</div>
          <div style={{ fontSize: 14, color: T.TEXT_LO, marginTop: 2 }}>Helena Vorr</div>
        </div>
        <MHPlayingBars/>
        <button style={{
          width: 56, height: 56, borderRadius: 999, background: T.ACCENT, border: 'none', cursor: 'pointer',
          display: 'flex', alignItems: 'center', justifyContent: 'center',
        }}><I.Pause size={20}/></button>
      </div>

      {/* Grid */}
      <div style={{ fontFamily: T.MONO, fontSize: 12, fontWeight: 700, letterSpacing: 1.5,
        color: T.ACCENT, marginBottom: 12 }}>// PER TE</div>
      <div style={{ flex: 1, display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 14 }}>
        {TILES.map((t, i) => (
          <div key={i} style={{
            display: 'flex', alignItems: 'center', gap: 14, padding: 12,
            background: T.CARD, borderRadius: 12,
          }}>
            <div style={{ width: 80, height: 80, flexShrink: 0 }}>
              <MHCover kind={t.kind} palette={t.palette} radius={8}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 17, fontWeight: 700, letterSpacing: -0.2,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.title}</div>
              <div style={{ fontSize: 13, color: T.TEXT_LO, marginTop: 4 }}>{t.sub}</div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

window.MHAutoScreens = { AAFrame, AANowPlaying, AABrowse };
