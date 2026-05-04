// MusicHub — Settings sub-pages chrome + sub-screens
const { T, I, MHCover } = window.MH;

// ── Shared sub-screen chrome ────────────────────────────
function SettingsSubScreen({ eyebrow, title, children, footer }) {
  return (
    <div style={{ width: '100%', height: '100%', background: `linear-gradient(180deg, ${T.BG_TOP} 0%, ${T.BG_BOTTOM} 320px)`, color: T.TEXT_HI, fontFamily: T.FONT, display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '60px 16px 20px', display: 'flex', alignItems: 'center', gap: 12 }}>
        <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 4, marginLeft: -4 }}><I.Back/></button>
        <div style={{ flex: 1 }}>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase' }}>{eyebrow}</div>
          <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.5, marginTop: 2 }}>{title}</div>
        </div>
      </div>
      <div style={{ flex: 1, overflowY: 'auto' }}>{children}</div>
      {footer && <div style={{ borderTop: '1px solid rgba(255,255,255,0.06)' }}>{footer}</div>}
    </div>
  );
}

// ── 7.1 Crossfade ───────────────────────────────────────
function CrossfadeScreen() {
  const [v, setV] = React.useState(6);
  return (
    <SettingsSubScreen eyebrow="// IMPOSTAZIONI" title="Crossfade">
      <div style={{ padding: '8px 20px 24px' }}>
        <div style={{ fontSize: 13.5, color: T.TEXT_LO, lineHeight: 1.55, marginBottom: 28 }}>
          Sovrappone le tracce in transizione. Una dissolvenza più lunga è ideale per ambient e mix DJ; spegnila per album che chiedono silenzio fra una traccia e l'altra.
        </div>

        <div style={{ background: T.CARD, borderRadius: 14, padding: '24px 20px' }}>
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 14 }}>
            <div style={{ fontSize: 13, color: T.TEXT_LO, fontFamily: T.MONO, letterSpacing: 1, textTransform: 'uppercase' }}>Durata</div>
            <div style={{ fontSize: 36, fontWeight: 800, letterSpacing: -1, color: T.ACCENT, fontFamily: T.MONO }}>{v}s</div>
          </div>
          {/* slider */}
          <div style={{ position: 'relative', height: 28, marginBottom: 8 }}>
            <div style={{ position: 'absolute', top: 13, left: 0, right: 0, height: 2, background: 'rgba(255,255,255,0.1)', borderRadius: 1 }}/>
            <div style={{ position: 'absolute', top: 13, left: 0, width: `${v/12*100}%`, height: 2, background: T.ACCENT, borderRadius: 1 }}/>
            <div style={{ position: 'absolute', top: 8, left: `calc(${v/12*100}% - 6px)`, width: 12, height: 12, borderRadius: 6, background: T.ACCENT, boxShadow: '0 4px 12px rgba(168,224,78,0.5)' }}/>
            {/* ticks */}
            {[0,2,4,6,8,10,12].map(t => (
              <div key={t} style={{ position: 'absolute', top: 22, left: `calc(${t/12*100}% - 1px)`, fontFamily: T.MONO, fontSize: 9, color: T.TEXT_LO2, transform: 'translateX(-50%)' }}>{t}s</div>
            ))}
          </div>
        </div>

        <div style={{ marginTop: 16, padding: '14px 16px', background: T.CARD, borderRadius: 12, display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 14, fontWeight: 600 }}>Audizione</div>
            <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 2 }}>Riproduce un'anteprima ad ogni cambio valore</div>
          </div>
          <Toggle on={true}/>
        </div>
      </div>
    </SettingsSubScreen>
  );
}

function Toggle({ on }) {
  return (
    <div style={{ width: 44, height: 26, borderRadius: 13, background: on ? T.ACCENT : 'rgba(255,255,255,0.15)', position: 'relative', flexShrink: 0 }}>
      <div style={{ position: 'absolute', top: 3, left: on ? 21 : 3, width: 20, height: 20, borderRadius: 10, background: on ? '#0A0A0A' : '#fff', boxShadow: '0 2px 6px rgba(0,0,0,0.3)' }}/>
    </div>
  );
}

