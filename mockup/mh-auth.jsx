// MusicHub — Auth & Onboarding
const { T, I, MHCover, MHLogo, MHScreen } = window.MH;

// ── 1.1 LoginScreen ─────────────────────────────────────
function LoginScreen({ state = 'idle' }) {
  const isError = state === 'error';
  return (
    <div style={{
      width: '100%', height: '100%', color: T.TEXT_HI, fontFamily: T.FONT,
      background: `radial-gradient(120% 60% at 50% 0%, #2A4615 0%, ${T.BG_TOP} 35%, ${T.BG_BOTTOM} 100%)`,
      display: 'flex', flexDirection: 'column', padding: '88px 28px 40px',
    }}>
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        {/* Hero monogram tile */}
        <div style={{
          width: 92, height: 92, borderRadius: 22,
          background: `linear-gradient(180deg, ${T.ACCENT} 0%, ${T.ACCENT_DIM} 100%)`,
          display: 'flex', alignItems: 'center', justifyContent: 'center',
          boxShadow: '0 24px 48px -12px rgba(168,224,78,0.45)',
          marginBottom: 32,
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

        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 12 }}>
          // BENVENUTO
        </div>
        <div style={{ fontSize: 32, fontWeight: 800, letterSpacing: -0.8, marginBottom: 12, textAlign: 'center', lineHeight: 1.1 }}>
          MusicHub
        </div>
        <div style={{ fontSize: 14, color: T.TEXT_LO, textAlign: 'center', maxWidth: 280, lineHeight: 1.5 }}>
          Per ascoltare la tua libreria, accedi con Google.
        </div>

        {isError && (
          <div style={{
            marginTop: 36, width: '100%',
            padding: '12px 14px', background: 'rgba(225,72,72,0.12)',
            border: '1px solid rgba(225,72,72,0.35)', borderRadius: 10,
            display: 'flex', alignItems: 'flex-start', gap: 10,
          }}>
            <div style={{ width: 8, height: 8, borderRadius: 4, background: '#E14848', marginTop: 6, flexShrink: 0 }}/>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 13, fontWeight: 600, color: '#FFB3B3' }}>Accesso non riuscito</div>
              <div style={{ fontSize: 11.5, color: 'rgba(255,179,179,0.7)', marginTop: 2, lineHeight: 1.4 }}>Verifica la connessione e riprova. Codice: <span style={{fontFamily: T.MONO}}>auth/network-error</span></div>
            </div>
          </div>
        )}
      </div>

      {/* Google CTA */}
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
        {state === 'signing-in' ? 'Accesso in corso…' : 'Accedi con Google'}
      </button>
      <div style={{ marginTop: 14, fontSize: 11, color: T.TEXT_LO2, textAlign: 'center', lineHeight: 1.5 }}>
        Continuando accetti i Termini e l'Informativa privacy.
      </div>
    </div>
  );
}

// ── 1.2 OnboardingScreen ────────────────────────────────
const GENRES = [
  'Indie', 'Elettronica', 'Hip-hop', 'Jazz', 'Classica', 'Ambient',
  'Rock', 'Pop', 'R&B', 'Lo-fi', 'Punk', 'Cantautorato',
];

