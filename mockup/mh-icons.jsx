// MusicHub — Android launcher icons
// 3 varianti × 2 temi (lime bg + black bg)
// Adaptive icon: 108dp viewport, safe zone 66dp (centered), keyline diameter 66dp.
// Each foreground SVG is designed at viewBox 0 0 108 108 — content inside the 66×66 safe zone.

const T = window.MH.T;

// Foreground marks — viewBox 108x108, content centered in 66x66 safe zone (21..87)

const MARK_PLAYWAVE = (color) => (
  <g>
    {/* outer triangle play */}
    <path d="M 41 32 L 75 54 L 41 76 Z" fill={color}/>
    {/* wave arcs to the right */}
    <path d="M 80 42 Q 88 54 80 66" stroke={color} strokeWidth="3.5" fill="none" strokeLinecap="round"/>
    <path d="M 86 36 Q 96 54 86 72" stroke={color} strokeWidth="3.5" fill="none" strokeLinecap="round" opacity="0.5"/>
  </g>
);

const MARK_MONOGRAM = (color) => (
  <g>
    {/* Custom M built from 3 strokes — left vertical, V notch, right vertical */}
    <path
      d="M 30 78 L 30 30 L 54 60 L 78 30 L 78 78"
      stroke={color}
      strokeWidth="9"
      strokeLinecap="round"
      strokeLinejoin="round"
      fill="none"
    />
    {/* tiny accent dot under V (signal indicator) */}
    <circle cx="54" cy="80" r="3" fill={color}/>
  </g>
);

const MARK_WAVEFORM = (color) => (
  <g>
    {/* 7 vertical bars of varying height — sound visualizer */}
    {[
      { x: 28, h: 18 },
      { x: 38, h: 32 },
      { x: 48, h: 46 },
      { x: 58, h: 38 },
      { x: 68, h: 50 },
      { x: 78, h: 28 },
    ].map((b, i) => (
      <rect
        key={i}
        x={b.x - 3}
        y={54 - b.h / 2}
        width="6"
        height={b.h}
        rx="3"
        fill={color}
      />
    ))}
  </g>
);

const VARIANTS = [
  { id: 'playwave', name: 'Play + wave', mark: MARK_PLAYWAVE },
  { id: 'monogram', name: 'Monogramma M', mark: MARK_MONOGRAM },
  { id: 'waveform', name: 'Onda sonora', mark: MARK_WAVEFORM },
];

const THEMES = [
  { id: 'lime', name: 'Lime', bg: T.ACCENT, fg: '#0A0A0A' },
  { id: 'dark', name: 'Dark', bg: '#0F0F0F', fg: T.ACCENT },
];

// ─────────────────────────────────────────────────────────
// Single icon SVG — adaptive icon viewport 108x108, content rendered through clip
// Mode: 'square' | 'squircle' | 'circle' | 'bare'
// ─────────────────────────────────────────────────────────
function IconSvg({ variant, theme, size = 192, mode = 'squircle', subtleBgPattern = true }) {
  const Mark = variant.mark;
  const id = `mh-${variant.id}-${theme.id}-${mode}-${Math.random().toString(36).slice(2, 7)}`;
  // Squircle path approximating Android adaptive squircle (rounded square)
  const squirclePath = "M 54 0 C 14 0 0 14 0 54 C 0 94 14 108 54 108 C 94 108 108 94 108 54 C 108 14 94 0 54 0 Z";

  let clipPath = null;
  if (mode === 'circle') clipPath = <circle cx="54" cy="54" r="54"/>;
  else if (mode === 'squircle') clipPath = <path d={squirclePath}/>;
  else if (mode === 'square') clipPath = <rect width="108" height="108" rx="22"/>;
  // bare = no clip

  return (
    <svg width={size} height={size} viewBox="0 0 108 108" style={{ display: 'block' }}>
      <defs>
        {clipPath && <clipPath id={`clip-${id}`}>{clipPath}</clipPath>}
        <linearGradient id={`bg-${id}`} x1="0" y1="0" x2="0" y2="1">
          <stop offset="0" stopColor={theme.bg}/>
          <stop offset="1" stopColor={theme.id === 'lime' ? '#8FC135' : '#1A1A1A'}/>
        </linearGradient>
      </defs>
      <g clipPath={clipPath ? `url(#clip-${id})` : undefined}>
        <rect width="108" height="108" fill={`url(#bg-${id})`}/>
        {subtleBgPattern && (
          <g opacity={theme.id === 'lime' ? 0.08 : 0.12}>
            {[20, 40, 60, 80].map(r => (
              <circle key={r} cx="54" cy="54" r={r} fill="none" stroke={theme.fg} strokeWidth="0.6"/>
            ))}
          </g>
        )}
        <Mark color={theme.fg}/>
      </g>
    </svg>
  );
}

