// MusicHub — Player surface sheets/dialogs + TrackAction + AddToPlaylist
const { T, I, MHCover, MHPlayingBars } = window.MH;

// ── Bottom sheet shell (mobile) ─────────────────────────
function Sheet({ eyebrow, title, children, height = 0.78, action }) {
  return (
    <div style={{ width: '100%', height: '100%', background: 'rgba(0,0,0,0.55)', display: 'flex', alignItems: 'flex-end', fontFamily: T.FONT }}>
      <div style={{
        width: '100%', height: `${height * 100}%`, borderRadius: '20px 20px 0 0',
        background: '#181818', color: T.TEXT_HI, display: 'flex', flexDirection: 'column',
        boxShadow: '0 -24px 48px -12px rgba(0,0,0,0.6)',
      }}>
        <div style={{ padding: '10px 0 6px', display: 'flex', justifyContent: 'center' }}>
          <div style={{ width: 36, height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.18)' }}/>
        </div>
        <div style={{ padding: '10px 20px 12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
          <div>
            {eyebrow && <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 4 }}>{eyebrow}</div>}
            <div style={{ fontSize: 18, fontWeight: 800, letterSpacing: -0.4 }}>{title}</div>
          </div>
          {action}
        </div>
        <div style={{ flex: 1, overflowY: 'auto' }}>{children}</div>
      </div>
    </div>
  );
}

// ── 3.1 QueueSheet ──────────────────────────────────────
function QueueSheet() {
  const now = { title: 'Undertow', artist: 'Helena Vorr', kind: 'blob', palette: { a:'#1E3A8A', b:'#06B6D4' } };
  const userQ = [
    { title: 'Slow Driver', artist: 'Iso Tide', dur: '4:12', kind: 'duotone', palette: { a:'#3A0CA3', b:'#F72585' } },
    { title: 'Citrine', artist: 'Mira Holt', dur: '3:48', kind: 'wave', palette: { a:'#5C2D8C', b:'#F0A6B0' } },
  ];
  const sysQ = [
    { title: 'Petals', artist: 'Lana Verdier', dur: '2:55', kind: 'arc', palette: { a:'#FF6B5B', b:'#3A1F8A' } },
    { title: 'Burnt Letters', artist: 'The Tessera', dur: '5:01', kind: 'grid', palette: { a:'#0E1F3A', b: T.ACCENT } },
    { title: 'Halflight', artist: 'Iso Tide', dur: '3:30', kind: 'triangles', palette: { bg:'#1A1A1A', fg:'#FF4D2E' } },
    { title: 'Nine Mile', artist: 'Mira Holt', dur: '4:22', kind: 'stripes', palette: { a:'#FFC857', b:'#1A1A1A' } },
  ];
  return (
    <Sheet
      eyebrow="// CODA"
      title="In riproduzione"
      action={
        <div style={{ display: 'flex', gap: 6 }}>
          <BarBtn><I.Shuffle size={18} color={T.ACCENT}/></BarBtn>
          <BarBtn><I.Repeat size={18} color={T.TEXT_LO}/></BarBtn>
          <BarBtn><I.More/></BarBtn>
        </div>
      }
    >
      <div style={{ padding: '0 20px' }}>
        <QRow t={now} now/>
        <SectionLabel>// IN CODA · UTENTE · 2</SectionLabel>
        {userQ.map((t, i) => <QRow key={i} t={t} draggable/>)}
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '14px 4px 6px' }}>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase' }}>// SUCCESSIVI · DA "SLOW HOURS"</div>
          <div style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO2 }}>+12</div>
        </div>
        {sysQ.map((t, i) => <QRow key={i} t={t} draggable/>)}
        <div style={{ height: 80 }}/>
      </div>

      <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, padding: '16px 20px 28px', borderTop: '1px solid rgba(255,255,255,0.05)', background: 'linear-gradient(180deg, rgba(24,24,24,0) 0%, #181818 32%)', display: 'flex', justifyContent: 'center' }}>
        <button style={{ padding: '10px 18px', background: 'rgba(255,255,255,0.06)', border: 'none', borderRadius: 999, color: T.TEXT_HI, fontWeight: 600, fontSize: 13, cursor: 'pointer', fontFamily: T.FONT }}>Cancella coda</button>
      </div>
    </Sheet>
  );
}
function BarBtn({ children }) {
  return <button style={{ width: 36, height: 36, borderRadius: 18, background: 'rgba(255,255,255,0.06)', border: 'none', display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>{children}</button>;
}
function SectionLabel({ children }) {
  return <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase', padding: '14px 4px 6px' }}>{children}</div>;
}
function QRow({ t, now, draggable }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 0' }}>
      <div style={{ width: 44, height: 44, flexShrink: 0, position: 'relative' }}>
        <MHCover kind={t.kind} palette={t.palette} radius={4}/>
        {now && <div style={{ position: 'absolute', inset: 0, background: 'rgba(0,0,0,0.5)', borderRadius: 4, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><MHPlayingBars/></div>}
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 600, color: now ? T.ACCENT : T.TEXT_HI, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.title}</div>
        <div style={{ fontSize: 12, color: T.TEXT_LO, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.artist}{t.dur && ` · ${t.dur}`}</div>
      </div>
      {draggable && (
        <div style={{ padding: 8, opacity: 0.6 }}>
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none">
            <path d="M4 8h16M4 16h16" stroke={T.TEXT_LO} strokeWidth="1.8" strokeLinecap="round"/>
          </svg>
        </div>
      )}
    </div>
  );
}

// ── 3.2 EqualizerSheet ──────────────────────────────────
function EqualizerSheet() {
  const bands = [32, 64, 125, 250, 500, 1000, 2000, 4000, 8000, 16000];
  const values = [3, 2, 0, -1, -2, -1, 1, 3, 4, 2];
  return (
    <Sheet
      eyebrow="// AUDIO"
      title="Equalizzatore"
      height={0.85}
      action={<button style={{ padding: '6px 12px', background: 'rgba(168,224,78,0.12)', border: 'none', borderRadius: 999, color: T.ACCENT, fontWeight: 700, fontSize: 11, fontFamily: T.MONO, cursor: 'pointer' }}>ATTIVO</button>}
    >
      <div style={{ padding: '0 20px 20px' }}>
        {/* Preset */}
        <div style={{ marginBottom: 24, padding: '14px 16px', background: T.CARD, borderRadius: 12, display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{ flex: 1 }}>
            <div style={{ fontFamily: T.MONO, fontSize: 9, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase' }}>Preset</div>
            <div style={{ fontSize: 15, fontWeight: 600, marginTop: 2 }}>Personalizzato</div>
          </div>
          <I.Chevron size={20} color={T.TEXT_LO}/>
        </div>

        {/* Sliders */}
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(10, 1fr)', gap: 4, height: 200, padding: '0 4px', alignItems: 'flex-end' }}>
          {bands.map((b, i) => {
            const v = values[i]; // -12..12
            const norm = (v + 12) / 24; // 0..1
            return (
              <div key={i} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', height: '100%' }}>
                <div style={{ flex: 1, width: 4, background: 'rgba(255,255,255,0.08)', borderRadius: 2, position: 'relative' }}>
                  {/* center line */}
                  <div style={{ position: 'absolute', top: '50%', left: -3, right: -3, height: 1, background: 'rgba(255,255,255,0.06)' }}/>
                  {v >= 0 ? (
                    <div style={{ position: 'absolute', left: 0, right: 0, top: `${100 - norm * 100}%`, bottom: '50%', background: T.ACCENT, borderRadius: 2 }}/>
                  ) : (
                    <div style={{ position: 'absolute', left: 0, right: 0, top: '50%', bottom: `${norm * 100}%`, background: T.ACCENT_DIM, borderRadius: 2 }}/>
                  )}
                  <div style={{ position: 'absolute', left: -4, top: `calc(${100 - norm * 100}% - 5px)`, width: 12, height: 12, borderRadius: 6, background: T.ACCENT, boxShadow: '0 2px 6px rgba(168,224,78,0.5)' }}/>
                </div>
              </div>
            );
          })}
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(10, 1fr)', gap: 4, marginTop: 8, padding: '0 4px' }}>
          {bands.map((b, i) => (
            <div key={i} style={{ textAlign: 'center', fontFamily: T.MONO, fontSize: 9, color: T.TEXT_LO2 }}>
              {b >= 1000 ? `${b / 1000}k` : b}
            </div>
          ))}
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(10, 1fr)', gap: 4, marginTop: 4, padding: '0 4px' }}>
          {values.map((v, i) => (
            <div key={i} style={{ textAlign: 'center', fontFamily: T.MONO, fontSize: 10, color: v > 0 ? T.ACCENT : (v < 0 ? '#888' : T.TEXT_LO2) }}>
              {v > 0 ? `+${v}` : v}
            </div>
          ))}
        </div>

        {/* audio session info */}
        <div style={{ marginTop: 24, padding: '12px 14px', background: 'rgba(255,255,255,0.03)', borderRadius: 10 }}>
          <div style={{ fontFamily: T.MONO, fontSize: 9, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase', marginBottom: 4 }}>// SESSIONE AUDIO</div>
          <div style={{ fontSize: 11.5, color: T.TEXT_LO, fontFamily: T.MONO, lineHeight: 1.6 }}>
            session_id: <span style={{ color: T.TEXT_HI }}>0x4B1F</span><br/>
            output: <span style={{ color: T.TEXT_HI }}>Galaxy Buds Pro · LDAC</span>
          </div>
        </div>
      </div>
    </Sheet>
  );
}

// ── 3.3 Sleep timer popover ─────────────────────────────
function SleepTimerSheet({ active = true }) {
  const opts = [5, 10, 15, 30, 45, 60];
  return (
    <Sheet eyebrow="// TIMER" title="Timer di sospensione" height={0.62}>
      <div style={{ padding: '0 20px 24px' }}>
        {active && (
          <div style={{ background: `linear-gradient(135deg, rgba(168,224,78,0.12) 0%, rgba(168,224,78,0.04) 100%)`, border: '1px solid rgba(168,224,78,0.25)', borderRadius: 14, padding: 18, marginBottom: 22, display: 'flex', alignItems: 'center', gap: 16 }}>
            <div style={{ flex: 1 }}>
              <div style={{ fontFamily: T.MONO, fontSize: 9, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 4 }}>// ATTIVO</div>
              <div style={{ fontSize: 38, fontWeight: 800, letterSpacing: -1.2, fontFamily: T.MONO, color: T.ACCENT, lineHeight: 1 }}>27:14</div>
              <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 4 }}>L'audio si fermerà alle 23:42</div>
            </div>
            <button style={{ padding: '10px 16px', background: 'rgba(255,255,255,0.08)', border: 'none', borderRadius: 999, color: T.TEXT_HI, fontWeight: 600, fontSize: 13, cursor: 'pointer', fontFamily: T.FONT }}>Annulla</button>
          </div>
        )}

        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: 8, marginBottom: 8 }}>
          {opts.map(o => (
            <button key={o} style={{
              padding: '14px 0', borderRadius: 12, border: 'none',
              background: T.CARD, color: T.TEXT_HI, fontFamily: T.FONT, cursor: 'pointer',
            }}>
              <div style={{ fontSize: 22, fontWeight: 800, fontFamily: T.MONO }}>{o}</div>
              <div style={{ fontSize: 10, color: T.TEXT_LO, fontFamily: T.MONO, marginTop: 2 }}>MIN</div>
            </button>
          ))}
        </div>
        <button style={{
          width: '100%', padding: '14px', borderRadius: 12, border: '1px solid rgba(168,224,78,0.3)',
          background: 'rgba(168,224,78,0.06)', color: T.ACCENT, cursor: 'pointer', fontFamily: T.FONT,
          fontSize: 14, fontWeight: 600,
        }}>Fine traccia</button>
      </div>
    </Sheet>
  );
}

// ── 3.5 Playback error dialog ───────────────────────────
function PlaybackErrorDialog() {
  return (
    <div style={{ width: '100%', height: '100%', background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: T.FONT, padding: 24 }}>
      <div style={{ width: '100%', maxWidth: 320, borderRadius: 16, background: '#1A1A1A', border: '1px solid rgba(225,72,72,0.25)', color: T.TEXT_HI, padding: 22 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 14 }}>
          <div style={{ width: 28, height: 28, borderRadius: 999, background: 'rgba(225,72,72,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="M12 8v5M12 17h.01M12 3l10 18H2L12 3z" stroke="#FF7A7A" strokeWidth="1.8" strokeLinejoin="round"/></svg>
          </div>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: '#FF7A7A', textTransform: 'uppercase' }}>// ERRORE PLAYBACK</div>
        </div>
        <div style={{ fontSize: 18, fontWeight: 700, letterSpacing: -0.3, marginBottom: 8 }}>Codec non supportato</div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, lineHeight: 1.55, marginBottom: 14 }}>
          Il file audio scaricato usa un codec che il tuo dispositivo non riproduce. Riscaricalo dalla sorgente per provare un formato compatibile.
        </div>
        <div style={{ padding: '8px 10px', background: '#0A0A0A', borderRadius: 8, marginBottom: 18, display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 9, fontFamily: T.MONO, color: T.TEXT_LO2, letterSpacing: 1 }}>CODE</span>
          <span style={{ fontSize: 11, fontFamily: T.MONO, color: T.TEXT_LO }}>player/codec-unsupported · opus@48k</span>
        </div>
        <div style={{ display: 'flex', gap: 8 }}>
          <button style={{ flex: 1, padding: '11px 8px', background: 'rgba(255,255,255,0.06)', border: 'none', borderRadius: 10, color: T.TEXT_HI, fontWeight: 600, fontSize: 13, cursor: 'pointer', fontFamily: T.FONT }}>Chiudi</button>
          <button style={{ flex: 1, padding: '11px 8px', background: 'rgba(255,255,255,0.06)', border: 'none', borderRadius: 10, color: T.TEXT_HI, fontWeight: 600, fontSize: 13, cursor: 'pointer', fontFamily: T.FONT }}>Riprova</button>
          <button style={{ flex: 1.2, padding: '11px 8px', background: T.ACCENT, border: 'none', borderRadius: 10, color: '#0A0A0A', fontWeight: 700, fontSize: 13, cursor: 'pointer', fontFamily: T.FONT }}>Riscarica</button>
        </div>
      </div>
    </div>
  );
}

// ── 3.6 Report wrong song dialog ────────────────────────
function ReportSongDialog() {
  return (
    <div style={{ width: '100%', height: '100%', background: 'rgba(0,0,0,0.6)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: T.FONT, padding: 24 }}>
      <div style={{ width: '100%', maxWidth: 320, borderRadius: 16, background: '#1A1A1A', border: '1px solid rgba(255,255,255,0.08)', color: T.TEXT_HI, padding: 22 }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: '#FF7A7A', textTransform: 'uppercase', marginBottom: 8 }}>
          // SEGNALA · DEFINITIVO
        </div>
        <div style={{ fontSize: 18, fontWeight: 700, letterSpacing: -0.3, marginBottom: 12 }}>Brano sbagliato?</div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: 10, background: '#0A0A0A', borderRadius: 10, marginBottom: 14 }}>
          <div style={{ width: 36, height: 36, flexShrink: 0 }}><MHCover kind="wave" palette={{ a:'#5C2D8C', b:'#F0A6B0' }} radius={4}/></div>
          <div style={{ minWidth: 0 }}>
            <div style={{ fontSize: 13, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>Citrine</div>
            <div style={{ fontSize: 11, color: T.TEXT_LO }}>Mira Holt</div>
          </div>
        </div>

        <div style={{ fontSize: 13, color: T.TEXT_LO, lineHeight: 1.55, marginBottom: 18 }}>
          Verrà rimosso da ricerca, playlist, brani che ti piacciono e cronologia <b style={{ color: T.TEXT_HI }}>su tutti i tuoi dispositivi</b>. Il match non verrà ri-scaricato in futuro.
        </div>

        <div style={{ display: 'flex', gap: 10 }}>
          <button style={{ flex: 1, padding: '12px 16px', background: 'rgba(255,255,255,0.06)', border: 'none', borderRadius: 999, color: T.TEXT_HI, fontWeight: 600, fontSize: 14, cursor: 'pointer', fontFamily: T.FONT }}>Annulla</button>
          <button style={{ flex: 1, padding: '12px 16px', background: '#E14848', border: 'none', borderRadius: 999, color: '#fff', fontWeight: 700, fontSize: 14, cursor: 'pointer', fontFamily: T.FONT }}>Segnala</button>
        </div>
      </div>
    </div>
  );
}

// ── 3.4 MiniPlayer swipe-to-close ───────────────────────
function MiniPlayerSwipe() {
  // Show the MiniPlayer mid-swipe with a fading trail
  return (
    <div style={{ width: '100%', height: '100%', background: `linear-gradient(180deg, ${T.BG_TOP} 0%, ${T.BG_BOTTOM} 100%)`, color: T.TEXT_HI, fontFamily: T.FONT, position: 'relative', overflow: 'hidden' }}>
      {/* fake content underneath */}
      <div style={{ padding: '64px 16px 16px' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 4 }}>// GESTO</div>
        <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.5 }}>Trascina per chiudere</div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, marginTop: 8, lineHeight: 1.5 }}>Da v0.12.6, scorri il mini-player verso destra (o sinistra) per fermare la riproduzione e rimuoverlo dalla schermata.</div>
      </div>

      {/* Annotation */}
      <div style={{ position: 'absolute', left: 16, right: 16, bottom: 220, display: 'flex', alignItems: 'center', gap: 8, fontFamily: T.MONO, fontSize: 10, fontWeight: 600, color: T.ACCENT, letterSpacing: 1, textTransform: 'uppercase' }}>
        <div style={{ height: 1, background: T.ACCENT, flex: 1 }}/>
        <span>Swipe</span>
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none"><path d="M5 12h14M13 5l7 7-7 7" stroke={T.ACCENT} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/></svg>
        <div style={{ height: 1, background: T.ACCENT, flex: 1 }}/>
      </div>

      {/* trail */}
      <div style={{ position: 'absolute', left: 8, right: 8, bottom: 140, height: 56, borderRadius: 10, opacity: 0.18, background: 'linear-gradient(135deg, #1E3A8A 0%, #06B6D4 100%)', filter: 'blur(8px)', transform: 'translateX(-32px)' }}/>
      <div style={{ position: 'absolute', left: 8, right: 8, bottom: 140, height: 56, borderRadius: 10, opacity: 0.32, background: 'linear-gradient(135deg, #1E3A8A 0%, #06B6D4 100%)', transform: 'translateX(-16px)' }}/>

      {/* mini player offset */}
      <div style={{ position: 'absolute', left: 0, right: 0, bottom: 80, transform: 'translateX(96px)', opacity: 0.95 }}>
        <div style={{ margin: '0 8px', borderRadius: 10, background: 'linear-gradient(135deg, #1E3A8A 0%, #06B6D4 100%)', padding: 1, boxShadow: '0 8px 24px rgba(0,0,0,0.4)' }}>
          <div style={{ background: '#161616', borderRadius: 9, padding: '8px 10px', display: 'flex', alignItems: 'center', gap: 10 }}>
            <div style={{ width: 40, height: 40 }}><MHCover kind="blob" palette={{ a:'#1E3A8A', b:'#06B6D4' }} radius={4}/></div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 600 }}>Undertow</div>
              <div style={{ fontSize: 11.5, color: T.TEXT_LO }}>Helena Vorr · Slow Hours</div>
              <div style={{ marginTop: 5, height: 2, borderRadius: 1, background: 'rgba(255,255,255,0.15)' }}>
                <div style={{ width: '38%', height: '100%', background: T.ACCENT, borderRadius: 1 }}/>
              </div>
            </div>
            <div style={{ width: 36, height: 36, borderRadius: 999, background: T.ACCENT, display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.Pause/></div>
          </div>
        </div>
      </div>

      {/* hint text below */}
      <div style={{ position: 'absolute', left: 0, right: 0, bottom: 36, textAlign: 'center', fontFamily: T.MONO, fontSize: 10, color: T.TEXT_LO2, letterSpacing: 1, textTransform: 'uppercase' }}>
        Rilascia per fermare
      </div>
    </div>
  );
}

