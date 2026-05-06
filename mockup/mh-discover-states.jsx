// MusicHub — Discover · Missing impl-only states
// Fills the gap between FindViewModel/SpotifyImport impl
// and the original mh-discover.jsx mockup.
const { T, I, MHCover } = window.MH;
const { SettingsSubScreen } = window.MHSettings;

// ════════════════════════════════════════════════════════
//   FIND  ·  background tracking + lifecycle states
// ════════════════════════════════════════════════════════

// ── Shared chrome ───────────────────────────────────────
function FindShell({ children, gradient }) {
  return (
    <div style={{
      width: '100%', height: '100%',
      background: gradient || `linear-gradient(180deg, ${T.BG_TOP} 0%, ${T.BG_BOTTOM} 360px)`,
      color: T.TEXT_HI, fontFamily: T.FONT,
      display: 'flex', flexDirection: 'column', overflow: 'hidden',
    }}>
      {children}
    </div>
  );
}

function FindSearchField({ value = '', placeholder = 'Cerca artista o brano…', readOnly = false, dim = false }) {
  return (
    <div style={{ padding: '8px 16px 14px', opacity: dim ? 0.55 : 1 }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '12px 14px', background: T.CARD, borderRadius: 12 }}>
        <I.Search size={18} color={T.TEXT_LO}/>
        <input
          defaultValue={value}
          placeholder={placeholder}
          readOnly={readOnly}
          style={{ flex: 1, background: 'transparent', border: 'none', outline: 'none', color: T.TEXT_HI, fontSize: 14, fontFamily: T.FONT }}
        />
        {value && <I.X size={18} color={T.TEXT_LO}/>}
      </div>
    </div>
  );
}

function FindTopBar({ eyebrow = '// SCOPRI · YT', title = 'Trova brani', back = true }) {
  return (
    <div style={{ padding: '60px 16px 12px', display: 'flex', alignItems: 'center', gap: 12 }}>
      {back && (
        <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 4, marginLeft: -4 }}>
          <I.Back/>
        </button>
      )}
      <div style={{ flex: 1 }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase' }}>
          {eyebrow}
        </div>
        <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.5, marginTop: 2 }}>{title}</div>
      </div>
    </div>
  );
}

// ── Status header (back + "<query>" + status + progress strip)
function StatusHeader({ query, status, pct, strip = true, color = T.ACCENT }) {
  return (
    <div style={{ paddingTop: 56, background: 'rgba(0,0,0,0.25)', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
      <div style={{ padding: '0 16px 12px', display: 'flex', alignItems: 'center', gap: 12 }}>
        <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 4, marginLeft: -4 }}>
          <I.Back/>
        </button>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 15, fontWeight: 700, letterSpacing: -0.2, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            "{query}"
          </div>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.2, color, textTransform: 'uppercase', marginTop: 2 }}>
            {status}
          </div>
        </div>
      </div>
      {strip && (
        <div style={{ height: 2, background: 'rgba(255,255,255,0.06)', position: 'relative', overflow: 'hidden' }}>
          {pct == null ? (
            <div style={{
              position: 'absolute', top: 0, left: 0, height: '100%', width: '40%',
              background: color, animation: 'mh-indet 1.4s ease-in-out infinite',
            }}/>
          ) : (
            <div style={{ width: `${pct * 100}%`, height: '100%', background: color, transition: 'width 0.3s' }}/>
          )}
        </div>
      )}
      <style>{`
        @keyframes mh-indet { 0%{left:-40%} 100%{left:100%} }
      `}</style>
    </div>
  );
}

// ── Active-request row (for Idle list)
// Mirrors ActiveRequestRow: query string, status label, linear progress.
function ActiveRequestRow({ query, status, pct, color = T.ACCENT, indeterminate = false }) {
  return (
    <div style={{ padding: '14px 0', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 8 }}>
        <div style={{ width: 28, height: 28, borderRadius: 8, background: 'rgba(168,224,78,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
          <I.Search size={14} color={color}/>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 13.5, fontWeight: 600, color: T.TEXT_HI, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            "{query}"
          </div>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, color, letterSpacing: 1.2, textTransform: 'uppercase', marginTop: 1 }}>
            {status}
          </div>
        </div>
        <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 4 }}>
          <I.X size={16} color={T.TEXT_LO2}/>
        </button>
      </div>
      <div style={{ height: 2, background: 'rgba(255,255,255,0.06)', borderRadius: 1, position: 'relative', overflow: 'hidden' }}>
        {indeterminate ? (
          <div style={{
            position: 'absolute', top: 0, height: '100%', width: '35%',
            background: color, animation: 'mh-indet 1.4s ease-in-out infinite',
          }}/>
        ) : (
          <div style={{ width: `${pct * 100}%`, height: '100%', background: color, transition: 'width 0.3s' }}/>
        )}
      </div>
    </div>
  );
}

