// MusicHub — Discover (Find / Spotify import) + Library drill-downs
const { T, I, MHCover } = window.MH;
const { SettingsSubScreen } = window.MHSettings;

// ── 2.1 FindScreen ──────────────────────────────────────
function FindScreen({ state = 'results' }) {
  const candidates = [
    { title: 'Strange Mercy', artist: 'Mira Holt', dur: '3:48', src: 'YouTube', match: 'add' },
    { title: 'Open Window', artist: 'Mira Holt', dur: '2:51', src: 'YouTube', match: 'queued' },
    { title: 'Quiet Field — live', artist: 'Mira Holt · KEXP', dur: '4:22', src: 'YouTube', match: 'add' },
    { title: 'Open Window — demo', artist: 'Mira Holt', dur: '2:31', src: 'YouTube', match: 'add' },
  ];
  return (
    <div style={{ width: '100%', height: '100%', background: `linear-gradient(180deg, ${T.BG_TOP} 0%, ${T.BG_BOTTOM} 320px)`, color: T.TEXT_HI, fontFamily: T.FONT, display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '60px 16px 12px', display: 'flex', alignItems: 'center', gap: 12 }}>
        <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 4, marginLeft: -4 }}><I.Back/></button>
        <div style={{ flex: 1 }}>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase' }}>// SCOPRI · YT</div>
          <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.5, marginTop: 2 }}>Trova brani</div>
        </div>
      </div>

      {/* search field */}
      <div style={{ padding: '8px 16px 14px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '12px 14px', background: T.CARD, borderRadius: 12 }}>
          <I.Search size={18} color={T.TEXT_LO}/>
          <input defaultValue="Mira Holt" style={{ flex: 1, background: 'transparent', border: 'none', outline: 'none', color: T.TEXT_HI, fontSize: 14, fontFamily: T.FONT }}/>
          <I.X size={18} color={T.TEXT_LO}/>
        </div>
      </div>

      {state === 'error' && (
        <div style={{ margin: '0 16px 12px', padding: '12px 14px', background: 'rgba(225,72,72,0.1)', border: '1px solid rgba(225,72,72,0.3)', borderRadius: 12, display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 13, fontWeight: 600, color: '#FFB3B3' }}>Connessione non riuscita</div>
            <div style={{ fontSize: 11.5, color: 'rgba(255,179,179,0.7)', marginTop: 2 }}>Verifica la rete e riprova</div>
          </div>
          <button style={{ padding: '6px 12px', background: 'rgba(255,255,255,0.08)', border: 'none', borderRadius: 999, color: T.TEXT_HI, fontWeight: 600, fontSize: 12, cursor: 'pointer', fontFamily: T.FONT }}>Riprova</button>
        </div>
      )}

      <div style={{ flex: 1, overflowY: 'auto', padding: '0 16px 32px' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase', padding: '4px 4px 12px' }}>// 4 RISULTATI · YT MATCH</div>

        {candidates.map((c, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 0', borderBottom: '1px solid rgba(255,255,255,0.05)' }}>
            <div style={{ width: 48, height: 48, flexShrink: 0 }}>
              <MHCover kind={['arc','wave','triangles','blob'][i%4]} palette={[{a:'#5C2D8C',b:'#F0A6B0'},{a:'#1E3A8A',b:'#06B6D4'},{bg:'#1A1A1A',fg:'#FF4D2E'},{a:'#3A0CA3',b:'#F72585'}][i%4]} radius={4}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{c.title}</div>
              <div style={{ fontSize: 12, color: T.TEXT_LO, display: 'flex', alignItems: 'center', gap: 6, marginTop: 2 }}>
                <span style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{c.artist}</span>
                <span>·</span>
                <span style={{ fontFamily: T.MONO }}>{c.dur}</span>
                <span style={{ padding: '1px 6px', background: 'rgba(255,77,46,0.15)', color: '#FF7A5C', borderRadius: 4, fontSize: 9, fontWeight: 700, letterSpacing: 0.5 }}>{c.src}</span>
              </div>
            </div>
            {c.match === 'add' ? (
              <button style={{ padding: '7px 14px', borderRadius: 999, border: 'none', background: T.ACCENT, color: '#0A0A0A', fontWeight: 700, fontSize: 12, cursor: 'pointer', fontFamily: T.FONT }}>Aggiungi</button>
            ) : (
              <div style={{ padding: '7px 12px', borderRadius: 999, background: 'rgba(168,224,78,0.12)', color: T.ACCENT, fontWeight: 600, fontSize: 11, fontFamily: T.MONO, display: 'flex', alignItems: 'center', gap: 4 }}>
                <I.Check size={12} color={T.ACCENT}/>In coda
              </div>
            )}
          </div>
        ))}

        {/* Loading skeleton row */}
        <div style={{ marginTop: 24, fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase', padding: '4px 4px 8px' }}>// CARICAMENTO</div>
        {[1,2].map(i => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 0', opacity: 0.5 }}>
            <div style={{ width: 48, height: 48, borderRadius: 4, background: 'rgba(255,255,255,0.06)', flexShrink: 0 }}/>
            <div style={{ flex: 1 }}>
              <div style={{ height: 12, width: '60%', borderRadius: 3, background: 'rgba(255,255,255,0.08)', marginBottom: 6 }}/>
              <div style={{ height: 10, width: '40%', borderRadius: 3, background: 'rgba(255,255,255,0.05)' }}/>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ── 2.2 SpotifyImportScreen ─────────────────────────────
function SpotifyImportScreen({ step = 'preview' }) {
  return (
    <SettingsSubScreen eyebrow="// IMPORTAZIONE" title="Importa da Spotify">
      <div style={{ padding: '0 20px 32px' }}>
        {/* Stepper */}
        <div style={{ display: 'flex', gap: 6, marginBottom: 22 }}>
          {['inst','file','prev','sync','done'].map((s, i) => {
            const done = ['inst','file','prev','sync','done'].indexOf(step === 'preview' ? 'prev' : step === 'progress' ? 'sync' : step) >= i;
            return <div key={i} style={{ flex: 1, height: 3, borderRadius: 1.5, background: done ? T.ACCENT : 'rgba(255,255,255,0.1)' }}/>;
          })}
        </div>

        {step === 'instructions' && (
          <div>
            <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, marginBottom: 10 }}>// PASSO 1 / 5 · ESPORTA CSV</div>
            <div style={{ fontSize: 18, fontWeight: 700, letterSpacing: -0.3, marginBottom: 14 }}>Esporta da Exportify</div>
            <ol style={{ paddingLeft: 18, margin: 0, color: T.TEXT_LO, fontSize: 13.5, lineHeight: 1.7 }}>
              <li>Apri <span style={{ color: T.ACCENT, fontFamily: T.MONO }}>watsonbox.github.io/exportify</span> e accedi con Spotify.</li>
              <li>Per ogni playlist, premi <b style={{ color: T.TEXT_HI }}>Export</b> per scaricare il CSV.</li>
              <li>Torna qui e seleziona il file dal tuo dispositivo.</li>
            </ol>
            <button style={{ marginTop: 18, padding: '10px 14px', background: 'rgba(255,255,255,0.06)', border: 'none', borderRadius: 10, color: T.TEXT_HI, fontWeight: 600, fontSize: 13, cursor: 'pointer', fontFamily: T.FONT }}>Apri Exportify ↗</button>
          </div>
        )}

        {step === 'preview' && (
          <div>
            <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, marginBottom: 10 }}>// PASSO 3 / 5 · ANTEPRIMA</div>
            <div style={{ fontSize: 16, fontWeight: 700, letterSpacing: -0.2, marginBottom: 4 }}>liked-songs.csv</div>
            <div style={{ fontSize: 12, color: T.TEXT_LO, fontFamily: T.MONO, marginBottom: 18 }}>284 brani · 3 playlist rilevate</div>

            {[
              { name: 'Slow Hours', owner: 'luca.r', n: 142, k: 'wave', p: { a:'#5C2D8C', b:'#F0A6B0' } },
              { name: 'Driving — Side A', owner: 'luca.r', n: 87, k: 'arc', p: { a:'#FF6B5B', b:'#3A1F8A' } },
              { name: 'Sunday Reset', owner: 'luca.r', n: 55, k: 'dot', p: { bg:'#0B3D2E', fg: T.ACCENT } },
            ].map((p, i) => (
              <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '12px 14px', background: T.CARD, borderRadius: 12, marginBottom: 8 }}>
                <div style={{ width: 44, height: 44 }}><MHCover kind={p.k} palette={p.p} radius={6}/></div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 14, fontWeight: 600 }}>{p.name}</div>
                  <div style={{ fontSize: 12, color: T.TEXT_LO, fontFamily: T.MONO }}>{p.n} brani · {p.owner}</div>
                </div>
                <div style={{ width: 22, height: 22, borderRadius: 6, background: T.ACCENT, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.Check size={14} color="#0A0A0A"/></div>
              </div>
            ))}

            <button style={{ width: '100%', marginTop: 18, padding: '14px 16px', background: T.ACCENT, border: 'none', borderRadius: 999, color: '#0A0A0A', fontWeight: 700, fontSize: 14, cursor: 'pointer', fontFamily: T.FONT }}>
              Importa 284 brani
            </button>
          </div>
        )}

        {step === 'progress' && (
          <div>
            <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, marginBottom: 10 }}>// PASSO 4 / 5 · CORRISPONDENZA</div>
            <div style={{ fontSize: 18, fontWeight: 700, marginBottom: 18 }}>Sto cercando i brani su YouTube…</div>
            <div style={{ background: T.CARD, borderRadius: 14, padding: 18 }}>
              <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 10 }}>
                <div style={{ fontSize: 13, color: T.TEXT_LO }}>Mira Holt — Strange Mercy</div>
                <div style={{ fontSize: 12, fontFamily: T.MONO, color: T.ACCENT }}>147 / 284</div>
              </div>
              <div style={{ height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.08)' }}>
                <div style={{ width: '52%', height: '100%', background: T.ACCENT, borderRadius: 2 }}/>
              </div>
              <div style={{ marginTop: 18, display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 10, fontFamily: T.MONO, fontSize: 11 }}>
                <Stat label="Trovati" v="142" c={T.ACCENT}/>
                <Stat label="Approx" v="3" c="#FFC857"/>
                <Stat label="Saltati" v="2" c={T.TEXT_LO}/>
              </div>
            </div>
          </div>
        )}

        {step === 'done' && (
          <div>
            <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, marginBottom: 10 }}>// PASSO 5 / 5 · COMPLETATO</div>
            <div style={{ display: 'flex', justifyContent: 'center', margin: '12px 0 18px' }}>
              <div style={{ width: 64, height: 64, borderRadius: 999, background: 'rgba(168,224,78,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
                <I.Check size={32} color={T.ACCENT}/>
              </div>
            </div>
            <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.5, textAlign: 'center', marginBottom: 8 }}>Importazione completata</div>
            <div style={{ fontSize: 13, color: T.TEXT_LO, textAlign: 'center', marginBottom: 22 }}>3 playlist · 279 brani · 5 saltati</div>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3,1fr)', gap: 10, marginBottom: 22 }}>
              <Stat label="Importati" v="279" c={T.ACCENT}/>
              <Stat label="Saltati" v="5" c="#FFC857"/>
              <Stat label="Errori" v="0" c={T.TEXT_LO}/>
            </div>
            <button style={{ width: '100%', padding: '14px', background: T.ACCENT, border: 'none', borderRadius: 999, color: '#0A0A0A', fontWeight: 700, fontSize: 14, cursor: 'pointer', fontFamily: T.FONT }}>Apri Slow Hours</button>
          </div>
        )}
      </div>
    </SettingsSubScreen>
  );
}
function Stat({ label, v, c }) {
  return (
    <div style={{ background: T.CARD, padding: 12, borderRadius: 10, textAlign: 'center' }}>
      <div style={{ fontSize: 22, fontWeight: 800, color: c, fontFamily: T.MONO }}>{v}</div>
      <div style={{ fontSize: 10, color: T.TEXT_LO, textTransform: 'uppercase', letterSpacing: 1, fontFamily: T.MONO, marginTop: 2 }}>{label}</div>
    </div>
  );
}

window.MHDiscover = { FindScreen, SpotifyImportScreen };
