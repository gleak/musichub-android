// Generative album covers — abstract geometric, no licensed artwork
// Each cover is keyed by id; pure SVG/CSS so they render crisp at any size.

function CoverGradientArc({ a = '#FF6B5B', b = '#3A1F8A' }) {
  return (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <defs>
        <linearGradient id="cga1" x1="0" y1="0" x2="1" y2="1">
          <stop offset="0%" stopColor={a}/>
          <stop offset="100%" stopColor={b}/>
        </linearGradient>
      </defs>
      <rect width="100" height="100" fill="url(#cga1)"/>
      <circle cx="20" cy="80" r="55" fill="none" stroke="rgba(255,255,255,0.22)" strokeWidth="1"/>
      <circle cx="20" cy="80" r="42" fill="none" stroke="rgba(255,255,255,0.16)" strokeWidth="1"/>
      <circle cx="20" cy="80" r="28" fill="none" stroke="rgba(255,255,255,0.10)" strokeWidth="1"/>
    </svg>
  );
}

function CoverGrid({ a = '#0E1F3A', b = '#A8E04E' }) {
  return (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="100" fill={a}/>
      {Array.from({ length: 9 }).map((_, i) => {
        const x = (i % 3) * 33;
        const y = Math.floor(i / 3) * 33;
        const on = [0, 2, 4, 6, 8, 5].includes(i);
        return <rect key={i} x={x + 6} y={y + 6} width="22" height="22" fill={on ? b : 'rgba(255,255,255,0.08)'} rx="2"/>;
      })}
    </svg>
  );
}

function CoverHalfMoon({ bg = '#E8DCC4', fg = '#1A1A1A' }) {
  return (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="100" fill={bg}/>
      <circle cx="50" cy="60" r="32" fill={fg}/>
      <circle cx="62" cy="52" r="28" fill={bg}/>
    </svg>
  );
}

function CoverTriangles({ bg = '#1A1A1A', fg = '#FF4D2E' }) {
  return (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="100" fill={bg}/>
      <polygon points="10,80 50,20 90,80" fill={fg}/>
      <polygon points="50,20 90,80 50,80" fill="rgba(0,0,0,0.25)"/>
      <line x1="0" y1="80" x2="100" y2="80" stroke="rgba(255,255,255,0.2)" strokeWidth="0.5"/>
    </svg>
  );
}

function CoverWave({ a = '#5C2D8C', b = '#F0A6B0' }) {
  return (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <defs>
        <linearGradient id="cw1" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor={a}/>
          <stop offset="100%" stopColor={b}/>
        </linearGradient>
      </defs>
      <rect width="100" height="100" fill="url(#cw1)"/>
      {[30, 45, 60, 75].map((y, i) => (
        <path key={i} d={`M0 ${y} Q 25 ${y - 8} 50 ${y} T 100 ${y}`} fill="none" stroke="rgba(255,255,255,0.5)" strokeWidth="0.8"/>
      ))}
    </svg>
  );
}

function CoverDot({ bg = '#0B3D2E', fg = '#A8E04E' }) {
  return (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="100" fill={bg}/>
      <circle cx="68" cy="32" r="18" fill={fg}/>
      <text x="10" y="88" fontFamily="-apple-system, system-ui" fontSize="9" fontWeight="700" fill="rgba(255,255,255,0.9)" letterSpacing="1">NIGHT MODE</text>
    </svg>
  );
}

function CoverStripes({ a = '#FFC857', b = '#1A1A1A' }) {
  return (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="100" fill={a}/>
      {[0, 20, 40, 60, 80].map((y, i) => (
        <rect key={i} x="0" y={y} width="100" height="6" fill={b} opacity={0.85}/>
      ))}
      <circle cx="78" cy="22" r="14" fill={b}/>
    </svg>
  );
}

function CoverBlob({ a = '#1E3A8A', b = '#06B6D4' }) {
  return (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="100" fill={a}/>
      <ellipse cx="50" cy="55" rx="38" ry="28" fill={b} opacity="0.85"/>
      <ellipse cx="40" cy="48" rx="20" ry="14" fill="rgba(255,255,255,0.25)"/>
    </svg>
  );
}

function CoverType({ bg = '#E8E2D5', fg = '#222' }) {
  return (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="100" fill={bg}/>
      <text x="6" y="58" fontFamily="Georgia, serif" fontStyle="italic" fontSize="32" fontWeight="700" fill={fg}>echo</text>
      <line x1="6" y1="68" x2="94" y2="68" stroke={fg} strokeWidth="0.6"/>
      <text x="6" y="80" fontFamily="-apple-system, system-ui" fontSize="6" fontWeight="600" fill={fg} letterSpacing="2">VOL. III · 2026</text>
    </svg>
  );
}

function CoverDuotone({ a = '#3A0CA3', b = '#F72585' }) {
  return (
    <svg viewBox="0 0 100 100" width="100%" height="100%" style={{ display: 'block' }}>
      <rect width="100" height="50" fill={a}/>
      <rect y="50" width="100" height="50" fill={b}/>
      <circle cx="50" cy="50" r="22" fill={a}/>
      <circle cx="50" cy="50" r="22" fill={b} clipPath="inset(0 50% 0 0)"/>
    </svg>
  );
}

// Cover registry — name → component
const COVERS = {
  arc: CoverGradientArc,
  grid: CoverGrid,
  moon: CoverHalfMoon,
  triangles: CoverTriangles,
  wave: CoverWave,
  dot: CoverDot,
  stripes: CoverStripes,
  blob: CoverBlob,
  type: CoverType,
  duotone: CoverDuotone,
};

function Cover({ kind, palette, radius = 8, style = {} }) {
  const C = COVERS[kind] || CoverGradientArc;
  return (
    <div style={{
      width: '100%', height: '100%', borderRadius: radius, overflow: 'hidden',
      boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
      ...style,
    }}>
      <C {...(palette || {})}/>
    </div>
  );
}

Object.assign(window, { Cover, COVERS });