// ── Terminal status row (IMPORTED / PARTIAL / FAILED / CANCELED)
function TerminalRow({ query, kind }) {
  const map = {
    IMPORTED:         { color: T.ACCENT,   label: 'IMPORTATO',         msg: 'Aggiunto alla libreria',         Glyph: I.Check },
    IMPORTED_PARTIAL: { color: '#FFC857',  label: 'IMPORTATO · PARZIALE', msg: 'Solo audio · video saltato',  Glyph: ({size,color}) => (
      <svg width={size||14} height={size||14} viewBox="0 0 24 24" fill="none">
        <path d="M12 8v5M12 16.5v.5" stroke={color} strokeWidth="2.4" strokeLinecap="round"/>
      </svg>
    )},
    FAILED:           { color: '#E14848',  label: 'NON TROVATO',       msg: 'Nessuna corrispondenza · YT',     Glyph: I.X },
    CANCELED:         { color: T.TEXT_LO,  label: 'ANNULLATO',         msg: 'Interrotto manualmente',          Glyph: ({size,color}) => (
      <svg width={size||14} height={size||14} viewBox="0 0 24 24" fill="none">
        <circle cx="12" cy="12" r="9" stroke={color} strokeWidth="1.8"/>
        <path d="M5.5 5.5l13 13" stroke={color} strokeWidth="1.8" strokeLinecap="round"/>
      </svg>
    )},
  };
  const m = map[kind];
  return (
    <div style={{ padding: '14px 0', borderBottom: '1px solid rgba(255,255,255,0.05)', display: 'flex', alignItems: 'center', gap: 10 }}>
      <div style={{ width: 28, height: 28, borderRadius: 8, background: `${m.color}26`, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
        <m.Glyph size={14} color={m.color}/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 13.5, fontWeight: 600, color: T.TEXT_HI, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
          "{query}"
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'center', marginTop: 2 }}>
          <span style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 700, color: m.color, letterSpacing: 1.2 }}>
            {m.label}
          </span>
          <span style={{ fontSize: 11, color: T.TEXT_LO }}>· {m.msg}</span>
        </div>
      </div>
      {kind === 'FAILED' && (
        <button style={{ padding: '6px 10px', borderRadius: 999, background: 'rgba(255,255,255,0.06)', border: 'none', color: T.TEXT_HI, fontSize: 11, fontWeight: 700, fontFamily: T.MONO, letterSpacing: 0.5, cursor: 'pointer' }}>
          RIPROVA
        </button>
      )}
    </div>
  );
}

// ── 11a · FindIdle · empty
function FindIdleEmpty() {
  return (
    <FindShell>
      <FindTopBar/>
      <FindSearchField/>
      <div style={{ flex: 1, padding: '0 16px 32px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ width: 64, height: 64, borderRadius: 999, background: 'rgba(255,255,255,0.04)', border: '1px solid rgba(255,255,255,0.06)', display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 18 }}>
          <I.Search size={28} color={T.TEXT_LO2}/>
        </div>
        <div style={{ fontSize: 16, fontWeight: 700, marginBottom: 6, textAlign: 'center' }}>Trova nuovi brani</div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, textAlign: 'center', maxWidth: 260, lineHeight: 1.5 }}>
          Le ricerche girano in background.<br/>Puoi avviarne più di una contemporaneamente.
        </div>
        <div style={{ marginTop: 28, padding: '8px 12px', borderRadius: 8, background: 'rgba(168,224,78,0.06)', border: '1px solid rgba(168,224,78,0.18)' }}>
          <span style={{ fontFamily: T.MONO, fontSize: 10, color: T.ACCENT, letterSpacing: 1.2, fontWeight: 600 }}>
            // FINDVIEWMODEL · IDLE
          </span>
        </div>
      </div>
    </FindShell>
  );
}

// ── 11b · FindIdle · with active background requests
function FindIdleActive() {
  return (
    <FindShell>
      <FindTopBar/>
      <FindSearchField/>
      <div style={{ flex: 1, overflowY: 'auto', padding: '0 16px 32px' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', padding: '4px 0 8px' }}>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase' }}>
            // 4 IN CORSO · BACKGROUND
          </div>
          <div style={{ fontSize: 11, color: T.TEXT_LO2, fontFamily: T.MONO }}>POLL · 2s</div>
        </div>

        <ActiveRequestRow query="Mira Holt — Strange Mercy" status="RICERCA SU YT" indeterminate color={T.ACCENT}/>
        <ActiveRequestRow query="Helena Vorr — Undertow"    status="SBLOCCO STREAM"  pct={0.42} color="#FFC857"/>
        <ActiveRequestRow query="Faro — Coastline"          status="DOWNLOAD · 68%"  pct={0.68} color={T.ACCENT}/>
        <ActiveRequestRow query="Low Tides — Open Window"   status="DOWNLOAD · 91%"  pct={0.91} color={T.ACCENT}/>

        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase', padding: '20px 0 8px' }}>
          // OGGI · COMPLETATE
        </div>
        <TerminalRow query="Slow Hours — Side A"    kind="IMPORTED"/>
        <TerminalRow query="Driving Companion v2"   kind="IMPORTED_PARTIAL"/>
        <TerminalRow query="Echoes (live KEXP)"     kind="FAILED"/>
        <TerminalRow query="Mira Holt — demos 2019" kind="CANCELED"/>
      </div>
    </FindShell>
  );
}

// ── 11c · FindIdle · pull-to-refresh
function FindIdlePullRefresh() {
  return (
    <FindShell>
      <FindTopBar/>
      <FindSearchField/>
      {/* Pull indicator slot — overlay above the list */}
      <div style={{ position: 'relative', flex: 1, overflow: 'hidden' }}>
        <div style={{
          position: 'absolute', top: 8, left: 0, right: 0, display: 'flex',
          flexDirection: 'column', alignItems: 'center', gap: 6, zIndex: 2,
        }}>
          <div style={{
            width: 32, height: 32, borderRadius: 16,
            background: 'rgba(20,20,20,0.92)',
            border: '1px solid rgba(255,255,255,0.08)',
            boxShadow: '0 8px 20px rgba(0,0,0,0.5)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <div style={{
              width: 16, height: 16, borderRadius: 8,
              border: `2px solid rgba(168,224,78,0.25)`,
              borderTopColor: T.ACCENT,
              animation: 'mh-spin 0.9s linear infinite',
            }}/>
          </div>
          <div style={{ fontFamily: T.MONO, fontSize: 10, color: T.ACCENT, letterSpacing: 1, fontWeight: 600 }}>
            AGGIORNO…
          </div>
        </div>

        <div style={{ position: 'absolute', inset: 0, padding: '76px 16px 32px', overflowY: 'auto', transform: 'translateY(8px)' }}>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase', padding: '4px 0 8px' }}>
            // 2 IN CORSO · BACKGROUND
          </div>
          <ActiveRequestRow query="Helena Vorr — Undertow" status="SBLOCCO STREAM" pct={0.46} color="#FFC857"/>
          <ActiveRequestRow query="Faro — Coastline"       status="DOWNLOAD · 71%" pct={0.71} color={T.ACCENT}/>
        </div>
      </div>
      <style>{`@keyframes mh-spin { to { transform: rotate(360deg); } }`}</style>
    </FindShell>
  );
}