// ── 7.2 Download offline ────────────────────────────────
function DownloadOfflineScreen() {
  const usedMB = 1842, capMB = 8000;
  return (
    <SettingsSubScreen eyebrow="// IMPOSTAZIONI" title="Download offline">
      <div style={{ padding: '8px 20px 32px' }}>
        {/* Storage gauge */}
        <div style={{ background: T.CARD, borderRadius: 14, padding: 18, marginBottom: 16 }}>
          <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 12 }}>
            <div style={{ fontSize: 12, fontFamily: T.MONO, color: T.TEXT_LO, letterSpacing: 1, textTransform: 'uppercase' }}>Spazio usato</div>
            <div style={{ fontSize: 13, color: T.TEXT_LO }}><span style={{ color: T.ACCENT, fontWeight: 700, fontFamily: T.MONO }}>{(usedMB/1000).toFixed(1)}GB</span> / {(capMB/1000)}GB</div>
          </div>
          <div style={{ height: 6, borderRadius: 3, background: 'rgba(255,255,255,0.08)', overflow: 'hidden' }}>
            <div style={{ width: `${usedMB/capMB*100}%`, height: '100%', background: T.ACCENT }}/>
          </div>
          <div style={{ marginTop: 10, fontSize: 12, color: T.TEXT_LO }}>
            <span style={{ color: T.TEXT_HI, fontWeight: 600 }}>284 brani</span> · 12 album · 6 playlist
          </div>
        </div>

        {/* Toggles */}
        <Row title="Solo Wi-Fi" sub="Non scaricare con dati mobili" trailing={<Toggle on={true}/>}/>
        <Row title="Download automatico" sub="Scarica le novità delle playlist sincronizzate appena online. Disattivato per default da v0.12.6 — ti chiediamo conferma prima di occupare spazio." trailing={<Toggle on={false}/>}/>

        <div style={{ height: 16 }}/>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase', padding: '0 4px 8px' }}>// GESTIONE</div>
        <Row title="Riscarica da origine" sub="Forza un nuovo fetch dalla sorgente per i brani con audio difettoso" trailing={<I.Chevron/>}/>
        <Row title="Svuota cache locale" sub="Mantiene i brani scaricati ma cancella i file temporanei" trailing={<I.Chevron/>}/>
        <Row title="Forza rigenerazione Daily Mix" sub="Ricalcola il Daily Mix di domani al prossimo aggiornamento" trailing={<I.Chevron/>}/>

        <div style={{ height: 24 }}/>
        <button style={{ width: '100%', padding: '14px 16px', borderRadius: 12, background: 'rgba(225,72,72,0.1)', border: '1px solid rgba(225,72,72,0.25)', color: '#FF7A7A', fontWeight: 600, fontSize: 14, cursor: 'pointer', fontFamily: T.FONT }}>
          Cancella tutti i download
        </button>
      </div>
    </SettingsSubScreen>
  );
}

function Row({ title, sub, trailing }) {
  return (
    <div style={{ padding: '14px 16px', background: T.CARD, borderRadius: 12, marginBottom: 8, display: 'flex', alignItems: 'center', gap: 14 }}>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 600 }}>{title}</div>
        {sub && <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 3, lineHeight: 1.45 }}>{sub}</div>}
      </div>
      {trailing}
    </div>
  );
}

// ── 7.3 Theme ───────────────────────────────────────────
function ThemeScreen() {
  const [pick, setPick] = React.useState('dark');
  const opts = [
    { id: 'light', label: 'Chiaro', preview: ['#F4F2EC', '#1A1A1A', T.ACCENT_DIM] },
    { id: 'dark', label: 'Scuro', preview: ['#0A0A0A', '#FFFFFF', T.ACCENT] },
    { id: 'system', label: 'Sistema', preview: ['linear-gradient(135deg,#0A0A0A 50%, #F4F2EC 50%)', '#FFFFFF', T.ACCENT] },
  ];
  return (
    <SettingsSubScreen eyebrow="// IMPOSTAZIONI" title="Tema">
      <div style={{ padding: '8px 20px 24px' }}>
        <div style={{ fontSize: 13.5, color: T.TEXT_LO, marginBottom: 24, lineHeight: 1.5 }}>L'app rispetta il tema di sistema per default. Scegli "Chiaro" o "Scuro" per fissarlo.</div>
        <div style={{ display: 'grid', gap: 12 }}>
          {opts.map(o => (
            <button key={o.id} onClick={() => setPick(o.id)} style={{
              padding: 16, borderRadius: 14, border: 'none',
              background: pick === o.id ? 'rgba(168,224,78,0.08)' : T.CARD,
              boxShadow: pick === o.id ? `0 0 0 1.5px ${T.ACCENT} inset` : '0 0 0 1px rgba(255,255,255,0.05) inset',
              display: 'flex', alignItems: 'center', gap: 14, cursor: 'pointer', fontFamily: T.FONT,
            }}>
              <div style={{ width: 56, height: 56, borderRadius: 10, background: o.preview[0], display: 'flex', alignItems: 'flex-end', padding: 6, gap: 4 }}>
                <div style={{ width: 8, height: 8, borderRadius: 4, background: o.preview[2] }}/>
                <div style={{ width: 24, height: 4, borderRadius: 2, background: o.preview[1], opacity: 0.7 }}/>
              </div>
              <div style={{ flex: 1, textAlign: 'left' }}>
                <div style={{ fontSize: 15, fontWeight: 600, color: T.TEXT_HI }}>{o.label}</div>
              </div>
              {pick === o.id && <I.Check size={20} color={T.ACCENT}/>}
            </button>
          ))}
        </div>
      </div>
    </SettingsSubScreen>
  );
}

