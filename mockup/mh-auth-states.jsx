// MusicHub — Auth & Onboarding · Missing impl-only states
// Fills the gap between AuthViewModel/LoginScreen/OnboardingScreen impl
// and the original mockup in mh-auth.jsx
const { T, I } = window.MH;

// ── 0a · State.Loading initial-probe ─────────────────────
// AuthViewModel attempts silent token refresh before any UI shows.
// Mockup never modeled this — we render it as a brand-locked splash
// with a thin progress strip + diagnostic line.
function AuthProbeScreen({ stage = 'token' }) {
  // stage: 'token' | 'me' | 'rejected-silent'
  const stageMap = {
    'token': { label: 'Verifico sessione', code: 'auth/token-refresh', pct: 0.35 },
    'me':    { label: 'Carico profilo',   code: 'auth/refresh-me',    pct: 0.78 },
    'rejected-silent': { label: 'Sessione scaduta', code: 'auth/token-rejected · clear', pct: 1 },
  };
  const s = stageMap[stage] || stageMap.token;
  const muted = stage === 'rejected-silent';
  return (
    <div style={{
      width: '100%', height: '100%', color: T.TEXT_HI, fontFamily: T.FONT,
      background: muted
        ? `radial-gradient(120% 60% at 50% 0%, #2A1515 0%, ${T.BG_TOP} 35%, ${T.BG_BOTTOM} 100%)`
        : `radial-gradient(120% 60% at 50% 0%, #2A4615 0%, ${T.BG_TOP} 35%, ${T.BG_BOTTOM} 100%)`,
      display: 'flex', flexDirection: 'column', alignItems: 'center',
      padding: '180px 28px 56px',
    }}>
      <div style={{
        width: 92, height: 92, borderRadius: 22,
        background: muted
          ? `linear-gradient(180deg, #4A4A4A 0%, #2C2C2C 100%)`
          : `linear-gradient(180deg, ${T.ACCENT} 0%, ${T.ACCENT_DIM} 100%)`,
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        boxShadow: muted ? 'none' : '0 24px 48px -12px rgba(168,224,78,0.45)',
        marginBottom: 32, opacity: muted ? 0.55 : 1,
      }}>
        <svg width="56" height="56" viewBox="0 0 108 108">
          {[
            {x:24,h:14,w:5},{x:32,h:30,w:5},{x:40,h:22,w:5},
            {x:48,h:54,w:6},{x:57,h:40,w:5},{x:65,h:62,w:6},
            {x:74,h:28,w:5},{x:82,h:18,w:5},
          ].map((b,i)=>(
            <rect key={i} x={b.x-b.w/2} y={54-b.h/2} width={b.w} height={b.h} rx={b.w/2} fill="#0A0A0A"/>
          ))}
        </svg>
      </div>

      <div style={{ fontSize: 28, fontWeight: 800, letterSpacing: -0.6, marginBottom: 10 }}>
        MusicHub
      </div>

      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 22 }}>
        {!muted && (
          <div style={{
            width: 14, height: 14, borderRadius: 7,
            border: `2px solid rgba(168,224,78,0.25)`,
            borderTopColor: T.ACCENT,
            animation: 'mh-spin 0.9s linear infinite',
          }}/>
        )}
        {muted && (
          <div style={{ width: 14, height: 14, borderRadius: 7, background: '#E14848' }}/>
        )}
        <div style={{ fontSize: 13, color: T.TEXT_LO, fontWeight: 500 }}>
          {s.label}…
        </div>
      </div>

      <div style={{ flex: 1 }}/>

      {/* Progress strip */}
      <div style={{ width: '100%', maxWidth: 280, marginBottom: 14 }}>
        <div style={{ height: 2, background: 'rgba(255,255,255,0.06)', borderRadius: 2, overflow: 'hidden' }}>
          <div style={{
            width: `${s.pct * 100}%`, height: '100%',
            background: muted ? '#E14848' : T.ACCENT,
            transition: 'width 0.3s ease',
          }}/>
        </div>
      </div>

      <div style={{ fontFamily: T.MONO, fontSize: 10, color: T.TEXT_LO2, letterSpacing: 0.5 }}>
        {s.code}
      </div>

      <style>{`@keyframes mh-spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}

// ── Shared genre grid for onboarding variants ───────────
const GENRES = [
  'Indie', 'Elettronica', 'Hip-hop', 'Jazz', 'Classica', 'Ambient',
  'Rock', 'Pop', 'R&B', 'Lo-fi', 'Punk', 'Cantautorato',
];

function GenreGrid({ picked, onToggle, dim = false }) {
  return (
    <div style={{ display: 'flex', flexWrap: 'wrap', gap: 10, alignContent: 'flex-start', opacity: dim ? 0.55 : 1, pointerEvents: dim ? 'none' : 'auto' }}>
      {GENRES.map(g => {
        const on = picked.has(g);
        return (
          <button key={g} onClick={() => onToggle && onToggle(g)} style={{
            padding: '10px 16px', borderRadius: 999, cursor: 'pointer',
            background: on ? T.ACCENT : 'rgba(255,255,255,0.06)',
            border: on ? 'none' : '1px solid rgba(255,255,255,0.1)',
            color: on ? '#0A0A0A' : T.TEXT_HI,
            fontSize: 14, fontWeight: 600, fontFamily: T.FONT,
            display: 'inline-flex', alignItems: 'center', gap: 6,
          }}>
            {on && <I.Check size={14} color="#0A0A0A"/>}
            {g}
          </button>
        );
      })}
    </div>
  );
}

// ── 0b · OnboardingScreen · error state ─────────────────
// OnboardingScreen.kt:107-114 — surfaces error above CTA.
function OnboardingErrorScreen() {
  const picked = new Set(['Indie', 'Elettronica', 'Jazz']);
  return (
    <div style={{ width: '100%', height: '100%', background: `linear-gradient(180deg, #1F1F1F 0%, #080808 100%)`, color: T.TEXT_HI, fontFamily: T.FONT, display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '64px 24px 12px' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 8 }}>
          // PASSO 1 / 1
        </div>
        <div style={{ fontSize: 28, fontWeight: 800, letterSpacing: -0.6, lineHeight: 1.1, marginBottom: 8 }}>
          Cosa ascolti?
        </div>
        <div style={{ fontSize: 13.5, color: T.TEXT_LO, lineHeight: 1.5 }}>
          Scegli almeno 3 generi. Useremo questo segnale per costruire il tuo Daily&nbsp;Mix.
        </div>
      </div>

      <div style={{ flex: 1, padding: '20px 20px 12px' }}>
        <GenreGrid picked={picked}/>
      </div>

      {/* Error band — sits between grid and CTA */}
      <div style={{ padding: '0 20px 12px' }}>
        <div style={{
          padding: '12px 14px',
          background: 'rgba(225,72,72,0.12)',
          border: '1px solid rgba(225,72,72,0.35)',
          borderRadius: 12,
          display: 'flex', alignItems: 'flex-start', gap: 10,
        }}>
          <div style={{ width: 8, height: 8, borderRadius: 4, background: '#E14848', marginTop: 6, flexShrink: 0 }}/>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 13, fontWeight: 600, color: '#FFB3B3' }}>Salvataggio non riuscito</div>
            <div style={{ fontSize: 11.5, color: 'rgba(255,179,179,0.72)', marginTop: 2, lineHeight: 1.45 }}>
              Verifica la connessione e riprova. Codice: <span style={{ fontFamily: T.MONO }}>onboarding/seed-genres</span>
            </div>
          </div>
          <button style={{ padding: '6px 10px', borderRadius: 999, border: '1px solid rgba(255,179,179,0.35)', background: 'transparent', color: '#FFB3B3', fontSize: 11, fontWeight: 700, fontFamily: T.MONO, letterSpacing: 0.5, cursor: 'pointer' }}>
            RIPROVA
          </button>
        </div>
      </div>

      <div style={{ padding: '12px 20px 32px', display: 'flex', alignItems: 'center', gap: 12, borderTop: '1px solid rgba(255,255,255,0.06)' }}>
        <button style={{ padding: '12px 18px', background: 'transparent', border: 'none', color: T.TEXT_LO, fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>Salta</button>
        <div style={{ flex: 1, fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO2, textAlign: 'center' }}>{picked.size} selezionati</div>
        <button style={{
          padding: '12px 22px', borderRadius: 999, border: 'none', cursor: 'pointer',
          background: T.ACCENT, color: '#0A0A0A', fontSize: 14, fontWeight: 700, fontFamily: T.FONT,
        }}>Continua</button>
      </div>
    </div>
  );
}