// ── 11d · Searching · pure (no candidates yet)
function FindSearching() {
  return (
    <FindShell>
      <StatusHeader query="Mira Holt" status="RICERCA SU YT…" />
      <FindSearchField value="Mira Holt" readOnly/>
      <div style={{ flex: 1, padding: '0 16px 32px' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase', padding: '4px 0 12px' }}>
          // INTERROGAZIONE INDICE
        </div>
        {[1,2,3,4,5].map(i => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 0', opacity: 0.5 }}>
            <div style={{ width: 48, height: 48, borderRadius: 4, background: 'rgba(255,255,255,0.06)', flexShrink: 0 }}/>
            <div style={{ flex: 1 }}>
              <div style={{ height: 12, width: `${75 - i*8}%`, borderRadius: 3, background: 'rgba(255,255,255,0.08)', marginBottom: 6 }}/>
              <div style={{ height: 10, width: `${50 - i*4}%`, borderRadius: 3, background: 'rgba(255,255,255,0.05)' }}/>
            </div>
          </div>
        ))}
        <div style={{ marginTop: 18, padding: '10px 14px', borderRadius: 10, background: 'rgba(168,224,78,0.06)', border: '1px solid rgba(168,224,78,0.15)', display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{ width: 12, height: 12, borderRadius: 6, border: `2px solid rgba(168,224,78,0.25)`, borderTopColor: T.ACCENT, animation: 'mh-spin 0.9s linear infinite' }}/>
          <div style={{ fontSize: 11.5, color: T.ACCENT, fontFamily: T.MONO, letterSpacing: 0.5 }}>
            La ricerca continua se chiudi questa schermata.
          </div>
        </div>
      </div>
      <style>{`@keyframes mh-spin { to { transform: rotate(360deg); } }`}</style>
    </FindShell>
  );
}

// ── 11e · Candidates list with selection highlight (UNLOCKING/DOWNLOADING)
function FindCandidatesSelected({ phase = 'unlocking' }) {
  // phase: 'unlocking' | 'downloading'
  const candidates = [
    { title: 'Strange Mercy',         artist: 'Mira Holt',       dur: '3:48', views: '1.2M',  src: 'YouTube', kind: 'arc',       palette: { a:'#5C2D8C', b:'#F0A6B0' } },
    { title: 'Open Window',           artist: 'Mira Holt',       dur: '2:51', views: '847K',  src: 'YouTube', kind: 'wave',      palette: { a:'#1E3A8A', b:'#06B6D4' } },
    { title: 'Quiet Field — live',    artist: 'Mira Holt · KEXP',dur: '4:22', views: '294K',  src: 'YouTube', kind: 'triangles', palette: { bg:'#1A1A1A', fg:'#FF4D2E' } },
    { title: 'Open Window — demo',    artist: 'Mira Holt',       dur: '2:31', views: '88K',   src: 'YouTube', kind: 'blob',      palette: { a:'#3A0CA3', b:'#F72585' } },
  ];
  const selectedIdx = 0;
  const status = phase === 'unlocking' ? 'SBLOCCO STREAM…' : 'DOWNLOAD · 64%';
  const pct = phase === 'unlocking' ? null : 0.64;
  const color = phase === 'unlocking' ? '#FFC857' : T.ACCENT;
  return (
    <FindShell>
      <StatusHeader query="Mira Holt" status={status} pct={pct} color={color}/>
      <FindSearchField value="Mira Holt" readOnly/>
      <div style={{ flex: 1, overflowY: 'auto', padding: '0 16px 32px' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase', padding: '4px 4px 8px' }}>
          // 4 RISULTATI · YT MATCH
        </div>
        {candidates.map((c, i) => {
          const isSelected = i === selectedIdx;
          return (
            <div key={i} style={{
              display: 'flex', alignItems: 'center', gap: 12, padding: '10px 12px',
              margin: '0 -8px',
              borderRadius: 10,
              background: isSelected ? 'rgba(168,224,78,0.10)' : 'transparent',
              border: isSelected ? `1px solid rgba(168,224,78,0.30)` : '1px solid transparent',
              borderBottom: isSelected ? '1px solid rgba(168,224,78,0.30)' : '1px solid rgba(255,255,255,0.05)',
              marginBottom: 2,
            }}>
              <div style={{ width: 48, height: 48, flexShrink: 0, position: 'relative' }}>
                <MHCover kind={c.kind} palette={c.palette} radius={4}/>
                {isSelected && (
                  <div style={{
                    position: 'absolute', inset: 0, borderRadius: 4,
                    background: 'rgba(0,0,0,0.55)',
                    display: 'flex', alignItems: 'center', justifyContent: 'center',
                  }}>
                    <div style={{ width: 18, height: 18, borderRadius: 9, border: `2px solid rgba(255,255,255,0.25)`, borderTopColor: color, animation: 'mh-spin 0.9s linear infinite' }}/>
                  </div>
                )}
              </div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 14, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{c.title}</div>
                <div style={{ fontSize: 12, color: T.TEXT_LO, display: 'flex', alignItems: 'center', gap: 6, marginTop: 2 }}>
                  <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{c.artist}</span>
                  <span>·</span>
                  <span style={{ fontFamily: T.MONO }}>{c.dur}</span>
                  <span>·</span>
                  <span style={{ fontFamily: T.MONO, color: T.TEXT_LO2 }}>{c.views} views</span>
                </div>
              </div>
              {isSelected ? (
                <div style={{ padding: '7px 10px', borderRadius: 999, background: `${color}26`, color, fontWeight: 700, fontSize: 10.5, fontFamily: T.MONO, letterSpacing: 0.8, display: 'flex', alignItems: 'center', gap: 4 }}>
                  {phase === 'unlocking' ? 'SBLOCCO' : `${Math.round(pct * 100)}%`}
                </div>
              ) : (
                <button style={{ padding: '7px 14px', borderRadius: 999, border: 'none', background: T.ACCENT, color: '#0A0A0A', fontWeight: 700, fontSize: 12, cursor: 'pointer', fontFamily: T.FONT, opacity: 0.4 }} disabled>
                  Aggiungi
                </button>
              )}
            </div>
          );
        })}
      </div>
      <style>{`@keyframes mh-spin { to { transform: rotate(360deg); } }`}</style>
    </FindShell>
  );
}

// ── 11f · Terminal IMPORTED success screen
function FindTerminalScreen({ kind = 'IMPORTED' }) {
  const map = {
    IMPORTED: {
      bg: `radial-gradient(120% 50% at 50% 0%, #2A4615 0%, ${T.BG_TOP} 35%, ${T.BG_BOTTOM} 100%)`,
      iconBg: 'rgba(168,224,78,0.15)', iconColor: T.ACCENT,
      title: 'Aggiunto alla libreria',
      sub: 'Mira Holt — Strange Mercy · 3:48',
      eyebrow: '// IMPORTED', eyebrowColor: T.ACCENT,
      Glyph: ({size, color}) => <I.Check size={size} color={color}/>,
      meta: [
        { k: 'Sorgente', v: 'YouTube' },
        { k: 'Bitrate',  v: '320 kbps' },
        { k: 'Durata',   v: '0.9s' },
      ],
      ctas: [
        { label: 'Apri brano',  primary: true },
        { label: 'Trova un altro', primary: false },
      ],
    },
    IMPORTED_PARTIAL: {
      bg: `radial-gradient(120% 50% at 50% 0%, #4A3A14 0%, ${T.BG_TOP} 35%, ${T.BG_BOTTOM} 100%)`,
      iconBg: 'rgba(255,200,87,0.15)', iconColor: '#FFC857',
      title: 'Importato · parzialmente',
      sub: 'Solo audio recuperato · video saltato',
      eyebrow: '// IMPORTED · PARTIAL', eyebrowColor: '#FFC857',
      Glyph: ({size, color}) => (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="12" r="10" stroke={color} strokeWidth="2"/>
          <path d="M12 7v6M12 16.5v.5" stroke={color} strokeWidth="2.4" strokeLinecap="round"/>
        </svg>
      ),
      meta: [
        { k: 'Audio',     v: 'OK · 320 kbps' },
        { k: 'Video',     v: 'Saltato' },
        { k: 'Motivo',    v: 'codec' },
      ],
      ctas: [
        { label: 'Apri brano',  primary: true },
        { label: 'Riprova video', primary: false },
      ],
    },
    FAILED: {
      bg: `radial-gradient(120% 50% at 50% 0%, #3A1818 0%, ${T.BG_TOP} 35%, ${T.BG_BOTTOM} 100%)`,
      iconBg: 'rgba(225,72,72,0.15)', iconColor: '#E14848',
      title: 'Brano non trovato',
      sub: 'Nessuna corrispondenza affidabile su YouTube',
      eyebrow: '// FAILED', eyebrowColor: '#E14848',
      Glyph: ({size, color}) => <I.X size={size} color={color}/>,
      meta: [
        { k: 'Tentati',   v: '12 candidati' },
        { k: 'Match max', v: '0.41' },
        { k: 'Soglia',    v: '0.65' },
      ],
      ctas: [
        { label: 'Riprova',          primary: true },
        { label: 'Affina la ricerca', primary: false },
      ],
    },
    CANCELED: {
      bg: `radial-gradient(120% 50% at 50% 0%, #2A2A2A 0%, ${T.BG_TOP} 35%, ${T.BG_BOTTOM} 100%)`,
      iconBg: 'rgba(154,154,154,0.15)', iconColor: T.TEXT_LO,
      title: 'Ricerca annullata',
      sub: 'Hai interrotto questa richiesta',
      eyebrow: '// CANCELED', eyebrowColor: T.TEXT_LO,
      Glyph: ({size, color}) => (
        <svg width={size} height={size} viewBox="0 0 24 24" fill="none">
          <circle cx="12" cy="12" r="10" stroke={color} strokeWidth="2"/>
          <path d="M5 5l14 14" stroke={color} strokeWidth="2" strokeLinecap="round"/>
        </svg>
      ),
      meta: [
        { k: 'Stato',     v: 'Interrotta' },
        { k: 'Progresso', v: '38%' },
        { k: 'Pulizia',   v: 'OK' },
      ],
      ctas: [
        { label: 'Riavvia ricerca', primary: true },
        { label: 'Torna indietro',  primary: false },
      ],
    },
  };
  const s = map[kind];
  return (
    <div style={{
      width: '100%', height: '100%', background: s.bg, color: T.TEXT_HI, fontFamily: T.FONT,
      display: 'flex', flexDirection: 'column',
    }}>
      <FindTopBar eyebrow={s.eyebrow} title="Trova brani"/>
      <div style={{ flex: 1, padding: '8px 24px 0', display: 'flex', flexDirection: 'column' }}>
        <div style={{ display: 'flex', justifyContent: 'center', margin: '24px 0 22px' }}>
          <div style={{
            width: 88, height: 88, borderRadius: 999, background: s.iconBg,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            border: `1px solid ${s.iconColor}40`,
          }}>
            <s.Glyph size={42} color={s.iconColor}/>
          </div>
        </div>
        <div style={{ fontSize: 24, fontWeight: 800, letterSpacing: -0.5, textAlign: 'center', marginBottom: 8, lineHeight: 1.15 }}>
          {s.title}
        </div>
        <div style={{ fontSize: 13.5, color: T.TEXT_LO, textAlign: 'center', marginBottom: 6 }}>
          "Mira Holt"
        </div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, textAlign: 'center', marginBottom: 28, lineHeight: 1.5 }}>
          {s.sub}
        </div>

        <div style={{
          background: T.CARD, borderRadius: 12, padding: '14px 16px',
          display: 'flex', flexDirection: 'column', gap: 10,
        }}>
          {s.meta.map((m, i) => (
            <div key={i} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
              <span style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO, letterSpacing: 0.5, textTransform: 'uppercase' }}>{m.k}</span>
              <span style={{ fontFamily: T.MONO, fontSize: 12, color: T.TEXT_HI, fontWeight: 600 }}>{m.v}</span>
            </div>
          ))}
        </div>

        <div style={{ flex: 1 }}/>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 8, padding: '20px 0 32px' }}>
          {s.ctas.map((c, i) => (
            <button key={i} style={{
              padding: '13px 16px', borderRadius: 999, border: 'none', cursor: 'pointer',
              background: c.primary ? T.ACCENT : 'rgba(255,255,255,0.06)',
              color: c.primary ? '#0A0A0A' : T.TEXT_HI,
              fontWeight: 700, fontSize: 14, fontFamily: T.FONT,
            }}>{c.label}</button>
          ))}
        </div>
      </div>
    </div>
  );
}

