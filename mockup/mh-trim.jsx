// MusicHub — Modalità "Taglia traccia"
// Doppia timeline: preview (ascolto libero) + trim (in/out)
const { T, I, MHCover } = window.MH;

// ── Pseudo-deterministic waveform (32 bars) ─────────────
function genWaveform(seed = 7, bars = 96) {
  // Mulberry32-ish — not random, but reads as "real" audio
  const out = [];
  let a = seed;
  for (let i = 0; i < bars; i++) {
    a = (a * 1664525 + 1013904223) >>> 0;
    const t = i / bars;
    // Envelope: build, plateau, soft fade
    const env = Math.min(1, t * 4) * (1 - Math.max(0, (t - 0.85)) * 6);
    const noise = ((a >>> 8) & 0xffff) / 65535;
    const sin = 0.4 + 0.55 * Math.abs(Math.sin(t * Math.PI * 7));
    out.push(Math.max(0.05, env * (0.55 * sin + 0.45 * noise)));
  }
  return out;
}

// ── Bars renderer ───────────────────────────────────────
function Bars({ data, color, dim, height = 56, gap = 1, mask }) {
  // mask: { from: 0..1, to: 0..1 } — bars outside fade
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap, height, width: '100%' }}>
      {data.map((v, i) => {
        const t = i / data.length;
        const inMask = !mask || (t >= mask.from && t <= mask.to);
        return (
          <div key={i} style={{
            flex: 1,
            height: `${Math.round(v * 100)}%`,
            minHeight: 2,
            background: inMask ? color : dim,
            borderRadius: 1,
          }}/>
        );
      })}
    </div>
  );
}

function fmt(t) {
  // 145.4 → 02:25.4
  const m = Math.floor(t / 60);
  const s = (t % 60);
  return `${String(m).padStart(2, '0')}:${s.toFixed(1).padStart(4, '0')}`;
}

