// MusicHub — "Per te" hub screen + Generated playlist detail
const { T, I, MHCover, MHPlayingBars, MHLogo, MHPlayerBar, MHBottomNav, MHScreen, MHSectionHeader } = window.MH;
const { GEN_PLAYLISTS, FAMILY_LABELS, TRACKS_FOR_GEN, GenCover, GenCardLarge, GenCardSmall } = window.MHGenerated;

// ─────────────────────────────────────────────────────────
// PER TE — Hub di tutte le playlist generate dal sistema
// ─────────────────────────────────────────────────────────
function ForYouScreen() {
  const rotation = GEN_PLAYLISTS.find(p => p.id === 'rotation');
  const mixes = GEN_PLAYLISTS.filter(p => p.family === 'daily');
  const releases = GEN_PLAYLISTS.find(p => p.id === 'releases');
  const radar = GEN_PLAYLISTS.find(p => p.id === 'radar');
  const capsule = GEN_PLAYLISTS.find(p => p.id === 'capsule');
  const mood = GEN_PLAYLISTS.find(p => p.id === 'mood');
  const next = GEN_PLAYLISTS.find(p => p.id === 'next');

  return (
    <MHScreen navActive="foryou" gradient={`linear-gradient(180deg, #1A2010 0%, ${T.BG_BOTTOM} 320px)`}>
      {/* Header */}
      <div style={{ padding: '8px 16px 6px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <MHLogo/>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <I.History/><I.Settings/>
        </div>
      </div>
      <div style={{ padding: '4px 16px 16px' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5,
          color: T.ACCENT, marginBottom: 4 }}>// GENERATA DAL SISTEMA</div>
        <div style={{ fontSize: 28, fontWeight: 800, letterSpacing: -0.8, lineHeight: 1.05 }}>Per te</div>
        <div style={{ fontSize: 12.5, color: T.TEXT_LO, marginTop: 6, fontFamily: T.MONO, letterSpacing: 0.2 }}>
          {GEN_PLAYLISTS.length} playlist · aggiornate oggi
        </div>
      </div>

      {/* HERO — Rotation */}
      <div style={{ padding: '0 16px 20px' }}>
        <div style={{
          background: `linear-gradient(135deg, rgba(168,224,78,0.18) 0%, rgba(168,224,78,0.04) 60%, transparent 100%)`,
          border: `1.5px solid rgba(168,224,78,0.3)`,
          borderRadius: 14, padding: 14,
          display: 'flex', gap: 14, alignItems: 'center',
        }}>
          <div style={{ width: 100, height: 100, flexShrink: 0 }}>
            <GenCover pl={rotation} size={100} radius={8}/>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontFamily: T.MONO, fontSize: 9.5, fontWeight: 700, letterSpacing: 1.4,
              color: T.ACCENT, marginBottom: 4 }}>// IN ROTAZIONE</div>
            <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.6, lineHeight: 1 }}>{rotation.title}</div>
            <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 4, lineHeight: 1.35 }}>
              {rotation.desc}
            </div>
            <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
              <button style={{ height: 32, padding: '0 14px', borderRadius: 999, background: T.ACCENT, border: 'none',
                display: 'flex', alignItems: 'center', gap: 6, cursor: 'pointer',
                fontSize: 12, fontWeight: 700, color: '#0a0a0a', fontFamily: 'inherit' }}>
                <I.Play size={11}/> RIPRODUCI
              </button>
              <button style={{ height: 32, width: 32, borderRadius: 999,
                background: 'rgba(255,255,255,0.08)', border: '1.5px solid rgba(255,255,255,0.12)',
                display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
                <I.Heart size={14} color={T.ACCENT} filled/>
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* MIX 1-6 — griglia */}
      <div style={{ padding: '0 16px' }}>
        <div style={{ display: 'flex', alignItems: 'baseline', justifyContent: 'space-between', marginBottom: 10 }}>
          <div>
            <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5,
              color: T.ACCENT, marginBottom: 2 }}>// 6 MIX</div>
            <div style={{ fontSize: 18, fontWeight: 800, letterSpacing: -0.4 }}>I tuoi mix giornalieri</div>
          </div>
          <div style={{ fontSize: 11, color: T.TEXT_LO, fontFamily: T.MONO }}>OGGI</div>
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2,1fr)', gap: 10, marginBottom: 8 }}>
          {mixes.map(m => (
            <div key={m.id} style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
              <div style={{ aspectRatio: '1', width: '100%' }}>
                <GenCover pl={m} size={170} radius={10}/>
              </div>
              <div>
                <div style={{ fontSize: 13, fontWeight: 700, letterSpacing: -0.2,
                  overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{m.title}</div>
                <div style={{ fontSize: 11, color: T.TEXT_LO, marginTop: 2,
                  overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{m.sub}</div>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Drop del venerdì + Radar — carousel */}
      <MHSectionHeader eyebrow="// SETTIMANALI" title="Aggiornamenti" action={null}/>
      <div style={{ display: 'flex', gap: 12, padding: '4px 16px 4px', overflowX: 'auto' }}>
        <GenCardLarge pl={releases}/>
        <GenCardLarge pl={radar}/>
        <GenCardLarge pl={capsule}/>
      </div>

      {/* Mood + Next — lista */}
      <MHSectionHeader eyebrow="// CONTESTO" title="Adesso e in poi" action={null}/>
      <div style={{ padding: '4px 16px', display: 'flex', flexDirection: 'column', gap: 8 }}>
        <GenCardSmall pl={mood}/>
        <GenCardSmall pl={next}/>
      </div>

      {/* Come funziona */}
      <div style={{ margin: '20px 16px 8px', padding: 14,
        background: 'rgba(255,255,255,0.03)', border: `1px solid ${T.DIVIDER}`, borderRadius: 12 }}>
        <div style={{ fontFamily: T.MONO, fontSize: 9.5, fontWeight: 700, letterSpacing: 1.4,
          color: T.ACCENT, marginBottom: 6 }}>// COME FUNZIONA</div>
        <div style={{ fontSize: 12, color: T.TEXT_LO, lineHeight: 1.5 }}>
          Le playlist sono generate ogni giorno dal motore di MusicHub
          analizzando i tuoi ascolti, gli artisti che segui e il momento della giornata.
          Più ascolti, più diventano accurate.
        </div>
      </div>

      <div style={{ textAlign: 'center', padding: '12px 16px 24px',
        fontFamily: T.MONO, fontSize: 10, color: 'rgba(255,255,255,0.25)', letterSpacing: 1 }}>— FINE —</div>
    </MHScreen>
  );
}

// ─────────────────────────────────────────────────────────
// DETTAGLIO PLAYLIST GENERATA
// ─────────────────────────────────────────────────────────
function GeneratedDetailScreen({ playlistId = 'mix-2' }) {
  const pl = GEN_PLAYLISTS.find(p => p.id === playlistId) || GEN_PLAYLISTS[2];
  const tracks = TRACKS_FOR_GEN.slice(0, 8).map((t, i) => ({ ...t, n: i + 1, playing: i === 0 }));

  // Hero gradient bashed on playlist palette
  const gradient = `linear-gradient(180deg, ${pl.palette.a}55 0%, ${pl.palette.a}15 25%, ${T.BG_BOTTOM} 65%)`;

  return (
    <MHScreen navActive="foryou" gradient={gradient}>
      {/* Top bar */}
      <div style={{ padding: '8px 16px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <I.Back/>
        <div style={{ display: 'flex', gap: 14 }}>
          <I.More/>
        </div>
      </div>

      {/* Hero */}
      <div style={{ padding: '8px 16px 16px', display: 'flex', flexDirection: 'column', alignItems: 'center', textAlign: 'center' }}>
        <div style={{ width: 200, height: 200, marginBottom: 16, boxShadow: '0 20px 50px rgba(0,0,0,0.5)' }}>
          <GenCover pl={pl} size={200} radius={10}/>
        </div>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 700, letterSpacing: 1.5,
          color: T.ACCENT, marginBottom: 4 }}>// GENERATA · {pl.cadence.toUpperCase()}</div>
        <div style={{ fontSize: 28, fontWeight: 900, letterSpacing: -0.8, lineHeight: 1.05 }}>{pl.title}</div>
        <div style={{ fontSize: 14, color: T.TEXT_LO, marginTop: 6, fontWeight: 500 }}>{pl.sub}</div>
        <div style={{ fontSize: 12, color: T.TEXT_LO2, marginTop: 10, lineHeight: 1.45, maxWidth: 280 }}>
          {pl.desc}
        </div>
      </div>

      {/* Metadata strip */}
      <div style={{ margin: '0 16px 16px', display: 'flex', gap: 8 }}>
        <div style={{ flex: 1, padding: '10px 12px', background: 'rgba(255,255,255,0.05)', borderRadius: 10 }}>
          <div style={{ fontFamily: T.MONO, fontSize: 9.5, fontWeight: 600, letterSpacing: 1.2, color: T.ACCENT }}>AGGIORNATA</div>
          <div style={{ fontSize: 12.5, fontWeight: 700, marginTop: 3 }}>Oggi · 06:00</div>
        </div>
        <div style={{ flex: 1, padding: '10px 12px', background: 'rgba(255,255,255,0.05)', borderRadius: 10 }}>
          <div style={{ fontFamily: T.MONO, fontSize: 9.5, fontWeight: 600, letterSpacing: 1.2, color: T.ACCENT }}>BRANI</div>
          <div style={{ fontSize: 12.5, fontWeight: 700, marginTop: 3 }}>{pl.count} · {pl.duration}</div>
        </div>
      </div>

      {/* Based on */}
      <div style={{ margin: '0 16px 18px', padding: 12,
        background: 'rgba(255,255,255,0.03)', border: `1px solid ${T.DIVIDER}`, borderRadius: 10 }}>
        <div style={{ fontFamily: T.MONO, fontSize: 9.5, fontWeight: 700, letterSpacing: 1.4,
          color: T.TEXT_LO, marginBottom: 6 }}>// BASATA SU</div>
        <div style={{ display: 'flex', flexWrap: 'wrap', gap: 6 }}>
          {pl.basedOn.map((b, i) => (
            <div key={i} style={{ padding: '4px 10px', borderRadius: 999,
              background: 'rgba(168,224,78,0.12)', border: `1px solid rgba(168,224,78,0.25)`,
              fontSize: 11, fontWeight: 600, color: T.ACCENT }}>{b}</div>
          ))}
        </div>
      </div>

      {/* Controls */}
      <div style={{ padding: '0 16px 18px', display: 'flex', alignItems: 'center', gap: 12 }}>
        <button style={{ flex: 1, height: 44, borderRadius: 999, background: T.ACCENT, border: 'none',
          display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8,
          fontSize: 13.5, fontWeight: 800, color: '#0a0a0a', cursor: 'pointer', fontFamily: 'inherit' }}>
          <I.Play size={13}/> RIPRODUCI
        </button>
        <button style={{ width: 44, height: 44, borderRadius: 999,
          background: 'rgba(255,255,255,0.06)', border: '1.5px solid rgba(255,255,255,0.12)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
          <I.Shuffle size={16} color={T.ACCENT}/>
        </button>
        <button style={{ width: 44, height: 44, borderRadius: 999,
          background: 'rgba(255,255,255,0.06)', border: '1.5px solid rgba(255,255,255,0.12)',
          display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}>
          <I.Heart size={16} color={T.ACCENT} filled/>
        </button>
      </div>

      {/* Tracks */}
      <div style={{ padding: '0 0 8px' }}>
        {tracks.map((t) => (
          <div key={t.n} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 16px' }}>
            <div style={{ width: 40, height: 40, flexShrink: 0 }}>
              <MHCover kind={t.kind} palette={t.palette} radius={4}/>
            </div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 600, color: t.playing ? T.ACCENT : T.TEXT_HI,
                display: 'flex', alignItems: 'center', gap: 6,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {t.title}{t.playing && <MHPlayingBars/>}
              </div>
              <div style={{ fontSize: 12, color: T.TEXT_LO, marginTop: 2,
                overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.artist}</div>
            </div>
            <div style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO, width: 32, textAlign: 'right' }}>{t.dur}</div>
            <I.More/>
          </div>
        ))}
      </div>

      {/* Refresh footer */}
      <div style={{ margin: '8px 16px 24px', padding: 14,
        background: 'rgba(168,224,78,0.06)', border: `1px solid rgba(168,224,78,0.15)`, borderRadius: 10,
        display: 'flex', alignItems: 'center', gap: 12 }}>
        <div style={{ width: 32, height: 32, borderRadius: 999,
          background: 'rgba(168,224,78,0.15)',
          display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M3 12a9 9 0 1 0 3-6.7M3 4v5h5" stroke={T.ACCENT} strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"/>
          </svg>
        </div>
        <div style={{ flex: 1, fontSize: 12, color: T.TEXT_LO, lineHeight: 1.4 }}>
          Prossimo aggiornamento <span style={{ color: T.TEXT_HI, fontWeight: 600 }}>domani · 06:00</span>
        </div>
      </div>
    </MHScreen>
  );
}

window.MHForYou = { ForYouScreen, GeneratedDetailScreen };