// ── 11g · Polling lifecycle (paused / resumed) — diagnostic-style overlay
function FindLifecycle({ paused = true }) {
  return (
    <FindShell>
      <FindTopBar/>
      <FindSearchField/>
      <div style={{ flex: 1, padding: '0 16px 32px' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase', padding: '4px 0 8px' }}>
          // 3 IN CORSO · BACKGROUND
        </div>
        {/* Active rows are dimmed when paused */}
        <div style={{ opacity: paused ? 0.45 : 1, transition: 'opacity 0.3s' }}>
          <ActiveRequestRow query="Mira Holt — Strange Mercy" status="RICERCA SU YT" indeterminate color={T.ACCENT}/>
          <ActiveRequestRow query="Helena Vorr — Undertow"    status="SBLOCCO STREAM" pct={0.42} color="#FFC857"/>
          <ActiveRequestRow query="Faro — Coastline"          status="DOWNLOAD · 68%" pct={0.68} color={T.ACCENT}/>
        </div>
      </div>

      {/* Lifecycle banner — fixed at bottom */}
      <div style={{ padding: '0 16px 24px' }}>
        <div style={{
          padding: '12px 14px', borderRadius: 12,
          background: paused ? 'rgba(255,200,87,0.08)' : 'rgba(168,224,78,0.08)',
          border: `1px solid ${paused ? 'rgba(255,200,87,0.30)' : 'rgba(168,224,78,0.30)'}`,
          display: 'flex', alignItems: 'center', gap: 12,
        }}>
          <div style={{
            width: 36, height: 36, borderRadius: 10,
            background: paused ? 'rgba(255,200,87,0.15)' : 'rgba(168,224,78,0.15)',
            display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
          }}>
            {paused ? (
              <svg width="16" height="16" viewBox="0 0 24 24" fill="#FFC857">
                <rect x="6" y="5" width="4" height="14" rx="1"/><rect x="14" y="5" width="4" height="14" rx="1"/>
              </svg>
            ) : (
              <svg width="16" height="16" viewBox="0 0 24 24" fill={T.ACCENT}><path d="M7 5v14l12-7z"/></svg>
            )}
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 13, fontWeight: 700, color: paused ? '#FFC857' : T.ACCENT, letterSpacing: 0.1 }}>
              {paused ? 'Polling in pausa' : 'Polling ripreso'}
            </div>
            <div style={{ fontFamily: T.MONO, fontSize: 10.5, color: T.TEXT_LO, marginTop: 2, letterSpacing: 0.3 }}>
              {paused ? 'lifecycle/onPause · jobs interrotti' : 'lifecycle/onResume · 3 jobs riavviati'}
            </div>
          </div>
        </div>
      </div>
    </FindShell>
  );
}