// ── Main screen ─────────────────────────────────────────
function TrimTrackScreen({ state = 'editing' }) {
  // state: editing | playingPreview | savedToast
  const total = 248.6; // 4:08.6
  const inT = 18.4;
  const outT = 162.0;
  const playhead = state === 'playingPreview' ? 92.4 : inT; // preview cursor
  const wave = genWaveform(11, 96);

  const inPct = inT / total, outPct = outT / total, headPct = playhead / total;

  return (
    <div style={{ width: '100%', height: '100%', background: `linear-gradient(180deg, #142A2A 0%, ${T.BG_BOTTOM} 60%)`, color: T.TEXT_HI, fontFamily: T.FONT, display: 'flex', flexDirection: 'column' }}>
      {/* Top bar */}
      <div style={{ paddingTop: 52, padding: '52px 16px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 4 }}><I.X/></button>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 700, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase' }}>// MODALITÀ · TAGLIO</div>
        <button style={{ padding: '8px 16px', borderRadius: 999, background: T.ACCENT, color: '#0A0A0A', border: 'none', fontWeight: 700, fontSize: 13, fontFamily: T.FONT, cursor: 'pointer' }}>Salva</button>
      </div>

      {/* Track header */}
      <div style={{ padding: '20px 16px 8px', display: 'flex', alignItems: 'center', gap: 14 }}>
        <div style={{ width: 60, height: 60, flexShrink: 0, boxShadow: '0 8px 20px rgba(0,0,0,0.5)' }}>
          <MHCover kind="blob" palette={{ a:'#1E3A8A', b:'#06B6D4' }} radius={6}/>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 18, fontWeight: 800, letterSpacing: -0.4, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>Undertow</div>
          <div style={{ fontSize: 13, color: T.TEXT_LO, marginTop: 2 }}>Helena Vorr · Slow Hours</div>
          <div style={{ fontSize: 11, color: T.TEXT_LO2, marginTop: 4, fontFamily: T.MONO }}>{fmt(total)} · originale</div>
        </div>
      </div>

      {/* ========== PREVIEW BAR ========== */}
      <div style={{ padding: '14px 16px 0' }}>
        <SectionLabel>// 01 · ASCOLTO · scrub libero</SectionLabel>
        <div style={{ marginTop: 10, padding: '12px 14px 10px', background: T.CARD, borderRadius: 14 }}>
          <div style={{ position: 'relative' }}>
            {/* Waveform — full track, dim outside trim region */}
            <Bars data={wave} color={T.TEXT_HI} dim="rgba(255,255,255,0.18)" height={64}
                  mask={{ from: inPct, to: outPct }}/>
            {/* In/out region overlay (subtle) */}
            <div style={{ position: 'absolute', top: -4, bottom: -4, left: `${inPct * 100}%`, width: `${(outPct - inPct) * 100}%`, background: 'rgba(168,224,78,0.06)', borderLeft: `1.5px dashed rgba(168,224,78,0.4)`, borderRight: `1.5px dashed rgba(168,224,78,0.4)`, pointerEvents: 'none' }}/>
            {/* Playhead */}
            <div style={{ position: 'absolute', top: -8, bottom: -8, left: `calc(${headPct * 100}% - 1px)`, width: 2, background: '#FFC857', boxShadow: '0 0 8px rgba(255,200,87,0.7)' }}>
              <div style={{ position: 'absolute', top: -10, left: -7, width: 16, height: 12, borderRadius: 3, background: '#FFC857' }}/>
            </div>
          </div>

          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 10, fontFamily: T.MONO, fontSize: 11 }}>
            <span style={{ color: T.TEXT_LO }}>00:00.0</span>
            <div style={{ display: 'flex', alignItems: 'center', gap: 4, padding: '3px 10px', borderRadius: 999, background: 'rgba(255,200,87,0.12)', color: '#FFC857', fontWeight: 700 }}>
              <div style={{ width: 6, height: 6, borderRadius: 3, background: '#FFC857' }}/>
              {fmt(playhead)}
            </div>
            <span style={{ color: T.TEXT_LO }}>{fmt(total)}</span>
          </div>

          {/* Preview transport */}
          <div style={{ marginTop: 12, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 18 }}>
            <PreviewBtn label="−5s" small><span style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700 }}>−5</span></PreviewBtn>
            <PreviewBtn label="Vai a IN"><JumpInIcon/></PreviewBtn>
            <button style={{ width: 56, height: 56, borderRadius: 999, background: state === 'playingPreview' ? '#FFC857' : T.ACCENT, border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer', boxShadow: '0 8px 18px rgba(168,224,78,0.3)' }}>
              {state === 'playingPreview' ? <I.Pause size={20}/> : <I.Play size={20}/>}
            </button>
            <PreviewBtn label="Vai a OUT"><JumpOutIcon/></PreviewBtn>
            <PreviewBtn label="+5s" small><span style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700 }}>+5</span></PreviewBtn>
          </div>
        </div>
      </div>

      {/* ========== TRIM BAR ========== */}
      <div style={{ padding: '20px 16px 0' }}>
        <SectionLabel>// 02 · TAGLIO · sposta i punti IN / OUT</SectionLabel>
        <div style={{ marginTop: 10, padding: '14px 14px 12px', background: T.CARD, borderRadius: 14, border: '1px solid rgba(168,224,78,0.18)' }}>
          {/* Trim track */}
          <div style={{ position: 'relative', padding: '10px 0' }}>
            {/* Background bar */}
            <div style={{ position: 'absolute', left: 0, right: 0, top: '50%', height: 2, background: 'rgba(255,255,255,0.08)', transform: 'translateY(-1px)' }}/>
            {/* Active region */}
            <div style={{ position: 'absolute', top: '50%', left: `${inPct * 100}%`, width: `${(outPct - inPct) * 100}%`, height: 8, background: T.ACCENT, transform: 'translateY(-4px)', borderRadius: 2 }}/>
            {/* Mini waveform inside the region — visual flair */}
            <div style={{ position: 'relative', height: 60 }}>
              <Bars data={wave} color="rgba(168,224,78,0.45)" dim="rgba(255,255,255,0.07)"
                    height={60} gap={1.4} mask={{ from: inPct, to: outPct }}/>
            </div>

            {/* IN handle */}
            <Handle pct={inPct} side="in" label="IN" time={inT}/>
            {/* OUT handle */}
            <Handle pct={outPct} side="out" label="OUT" time={outT}/>
          </div>

          {/* Numeric in/out + nudge */}
          <div style={{ display: 'flex', gap: 10, marginTop: 14 }}>
            <NudgeBox label="IN" time={inT} accent={T.ACCENT}/>
            <NudgeBox label="OUT" time={outT} accent={T.ACCENT}/>
          </div>

          {/* Quick actions */}
          <div style={{ display: 'flex', gap: 8, marginTop: 12, flexWrap: 'wrap' }}>
            <Pill ic="silence" label="Aggancia al silenzio"/>
            <Pill ic="fade" label="Fade in/out" active/>
            <Pill ic="ab" label="Anteprima A/B"/>
          </div>
        </div>
      </div>

      {/* Result summary */}
      <div style={{ padding: '14px 16px 0' }}>
        <div style={{ background: 'rgba(168,224,78,0.06)', border: '1px solid rgba(168,224,78,0.18)', borderRadius: 12, padding: '12px 14px', display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontFamily: T.MONO, fontSize: 9, fontWeight: 700, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 4 }}>// RISULTATO</div>
            <div style={{ fontSize: 14, fontWeight: 700, fontFamily: T.MONO }}>
              {fmt(outT - inT)} <span style={{ color: T.TEXT_LO, fontWeight: 500 }}>· tagliato {fmt(total - (outT - inT))} dall'originale</span>
            </div>
          </div>
        </div>
      </div>

      {/* Toast */}
      {state === 'savedToast' && (
        <div style={{ position: 'absolute', left: 16, right: 16, bottom: 36, padding: '14px 16px', background: '#181818', border: '1px solid rgba(168,224,78,0.3)', borderRadius: 14, display: 'flex', alignItems: 'center', gap: 12, boxShadow: '0 12px 32px rgba(0,0,0,0.6)' }}>
          <div style={{ width: 32, height: 32, borderRadius: 999, background: T.ACCENT, display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <I.Check size={16} color="#0A0A0A"/>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 13.5, fontWeight: 700 }}>Salvato come · Undertow (cut)</div>
            <div style={{ fontSize: 11.5, color: T.TEXT_LO, fontFamily: T.MONO }}>Versione locale · sostituirà l'originale nelle playlist? <span style={{ color: T.ACCENT }}>Sì / No</span></div>
          </div>
        </div>
      )}

      <div style={{ flex: 1 }}/>

      {/* Bottom hint */}
      <div style={{ padding: '12px 16px 32px', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6, color: T.TEXT_LO2, fontFamily: T.MONO, fontSize: 10, letterSpacing: 0.5 }}>
        <Lightbulb/> TIENI PREMUTO UN MARCATORE PER ZOOM ×8
      </div>
    </div>
  );
}