function OnboardingScreen() {
  const [picked, setPicked] = React.useState(new Set(['Indie', 'Elettronica', 'Jazz']));
  const toggle = g => {
    const n = new Set(picked);
    n.has(g) ? n.delete(g) : n.add(g);
    setPicked(n);
  };
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

      <div style={{ flex: 1, padding: '20px 20px 12px', display: 'flex', flexWrap: 'wrap', gap: 10, alignContent: 'flex-start' }}>
        {GENRES.map(g => {
          const on = picked.has(g);
          return (
            <button key={g} onClick={() => toggle(g)} style={{
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

      <div style={{ padding: '12px 20px 32px', display: 'flex', alignItems: 'center', gap: 12, borderTop: '1px solid rgba(255,255,255,0.06)' }}>
        <button style={{ padding: '12px 18px', background: 'transparent', border: 'none', color: T.TEXT_LO, fontSize: 14, fontWeight: 600, cursor: 'pointer' }}>Salta</button>
        <div style={{ flex: 1, fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO2, textAlign: 'center' }}>{picked.size} selezionati</div>
        <button style={{
          padding: '12px 22px', borderRadius: 999, border: 'none', cursor: 'pointer',
          background: T.ACCENT, color: '#0A0A0A', fontSize: 14, fontWeight: 700, fontFamily: T.FONT,
          opacity: picked.size >= 3 ? 1 : 0.4,
        }}>Continua</button>
      </div>
    </div>
  );
}

// ── 1.2b OnboardingSheet (welcome) ──────────────────────
function OnboardingSheet() {
  return (
    <div style={{ width: '100%', height: '100%', background: 'rgba(0,0,0,0.7)', display: 'flex', alignItems: 'flex-end', fontFamily: T.FONT }}>
      <div style={{
        width: '100%', borderRadius: '24px 24px 0 0',
        background: `linear-gradient(180deg, #2A4615 0%, #181818 60%)`,
        padding: '24px 24px 40px', color: T.TEXT_HI,
      }}>
        <div style={{ width: 36, height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.18)', margin: '0 auto 24px' }}/>

        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 20 }}>
          <div style={{
            width: 76, height: 76, borderRadius: 18,
            background: `linear-gradient(180deg, ${T.ACCENT} 0%, ${T.ACCENT_DIM} 100%)`,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
          }}>
            <svg width="48" height="48" viewBox="0 0 108 108">
              {[{x:24,h:14,w:5},{x:32,h:30,w:5},{x:40,h:22,w:5},{x:48,h:54,w:6},{x:57,h:40,w:5},{x:65,h:62,w:6},{x:74,h:28,w:5},{x:82,h:18,w:5}].map((b,i)=>(
                <rect key={i} x={b.x-b.w/2} y={54-b.h/2} width={b.w} height={b.h} rx={b.w/2} fill="#0A0A0A"/>
              ))}
            </svg>
          </div>
        </div>

        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textAlign: 'center', marginBottom: 8 }}>
          // BENVENUTO IN MUSICHUB
        </div>
        <div style={{ fontSize: 24, fontWeight: 800, letterSpacing: -0.5, textAlign: 'center', marginBottom: 10 }}>
          La tua libreria,<br/>il tuo ritmo.
        </div>
        <div style={{ fontSize: 13.5, color: T.TEXT_LO, textAlign: 'center', lineHeight: 1.55, marginBottom: 28, padding: '0 4px' }}>
          Importa playlist da Spotify, scarica per l'offline e lascia che Daily Mix impari cosa ti piace.
        </div>

        <button style={{
          width: '100%', padding: '14px 16px', borderRadius: 999, border: 'none',
          background: T.ACCENT, color: '#0A0A0A', fontWeight: 700, fontSize: 15, cursor: 'pointer',
        }}>Inizia</button>
      </div>
    </div>
  );
}

// ── 1.3 Account-switch dialog ───────────────────────────
function AccountSwitchDialog() {
  return (
    <div style={{ width: '100%', height: '100%', background: 'rgba(0,0,0,0.65)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: T.FONT, padding: 24 }}>
      <div style={{
        width: '100%', maxWidth: 320, borderRadius: 16,
        background: '#1A1A1A', border: '1px solid rgba(255,255,255,0.08)',
        color: T.TEXT_HI, padding: 22,
      }}>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 8 }}>
          // CAMBIA ACCOUNT
        </div>
        <div style={{ fontSize: 18, fontWeight: 700, letterSpacing: -0.3, marginBottom: 10 }}>
          Disconnettersi da MusicHub?
        </div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, lineHeight: 1.55, marginBottom: 18 }}>
          La libreria scaricata e i preferiti restano sul dispositivo. La sincronizzazione con il cloud si interrompe finché non accedi di nuovo.
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 12px', background: 'rgba(255,255,255,0.04)', borderRadius: 10, marginBottom: 18 }}>
          <div style={{ width: 32, height: 32, borderRadius: 999, background: 'linear-gradient(135deg, #5C2D8C 0%, #F0A6B0 100%)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 13, fontWeight: 700 }}>L</div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 13, fontWeight: 600 }}>luca.r@gmail.com</div>
            <div style={{ fontSize: 11, color: T.TEXT_LO, fontFamily: T.MONO }}>Account corrente</div>
          </div>
        </div>

        <div style={{ display: 'flex', gap: 10 }}>
          <button style={{ flex: 1, padding: '12px 16px', background: 'rgba(255,255,255,0.06)', border: 'none', borderRadius: 999, color: T.TEXT_HI, fontWeight: 600, fontSize: 14, cursor: 'pointer' }}>Annulla</button>
          <button style={{ flex: 1, padding: '12px 16px', background: '#E14848', border: 'none', borderRadius: 999, color: '#fff', fontWeight: 700, fontSize: 14, cursor: 'pointer' }}>Disconnetti</button>
        </div>
      </div>
    </div>
  );
}

window.MHAuth = { LoginScreen, OnboardingScreen, OnboardingSheet, AccountSwitchDialog };