// ════════════════════════════════════════════════════════
//   SPOTIFY IMPORT  ·  missing intermediate states
// ════════════════════════════════════════════════════════

// Reusable stepper — `step` is a 0-indexed integer over 5 stops.
function Stepper({ step }) {
  return (
    <div style={{ display: 'flex', gap: 6, marginBottom: 22 }}>
      {[0,1,2,3,4].map(i => (
        <div key={i} style={{ flex: 1, height: 3, borderRadius: 1.5, background: i <= step ? T.ACCENT : 'rgba(255,255,255,0.1)' }}/>
      ))}
    </div>
  );
}

// ── SP-1 · Idle — explicit "Scegli file CSV" CTA
function SpotifyImportIdle() {
  return (
    <SettingsSubScreen eyebrow="// IMPORTAZIONE" title="Importa da Spotify">
      <div style={{ padding: '0 20px 32px' }}>
        <Stepper step={1}/>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, marginBottom: 10 }}>
          // PASSO 2 / 5 · SCEGLI FILE
        </div>
        <div style={{ fontSize: 18, fontWeight: 700, letterSpacing: -0.3, marginBottom: 8 }}>
          Seleziona il CSV esportato
        </div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, lineHeight: 1.55, marginBottom: 22 }}>
          Esportify produce un file <span style={{ fontFamily: T.MONO, color: T.TEXT_HI }}>.csv</span> per ogni playlist.
          Puoi caricarne uno alla volta.
        </div>

        {/* Drop zone-style affordance */}
        <div style={{
          padding: '24px 18px',
          border: '1.5px dashed rgba(168,224,78,0.4)',
          borderRadius: 14,
          background: 'rgba(168,224,78,0.04)',
          display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 10,
          marginBottom: 22,
        }}>
          <div style={{ width: 44, height: 44, borderRadius: 12, background: 'rgba(168,224,78,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none">
              <path d="M14 3H7a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V8z" stroke={T.ACCENT} strokeWidth="1.6" strokeLinejoin="round"/>
              <path d="M14 3v5h5" stroke={T.ACCENT} strokeWidth="1.6" strokeLinejoin="round"/>
              <path d="M9 14l3-3 3 3M12 11v7" stroke={T.ACCENT} strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round"/>
            </svg>
          </div>
          <div style={{ fontSize: 13.5, fontWeight: 600, color: T.TEXT_HI }}>Nessun file selezionato</div>
          <div style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO2, letterSpacing: 0.3 }}>
            .csv · max 10 MB
          </div>
        </div>

        <button style={{
          width: '100%', padding: '14px 16px', background: T.ACCENT, border: 'none',
          borderRadius: 999, color: '#0A0A0A', fontWeight: 700, fontSize: 14,
          cursor: 'pointer', fontFamily: T.FONT,
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10,
        }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
            <path d="M21 12v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6M16 6l-4-4-4 4M12 2v14" stroke="#0A0A0A" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
          Scegli file CSV
        </button>
        <button style={{
          width: '100%', marginTop: 10, padding: '12px 14px',
          background: 'transparent', border: 'none',
          color: T.TEXT_LO, fontWeight: 600, fontSize: 13, cursor: 'pointer', fontFamily: T.FONT,
        }}>
          ↩ Torna alle istruzioni
        </button>
      </div>
    </SettingsSubScreen>
  );
}

