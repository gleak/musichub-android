// MusicHub — System-generated playlists (data + cover renderers + cards)
// Originali, non replicano playlist brandizzate di terzi.

const { T, I, MHCover, MHPlayingBars } = window.MH;

// ─────────────────────────────────────────────────────────
// DATA — 6 playlist generate dal sistema
// ─────────────────────────────────────────────────────────
const GEN_PLAYLISTS = [
  {
    id: 'rotation',
    family: 'rotation',
    title: 'Rotation',
    sub: 'In rotazione · S18',
    desc: 'I brani che stai ascoltando di più questa settimana, mescolati a 3 sorprese.',
    update: 'Aggiornata oggi',
    cadence: 'Ogni giorno',
    count: 28,
    duration: '1h 42m',
    basedOn: ['Marina Vega', 'Helena Vorr', '+ 6 artisti'],
    accent: '#A8E04E',
    palette: { a: '#1A1A1A', b: '#A8E04E' },
    coverKind: 'rotation',
    badge: 'S18',
  },
  {
    id: 'mix-1',
    family: 'daily',
    title: 'Mix 01',
    sub: 'Indie atmosferico',
    desc: 'Cantautorato lento, riverberi e batterie ovattate. Per le mattine grigie.',
    update: 'Aggiornata oggi',
    cadence: 'Ogni giorno',
    count: 32,
    duration: '2h 04m',
    basedOn: ['Marina Vega', 'Tobi Akin', '+ 4 artisti'],
    accent: '#FF6B5B',
    palette: { a: '#FF6B5B', b: '#3A1F8A' },
    coverKind: 'mix',
    badge: '01',
  },
  {
    id: 'mix-2',
    family: 'daily',
    title: 'Mix 02',
    sub: 'Elettronica notturna',
    desc: 'Synth pads, IDM ipnotica e ambient. Profondità per le ore tarde.',
    update: 'Aggiornata oggi',
    cadence: 'Ogni giorno',
    count: 28,
    duration: '1h 56m',
    basedOn: ['Lou Hessler', 'The Astral Kit', '+ 5 artisti'],
    accent: '#06B6D4',
    palette: { a: '#0E1F3A', b: '#06B6D4' },
    coverKind: 'mix',
    badge: '02',
  },
  {
    id: 'mix-3',
    family: 'daily',
    title: 'Mix 03',
    sub: 'Hip-hop e neo-soul',
    desc: 'Ritmi spezzati, basso caldo, voci morbide. Energia controllata.',
    update: 'Aggiornata oggi',
    cadence: 'Ogni giorno',
    count: 30,
    duration: '1h 48m',
    basedOn: ['Sero', 'Tobi Akin', '+ 4 artisti'],
    accent: '#FFC857',
    palette: { a: '#FFC857', b: '#1A1A1A' },
    coverKind: 'mix',
    badge: '03',
  },
  {
    id: 'mix-4',
    family: 'daily',
    title: 'Mix 04',
    sub: 'Jazz e dintorni',
    desc: 'Quartetti contemporanei, piano trio e qualche escursione fusion.',
    update: 'Aggiornata oggi',
    cadence: 'Ogni giorno',
    count: 26,
    duration: '2h 12m',
    basedOn: ['Camille Roche', '+ 3 artisti'],
    accent: '#5C2D8C',
    palette: { a: '#5C2D8C', b: '#F0A6B0' },
    coverKind: 'mix',
    badge: '04',
  },
  {
    id: 'mix-5',
    family: 'daily',
    title: 'Mix 05',
    sub: 'Rock alternativo',
    desc: 'Chitarre strato e ritornelli grandi. Per le corse del pomeriggio.',
    update: 'Aggiornata oggi',
    cadence: 'Ogni giorno',
    count: 34,
    duration: '2h 21m',
    basedOn: ['The Astral Kit', '+ 5 artisti'],
    accent: '#FF4D2E',
    palette: { a: '#7C2D12', b: '#FF4D2E' },
    coverKind: 'mix',
    badge: '05',
  },
  {
    id: 'mix-6',
    family: 'daily',
    title: 'Mix 06',
    sub: 'Acustico e folk',
    desc: 'Voce, chitarra, archi essenziali. Tutto quello che serve.',
    update: 'Aggiornata oggi',
    cadence: 'Ogni giorno',
    count: 24,
    duration: '1h 38m',
    basedOn: ['Marina Vega', 'Camille Roche'],
    accent: '#E8DCC4',
    palette: { a: '#E8DCC4', b: '#7C2D12' },
    coverKind: 'mix',
    badge: '06',
  },
  {
    id: 'releases',
    family: 'releases',
    title: 'Nuove uscite',
    sub: 'Drop del venerdì',
    desc: 'Brani usciti questa settimana dagli artisti che segui.',
    update: 'Aggiornata venerdì',
    cadence: 'Ogni venerdì',
    count: 22,
    duration: '1h 24m',
    basedOn: ['12 artisti che segui'],
    accent: '#A8E04E',
    palette: { a: '#A8E04E', b: '#0a0a0a' },
    coverKind: 'releases',
    badge: 'VEN',
    date: '01 MAG',
  },
  {
    id: 'capsule',
    family: 'capsule',
    title: 'Time capsule',
    sub: 'Tre anni fa, oggi',
    desc: 'I brani che ascoltavi a maggio 2023. Un piccolo viaggio indietro.',
    update: 'Aggiornata oggi',
    cadence: 'Mensile',
    count: 30,
    duration: '1h 52m',
    basedOn: ['Cronologia 2023'],
    accent: '#F0A6B0',
    palette: { a: '#F0A6B0', b: '#3A1F8A' },
    coverKind: 'capsule',
    badge: '2023',
  },
  {
    id: 'radar',
    family: 'radar',
    title: 'Radar',
    sub: 'Artisti emergenti',
    desc: 'Voci nuove che potrebbero piacerti, scelte ogni lunedì.',
    update: 'Aggiornata lunedì',
    cadence: 'Ogni lunedì',
    count: 25,
    duration: '1h 36m',
    basedOn: ['Cluster di gusto', 'Trend regione IT'],
    accent: '#06B6D4',
    palette: { a: '#06B6D4', b: '#1E3A8A' },
    coverKind: 'radar',
    badge: 'LUN',
  },
  {
    id: 'mood',
    family: 'mood',
    title: 'Sera tranquilla',
    sub: 'Mood del momento',
    desc: 'Adattata all\'ora e al meteo. Cambia titolo durante la giornata.',
    update: 'Aggiornata adesso',
    cadence: 'Continua',
    count: 18,
    duration: '1h 12m',
    basedOn: ['Ora · 21:14', 'Meteo · sereno'],
    accent: '#3A0CA3',
    palette: { a: '#3A0CA3', b: '#F72585' },
    coverKind: 'mood',
    badge: 'NOW',
  },
  {
    id: 'next',
    family: 'next',
    title: 'In poi',
    sub: 'Continua dall\'ascolto',
    desc: 'Una coda automatica simile all\'ultimo album che hai sentito.',
    update: 'Aggiornata adesso',
    cadence: 'Continua',
    count: 40,
    duration: '2h 38m',
    basedOn: ['Slow Hours · Marina Vega'],
    accent: '#FFC857',
    palette: { a: '#FFC857', b: '#3A1F8A' },
    coverKind: 'next',
    badge: '→',
  },
];