// ── 0c · OnboardingScreen · saving state ────────────────
// Spinner inside CTA while seedGenres is in flight.
function OnboardingSavingScreen() {
  const picked = new Set(['Indie', 'Elettronica', 'Jazz']);
  return (
    <div style={{ width: '100%', height: '100%', background: `linear-gradient(180deg, #1F1F1F 0%, #080808 100%)`, color: T.TEXT_HI, fontFamily: T.FONT, display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '64px 24px 12px' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 8 }}>
          // PASSO 1 / 1
        </div>
        <div style={{ fontSize: 28, fontWeight: 800, letterSpacing: -0.6, lineHeight: 1.1, marginBottom: 8 }}>
          Cosa ascolti?
        </div>
        <div style={{ fontSize: 13.5, color: T.TEXT_LO, lineHeight: 1.5 }}>
          Scegli almeno 3 generi. Useremo questo segnale per costruire il tuo Daily&nbsp;Mix.
        </div>
      </div>

      <div style={{ flex: 1, padding: '20px 20px 12px' }}>
        <GenreGrid picked={picked} dim/>
      </div>

      <div style={{ padding: '12px 20px 32px', display: 'flex', alignItems: 'center', gap: 12, borderTop: '1px solid rgba(255,255,255,0.06)' }}>
        <button disabled style={{ padding: '12px 18px', background: 'transparent', border: 'none', color: T.TEXT_LO2, fontSize: 14, fontWeight: 600, cursor: 'not-allowed', opacity: 0.5 }}>Salta</button>
        <div style={{ flex: 1, fontFamily: T.MONO, fontSize: 11, color: T.ACCENT, textAlign: 'center', letterSpacing: 0.5 }}>SALVATAGGIO…</div>
        <button disabled style={{
          padding: '12px 22px', borderRadius: 999, border: 'none', cursor: 'wait',
          background: T.ACCENT, color: '#0A0A0A', fontSize: 14, fontWeight: 700, fontFamily: T.FONT,
          display: 'inline-flex', alignItems: 'center', gap: 8,
        }}>
          <div style={{
            width: 12, height: 12, borderRadius: 6,
            border: '2px solid rgba(10,10,10,0.25)',
            borderTopColor: '#0A0A0A',
            animation: 'mh-spin 0.8s linear infinite',
          }}/>
          Salvo…
        </button>
      </div>
      <style>{`@keyframes mh-spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}

// ── 0d · OnboardingScreen · "Scegli ancora N" CTA ──────
// Dynamic CTA label below the 3-genre threshold.
function OnboardingNeedsMoreScreen() {
  const picked = new Set(['Indie']);
  const need = 3 - picked.size;
  return (
    <div style={{ width: '100%', height: '100%', background: `linear-gradient(180deg, #1F1F1F 0%, #080808 100%)`, color: T.TEXT_HI, fontFamily: T.FONT, display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '64px 24px 12px' }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 8 }}>
          // PASSO 1 / 1
        </div>
        <div style={{ fontSize: 28, fontWeight: 800, letterSpacing: -0.6, lineHeight: 1.1, marginBottom: 8 }}>
          Cosa ascolti?
        </div>
        <div style={{ fontSize: 13.5, color: T.TEXT_LO, lineHeight: 1.5 }}>
          Scegli almeno 3 generi. Useremo questo segnale per costruire il tuo Daily&nbsp;Mix.
        </div>
      </div>

      <div style={{ flex: 1, padding: '20px 20px 12px' }}>
        <GenreGrid picked={picked}/>
      </div>

      <div style={{ padding: '12px 20px 32px', display: 'flex', alignItems: 'center', gap: 12, borderTop: '1px solid rgba(255,255,255,0.06)' }}>
        <button style={{ padding: '12px 18px', background: 'transparent', border: 'none', color: T.TEXT_LO, fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>Salta</button>
        <div style={{ flex: 1, fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO2, textAlign: 'center' }}>{picked.size} / 3 minimo</div>
        <button style={{
          padding: '12px 18px', borderRadius: 999,
          border: '1px dashed rgba(168,224,78,0.5)', cursor: 'not-allowed',
          background: 'rgba(168,224,78,0.08)', color: T.ACCENT,
          fontSize: 13.5, fontWeight: 700, fontFamily: T.FONT,
        }}>
          Scegli ancora {need}
        </button>
      </div>
    </div>
  );
}

// ── 0e · LoginScreen · signing-in ───────────────────────
// Spinner while Google credential exchange is in flight.
function LoginSigningInScreen() {
  return (
    <div style={{
      width: '100%', height: '100%', color: T.TEXT_HI, fontFamily: T.FONT,
      background: `radial-gradient(120% 60% at 50% 0%, #2A4615 0%, ${T.BG_TOP} 35%, ${T.BG_BOTTOM} 100%)`,
      display: 'flex', flexDirection: 'column', padding: '88px 28px 40px',
    }}>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <div style={{
          width: 92, height: 92, borderRadius: 22,
          background: `linear-gradient(180deg, ${T.ACCENT} 0%, ${T.ACCENT_DIM} 100%)`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 24px 48px -12px rgba(168,224,78,0.45)',
          marginBottom: 32,
        }}>
          <svg width="56" height="56" viewBox="0 0 108 108">
            {[{x:24,h:14,w:5},{x:32,h:30,w:5},{x:40,h:22,w:5},{x:48,h:54,w:6},{x:57,h:40,w:5},{x:65,h:62,w:6},{x:74,h:28,w:5},{x:82,h:18,w:5}].map((b,i)=>(
              <rect key={i} x={b.x-b.w/2} y={54-b.h/2} width={b.w} height={b.h} rx={b.w/2} fill="#0A0A0A"/>
            ))}
          </svg>
        </div>

        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 12 }}>
          // BENVENUTO
        </div>
        <div style={{ fontSize: 32, fontWeight: 800, letterSpacing: -0.8, marginBottom: 12, textAlign: 'center', lineHeight: 1.1 }}>
          MusicHub
        </div>
        <div style={{ fontSize: 14, color: T.TEXT_LO, textAlign: 'center', maxWidth: 280, lineHeight: 1.5 }}>
          Per ascoltare la tua libreria, accedi con Google.
        </div>
      </div>

      <button disabled style={{
        width: '100%', padding: '14px 16px', border: 'none', borderRadius: 999,
        background: 'rgba(255,255,255,0.85)', color: '#1F1F1F', cursor: 'wait',
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12,
        fontSize: 15, fontWeight: 600, fontFamily: T.FONT,
        boxShadow: '0 8px 24px rgba(0,0,0,0.4)',
      }}>
        <div style={{
          width: 16, height: 16, borderRadius: 8,
          border: '2px solid rgba(31,31,31,0.2)',
          borderTopColor: '#1F1F1F',
          animation: 'mh-spin 0.8s linear infinite',
        }}/>
        Accesso in corso…
      </button>
      <div style={{ marginTop: 14, fontSize: 11, color: T.TEXT_LO2, textAlign: 'center', lineHeight: 1.5, fontFamily: T.MONO }}>
        auth/google · credential-exchange
      </div>
      <style>{`@keyframes mh-spin { to { transform: rotate(360deg); } }`}</style>
    </div>
  );
}

// ── 0f · LoginScreen · picker-cancel toast ──────────────
// AuthViewModel.kt:59-62 — user closed Google picker without choosing.
// Stays on Login but flashes a soft toast (vs full red banner of network-error).
function LoginPickerCancelScreen() {
  return (
    <div style={{
      position: 'relative',
      width: '100%', height: '100%', color: T.TEXT_HI, fontFamily: T.FONT,
      background: `radial-gradient(120% 60% at 50% 0%, #2A4615 0%, ${T.BG_TOP} 35%, ${T.BG_BOTTOM} 100%)`,
      display: 'flex', flexDirection: 'column', padding: '88px 28px 40px',
    }}>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <div style={{
          width: 92, height: 92, borderRadius: 22,
          background: `linear-gradient(180deg, ${T.ACCENT} 0%, ${T.ACCENT_DIM} 100%)`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 24px 48px -12px rgba(168,224,78,0.45)',
          marginBottom: 32,
        }}>
          <svg width="56" height="56" viewBox="0 0 108 108">
            {[{x:24,h:14,w:5},{x:32,h:30,w:5},{x:40,h:22,w:5},{x:48,h:54,w:6},{x:57,h:40,w:5},{x:65,h:62,w:6},{x:74,h:28,w:5},{x:82,h:18,w:5}].map((b,i)=>(
              <rect key={i} x={b.x-b.w/2} y={54-b.h/2} width={b.w} height={b.h} rx={b.w/2} fill="#0A0A0A"/>
            ))}
          </svg>
        </div>

        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 12 }}>
          // BENVENUTO
        </div>
        <div style={{ fontSize: 32, fontWeight: 800, letterSpacing: -0.8, marginBottom: 12, textAlign: 'center', lineHeight: 1.1 }}>
          MusicHub
        </div>
        <div style={{ fontSize: 14, color: T.TEXT_LO, textAlign: 'center', maxWidth: 280, lineHeight: 1.5 }}>
          Per ascoltare la tua libreria, accedi con Google.
        </div>
      </div>

      <button style={{
        width: '100%', padding: '14px 16px', border: 'none', borderRadius: 999,
        background: '#FFFFFF', color: '#1F1F1F', cursor: 'pointer',
        display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 12,
        fontSize: 15, fontWeight: 600, fontFamily: T.FONT,
        boxShadow: '0 8px 24px rgba(0,0,0,0.4)',
      }}>
        <svg width="20" height="20" viewBox="0 0 48 48">
          <path fill="#FFC107" d="M43.6 20.5H42V20H24v8h11.3a12 12 0 11-3.5-13l5.7-5.7C33.6 6.5 29 4.5 24 4.5 13.2 4.5 4.5 13.2 4.5 24S13.2 43.5 24 43.5c10.8 0 19.5-8.7 19.5-19.5 0-1.2-.1-2.3-.4-3.5z"/>
          <path fill="#FF3D00" d="M6.3 14.7l6.6 4.8C14.7 15.4 19 12.5 24 12.5c3 0 5.7 1.1 7.8 3l5.7-5.7C33.6 6.5 29 4.5 24 4.5c-7.4 0-13.8 4.2-17.7 10.2z"/>
          <path fill="#4CAF50" d="M24 43.5c4.9 0 9.4-1.9 12.8-5l-5.9-5c-2 1.4-4.4 2.2-6.9 2.2a12 12 0 01-11.3-7.9l-6.5 5C9.7 39.1 16.3 43.5 24 43.5z"/>
          <path fill="#1976D2" d="M43.6 20.5H42V20H24v8h11.3a12 12 0 01-4.1 5.5l5.9 5c-.4.4 6.4-4.7 6.4-14.5 0-1.2-.1-2.3-.4-3.5z"/>
        </svg>
        Accedi con Google
      </button>
      <div style={{ marginTop: 14, fontSize: 11, color: T.TEXT_LO2, textAlign: 'center', lineHeight: 1.5 }}>
        Continuando accetti i Termini e l'Informativa privacy.
      </div>

      {/* Soft toast — picker cancelled */}
      <div style={{
        position: 'absolute', left: 16, right: 16, bottom: 130,
        padding: '10px 14px', borderRadius: 12,
        background: 'rgba(28,28,28,0.96)',
        border: '1px solid rgba(255,255,255,0.08)',
        boxShadow: '0 12px 32px rgba(0,0,0,0.5)',
        display: 'flex', alignItems: 'center', gap: 10,
        backdropFilter: 'blur(8px)',
      }}>
        <div style={{ width: 6, height: 6, borderRadius: 3, background: T.TEXT_LO, flexShrink: 0 }}/>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ fontSize: 12.5, fontWeight: 600, color: T.TEXT_HI }}>Accesso annullato</div>
          <div style={{ fontSize: 10.5, color: T.TEXT_LO, fontFamily: T.MONO, marginTop: 1 }}>auth/picker-cancel</div>
        </div>
      </div>
    </div>
  );
}