// ── 4.1 AddToPlaylistSheet ──────────────────────────────
function AddToPlaylistSheet() {
  const playlists = [
    { name: 'Slow Hours', n: 142, k: 'wave', p: { a:'#5C2D8C', b:'#F0A6B0' } },
    { name: 'Driving — Side A', n: 87, k: 'arc', p: { a:'#FF6B5B', b:'#3A1F8A' } },
    { name: 'Sunday Reset', n: 55, k: 'dot', p: { bg:'#0B3D2E', fg: T.ACCENT } },
    { name: 'Late Studio', n: 38, k: 'duotone', p: { a:'#3A0CA3', b:'#F72585' } },
    { name: 'Echo, Vol. III', n: 24, k: 'type', p: { bg:'#E8E2D5', fg:'#222' } },
    { name: 'Glove Compartment', n: 12, k: 'stripes', p: { a:'#FFC857', b:'#1A1A1A' } },
  ];
  return (
    <Sheet eyebrow="// AGGIUNGI A" title="Le mie playlist" height={0.82}>
      <div style={{ padding: '0 20px 12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 12px', background: 'rgba(255,255,255,0.04)', borderRadius: 10, marginBottom: 14 }}>
          <I.Search size={16} color={T.TEXT_LO}/>
          <div style={{ flex: 1, fontSize: 13, color: T.TEXT_LO2 }}>Cerca playlist…</div>
        </div>
      </div>
      <div style={{ padding: '0 20px' }}>
        {playlists.map((p, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 0' }}>
            <div style={{ width: 48, height: 48 }}><MHCover kind={p.k} palette={p.p} radius={6}/></div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 600 }}>{p.name}</div>
              <div style={{ fontSize: 12, color: T.TEXT_LO, fontFamily: T.MONO }}>{p.n} brani</div>
            </div>
            <div style={{ width: 26, height: 26, borderRadius: 13, border: i === 0 ? 'none' : '1.5px solid rgba(255,255,255,0.18)', background: i === 0 ? T.ACCENT : 'transparent', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
              {i === 0 && <I.Check size={14} color="#0A0A0A"/>}
            </div>
          </div>
        ))}
        <div style={{ height: 80 }}/>
      </div>

      <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, padding: '14px 20px 28px', borderTop: '1px solid rgba(255,255,255,0.06)', background: '#181818' }}>
        <button style={{ width: '100%', padding: '13px 16px', background: 'transparent', border: '1.5px dashed rgba(168,224,78,0.4)', color: T.ACCENT, borderRadius: 12, fontWeight: 600, fontSize: 14, cursor: 'pointer', fontFamily: T.FONT, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
          <I.Plus size={18} color={T.ACCENT}/> Crea nuova playlist
        </button>
      </div>
    </Sheet>
  );
}