const FAMILY_LABELS = {
  rotation: 'In rotazione',
  daily: 'Mix giornalieri',
  releases: 'Nuove uscite',
  capsule: 'Time capsule',
  radar: 'Radar',
  mood: 'Mood',
  next: 'In poi',
};

const TRACKS_FOR_GEN = [
  { title: 'Undertow', artist: 'Helena Vorr', dur: '4:02', kind: 'blob', palette: { a: '#1E3A8A', b: '#06B6D4' } },
  { title: 'Long way home', artist: 'Marina Vega', dur: '4:18', kind: 'moon', palette: { bg: '#E8DCC4', fg: '#1A1A1A' }, liked: true },
  { title: 'Plein soleil', artist: 'Camille Roche', dur: '3:31', kind: 'wave', palette: { a: '#5C2D8C', b: '#F0A6B0' } },
  { title: 'Carbon Mirror', artist: 'Lou Hessler', dur: '3:42', kind: 'grid', palette: { a: '#0E1F3A', b: T.ACCENT } },
  { title: 'Pyre', artist: 'Tobi Akin', dur: '2:55', kind: 'triangles', palette: { bg: '#1A1A1A', fg: '#FF4D2E' } },
  { title: 'Glasshouse', artist: 'The Astral Kit', dur: '5:07', kind: 'stripes', palette: { a: '#FFC857', b: '#1A1A1A' } },
  { title: 'After', artist: 'Marina Vega', dur: '2:18', kind: 'arc', palette: { a: '#FF6B5B', b: '#3A1F8A' } },
  { title: 'Cold lights', artist: 'Marina Vega', dur: '3:14', kind: 'type', palette: { bg: '#E8E2D5', fg: '#222' } },
  { title: 'Echo', artist: 'Helena Vorr', dur: '3:48', kind: 'dot', palette: { bg: '#0B3D2E', fg: T.ACCENT } },
];