function SectionLabel({ children }) {
  return <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 700, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase' }}>{children}</div>;
}

function PreviewBtn({ children, label, small }) {
  const sz = small ? 36 : 40;
  return (
    <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4 }}>
      <button style={{ width: sz, height: sz, borderRadius: 999, background: 'rgba(255,255,255,0.06)', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', color: T.TEXT_HI, cursor: 'pointer' }}>
        {children}
      </button>
      <div style={{ fontSize: 9, fontFamily: T.MONO, color: T.TEXT_LO2, letterSpacing: 0.5 }}>{label}</div>
    </div>
  );
}

function JumpInIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
      <path d="M5 4v16M9 12l9-7v14l-9-7z" stroke={T.ACCENT} strokeWidth="1.7" strokeLinejoin="round" fill={T.ACCENT}/>
    </svg>
  );
}
function JumpOutIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
      <path d="M19 4v16M15 12L6 5v14l9-7z" stroke={T.ACCENT} strokeWidth="1.7" strokeLinejoin="round" fill={T.ACCENT}/>
    </svg>
  );
}

function Handle({ pct, side, label, time }) {
  const isIn = side === 'in';
  return (
    <div style={{
      position: 'absolute',
      left: `calc(${pct * 100}% - 12px)`, top: 0, bottom: 0,
      width: 24, display: 'flex', flexDirection: 'column', alignItems: 'center',
    }}>
      <div style={{
        position: 'absolute', top: -6, bottom: -6,
        left: 11, width: 2, background: T.ACCENT, boxShadow: '0 0 10px rgba(168,224,78,0.7)',
      }}/>
      <div style={{
        position: 'absolute', top: '50%', left: 0,
        transform: 'translateY(-50%)',
        width: 24, height: 36, background: T.ACCENT,
        borderRadius: isIn ? '4px 2px 2px 4px' : '2px 4px 4px 2px',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        boxShadow: '0 4px 14px rgba(168,224,78,0.45)',
        cursor: 'ew-resize',
      }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          <div style={{ width: 2, height: 12, background: '#0A0A0A', borderRadius: 1 }}/>
          <div style={{ width: 2, height: 12, background: '#0A0A0A', borderRadius: 1, marginTop: -10, marginLeft: 4 }}/>
        </div>
      </div>
      <div style={{
        position: 'absolute',
        bottom: -28, left: '50%', transform: 'translateX(-50%)',
        background: '#0A0A0A', border: `1px solid ${T.ACCENT}`,
        padding: '3px 7px', borderRadius: 4,
        fontFamily: T.MONO, fontSize: 9.5, fontWeight: 700, color: T.ACCENT, letterSpacing: 0.5, whiteSpace: 'nowrap',
      }}>
        {label} · {fmt(time)}
      </div>
    </div>
  );
}

function NudgeBox({ label, time, accent }) {
  return (
    <div style={{ flex: 1, padding: 12, background: '#0A0A0A', border: '1px solid rgba(255,255,255,0.06)', borderRadius: 10 }}>
      <div style={{ fontFamily: T.MONO, fontSize: 9, fontWeight: 700, color: accent, letterSpacing: 1.5, marginBottom: 6 }}>// {label}</div>
      <div style={{ fontFamily: T.MONO, fontSize: 22, fontWeight: 800, letterSpacing: -0.5, color: T.TEXT_HI, lineHeight: 1 }}>{fmt(time)}</div>
      <div style={{ display: 'flex', gap: 6, marginTop: 10 }}>
        {['−1s', '−.1', '+.1', '+1s'].map(l => (
          <div key={l} style={{ flex: 1, padding: '6px 0', textAlign: 'center', borderRadius: 6, background: 'rgba(255,255,255,0.06)', fontFamily: T.MONO, fontSize: 10, fontWeight: 700, color: T.TEXT_HI }}>{l}</div>
        ))}
      </div>
    </div>
  );
}

function Pill({ ic, label, active }) {
  let icon;
  switch (ic) {
    case 'silence': icon = (
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
        <path d="M3 12h4l3-7v14l-3-7H3z" stroke="currentColor" strokeWidth="1.6" strokeLinejoin="round"/>
        <path d="M16 8l5 8M21 8l-5 8" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round"/>
      </svg>
    ); break;
    case 'fade': icon = (
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
        <path d="M3 19l8-14 5 14M14 19h7" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
      </svg>
    ); break;
    case 'ab': icon = (
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
        <path d="M5 8l4 8h-2.5L6 14H4l-.5 2H1l4-8zm14 0v8m-3-4h6" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
      </svg>
    ); break;
  }
  return (
    <div style={{
      display: 'inline-flex', alignItems: 'center', gap: 6,
      padding: '8px 12px', borderRadius: 999,
      background: active ? 'rgba(168,224,78,0.15)' : 'rgba(255,255,255,0.06)',
      color: active ? T.ACCENT : T.TEXT_HI,
      border: active ? '1px solid rgba(168,224,78,0.4)' : '1px solid transparent',
      fontSize: 12, fontWeight: 600, fontFamily: T.FONT,
    }}>
      {icon} {label}
    </div>
  );
}

function Lightbulb() {
  return (
    <svg width="11" height="11" viewBox="0 0 24 24" fill="none">
      <path d="M9 18h6M10 21h4M12 3a6 6 0 00-4 10.5c.5.5 1 1.5 1 2.5h6c0-1 .5-2 1-2.5A6 6 0 0012 3z" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
    </svg>
  );
}

window.MHTrim = { TrimTrackScreen };
