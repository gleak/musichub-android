// MusicHub — App update channel + Queued events
const { T, I, MHCover, MHScreen } = window.MH;
const { SettingsSubScreen } = window.MHSettings;

// ── 8.1 AppUpdateBanner — three states stacked over Home preview
function AppUpdateBannerHome({ state = 'available' }) {
  let banner;
  if (state === 'available') {
    banner = (
      <div style={{ margin: '0 16px 14px', borderRadius: 14, background: `linear-gradient(135deg, rgba(168,224,78,0.16) 0%, rgba(168,224,78,0.04) 100%)`, border: '1px solid rgba(168,224,78,0.3)', padding: '12px 14px', display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ width: 36, height: 36, borderRadius: 10, background: T.ACCENT, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M12 4v12M6 12l6 6 6-6M4 21h16" stroke="#0A0A0A" strokeWidth="2.2" strokeLinecap="round" strokeLinejoin="round"/></svg>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontFamily: T.MONO, fontSize: 9, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase' }}>// AGGIORNAMENTO</div>
          <div style={{ fontSize: 13.5, fontWeight: 600, marginTop: 2 }}>v0.13.1 → <span style={{ color: T.ACCENT, fontFamily: T.MONO }}>v0.14.0</span></div>
        </div>
        <button style={{ padding: '8px 14px', background: T.ACCENT, border: 'none', borderRadius: 999, color: '#0A0A0A', fontWeight: 700, fontSize: 12, cursor: 'pointer', fontFamily: T.FONT }}>Installa</button>
        <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 4 }}><I.X size={18} color={T.TEXT_LO}/></button>
      </div>
    );
  } else if (state === 'progress') {
    banner = (
      <div style={{ margin: '0 16px 14px', borderRadius: 14, background: T.CARD, border: '1px solid rgba(255,255,255,0.06)', padding: '12px 14px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 8 }}>
          <div style={{ width: 36, height: 36, borderRadius: 10, background: 'rgba(168,224,78,0.14)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
            <Spinner/>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontFamily: T.MONO, fontSize: 9, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase' }}>// SCARICAMENTO APK</div>
            <div style={{ fontSize: 13.5, fontWeight: 600, marginTop: 2 }}>v0.14.0 · <span style={{ fontFamily: T.MONO, color: T.TEXT_LO }}>4.2 / 12.8 MB</span></div>
          </div>
          <div style={{ fontSize: 16, fontFamily: T.MONO, fontWeight: 800, color: T.ACCENT }}>33%</div>
        </div>
        <div style={{ height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.08)' }}>
          <div style={{ width: '33%', height: '100%', background: T.ACCENT, borderRadius: 2 }}/>
        </div>
      </div>
    );
  } else if (state === 'failed') {
    banner = (
      <div style={{ margin: '0 16px 14px', borderRadius: 14, background: 'rgba(225,72,72,0.08)', border: '1px solid rgba(225,72,72,0.3)', padding: '12px 14px', display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ width: 36, height: 36, borderRadius: 10, background: 'rgba(225,72,72,0.18)', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M12 8v5M12 17h.01M12 3l10 18H2L12 3z" stroke="#FF7A7A" strokeWidth="1.8" strokeLinejoin="round"/></svg>
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontFamily: T.MONO, fontSize: 9, fontWeight: 600, letterSpacing: 1.5, color: '#FF7A7A', textTransform: 'uppercase' }}>// AGGIORNAMENTO FALLITO</div>
          <div style={{ fontSize: 13.5, fontWeight: 600, marginTop: 2 }}>Scaricamento interrotto</div>
        </div>
        <button style={{ padding: '8px 14px', background: 'rgba(255,255,255,0.08)', border: 'none', borderRadius: 999, color: T.TEXT_HI, fontWeight: 600, fontSize: 12, cursor: 'pointer', fontFamily: T.FONT }}>Riprova</button>
      </div>
    );
  }

  // Home page mock background
  return (
    <MHScreen navActive="home">
      <div style={{ padding: '0 16px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <div>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase' }}>// MARTEDÌ POMERIGGIO</div>
          <div style={{ fontSize: 24, fontWeight: 800, letterSpacing: -0.5, marginTop: 2 }}>Bentornato, Luca</div>
        </div>
        <div style={{ display: 'flex', gap: 6 }}>
          <button style={{ width: 36, height: 36, borderRadius: 999, background: T.CARD, border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.History/></button>
          <button style={{ width: 36, height: 36, borderRadius: 999, background: T.CARD, border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.Settings/></button>
        </div>
      </div>

      <div style={{ padding: '14px 0 0' }}>{banner}</div>

      <div style={{ padding: '0 16px' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 8 }}>// CONTINUA AD ASCOLTARE</div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 10 }}>
          {[
            { k: 'wave', p: { a:'#5C2D8C', b:'#F0A6B0' }, t: 'Slow Hours' },
            { k: 'arc', p: { a:'#FF6B5B', b:'#3A1F8A' }, t: 'Driving · A' },
            { k: 'grid', p: { a:'#0E1F3A', b: T.ACCENT }, t: 'Casa · Cena' },
            { k: 'duotone', p: { a:'#3A0CA3', b:'#F72585' }, t: 'Mi piace' },
          ].map((c, i) => (
            <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 10, background: T.CARD, borderRadius: 8 }}>
              <div style={{ width: 48, height: 48, flexShrink: 0 }}><MHCover kind={c.k} palette={c.p} radius={8}/></div>
              <div style={{ fontSize: 12, fontWeight: 600, paddingRight: 6, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{c.t}</div>
            </div>
          ))}
        </div>
      </div>
    </MHScreen>
  );
}

function Spinner() {
  return (
    <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
      <circle cx="12" cy="12" r="9" stroke="rgba(168,224,78,0.25)" strokeWidth="2.4"/>
      <path d="M21 12a9 9 0 00-9-9" stroke={T.ACCENT} strokeWidth="2.4" strokeLinecap="round"/>
    </svg>
  );
}

// ── 8.2 ChangelogSheet ──────────────────────────────────
function ChangelogSheet() {
  const entries = [
    { t: 'Audio nitido come mai prima', d: 'Nuovo decoder Opus a 96kHz; le tracce ad alta risoluzione suonano più aperte.' },
    { t: 'Mini-player a portata di pollice', d: 'Trascina di lato per fermare la riproduzione e nasconderlo.' },
    { t: 'Errori di playback più chiari', d: 'Niente più toast: ora un dialogo ti dice cosa fare (riprova / riscarica).' },
    { t: 'Per te, ma davvero per te', d: 'Daily Mix usa anche i tuoi "non consigliarmi" per affinare le scelte.' },
  ];
  return (
    <div style={{ width: '100%', height: '100%', background: 'rgba(0,0,0,0.55)', display: 'flex', alignItems: 'flex-end', fontFamily: T.FONT }}>
      <div style={{ width: '100%', height: '90%', borderRadius: '20px 20px 0 0', background: '#181818', color: T.TEXT_HI, display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '10px 0 6px', display: 'flex', justifyContent: 'center' }}>
          <div style={{ width: 36, height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.18)' }}/>
        </div>

        {/* Hero block */}
        <div style={{ padding: '20px 24px 24px', background: `linear-gradient(180deg, rgba(168,224,78,0.16) 0%, rgba(24,24,24,0) 100%)` }}>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 8 }}>
            // NOVITÀ · v0.14.0
          </div>
          <div style={{ fontSize: 28, fontWeight: 800, letterSpacing: -0.7, lineHeight: 1.1 }}>
            Quattro cose<br/>che ti piaceranno.
          </div>
          <div style={{ marginTop: 10, fontSize: 12, color: T.TEXT_LO, fontFamily: T.MONO }}>
            <span style={{ color: T.TEXT_LO2 }}>v0.13.1 →</span> <span style={{ color: T.ACCENT }}>v0.14.0</span>
          </div>
        </div>

        <div style={{ flex: 1, overflowY: 'auto', padding: '12px 24px 0' }}>
          {entries.map((e, i) => (
            <div key={i} style={{ padding: '14px 0', borderBottom: i < entries.length - 1 ? '1px solid rgba(255,255,255,0.06)' : 'none', display: 'flex', gap: 14 }}>
              <div style={{ fontFamily: T.MONO, fontSize: 13, fontWeight: 700, color: T.ACCENT, width: 18, flexShrink: 0, paddingTop: 1 }}>{String(i + 1).padStart(2, '0')}</div>
              <div style={{ flex: 1, minWidth: 0 }}>
                <div style={{ fontSize: 16, fontWeight: 700, letterSpacing: -0.2, marginBottom: 4 }}>{e.t}</div>
                <div style={{ fontSize: 13, color: T.TEXT_LO, lineHeight: 1.5 }}>{e.d}</div>
              </div>
            </div>
          ))}
          {/* Pager dots */}
          <div style={{ padding: '20px 0 4px', display: 'flex', justifyContent: 'center', gap: 6 }}>
            <div style={{ width: 18, height: 4, borderRadius: 2, background: T.ACCENT }}/>
            <div style={{ width: 4, height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.2)' }}/>
            <div style={{ width: 4, height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.2)' }}/>
          </div>
        </div>

        <div style={{ padding: '12px 24px 36px', borderTop: '1px solid rgba(255,255,255,0.05)' }}>
          <button style={{ width: '100%', padding: '14px', background: T.ACCENT, border: 'none', borderRadius: 999, color: '#0A0A0A', fontWeight: 700, fontSize: 14, cursor: 'pointer', fontFamily: T.FONT }}>Continua</button>
        </div>
      </div>
    </div>
  );
}

// ── 8.3 Eventi in coda diagnostic ───────────────────────
function QueuedEventsScreen() {
  const events = [
    { t: 'Mi piace · Citrine', sub: 'Mira Holt', n: 1, ic: 'heart' },
    { t: 'Segui artista · Iso Tide', sub: '', n: 1, ic: 'user' },
    { t: 'Riproduzioni', sub: 'Tre tracce ascoltate offline', n: 3, ic: 'play' },
    { t: 'Non consigliarmi · Hollow Ave', sub: 'Lana Verdier', n: 1, ic: 'thumb' },
    { t: 'Aggiunta a playlist · Slow Hours', sub: '+2 brani', n: 2, ic: 'plus' },
  ];
  const total = events.reduce((s, e) => s + e.n, 0);
  return (
    <SettingsSubScreen eyebrow="// PROFILO · DIAGNOSTICA" title="Eventi in coda">
      <div style={{ padding: '0 20px 24px' }}>
        <div style={{ background: T.CARD, borderRadius: 14, padding: 18, marginBottom: 18 }}>
          <div style={{ fontFamily: T.MONO, fontSize: 9, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase', marginBottom: 10 }}>// IN ATTESA DI SYNC</div>
          <div style={{ display: 'flex', alignItems: 'baseline', gap: 8 }}>
            <div style={{ fontSize: 56, fontWeight: 800, letterSpacing: -2, fontFamily: T.MONO, color: T.ACCENT, lineHeight: 1 }}>{total}</div>
            <div style={{ fontSize: 14, color: T.TEXT_LO, marginBottom: 6 }}>eventi pronti</div>
          </div>
          <div style={{ marginTop: 14, fontSize: 12, color: T.TEXT_LO, lineHeight: 1.55 }}>
            Le azioni offline (mi piace, follow, riproduzioni) si svuotano da sole quando torni online. Nessun dato perso.
          </div>
        </div>

        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase', padding: '4px 4px 8px' }}>// DETTAGLIO</div>
        {events.map((e, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 0', borderBottom: '1px solid rgba(255,255,255,0.06)' }}>
            <EventIcon k={e.ic}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13.5, fontWeight: 600 }}>{e.t}</div>
              {e.sub && <div style={{ fontSize: 11.5, color: T.TEXT_LO, marginTop: 1 }}>{e.sub}</div>}
            </div>
            <div style={{ padding: '3px 9px', background: 'rgba(168,224,78,0.12)', color: T.ACCENT, fontFamily: T.MONO, fontSize: 11, fontWeight: 700, borderRadius: 999 }}>×{e.n}</div>
          </div>
        ))}

        <div style={{ marginTop: 18, padding: '10px 14px', background: 'rgba(255,255,255,0.03)', borderRadius: 10, display: 'flex', alignItems: 'center', gap: 10 }}>
          <Spinner/>
          <div style={{ fontSize: 12, color: T.TEXT_LO, fontFamily: T.MONO }}>Prossimo flush automatico tra <span style={{ color: T.ACCENT }}>00:42</span></div>
        </div>
      </div>
    </SettingsSubScreen>
  );
}

function EventIcon({ k }) {
  const props = { width: 18, height: 18, viewBox: '0 0 24 24', fill: 'none', stroke: T.ACCENT, strokeWidth: 1.7, strokeLinecap: 'round', strokeLinejoin: 'round' };
  let inner;
  switch (k) {
    case 'heart': inner = <path d="M12 21s-7-4.5-9.5-9A5 5 0 0112 6.5 5 5 0 0121.5 12c-2.5 4.5-9.5 9-9.5 9z" fill={T.ACCENT}/>; break;
    case 'user': inner = <><circle cx="12" cy="9" r="4"/><path d="M4 21c1-4 4-6 8-6s7 2 8 6"/></>; break;
    case 'play': inner = <path d="M7 5v14l12-7z" fill={T.ACCENT}/>; break;
    case 'thumb': inner = <path d="M14 9V5a3 3 0 00-3-3l-4 9v11h11.3a2 2 0 002-1.7L22 11a2 2 0 00-2-2zM7 22H4a2 2 0 01-2-2v-7a2 2 0 012-2h3"/>; break;
    case 'plus': inner = <path d="M12 5v14M5 12h14"/>; break;
    default: inner = null;
  }
  return (
    <div style={{ width: 36, height: 36, flexShrink: 0, borderRadius: 10, background: 'rgba(168,224,78,0.1)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
      <svg {...props}>{inner}</svg>
    </div>
  );
}

window.MHUpdate = { AppUpdateBannerHome, ChangelogSheet, QueuedEventsScreen };