// ── SP-2 · FetchingPlaylist — CSV parse intermediate
function SpotifyImportFetching() {
  return (
    <SettingsSubScreen eyebrow="// IMPORTAZIONE" title="Importa da Spotify">
      <div style={{ padding: '0 20px 32px' }}>
        <Stepper step={2}/>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, marginBottom: 10 }}>
          // PASSO 3 / 5 · LETTURA FILE
        </div>
        <div style={{ fontSize: 18, fontWeight: 700, letterSpacing: -0.3, marginBottom: 22 }}>
          Leggo il file…
        </div>

        <div style={{
          background: T.CARD, borderRadius: 14, padding: 20,
          display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 16,
        }}>
          <div style={{
            width: 56, height: 56, borderRadius: 14,
            background: 'rgba(168,224,78,0.10)',
            border: '1px solid rgba(168,224,78,0.25)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            position: 'relative',
          }}>
            <svg width="28" height="28" viewBox="0 0 24 24" fill="none">
              <path d="M14 3H7a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V8z" stroke={T.ACCENT} strokeWidth="1.6" strokeLinejoin="round"/>
              <path d="M14 3v5h5" stroke={T.ACCENT} strokeWidth="1.6" strokeLinejoin="round"/>
              <path d="M8 13h8M8 16h5" stroke={T.ACCENT} strokeWidth="1.6" strokeLinecap="round"/>
            </svg>
            <div style={{
              position: 'absolute', right: -4, bottom: -4,
              width: 22, height: 22, borderRadius: 11,
              background: T.BG_TOP,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
            }}>
              <div style={{
                width: 14, height: 14, borderRadius: 7,
                border: `2px solid rgba(168,224,78,0.25)`,
                borderTopColor: T.ACCENT,
                animation: 'mh-spin 0.85s linear infinite',
              }}/>
            </div>
          </div>
          <div style={{ textAlign: 'center' }}>
            <div style={{ fontSize: 14, fontWeight: 600, color: T.TEXT_HI }}>
              liked-songs.csv
            </div>
            <div style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO, marginTop: 4, letterSpacing: 0.3 }}>
              412 KB · parsing
            </div>
          </div>

          <div style={{ width: '100%', height: 2, background: 'rgba(255,255,255,0.06)', borderRadius: 1, position: 'relative', overflow: 'hidden' }}>
            <div style={{
              position: 'absolute', top: 0, height: '100%', width: '40%',
              background: T.ACCENT, animation: 'mh-indet 1.4s ease-in-out infinite',
            }}/>
          </div>

          <div style={{ width: '100%', display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 10 }}>
            <div style={{ padding: '8px 10px', background: 'rgba(255,255,255,0.04)', borderRadius: 8 }}>
              <div style={{ fontFamily: T.MONO, fontSize: 9.5, color: T.TEXT_LO2, letterSpacing: 0.5, textTransform: 'uppercase' }}>RIGHE</div>
              <div style={{ fontFamily: T.MONO, fontSize: 14, color: T.TEXT_HI, fontWeight: 600, marginTop: 2 }}>284</div>
            </div>
            <div style={{ padding: '8px 10px', background: 'rgba(255,255,255,0.04)', borderRadius: 8 }}>
              <div style={{ fontFamily: T.MONO, fontSize: 9.5, color: T.TEXT_LO2, letterSpacing: 0.5, textTransform: 'uppercase' }}>COLONNE</div>
              <div style={{ fontFamily: T.MONO, fontSize: 14, color: T.TEXT_HI, fontWeight: 600, marginTop: 2 }}>23</div>
            </div>
          </div>
        </div>

        <div style={{ marginTop: 16, fontSize: 12, color: T.TEXT_LO, textAlign: 'center', fontFamily: T.MONO, letterSpacing: 0.3 }}>
          spotify/csv-parse · uri-extract
        </div>
      </div>
      <style>{`
        @keyframes mh-spin { to { transform: rotate(360deg); } }
        @keyframes mh-indet { 0%{left:-40%} 100%{left:100%} }
      `}</style>
    </SettingsSubScreen>
  );
}