// ─────────────────────────────────────────────────────────
// COVER RENDERERS — distinte per famiglia di playlist generata
// ─────────────────────────────────────────────────────────
function GenCover({ pl, size = 100, radius = 8 }) {
  const { coverKind, palette, badge, title, sub, date } = pl;
  const cs = { width: '100%', height: '100%', borderRadius: radius, overflow: 'hidden', position: 'relative' };
  const labelStyle = (color) => ({
    position: 'absolute', top: 8, left: 8, fontFamily: T.MONO, fontSize: Math.max(8, size * 0.085),
    fontWeight: 700, letterSpacing: 1.2, color,
  });

  if (coverKind === 'mix') {
    return (
      <div style={{ ...cs, background: `linear-gradient(135deg, ${palette.a} 0%, ${palette.b} 100%)` }}>
        <div style={labelStyle('rgba(255,255,255,0.85)')}>// MIX</div>
        <div style={{
          position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center',
          fontFamily: T.MONO, fontSize: size * 0.5, fontWeight: 800, color: '#fff', letterSpacing: -2,
          textShadow: '0 4px 12px rgba(0,0,0,0.35)',
        }}>{badge}</div>
        <div style={{ position: 'absolute', bottom: 8, left: 8, right: 8,
          fontSize: Math.max(9, size * 0.1), fontWeight: 700, color: '#fff',
          textShadow: '0 1px 4px rgba(0,0,0,0.4)', lineHeight: 1.1 }}>{sub}</div>
      </div>
    );
  }

  if (coverKind === 'rotation') {
    const rings = [0.85, 0.6, 0.35];
    return (
      <div style={{ ...cs, background: '#0F0F0F' }}>
        <svg viewBox="0 0 100 100" style={{ position: 'absolute', inset: 0, width: '100%', height: '100%' }}>
          {rings.map((r, i) => (
            <circle key={i} cx="50" cy="50" r={r * 45} fill="none"
              stroke={i === 0 ? T.ACCENT : 'rgba(168,224,78,0.35)'} strokeWidth={i === 0 ? 1.5 : 0.6}
              strokeDasharray={i === 1 ? '3 4' : 'none'}/>
          ))}
          <circle cx="50" cy="50" r="3" fill={T.ACCENT}/>
          <circle cx="84" cy="50" r="2.5" fill={T.ACCENT}/>
        </svg>
        <div style={labelStyle(T.ACCENT)}>// ROTATION</div>
        <div style={{ position: 'absolute', bottom: 8, left: 8,
          fontFamily: T.MONO, fontSize: Math.max(10, size * 0.12), fontWeight: 800, color: '#fff' }}>{badge}</div>
      </div>
    );
  }

  if (coverKind === 'releases') {
    return (
      <div style={{ ...cs, background: palette.b }}>
        <div style={{ position: 'absolute', inset: 0,
          background: `linear-gradient(180deg, transparent 0%, ${palette.a} 200%)`, opacity: 0.4 }}/>
        <div style={{ position: 'absolute', top: '50%', left: 0, right: 0, transform: 'translateY(-50%)',
          textAlign: 'center', color: T.ACCENT,
          fontFamily: T.MONO, fontSize: size * 0.32, fontWeight: 800, letterSpacing: -1, lineHeight: 1 }}>
          {badge}<br/>
          <span style={{ fontSize: size * 0.13, letterSpacing: 1, color: '#fff', opacity: 0.7 }}>{date}</span>
        </div>
        <div style={labelStyle('rgba(168,224,78,0.85)')}>// DROP</div>
      </div>
    );
  }

  if (coverKind === 'capsule') {
    return (
      <div style={{ ...cs, background: '#1A1A1A', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{
          width: '70%', aspectRatio: '1', background: `linear-gradient(135deg, ${palette.a} 0%, ${palette.b} 100%)`,
          borderRadius: 4, transform: 'rotate(-4deg)',
          boxShadow: '0 6px 16px rgba(0,0,0,0.4)',
          border: '4px solid #F5F0E8',
        }}/>
        <div style={labelStyle('rgba(255,255,255,0.6)')}>// CAPSULE</div>
        <div style={{ position: 'absolute', bottom: 8, right: 10,
          fontFamily: T.MONO, fontSize: Math.max(10, size * 0.13), fontWeight: 800, color: '#fff', opacity: 0.85 }}>{badge}</div>
      </div>
    );
  }

  if (coverKind === 'radar') {
    return (
      <div style={{ ...cs, background: palette.b }}>
        <svg viewBox="0 0 100 100" style={{ position: 'absolute', inset: 0, width: '100%', height: '100%' }}>
          {[0.3, 0.55, 0.8].map((r, i) => (
            <circle key={i} cx="50" cy="50" r={r * 45} fill="none" stroke={palette.a} strokeWidth="0.6" opacity={0.4 + i * 0.1}/>
          ))}
          <line x1="50" y1="50" x2="92" y2="22" stroke={palette.a} strokeWidth="1.2"/>
          <circle cx="76" cy="32" r="3" fill={palette.a}/>
          <circle cx="35" cy="65" r="2" fill={palette.a} opacity={0.6}/>
          <circle cx="62" cy="72" r="1.5" fill={palette.a} opacity={0.5}/>
        </svg>
        <div style={labelStyle(palette.a)}>// RADAR</div>
      </div>
    );
  }

  if (coverKind === 'mood') {
    return (
      <div style={{ ...cs, background: `radial-gradient(ellipse at 30% 20%, ${palette.b} 0%, ${palette.a} 70%)` }}>
        <div style={{ position: 'absolute', bottom: 8, left: 10, right: 10,
          fontSize: Math.max(11, size * 0.14), fontWeight: 800, color: '#fff', letterSpacing: -0.3,
          textShadow: '0 1px 4px rgba(0,0,0,0.3)', lineHeight: 1.1 }}>{title}</div>
        <div style={labelStyle('rgba(255,255,255,0.85)')}>// MOOD</div>
      </div>
    );
  }

  if (coverKind === 'next') {
    return (
      <div style={{ ...cs, background: '#0F0F0F' }}>
        <svg viewBox="0 0 100 100" style={{ position: 'absolute', inset: 0, width: '100%', height: '100%' }}>
          <defs>
            <linearGradient id={`ng${pl.id}`} x1="0" x2="1">
              <stop offset="0" stopColor={palette.a}/>
              <stop offset="1" stopColor={palette.b}/>
            </linearGradient>
          </defs>
          {[0, 1, 2, 3].map(i => (
            <path key={i} d={`M ${10 + i * 10} 50 Q 50 ${30 - i * 5}, ${90 - i * 5} 50`}
              fill="none" stroke={`url(#ng${pl.id})`} strokeWidth="1" opacity={1 - i * 0.18}/>
          ))}
          <path d="M70 35 L82 50 L70 65" fill="none" stroke={palette.a} strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
        <div style={labelStyle(palette.a)}>// NEXT</div>
      </div>
    );
  }

  return <MHCover kind="arc" palette={palette} radius={radius}/>;
}

// ─────────────────────────────────────────────────────────
// CARDS
// ─────────────────────────────────────────────────────────
function GenCardLarge({ pl, onClick }) {
  return (
    <div onClick={onClick} style={{
      width: 144, flexShrink: 0, cursor: 'pointer',
    }}>
      <div style={{ width: 144, height: 144, marginBottom: 8 }}>
        <GenCover pl={pl} size={144} radius={10}/>
      </div>
      <div style={{ fontSize: 13.5, fontWeight: 700, letterSpacing: -0.2, color: T.TEXT_HI,
        overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{pl.title}</div>
      <div style={{ fontSize: 11.5, color: T.TEXT_LO, marginTop: 2, lineHeight: 1.3,
        overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{pl.sub}</div>
    </div>
  );
}

function GenCardSmall({ pl, onClick }) {
  return (
    <div onClick={onClick} style={{ display: 'flex', alignItems: 'center', gap: 12,
      background: T.CARD, borderRadius: 10, padding: 10, cursor: 'pointer' }}>
      <div style={{ width: 56, height: 56, flexShrink: 0 }}>
        <GenCover pl={pl} size={56} radius={6}/>
      </div>
      <div style={{ flex: 1, minWidth: 0 }}>
        <div style={{ fontSize: 14, fontWeight: 700, color: T.TEXT_HI,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{pl.title}</div>
        <div style={{ fontSize: 11.5, color: T.TEXT_LO, marginTop: 2,
          overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{pl.sub}</div>
      </div>
      <div style={{ fontFamily: T.MONO, fontSize: 9.5, fontWeight: 700, letterSpacing: 1,
        color: pl.accent, padding: '4px 8px', borderRadius: 999,
        background: 'rgba(255,255,255,0.06)' }}>{pl.cadence.toUpperCase()}</div>
    </div>
  );
}

window.MHGenerated = {
  GEN_PLAYLISTS, FAMILY_LABELS, TRACKS_FOR_GEN, GenCover, GenCardLarge, GenCardSmall,
};
