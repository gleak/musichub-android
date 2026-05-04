// MusicHub — Android Auto increments (v0.13.1+)
const { T, I, MHCover, MHPlayingBars } = window.MH;

const AABG = 'linear-gradient(180deg, #1F1F1F 0%, #080808 100%)';
const AAPad = { width: '100%', height: '100%', padding: 24, background: AABG, color: T.TEXT_HI, fontFamily: T.FONT, display: 'flex', flexDirection: 'column' };

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

// ── 9.1 AA Genres tile ──────────────────────────────────
function AAGenres() {
  const GENRES = [
    { name: 'Indie', color: '#3A0CA3', kind: 'duotone', p: { a:'#3A0CA3', b:'#F72585' } },
    { name: 'Elettronica', color: '#06B6D4', kind: 'blob', p: { a:'#1E3A8A', b:'#06B6D4' } },
    { name: 'Hip-hop', color: '#FF4D2E', kind: 'triangles', p: { bg:'#1A1A1A', fg:'#FF4D2E' } },
    { name: 'Jazz', color: '#FFC857', kind: 'stripes', p: { a:'#FFC857', b:'#1A1A1A' } },
    { name: 'Classica', color: '#5C2D8C', kind: 'wave', p: { a:'#5C2D8C', b:'#F0A6B0' } },
    { name: 'Ambient', color: '#0B3D2E', kind: 'dot', p: { bg:'#0B3D2E', fg: T.ACCENT } },
    { name: 'Rock', color: '#7C2D12', kind: 'arc', p: { a:'#FF6B5B', b:'#3A1F8A' } },
    { name: 'Pop', color: '#F0A6B0', kind: 'grid', p: { a:'#0E1F3A', b: T.ACCENT } },
  ];
  return (
    <div style={AAPad}>
      <AATabs active="Cerca"/>
      <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 18 }}>
        <button style={{ width: 52, height: 52, borderRadius: 999, background: 'rgba(255,255,255,0.08)', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.Back size={22}/></button>
        <div style={{ fontFamily: T.MONO, fontSize: 12, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5 }}>// SFOGLIA · TUTTI I GENERI</div>
      </div>
      <div style={{ flex: 1, display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gridTemplateRows: 'repeat(2, 1fr)', gap: 14 }}>
        {GENRES.map(g => (
          <div key={g.name} style={{ position: 'relative', borderRadius: 16, overflow: 'hidden', border: '1.5px solid rgba(255,255,255,0.06)' }}>
            <div style={{ position: 'absolute', inset: 0, opacity: 0.85 }}>
              <MHCover kind={g.kind} palette={g.p} radius={0}/>
            </div>
            <div style={{ position: 'absolute', inset: 0, background: `linear-gradient(180deg, rgba(0,0,0,0.1) 0%, rgba(0,0,0,0.55) 100%)` }}/>
            <div style={{ position: 'absolute', left: 18, right: 18, bottom: 16, fontSize: 22, fontWeight: 800, letterSpacing: -0.4, lineHeight: 1.1 }}>{g.name}</div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ── 9.2 AA Now Playing with lyric ticker description ────
function AANowPlayingTicker() {
  return (
    <div style={{ width: '100%', height: '100%', display: 'flex', background: AABG, color: T.TEXT_HI, fontFamily: T.FONT }}>
      {/* Cover panel */}
      <div style={{ width: '46%', padding: 32, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ width: 380, height: 380, boxShadow: '0 24px 60px rgba(0,0,0,0.6)' }}>
          <MHCover kind="blob" palette={{ a:'#1E3A8A', b:'#06B6D4' }} radius={16}/>
        </div>
      </div>

      {/* Right panel */}
      <div style={{ flex: 1, padding: '40px 40px 24px 0', display: 'flex', flexDirection: 'column' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5, marginBottom: 12 }}>// IN RIPRODUZIONE</div>
        <div style={{ fontSize: 44, fontWeight: 800, letterSpacing: -1, lineHeight: 1.05 }}>Undertow</div>
        <div style={{ fontSize: 22, color: T.TEXT_LO, marginTop: 6 }}>Helena Vorr · Slow Hours</div>

        {/* Lyric ticker */}
        <div style={{ marginTop: 28, padding: '14px 18px', background: 'rgba(168,224,78,0.08)', border: '1.5px solid rgba(168,224,78,0.25)', borderRadius: 14, position: 'relative', overflow: 'hidden' }}>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5, marginBottom: 6 }}>// ORA</div>
          <div style={{ fontSize: 24, fontWeight: 700, letterSpacing: -0.3, lineHeight: 1.2, color: T.TEXT_HI, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>
            And the tide pulled me under, slow
          </div>
        </div>

        {/* Progress */}
        <div style={{ marginTop: 32, display: 'flex', alignItems: 'center', gap: 14, fontFamily: T.MONO, fontSize: 14, color: T.TEXT_LO }}>
          <span>1:38</span>
          <div style={{ flex: 1, height: 4, background: 'rgba(255,255,255,0.12)', borderRadius: 2, position: 'relative' }}>
            <div style={{ width: '38%', height: '100%', background: T.ACCENT, borderRadius: 2 }}/>
            <div style={{ position: 'absolute', left: 'calc(38% - 8px)', top: -6, width: 16, height: 16, borderRadius: 8, background: T.ACCENT }}/>
          </div>
          <span style={{ color: T.TEXT_HI }}>4:12</span>
        </div>

        {/* Controls */}
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 24, marginTop: 16 }}>
          <CtrlBtn><I.Shuffle size={28}/></CtrlBtn>
          <CtrlBtn><I.SkipPrev size={32}/></CtrlBtn>
          <button style={{ width: 96, height: 96, borderRadius: 999, background: T.ACCENT, border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 16px 36px rgba(168,224,78,0.4)' }}>
            <I.Pause size={36}/>
          </button>
          <CtrlBtn><I.SkipNext size={32}/></CtrlBtn>
          <CtrlBtn><I.Repeat size={28}/></CtrlBtn>
        </div>

        {/* Custom layout commands strip */}
        <div style={{ display: 'flex', gap: 12, paddingTop: 8 }}>
          <CmdChip ic="heart" label="Mi piace" active/>
          <CmdChip ic="sleep" label="Timer · 27 min" active/>
          <CmdChip ic="queue" label="Coda"/>
          <div style={{ flex: 1 }}/>
          <CmdChip ic="cast" label="BMW Audio"/>
        </div>
      </div>
    </div>
  );
}

function CtrlBtn({ children }) {
  return <button style={{ width: 64, height: 64, borderRadius: 999, background: 'rgba(255,255,255,0.06)', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>{children}</button>;
}

function CmdChip({ ic, label, active, disabled }) {
  const c = active ? T.ACCENT : (disabled ? T.TEXT_LO2 : T.TEXT_HI);
  const props = { width: 22, height: 22, viewBox: '0 0 24 24', fill: 'none', stroke: c, strokeWidth: 1.8, strokeLinecap: 'round', strokeLinejoin: 'round' };
  let inner;
  switch (ic) {
    case 'heart': inner = <path d="M12 21s-7-4.5-9.5-9A5 5 0 0112 6.5 5 5 0 0121.5 12c-2.5 4.5-9.5 9-9.5 9z" fill={c}/>; break;
    case 'sleep': inner = <path d="M12 3a9 9 0 109 9 7 7 0 01-9-9z"/>; break;
    case 'queue': inner = <path d="M3 6h12M3 12h12M3 18h8M17 14v6M14 17h6"/>; break;
    case 'cast': inner = <><path d="M3 8V6a2 2 0 012-2h14a2 2 0 012 2v12a2 2 0 01-2 2h-6"/><path d="M3 13a7 7 0 017 7M3 17a3 3 0 013 3M3 20.5h.01"/></>; break;
  }
  return (
    <div style={{
      padding: '12px 18px', borderRadius: 999, display: 'flex', alignItems: 'center', gap: 10,
      background: active ? 'rgba(168,224,78,0.12)' : 'rgba(255,255,255,0.06)',
      border: active ? '1.5px solid rgba(168,224,78,0.4)' : '1.5px solid transparent',
      opacity: disabled ? 0.5 : 1,
    }}>
      <svg {...props}>{inner}</svg>
      <div style={{ fontSize: 15, fontWeight: 700, color: c, letterSpacing: -0.2 }}>{label}</div>
    </div>
  );
}

// ── 9.3 AA Custom commands — driving safe sleep timer ───
function AASleepDriving() {
  const opts = [5, 10, 15, 30, 45, 60];
  return (
    <div style={{ width: '100%', height: '100%', background: AABG, color: T.TEXT_HI, fontFamily: T.FONT, padding: 28, display: 'flex', flexDirection: 'column' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 14, marginBottom: 8 }}>
        <button style={{ width: 52, height: 52, borderRadius: 999, background: 'rgba(255,255,255,0.08)', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.X size={22}/></button>
        <div style={{ fontFamily: T.MONO, fontSize: 12, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5 }}>// TIMER · DRIVER-SAFE</div>
      </div>

      <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 32 }}>
        {/* Active state column */}
        <div style={{ flex: 1, padding: '36px 32px', borderRadius: 18, background: `linear-gradient(135deg, rgba(168,224,78,0.14) 0%, rgba(168,224,78,0.04) 100%)`, border: '1.5px solid rgba(168,224,78,0.25)' }}>
          <div style={{ fontFamily: T.MONO, fontSize: 12, fontWeight: 700, color: T.ACCENT, letterSpacing: 1.5, marginBottom: 14 }}>// ATTIVO</div>
          <div style={{ fontSize: 96, fontWeight: 800, letterSpacing: -3, fontFamily: T.MONO, color: T.ACCENT, lineHeight: 1 }}>27:14</div>
          <div style={{ fontSize: 18, color: T.TEXT_LO, marginTop: 14 }}>Si fermerà tra 27 minuti</div>
          <button style={{ marginTop: 28, padding: '16px 28px', background: 'rgba(255,255,255,0.08)', border: 'none', borderRadius: 999, color: T.TEXT_HI, fontWeight: 700, fontSize: 17, fontFamily: T.FONT, cursor: 'pointer' }}>Annulla</button>
        </div>

        {/* Numeric chip column — large hit targets */}
        <div style={{ flex: 1.1 }}>
          <div style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700, color: T.TEXT_LO, letterSpacing: 1.5, marginBottom: 14 }}>// CAMBIA · TAP RAPIDO</div>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 12, marginBottom: 12 }}>
            {opts.map(o => (
              <div key={o} style={{
                padding: '24px 0', borderRadius: 14, background: 'rgba(255,255,255,0.06)', textAlign: 'center',
                border: '1.5px solid transparent',
              }}>
                <div style={{ fontSize: 36, fontWeight: 800, fontFamily: T.MONO, color: T.TEXT_HI, lineHeight: 1 }}>{o}</div>
                <div style={{ fontSize: 12, color: T.TEXT_LO, fontFamily: T.MONO, marginTop: 6, letterSpacing: 1 }}>MIN</div>
              </div>
            ))}
          </div>
          <div style={{
            padding: '20px 0', borderRadius: 14, background: 'rgba(168,224,78,0.06)',
            border: '1.5px solid rgba(168,224,78,0.3)', textAlign: 'center',
            color: T.ACCENT, fontSize: 19, fontWeight: 700, fontFamily: T.FONT,
          }}>Fine traccia</div>
          <div style={{ marginTop: 18, padding: '12px 16px', background: 'rgba(255,200,87,0.08)', border: '1px solid rgba(255,200,87,0.25)', borderRadius: 10, display: 'flex', alignItems: 'center', gap: 10 }}>
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M12 8v5M12 17h.01M12 3l10 18H2L12 3z" stroke="#FFC857" strokeWidth="1.7"/></svg>
            <div style={{ fontSize: 13, color: '#FFE0A0', fontFamily: T.MONO }}>Input numerico libero disabilitato in marcia · solo preset</div>
          </div>
        </div>
      </div>
    </div>
  );
}

window.MHAutoExtra = { AAGenres, AANowPlayingTicker, AASleepDriving };