// ─────────────────────────────────────────────────────────
// Technical view — keyline grid + safe zone overlay
// ─────────────────────────────────────────────────────────
function IconKeylineSvg({ variant, theme, size = 192 }) {
  const Mark = variant.mark;
  return (
    <svg width={size} height={size} viewBox="0 0 108 108" style={{ display: 'block', background: '#1A1A1A' }}>
      {/* full 108 area — soft fill */}
      <rect width="108" height="108" fill={theme.bg} opacity="0.15"/>
      {/* safe zone 66×66 centered (offset 21) */}
      <rect x="21" y="21" width="66" height="66" fill={theme.bg} opacity="0.25"/>
      {/* keyline circle 66 */}
      <circle cx="54" cy="54" r="33" fill="none" stroke={T.ACCENT} strokeWidth="0.4" strokeDasharray="2 2"/>
      {/* keyline square 66 */}
      <rect x="21" y="21" width="66" height="66" fill="none" stroke={T.ACCENT} strokeWidth="0.4" strokeDasharray="2 2"/>
      {/* center cross */}
      <line x1="54" y1="0" x2="54" y2="108" stroke={T.ACCENT} strokeWidth="0.2" opacity="0.4"/>
      <line x1="0" y1="54" x2="108" y2="54" stroke={T.ACCENT} strokeWidth="0.2" opacity="0.4"/>
      <Mark color={theme.fg}/>
    </svg>
  );
}

// ─────────────────────────────────────────────────────────
// Mock launcher row — shows the icon among other apps in dark mode
// ─────────────────────────────────────────────────────────
function LauncherRow({ variant, theme, mode = 'squircle' }) {
  const fakeApps = [
    { name: 'Maps', color: '#2D7CFF', glyph: '◆' },
    { name: 'Camera', color: '#222', glyph: '◉' },
    { name: 'Phone', color: '#0FA958', glyph: '☎' },
  ];
  const renderFake = (app, i) => (
    <div key={i} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, width: 64 }}>
      <div style={{
        width: 56, height: 56,
        borderRadius: mode === 'circle' ? 999 : (mode === 'squircle' ? 18 : 12),
        background: app.color, color: '#fff',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        fontSize: 22, fontWeight: 700,
        boxShadow: '0 4px 12px rgba(0,0,0,0.4)',
      }}>{app.glyph}</div>
      <div style={{ fontSize: 10.5, color: '#fff', opacity: 0.85 }}>{app.name}</div>
    </div>
  );
  return (
    <div style={{ background: 'rgba(255,255,255,0.04)', border: `1px solid ${T.DIVIDER}`,
      borderRadius: 14, padding: '14px 12px',
      display: 'flex', alignItems: 'center', justifyContent: 'space-around', gap: 8 }}>
      {renderFake(fakeApps[0], 0)}
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, width: 72 }}>
        <div style={{ position: 'relative' }}>
          <IconSvg variant={variant} theme={theme} size={64} mode={mode}/>
          <div style={{ position: 'absolute', inset: 0, borderRadius:
            mode === 'circle' ? 999 : (mode === 'squircle' ? 20 : 14),
            boxShadow: '0 4px 14px rgba(0,0,0,0.45)', pointerEvents: 'none' }}/>
        </div>
        <div style={{ fontSize: 11, color: T.ACCENT, fontWeight: 700 }}>MusicHub</div>
      </div>
      {renderFake(fakeApps[1], 1)}
      {renderFake(fakeApps[2], 2)}
    </div>
  );
}