// ── 7.4 Disliked ────────────────────────────────────────
function DislikedScreen() {
  const [tab, setTab] = React.useState('tracks');
  const tracks = [
    { title: 'Bright Corners', artist: 'The Tessera', kind: 'grid', palette: { a:'#0E1F3A', b: T.ACCENT } },
    { title: 'Hollow Ave', artist: 'Lana Verdier', kind: 'wave', palette: { a:'#5C2D8C', b:'#F0A6B0' } },
    { title: 'Mute the Hour', artist: 'Iso Tide', kind: 'duotone', palette: { a:'#3A0CA3', b:'#F72585' } },
  ];
  const artists = [
    { name: 'The Tessera', kind: 'artist', palette: { bg:'#2A1E12', fg:'#E8DCC4' } },
    { name: 'Iso Tide', kind: 'artist', palette: { bg:'#0E1F3A', fg:'#06B6D4' } },
  ];
  return (
    <SettingsSubScreen eyebrow="// CONSIGLI" title="Non consigliati">
      <div style={{ padding: '0 20px 20px' }}>
        <div style={{ display: 'flex', gap: 8, marginBottom: 18 }}>
          {[['tracks','Brani', tracks.length], ['artists','Artisti', artists.length]].map(([id,l,n]) => {
            const on = tab === id;
            return (
              <button key={id} onClick={() => setTab(id)} style={{
                padding: '8px 14px', borderRadius: 999, cursor: 'pointer', fontFamily: T.FONT,
                background: on ? T.ACCENT : 'rgba(255,255,255,0.06)',
                color: on ? '#0A0A0A' : T.TEXT_HI,
                border: 'none', fontSize: 13, fontWeight: 600,
              }}>{l} · <span style={{ fontFamily: T.MONO, opacity: 0.8 }}>{n}</span></button>
            );
          })}
        </div>

        {tab === 'tracks' && tracks.map((t, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 0', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
            <div style={{ width: 44, height: 44, flexShrink: 0, opacity: 0.5 }}><MHCover kind={t.kind} palette={t.palette} radius={4}/></div>
            <div style={{ flex: 1, minWidth: 0, opacity: 0.7 }}>
              <div style={{ fontSize: 14, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.title}</div>
              <div style={{ fontSize: 12, color: T.TEXT_LO }}>{t.artist}</div>
            </div>
            <button style={{ padding: '6px 12px', borderRadius: 999, border: '1px solid rgba(168,224,78,0.4)', background: 'transparent', color: T.ACCENT, fontWeight: 600, fontSize: 12, cursor: 'pointer', fontFamily: T.FONT }}>Ripristina</button>
          </div>
        ))}
        {tab === 'artists' && artists.map((a, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 0', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
            <div style={{ width: 44, height: 44, flexShrink: 0, borderRadius: 999, overflow: 'hidden', opacity: 0.5 }}><MHCover kind={a.kind} palette={a.palette}/></div>
            <div style={{ flex: 1, minWidth: 0, opacity: 0.7 }}>
              <div style={{ fontSize: 14, fontWeight: 600 }}>{a.name}</div>
              <div style={{ fontSize: 12, color: T.TEXT_LO }}>Artista</div>
            </div>
            <button style={{ padding: '6px 12px', borderRadius: 999, border: '1px solid rgba(168,224,78,0.4)', background: 'transparent', color: T.ACCENT, fontWeight: 600, fontSize: 12, cursor: 'pointer', fontFamily: T.FONT }}>Ripristina</button>
          </div>
        ))}
      </div>
    </SettingsSubScreen>
  );
}

window.MHSettings = { SettingsSubScreen, CrossfadeScreen, DownloadOfflineScreen, ThemeScreen, DislikedScreen };