// ── SP-3 · Error state with Try Again
function SpotifyImportError() {
  return (
    <SettingsSubScreen eyebrow="// IMPORTAZIONE" title="Importa da Spotify">
      <div style={{ padding: '0 20px 32px' }}>
        <Stepper step={2}/>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: '#E14848', marginBottom: 10 }}>
          // ERRORE · LETTURA FILE
        </div>
        <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.5, marginBottom: 8, lineHeight: 1.15 }}>
          Non riesco a leggere il file
        </div>
        <div style={{ fontSize: 13.5, color: T.TEXT_LO, lineHeight: 1.55, marginBottom: 22 }}>
          Il CSV potrebbe essere corrotto o non in formato Exportify.
        </div>

        <div style={{
          padding: '14px 16px', borderRadius: 12,
          background: 'rgba(225,72,72,0.10)',
          border: '1px solid rgba(225,72,72,0.30)',
          marginBottom: 22,
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 10 }}>
            <div style={{
              width: 28, height: 28, borderRadius: 8,
              background: 'rgba(225,72,72,0.18)',
              display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            }}>
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
                <path d="M12 9v5M12 17.5v.5" stroke="#E14848" strokeWidth="2.4" strokeLinecap="round"/>
                <path d="M3 19L12 4l9 15z" stroke="#E14848" strokeWidth="1.6" strokeLinejoin="round"/>
              </svg>
            </div>
            <div style={{ fontSize: 13, fontWeight: 700, color: '#FFB3B3' }}>
              Header mancanti: Track URI, Artist Name
            </div>
          </div>
          <div style={{
            background: 'rgba(0,0,0,0.35)', borderRadius: 8, padding: '10px 12px',
            fontFamily: T.MONO, fontSize: 11, color: 'rgba(255,179,179,0.85)', lineHeight: 1.6,
          }}>
            <div style={{ color: T.TEXT_LO2, fontSize: 9.5, letterSpacing: 0.5, textTransform: 'uppercase', marginBottom: 4 }}>STACK</div>
            spotify/csv-parse:42<br/>
            └ MissingHeaderException<br/>
            &nbsp;&nbsp;&nbsp;at Headers.require(line 7)
          </div>
        </div>

        <div style={{ background: T.CARD, borderRadius: 12, padding: '12px 14px', marginBottom: 22 }}>
          <div style={{ fontFamily: T.MONO, fontSize: 10, color: T.TEXT_LO, letterSpacing: 0.8, textTransform: 'uppercase', marginBottom: 6 }}>
            // SUGGERIMENTI
          </div>
          <ul style={{ paddingLeft: 18, margin: 0, color: T.TEXT_LO, fontSize: 12.5, lineHeight: 1.6 }}>
            <li>Verifica di aver esportato da <span style={{ fontFamily: T.MONO, color: T.TEXT_HI }}>Exportify</span>.</li>
            <li>Apri il file e controlla che la prima riga contenga le intestazioni.</li>
            <li>Se il problema persiste, riesporta la playlist.</li>
          </ul>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
          <button style={{
            width: '100%', padding: '14px 16px', background: T.ACCENT, border: 'none',
            borderRadius: 999, color: '#0A0A0A', fontWeight: 700, fontSize: 14,
            cursor: 'pointer', fontFamily: T.FONT,
          }}>
            Riprova
          </button>
          <button style={{
            width: '100%', padding: '12px 14px', background: 'transparent', border: 'none',
            color: T.TEXT_LO, fontWeight: 600, fontSize: 13, cursor: 'pointer', fontFamily: T.FONT,
          }}>
            Scegli un altro file
          </button>
        </div>
      </div>
    </SettingsSubScreen>
  );
}

// ── SP-4 · Confirming step — playlist rename + Annulla
function SpotifyImportConfirming() {
  return (
    <SettingsSubScreen eyebrow="// IMPORTAZIONE" title="Importa da Spotify">
      <div style={{ padding: '0 20px 32px' }}>
        <Stepper step={2}/>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, marginBottom: 10 }}>
          // PASSO 3 / 5 · CONFERMA NOME
        </div>
        <div style={{ fontSize: 20, fontWeight: 800, letterSpacing: -0.4, marginBottom: 8, lineHeight: 1.15 }}>
          Come la chiamiamo?
        </div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, lineHeight: 1.55, marginBottom: 22 }}>
          Puoi rinominare la playlist prima dell'importazione.
        </div>

        {/* OutlinedTextField — Material 3-style with floating label */}
        <div style={{ position: 'relative', marginBottom: 14 }}>
          <div style={{
            border: `1.5px solid ${T.ACCENT}`,
            borderRadius: 12,
            padding: '18px 14px 12px',
            background: 'transparent',
          }}>
            <input
              defaultValue="Slow Hours"
              style={{
                width: '100%', background: 'transparent', border: 'none', outline: 'none',
                color: T.TEXT_HI, fontSize: 16, fontWeight: 600, fontFamily: T.FONT, letterSpacing: -0.2,
              }}
            />
          </div>
          <div style={{
            position: 'absolute', top: -8, left: 12, padding: '0 6px',
            background: T.BG_TOP, fontSize: 11, color: T.ACCENT, fontWeight: 600, fontFamily: T.FONT, letterSpacing: 0.2,
          }}>
            Nome playlist
          </div>
          <div style={{ display: 'flex', justifyContent: 'space-between', padding: '6px 12px 0', fontFamily: T.MONO, fontSize: 10.5, color: T.TEXT_LO2 }}>
            <span>Originale: liked-songs</span>
            <span>10 / 60</span>
          </div>
        </div>

        {/* Summary card */}
        <div style={{ background: T.CARD, borderRadius: 12, padding: '14px 16px', marginBottom: 14 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 12 }}>
            <div style={{ width: 44, height: 44 }}>
              <MHCover kind="wave" palette={{ a:'#5C2D8C', b:'#F0A6B0' }} radius={6}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 600 }}>Slow Hours</div>
              <div style={{ fontSize: 12, color: T.TEXT_LO, fontFamily: T.MONO, marginTop: 2 }}>
                284 brani · da liked-songs.csv
              </div>
            </div>
          </div>
          <div style={{
            display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8,
            paddingTop: 10, borderTop: '1px solid rgba(255,255,255,0.06)',
          }}>
            <Mini label="Brani"   v="284"/>
            <Mini label="Durata"  v="18h"/>
            <Mini label="Artisti" v="142"/>
          </div>
        </div>

        {/* Privacy toggle row */}
        <div style={{
          background: T.CARD, borderRadius: 12, padding: '12px 14px',
          display: 'flex', alignItems: 'center', gap: 12, marginBottom: 26,
        }}>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 13.5, fontWeight: 600, color: T.TEXT_HI }}>Mantieni privata</div>
            <div style={{ fontSize: 11.5, color: T.TEXT_LO, marginTop: 2 }}>Visibile solo a te</div>
          </div>
          <div style={{ width: 36, height: 22, borderRadius: 11, background: T.ACCENT, position: 'relative', flexShrink: 0 }}>
            <div style={{ position: 'absolute', top: 2, right: 2, width: 18, height: 18, borderRadius: 9, background: '#0A0A0A' }}/>
          </div>
        </div>

        {/* Dual CTAs */}
        <div style={{ display: 'flex', gap: 10 }}>
          <button style={{
            flex: 1, padding: '14px 14px', background: 'transparent',
            border: '1px solid rgba(255,255,255,0.18)', borderRadius: 999,
            color: T.TEXT_HI, fontWeight: 600, fontSize: 14, cursor: 'pointer', fontFamily: T.FONT,
          }}>
            Annulla
          </button>
          <button style={{
            flex: 2, padding: '14px 14px', background: T.ACCENT, border: 'none',
            borderRadius: 999, color: '#0A0A0A', fontWeight: 700, fontSize: 14,
            cursor: 'pointer', fontFamily: T.FONT,
          }}>
            Importa 284 brani
          </button>
        </div>
      </div>
    </SettingsSubScreen>
  );
}