// ─────────────────────────────────────────────────────────
// Variant card — shows all the views for one (variant, theme)
// ─────────────────────────────────────────────────────────
function VariantCard({ variant, theme }) {
  return (
    <div style={{
      background: '#0F0F0F', border: `1px solid ${T.DIVIDER}`, borderRadius: 16,
      padding: 18, color: T.TEXT_HI, fontFamily: T.FONT,
    }}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 10, marginBottom: 14 }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 700, letterSpacing: 1.4, color: T.ACCENT }}>
          // {variant.name.toUpperCase()} · {theme.name.toUpperCase()}
        </div>
        <div style={{ flex: 1 }}/>
        <div style={{ fontSize: 11, color: T.TEXT_LO, fontFamily: T.MONO }}>108dp · safe 66</div>
      </div>

      {/* Hero shape variations */}
      <div style={{ display: 'flex', gap: 14, marginBottom: 14, alignItems: 'flex-end' }}>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
          <IconSvg variant={variant} theme={theme} size={140} mode="squircle"/>
          <div style={{ fontSize: 10, color: T.TEXT_LO, fontFamily: T.MONO }}>SQUIRCLE</div>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
          <IconSvg variant={variant} theme={theme} size={100} mode="circle"/>
          <div style={{ fontSize: 10, color: T.TEXT_LO, fontFamily: T.MONO }}>CIRCLE</div>
        </div>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
          <IconSvg variant={variant} theme={theme} size={100} mode="square"/>
          <div style={{ fontSize: 10, color: T.TEXT_LO, fontFamily: T.MONO }}>SQUARE</div>
        </div>
        <div style={{ flex: 1 }}/>
        <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6 }}>
          <IconKeylineSvg variant={variant} theme={theme} size={100}/>
          <div style={{ fontSize: 10, color: T.TEXT_LO, fontFamily: T.MONO }}>KEYLINE</div>
        </div>
      </div>

      {/* Launcher mock */}
      <div style={{ marginBottom: 12 }}>
        <LauncherRow variant={variant} theme={theme} mode="squircle"/>
      </div>

      {/* Density preview */}
      <div style={{ display: 'flex', alignItems: 'flex-end', gap: 12, padding: '10px 6px',
        background: 'rgba(255,255,255,0.025)', borderRadius: 10 }}>
        {[
          { dp: 'mdpi', px: 48 },
          { dp: 'hdpi', px: 72 },
          { dp: 'xhdpi', px: 96 },
          { dp: 'xxhdpi', px: 144 },
          { dp: 'xxxhdpi', px: 192 },
        ].map(d => (
          <div key={d.dp} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 4, flex: 1 }}>
            <IconSvg variant={variant} theme={theme} size={Math.min(d.px, 72)} mode="squircle"/>
            <div style={{ fontFamily: T.MONO, fontSize: 9, color: T.TEXT_LO, letterSpacing: 0.4 }}>{d.dp}</div>
            <div style={{ fontFamily: T.MONO, fontSize: 9, color: T.TEXT_LO2 }}>{d.px}px</div>
          </div>
        ))}
      </div>
    </div>
  );
}

// ─────────────────────────────────────────────────────────
// Main screen — all variants × themes laid out for review
// ─────────────────────────────────────────────────────────
function IconsScreen() {
  return (
    <div style={{
      width: 1320, padding: 28, background: T.BG_BOTTOM, color: T.TEXT_HI,
      fontFamily: T.FONT,
    }}>
      <div style={{ marginBottom: 18 }}>
        <div style={{ fontFamily: T.MONO, fontSize: 11, fontWeight: 700, letterSpacing: 1.5, color: T.ACCENT, marginBottom: 6 }}>
          // ANDROID LAUNCHER · ADAPTIVE ICONS
        </div>
        <div style={{ fontSize: 28, fontWeight: 800, letterSpacing: -0.6 }}>Icone applicazione MusicHub</div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, marginTop: 6, maxWidth: 720, lineHeight: 1.5 }}>
          3 varianti del mark — Play+wave, Monogramma M, Onda sonora — in 2 temi (Lime bg / Dark bg).
          Ogni icona è disegnata su viewport adaptive 108dp con contenuto dentro il safe zone 66dp,
          per essere ritagliata correttamente da qualsiasi launcher (squircle, circle, square).
        </div>
      </div>

      <div style={{ display: 'grid', gridTemplateColumns: 'repeat(2, 1fr)', gap: 18 }}>
        {VARIANTS.map(v => THEMES.map(th => (
          <VariantCard key={v.id + th.id} variant={v} theme={th}/>
        ))).flat()}
      </div>

      <div style={{ marginTop: 22, padding: 18, background: 'rgba(168,224,78,0.06)',
        border: `1px solid rgba(168,224,78,0.2)`, borderRadius: 12 }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 700, letterSpacing: 1.4, color: T.ACCENT, marginBottom: 8 }}>
          // EXPORT PER ANDROID STUDIO
        </div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, lineHeight: 1.55 }}>
          PNG nelle 5 densità (48/72/96/144/192) + adaptive foreground 432×432 +
          adaptive background 432×432 + monochrome (Android 13+) sono pronti in <code style={{
            fontFamily: T.MONO, color: T.ACCENT, background: 'rgba(168,224,78,0.1)',
            padding: '2px 6px', borderRadius: 4 }}>android-icons/</code>.
          Drop diretto in <code style={{ fontFamily: T.MONO, color: T.ACCENT,
            background: 'rgba(168,224,78,0.1)', padding: '2px 6px', borderRadius: 4 }}>app/src/main/res/</code>.
        </div>
      </div>
    </div>
  );
}

window.MHIcons = { IconsScreen, VARIANTS, THEMES, IconSvg, MARK_PLAYWAVE, MARK_MONOGRAM, MARK_WAVEFORM };