// ── 4.2 AddSongsToPlaylistSheet ─────────────────────────
function AddSongsToPlaylistSheet() {
  const tracks = [
    { title: 'Strange Mercy', artist: 'Mira Holt', dur: '3:48', kind: 'wave', palette: { a:'#5C2D8C', b:'#F0A6B0' }, on: true },
    { title: 'Open Window', artist: 'Mira Holt', dur: '2:51', kind: 'wave', palette: { a:'#5C2D8C', b:'#F0A6B0' }, on: true },
    { title: 'Citrine', artist: 'Mira Holt', dur: '4:12', kind: 'wave', palette: { a:'#5C2D8C', b:'#F0A6B0' }, on: false },
    { title: 'Halflight', artist: 'Iso Tide', dur: '3:30', kind: 'triangles', palette: { bg:'#1A1A1A', fg:'#FF4D2E' }, on: true },
    { title: 'Hollow Ave', artist: 'Lana Verdier', dur: '3:01', kind: 'arc', palette: { a:'#FF6B5B', b:'#3A1F8A' }, on: false },
    { title: 'Burnt Letters', artist: 'The Tessera', dur: '5:01', kind: 'grid', palette: { a:'#0E1F3A', b: T.ACCENT }, on: true },
    { title: 'Petals', artist: 'Lana Verdier', dur: '2:55', kind: 'arc', palette: { a:'#FF6B5B', b:'#3A1F8A' }, on: false },
  ];
  const n = tracks.filter(t => t.on).length;
  return (
    <Sheet eyebrow="// AGGIUNGI A · SLOW HOURS" title="Aggiungi brani" height={0.86}>
      <div style={{ padding: '0 20px 12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 12px', background: 'rgba(255,255,255,0.04)', borderRadius: 10, marginBottom: 14 }}>
          <I.Search size={16} color={T.TEXT_LO}/>
          <div style={{ flex: 1, fontSize: 13, color: T.TEXT_HI }}>mira</div>
          <I.X size={16} color={T.TEXT_LO}/>
        </div>
      </div>
      <div style={{ padding: '0 20px' }}>
        {tracks.map((t, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 0' }}>
            <div style={{ width: 22, height: 22, borderRadius: 6, border: t.on ? 'none' : '1.5px solid rgba(255,255,255,0.18)', background: t.on ? T.ACCENT : 'transparent', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
              {t.on && <I.Check size={14} color="#0A0A0A"/>}
            </div>
            <div style={{ width: 44, height: 44, flexShrink: 0 }}><MHCover kind={t.kind} palette={t.palette} radius={4}/></div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.title}</div>
              <div style={{ fontSize: 12, color: T.TEXT_LO }}>{t.artist} · <span style={{ fontFamily: T.MONO }}>{t.dur}</span></div>
            </div>
          </div>
        ))}
        <div style={{ height: 80 }}/>
      </div>

      <div style={{ position: 'absolute', left: 0, right: 0, bottom: 0, padding: '14px 20px 28px', borderTop: '1px solid rgba(255,255,255,0.06)', background: '#181818' }}>
        <button style={{ width: '100%', padding: '13px 16px', background: T.ACCENT, border: 'none', color: '#0A0A0A', borderRadius: 999, fontWeight: 700, fontSize: 14, cursor: 'pointer', fontFamily: T.FONT }}>
          Aggiungi {n} brani
        </button>
      </div>
    </Sheet>
  );
}

// ── 4.3 TrackActionSheet ────────────────────────────────
function TrackActionSheet() {
  const items = [
    { ic: '+queue', l: 'Aggiungi alla coda' },
    { ic: 'next', l: 'Riproduci dopo' },
    { ic: 'plus', l: 'Aggiungi a playlist' },
    { ic: 'heart', l: 'Mi piace', accent: true },
    { ic: 'share', l: 'Condividi' },
    { ic: 'artist', l: "Vai all'artista" },
    { ic: 'album', l: "Vai all'album" },
    { ic: 'download', l: 'Scarica' },
    { ic: 'lyrics', l: 'Mostra testo' },
    { ic: 'video', l: 'Mostra video' },
    { ic: 'sleep', l: 'Timer di sospensione' },
    { divider: true },
    { ic: 'thumb', l: 'Non consigliarmi questo brano', muted: true },
    { ic: 'thumb', l: 'Non consigliarmi questo artista', muted: true },
    { ic: 'flag', l: 'Segnala brano sbagliato', danger: true },
  ];
  const Icon = ({ k, color }) => {
    const c = color || T.TEXT_LO;
    const props = { width: 20, height: 20, viewBox: '0 0 24 24', fill: 'none', stroke: c, strokeWidth: 1.7, strokeLinecap: 'round', strokeLinejoin: 'round' };
    switch (k) {
      case '+queue': return <svg {...props}><path d="M3 6h12M3 12h12M3 18h8M17 14v6M14 17h6"/></svg>;
      case 'next': return <svg {...props}><path d="M5 5v14l8-7zM13 5v14l8-7z"/></svg>;
      case 'plus': return <svg {...props}><path d="M12 5v14M5 12h14"/></svg>;
      case 'heart': return <svg {...props} fill={color || T.ACCENT}><path d="M12 21s-7-4.5-9.5-9A5 5 0 0112 6.5 5 5 0 0121.5 12c-2.5 4.5-9.5 9-9.5 9z"/></svg>;
      case 'share': return <svg {...props}><circle cx="6" cy="12" r="3"/><circle cx="18" cy="6" r="3"/><circle cx="18" cy="18" r="3"/><path d="M8.5 10.5l7-3M8.5 13.5l7 3"/></svg>;
      case 'artist': return <svg {...props}><circle cx="12" cy="9" r="4"/><path d="M4 21c1-4 4-6 8-6s7 2 8 6"/></svg>;
      case 'album': return <svg {...props}><circle cx="12" cy="12" r="9"/><circle cx="12" cy="12" r="2"/></svg>;
      case 'download': return <svg {...props}><path d="M12 4v12M6 12l6 6 6-6M4 21h16"/></svg>;
      case 'lyrics': return <svg {...props}><path d="M5 5h14v10l-4 4H5z M9 9h6 M9 13h4"/></svg>;
      case 'video': return <svg {...props}><rect x="3" y="6" width="14" height="12" rx="2"/><path d="M17 10l4-2v8l-4-2z"/></svg>;
      case 'sleep': return <svg {...props}><path d="M12 3a9 9 0 109 9 7 7 0 01-9-9z"/></svg>;
      case 'thumb': return <svg {...props}><path d="M14 9V5a3 3 0 00-3-3l-4 9v11h11.3a2 2 0 002-1.7L22 11a2 2 0 00-2-2zM7 22H4a2 2 0 01-2-2v-7a2 2 0 012-2h3"/></svg>;
      case 'flag': return <svg {...props}><path d="M4 21V4M4 5h13l-2 4 2 4H4"/></svg>;
      default: return null;
    }
  };
  return (
    <Sheet
      eyebrow="// AZIONI"
      title={<span>Citrine <span style={{ color: T.TEXT_LO, fontWeight: 500, fontSize: 14 }}>· Mira Holt</span></span>}
      height={0.92}
    >
      <div style={{ padding: '0 8px 24px' }}>
        {items.map((it, i) => {
          if (it.divider) return <div key={i} style={{ height: 1, background: 'rgba(255,255,255,0.06)', margin: '8px 12px' }}/>;
          const c = it.danger ? '#FF7A7A' : (it.muted ? T.TEXT_LO : T.TEXT_HI);
          return (
            <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 16, padding: '12px 16px', borderRadius: 10, cursor: 'pointer' }}>
              <Icon k={it.ic} color={it.accent ? T.ACCENT : (it.danger ? '#FF7A7A' : T.TEXT_LO)}/>
              <div style={{ fontSize: 15, fontWeight: 500, color: c }}>{it.l}</div>
            </div>
          );
        })}
      </div>
    </Sheet>
  );
}

window.MHPlayerSheets = {
  QueueSheet, EqualizerSheet, SleepTimerSheet, PlaybackErrorDialog,
  ReportSongDialog, MiniPlayerSwipe, AddToPlaylistSheet, AddSongsToPlaylistSheet,
  TrackActionSheet,
};