function Mini({ label, v }) {
  return (
    <div>
      <div style={{ fontFamily: T.MONO, fontSize: 9.5, color: T.TEXT_LO2, letterSpacing: 0.5, textTransform: 'uppercase' }}>{label}</div>
      <div style={{ fontFamily: T.MONO, fontSize: 14, color: T.TEXT_HI, fontWeight: 600, marginTop: 2 }}>{v}</div>
    </div>
  );
}

// ── SP-5 · Done · pluralized + "Torna alle playlist"
function SpotifyImportDone({ variant = 'plural' }) {
  // variant: 'plural' (279 brani) | 'singular' (1 brano)
  const isPlural = variant === 'plural';
  const added = isPlural ? 279 : 1;
  const downloading = isPlural ? 142 : 1;
  return (
    <SettingsSubScreen eyebrow="// IMPORTAZIONE" title="Importa da Spotify">
      <div style={{ padding: '0 20px 32px' }}>
        <Stepper step={4}/>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, marginBottom: 10 }}>
          // PASSO 5 / 5 · COMPLETATO
        </div>

        <div style={{ display: 'flex', justifyContent: 'center', margin: '12px 0 18px' }}>
          <div style={{
            width: 72, height: 72, borderRadius: 999,
            background: 'rgba(168,224,78,0.15)',
            border: '1px solid rgba(168,224,78,0.30)',
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <I.Check size={36} color={T.ACCENT}/>
          </div>
        </div>

        <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.5, textAlign: 'center', marginBottom: 10, lineHeight: 1.15 }}>
          Importazione completata
        </div>

        {/* Pluralized body — bran<o|i> aggiunt<o|i> · <N> in scaricamento */}
        <div style={{ fontSize: 14, color: T.TEXT_LO, textAlign: 'center', marginBottom: 4, lineHeight: 1.5 }}>
          <span style={{ color: T.TEXT_HI, fontWeight: 600 }}>{added}</span>{' '}
          {isPlural ? 'brani aggiunti' : 'brano aggiunto'} alla libreria
        </div>
        <div style={{ fontSize: 12.5, color: T.ACCENT, textAlign: 'center', marginBottom: 24, fontFamily: T.MONO, letterSpacing: 0.3 }}>
          {downloading} in scaricamento
        </div>

        <div style={{
          background: T.CARD, borderRadius: 14, padding: '16px',
          display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 8, marginBottom: 22,
        }}>
          <Stat label={isPlural ? 'Importati' : 'Importato'} v={String(added)} c={T.ACCENT}/>
          <Stat label="Saltati" v={isPlural ? '5' : '0'} c="#FFC857"/>
          <Stat label="Errori" v="0" c={T.TEXT_LO}/>
        </div>

        {/* Highlight playlist row */}
        <div style={{ background: T.CARD, borderRadius: 12, padding: '12px 14px', display: 'flex', alignItems: 'center', gap: 12, marginBottom: 22 }}>
          <div style={{ width: 44, height: 44, flexShrink: 0 }}>
            <MHCover kind="wave" palette={{ a:'#5C2D8C', b:'#F0A6B0' }} radius={6}/>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 14, fontWeight: 600 }}>Slow Hours</div>
            <div style={{ fontSize: 11.5, color: T.TEXT_LO, fontFamily: T.MONO, marginTop: 2 }}>
              {added} {isPlural ? 'brani' : 'brano'} · creata ora
            </div>
          </div>
          <I.Chevron size={16} color={T.TEXT_LO}/>
        </div>

        {/* Dual CTAs — primary "Apri Slow Hours" + secondary "Torna alle playlist" */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          <button style={{
            width: '100%', padding: '14px', background: T.ACCENT, border: 'none',
            borderRadius: 999, color: '#0A0A0A', fontWeight: 700, fontSize: 14,
            cursor: 'pointer', fontFamily: T.FONT,
          }}>
            Apri Slow Hours
          </button>
          <button style={{
            width: '100%', padding: '12px', background: 'transparent', border: 'none',
            color: T.TEXT_LO, fontWeight: 600, fontSize: 13, cursor: 'pointer', fontFamily: T.FONT,
            display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 6,
          }}>
            ← Torna alle playlist
          </button>
        </div>
      </div>
    </SettingsSubScreen>
  );
}

function Stat({ label, v, c }) {
  return (
    <div style={{ background: 'rgba(255,255,255,0.03)', padding: '10px 8px', borderRadius: 10, textAlign: 'center' }}>
      <div style={{ fontSize: 22, fontWeight: 800, color: c, fontFamily: T.MONO, lineHeight: 1 }}>{v}</div>
      <div style={{ fontSize: 9.5, color: T.TEXT_LO, textTransform: 'uppercase', letterSpacing: 1, fontFamily: T.MONO, marginTop: 4 }}>{label}</div>
    </div>
  );
}

window.MHDiscoverStates = {
  FindIdleEmpty,
  FindIdleActive,
  FindIdlePullRefresh,
  FindSearching,
  FindCandidatesSelected,
  FindTerminalScreen,
  FindLifecycle,
  SpotifyImportIdle,
  SpotifyImportFetching,
  SpotifyImportError,
  SpotifyImportConfirming,
  SpotifyImportDone,
};