// ── 0g · OnboardingSheet · 3-feature explainer ──────────
// Replaces the mockup's hero treatment with the impl's
// LibraryMusic / MusicNote / PlayCircle three-row explainer.
function OnboardingSheetExplainer() {
  const features = [
    { icon: 'library', title: 'La tua libreria, ovunque', body: 'Importa playlist da Spotify e ritrova tutto in un posto solo.' },
    { icon: 'note',    title: 'Daily Mix che impara', body: 'Più ascolti, più i suggerimenti diventano tuoi.' },
    { icon: 'play',    title: 'Offline · senza interruzioni', body: 'Scarica album e playlist per il viaggio o la metro.' },
  ];
  const Icon = ({ kind }) => {
    const common = { width: 22, height: 22, viewBox: '0 0 24 24', fill: 'none' };
    if (kind === 'library') return (
      <svg {...common}><path d="M4 5h2v14H4zM8 5h2v14H8zM13 5l7 2-3 13-7-2z" stroke="#0A0A0A" strokeWidth="1.6" strokeLinejoin="round"/></svg>
    );
    if (kind === 'note') return (
      <svg {...common}><path d="M9 18V6l10-2v12" stroke="#0A0A0A" strokeWidth="1.6" strokeLinejoin="round"/><circle cx="7" cy="18" r="2.5" stroke="#0A0A0A" strokeWidth="1.6"/><circle cx="17" cy="16" r="2.5" stroke="#0A0A0A" strokeWidth="1.6"/></svg>
    );
    return (
      <svg {...common}><circle cx="12" cy="12" r="9" stroke="#0A0A0A" strokeWidth="1.6"/><path d="M10 8.5v7l6-3.5z" fill="#0A0A0A"/></svg>
    );
  };

  return (
    <div style={{ width: '100%', height: '100%', background: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'flex-end', fontFamily: T.FONT }}>
      <div style={{
        width: '100%', borderRadius: '24px 24px 0 0',
        background: `linear-gradient(180deg, #233A12 0%, #131313 60%)`,
        padding: '20px 24px 36px', color: T.TEXT_HI,
      }}>
        <div style={{ width: 36, height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.18)', margin: '0 auto 18px' }}/>

        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 8 }}>
          // BENVENUTO IN MUSICHUB
        </div>
        <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.4, marginBottom: 18, lineHeight: 1.15 }}>
          La tua libreria,<br/>il tuo ritmo.
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: 14, marginBottom: 26 }}>
          {features.map((f, i) => (
            <div key={i} style={{ display: 'flex', alignItems: 'flex-start', gap: 14 }}>
              <div style={{
                width: 40, height: 40, borderRadius: 12, flexShrink: 0,
                background: `linear-gradient(180deg, ${T.ACCENT} 0%, ${T.ACCENT_DIM} 100%)`,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
              }}>
                <Icon kind={f.icon}/>
              </div>
              <div style={{ flex: 1, minWidth: 0, paddingTop: 1 }}>
                <div style={{ fontSize: 14, fontWeight: 700, color: T.TEXT_HI, marginBottom: 2 }}>{f.title}</div>
                <div style={{ fontSize: 12.5, color: T.TEXT_LO, lineHeight: 1.45 }}>{f.body}</div>
              </div>
            </div>
          ))}
        </div>

        <button style={{
          width: '100%', padding: '14px 16px', borderRadius: 999, border: 'none',
          background: T.ACCENT, color: '#0A0A0A', fontWeight: 700, fontSize: 15, cursor: 'pointer',
        }}>Inizia</button>
      </div>
    </div>
  );
}

window.MHAuthStates = {
  AuthProbeScreen,
  OnboardingErrorScreen,
  OnboardingSavingScreen,
  OnboardingNeedsMoreScreen,
  LoginSigningInScreen,
  LoginPickerCancelScreen,
  OnboardingSheetExplainer,
};
