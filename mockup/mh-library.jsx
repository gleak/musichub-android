// MusicHub — Library drill-downs + Collab playlists
const { T, I, MHCover, MHScreen, MHPlayingBars } = window.MH;

// ── 5.1 AlbumListScreen ─────────────────────────────────
function AlbumListScreen() {
  const albums = [
    { title: 'Slow Hours', artist: 'Helena Vorr', k: 'wave', p: { a:'#5C2D8C', b:'#F0A6B0' } },
    { title: 'Echo, Vol. III', artist: 'echo', k: 'type', p: { bg:'#E8E2D5', fg:'#222' } },
    { title: 'Burnt Letters', artist: 'The Tessera', k: 'grid', p: { a:'#0E1F3A', b: T.ACCENT } },
    { title: 'Halflight', artist: 'Iso Tide', k: 'triangles', p: { bg:'#1A1A1A', fg:'#FF4D2E' } },
    { title: 'Night Mode', artist: 'Mira Holt', k: 'dot', p: { bg:'#0B3D2E', fg: T.ACCENT } },
    { title: 'Open Skies', artist: 'Lana Verdier', k: 'arc', p: { a:'#FF6B5B', b:'#3A1F8A' } },
    { title: 'Glove Compartment', artist: 'Iso Tide', k: 'stripes', p: { a:'#FFC857', b:'#1A1A1A' } },
    { title: 'Strange Mercy', artist: 'Mira Holt', k: 'duotone', p: { a:'#3A0CA3', b:'#F72585' } },
  ];
  return (
    <MHScreen navActive="library">
      <div style={{ padding: '0 16px 4px', display: 'flex', alignItems: 'center', gap: 12 }}>
        <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 4, marginLeft: -4 }}><I.Back/></button>
        <div style={{ flex: 1 }}>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase' }}>// LIBRERIA</div>
          <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.5 }}>Album · <span style={{ fontFamily: T.MONO, color: T.TEXT_LO, fontWeight: 600, fontSize: 16 }}>{albums.length}</span></div>
        </div>
      </div>
      <div style={{ padding: '12px 16px', display: 'flex', alignItems: 'center', gap: 8 }}>
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 10, padding: '10px 12px', background: T.CARD, borderRadius: 10 }}>
          <I.Search size={16} color={T.TEXT_LO}/>
          <div style={{ fontSize: 13, color: T.TEXT_LO2 }}>Cerca album…</div>
        </div>
        <button style={{ padding: '10px 14px', background: T.CARD, border: 'none', borderRadius: 10, color: T.TEXT_HI, fontSize: 12, fontWeight: 600, cursor: 'pointer', fontFamily: T.FONT, display: 'flex', alignItems: 'center', gap: 6 }}>
          <I.Filter size={14}/> Recenti
        </button>
      </div>
      <div style={{ padding: '10px 16px 16px', display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 14 }}>
        {albums.map((a, i) => (
          <div key={i}>
            <div style={{ aspectRatio: '1 / 1', marginBottom: 8 }}>
              <MHCover kind={a.k} palette={a.p} radius={8}/>
            </div>
            <div style={{ fontSize: 13, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{a.title}</div>
            <div style={{ fontSize: 11.5, color: T.TEXT_LO, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{a.artist}</div>
          </div>
        ))}
      </div>
    </MHScreen>
  );
}

// ── 5.2 ArtistListScreen ────────────────────────────────
function ArtistListScreen() {
  const artists = [
    { n: 'Helena Vorr', k: 'artist', p: { bg:'#2A1E12', fg:'#E8DCC4' } },
    { n: 'Iso Tide', k: 'artist', p: { bg:'#0E1F3A', fg:'#06B6D4' } },
    { n: 'Lana Verdier', k: 'artist', p: { bg:'#3A1F8A', fg:'#F0A6B0' } },
    { n: 'Mira Holt', k: 'artist', p: { bg:'#5C2D8C', fg:'#FFC857' } },
    { n: 'echo', k: 'artist', p: { bg:'#1A1A1A', fg:'#E8E2D5' } },
    { n: 'The Tessera', k: 'artist', p: { bg:'#0B3D2E', fg: T.ACCENT } },
    { n: 'Veronica Sand', k: 'artist', p: { bg:'#3A0CA3', fg:'#F72585' } },
    { n: 'Yoke', k: 'artist', p: { bg:'#FFC857', fg:'#1A1A1A' } },
  ];
  return (
    <MHScreen navActive="library">
      <div style={{ padding: '0 16px 4px', display: 'flex', alignItems: 'center', gap: 12 }}>
        <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 4, marginLeft: -4 }}><I.Back/></button>
        <div style={{ flex: 1 }}>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase' }}>// LIBRERIA</div>
          <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.5 }}>Artisti · <span style={{ fontFamily: T.MONO, color: T.TEXT_LO, fontWeight: 600, fontSize: 16 }}>{artists.length}</span></div>
        </div>
      </div>
      <div style={{ display: 'flex' }}>
        <div style={{ flex: 1, padding: '12px 12px 12px 16px' }}>
          {artists.map((a, i) => (
            <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '8px 0' }}>
              <div style={{ width: 56, height: 56, flexShrink: 0, borderRadius: 999, overflow: 'hidden' }}>
                <MHCover kind={a.k} palette={a.p}/>
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 15, fontWeight: 600 }}>{a.n}</div>
                <div style={{ fontSize: 12, color: T.TEXT_LO, fontFamily: T.MONO }}>Artista</div>
              </div>
              <I.Chevron size={20}/>
            </div>
          ))}
        </div>
        {/* A-Z scrub */}
        <div style={{ width: 18, padding: '12px 4px 12px 0', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 1 }}>
          {['A','B','C','D','E','F','G','H','I','L','M','N','O','P','Q','R','S','T','V','Y','Z'].map(l => (
            <div key={l} style={{
              fontSize: 9, fontFamily: T.MONO, fontWeight: 600,
              color: ['H','I','L','M','T','V','Y','E'].includes(l) ? T.ACCENT : T.TEXT_LO2,
            }}>{l}</div>
          ))}
        </div>
      </div>
    </MHScreen>
  );
}

// ── 5.3 LikedScreen ─────────────────────────────────────
function LikedScreen() {
  const tracks = [
    { title: 'Undertow', artist: 'Helena Vorr', dur: '3:48', kind: 'blob', palette: { a:'#1E3A8A', b:'#06B6D4' } },
    { title: 'Citrine', artist: 'Mira Holt', dur: '4:12', kind: 'wave', palette: { a:'#5C2D8C', b:'#F0A6B0' } },
    { title: 'Petals', artist: 'Lana Verdier', dur: '2:55', kind: 'arc', palette: { a:'#FF6B5B', b:'#3A1F8A' } },
    { title: 'Halflight', artist: 'Iso Tide', dur: '3:30', kind: 'triangles', palette: { bg:'#1A1A1A', fg:'#FF4D2E' } },
    { title: 'Burnt Letters', artist: 'The Tessera', dur: '5:01', kind: 'grid', palette: { a:'#0E1F3A', b: T.ACCENT } },
    { title: 'Open Window', artist: 'Mira Holt', dur: '2:51', kind: 'dot', palette: { bg:'#0B3D2E', fg: T.ACCENT } },
    { title: 'Slow Driver', artist: 'Iso Tide', dur: '4:20', kind: 'duotone', palette: { a:'#3A0CA3', b:'#F72585' } },
    { title: 'Hollow Ave', artist: 'Lana Verdier', dur: '3:01', kind: 'stripes', palette: { a:'#FFC857', b:'#1A1A1A' } },
  ];
  return (
    <MHScreen navActive="library" gradient={`linear-gradient(180deg, #3A0CA3 0%, #F72585 30%, ${T.BG_BOTTOM} 60%)`}>
      <div style={{ padding: '0 16px 12px', display: 'flex', alignItems: 'center' }}>
        <button style={{ background: 'rgba(0,0,0,0.3)', backdropFilter: 'blur(10px)', border: 'none', borderRadius: 999, width: 36, height: 36, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.Back/></button>
      </div>
      {/* Hero */}
      <div style={{ padding: '12px 16px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <div style={{ width: 200, height: 200, marginBottom: 16, boxShadow: '0 24px 48px rgba(0,0,0,0.5)' }}>
          <MHCover kind="duotone" palette={{ a:'#3A0CA3', b:'#F72585' }} radius={10}/>
        </div>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 6 }}>// LIBRERIA · MI PIACE</div>
        <div style={{ fontSize: 28, fontWeight: 800, letterSpacing: -0.6 }}>Brani che ti piacciono</div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, marginTop: 4, fontFamily: T.MONO }}>284 brani · 18h 42m</div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginTop: 22 }}>
          <button style={{ width: 44, height: 44, borderRadius: 999, background: 'transparent', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.Shuffle size={22} color={T.ACCENT}/></button>
          <button style={{ width: 56, height: 56, borderRadius: 999, background: T.ACCENT, border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 12px 28px rgba(168,224,78,0.5)' }}>
            <I.Play size={20} color="#0A0A0A"/>
          </button>
          <button style={{ width: 44, height: 44, borderRadius: 999, background: 'transparent', border: 'none', cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.Download size={22}/></button>
        </div>
      </div>

      <div style={{ padding: '0 16px' }}>
        {tracks.map((t, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '10px 0' }}>
            <div style={{ fontFamily: T.MONO, fontSize: 11, color: T.TEXT_LO2, width: 18, textAlign: 'right' }}>{i + 1}</div>
            <div style={{ width: 44, height: 44, flexShrink: 0 }}><MHCover kind={t.kind} palette={t.palette} radius={4}/></div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 600, color: i === 0 ? T.ACCENT : T.TEXT_HI, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap', display: 'flex', alignItems: 'center', gap: 6 }}>
                {t.title}
                {i === 0 && <MHPlayingBars/>}
              </div>
              <div style={{ fontSize: 12, color: T.TEXT_LO }}>{t.artist} · <span style={{ fontFamily: T.MONO }}>{t.dur}</span></div>
            </div>
            <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 6 }}><I.Heart filled color={T.ACCENT}/></button>
            <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 6 }}><I.More/></button>
          </div>
        ))}
      </div>
    </MHScreen>
  );
}

// ── 5.4 GenreDetailScreen ───────────────────────────────
function GenreDetailScreen() {
  const tracks = [
    { title: 'Citrine', artist: 'Mira Holt · Slow Hours', dur: '4:12', kind: 'wave', palette: { a:'#5C2D8C', b:'#F0A6B0' } },
    { title: 'Halflight', artist: 'Iso Tide · Halflight', dur: '3:30', kind: 'triangles', palette: { bg:'#1A1A1A', fg:'#FF4D2E' } },
    { title: 'Petals', artist: 'Lana Verdier · Open Skies', dur: '2:55', kind: 'arc', palette: { a:'#FF6B5B', b:'#3A1F8A' } },
    { title: 'Burnt Letters', artist: 'The Tessera · Burnt Letters', dur: '5:01', kind: 'grid', palette: { a:'#0E1F3A', b: T.ACCENT } },
    { title: 'Slow Driver', artist: 'Iso Tide · Halflight', dur: '4:20', kind: 'duotone', palette: { a:'#3A0CA3', b:'#F72585' } },
    { title: 'Hollow Ave', artist: 'Lana Verdier · Open Skies', dur: '3:01', kind: 'stripes', palette: { a:'#FFC857', b:'#1A1A1A' } },
    { title: 'Open Window', artist: 'Mira Holt · Strange Mercy', dur: '2:51', kind: 'dot', palette: { bg:'#0B3D2E', fg: T.ACCENT } },
  ];
  return (
    <MHScreen navActive="search">
      <div style={{ padding: '0 16px 6px', display: 'flex', alignItems: 'center', gap: 12 }}>
        <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 4, marginLeft: -4 }}><I.Back/></button>
        <div style={{ flex: 1 }}>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase' }}>// SFOGLIA · GENERE</div>
          <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.5 }}>Indie</div>
        </div>
      </div>

      {/* Removable pill + search */}
      <div style={{ padding: '8px 16px 10px', display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6, padding: '7px 6px 7px 12px', borderRadius: 999, background: T.ACCENT, color: '#0A0A0A', fontSize: 13, fontWeight: 700, fontFamily: T.FONT }}>
          Indie
          <div style={{ width: 18, height: 18, borderRadius: 9, background: 'rgba(0,0,0,0.15)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.X size={11} color="#0A0A0A"/></div>
        </div>
        <div style={{ display: 'inline-flex', alignItems: 'center', gap: 6, padding: '7px 12px', borderRadius: 999, background: T.CARD, color: T.TEXT_HI, fontSize: 13, fontWeight: 500 }}>
          <I.Filter size={14}/> Più popolari
        </div>
      </div>
      <div style={{ padding: '0 16px 12px' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '10px 12px', background: T.CARD, borderRadius: 10 }}>
          <I.Search size={16} color={T.TEXT_LO}/>
          <div style={{ flex: 1, fontSize: 13, color: T.TEXT_LO2 }}>Cerca dentro Indie…</div>
        </div>
      </div>

      <div style={{ padding: '4px 16px 0', display: 'flex', alignItems: 'center', gap: 8, marginBottom: 8 }}>
        <button style={{ flex: 1, padding: '12px', borderRadius: 999, border: 'none', background: T.ACCENT, color: '#0A0A0A', fontWeight: 700, fontSize: 14, fontFamily: T.FONT, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
          <I.Play size={14} color="#0A0A0A"/> Riproduci tutti
        </button>
        <button style={{ width: 44, height: 44, borderRadius: 999, border: 'none', background: T.CARD, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.Shuffle size={20} color={T.ACCENT}/></button>
      </div>

      <div style={{ padding: '12px 16px 4px', fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase' }}>// 247 BRANI</div>
      <div style={{ padding: '0 16px' }}>
        {tracks.map((t, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 0' }}>
            <div style={{ width: 44, height: 44, flexShrink: 0 }}><MHCover kind={t.kind} palette={t.palette} radius={4}/></div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.title}</div>
              <div style={{ fontSize: 12, color: T.TEXT_LO, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.artist} · <span style={{ fontFamily: T.MONO }}>{t.dur}</span></div>
            </div>
            <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 6 }}><I.Heart/></button>
            <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 6 }}><I.More/></button>
          </div>
        ))}
      </div>
    </MHScreen>
  );
}

// ── 6.x Collab playlist — owner POV ─────────────────────
function CollabPlaylistOwner() {
  return <CollabPlaylistDetail role="owner"/>;
}
function CollabPlaylistMember() {
  return <CollabPlaylistDetail role="member"/>;
}

function CollabPlaylistDetail({ role }) {
  const tracks = [
    { title: 'Citrine', artist: 'Mira Holt', dur: '4:12', kind: 'wave', palette: { a:'#5C2D8C', b:'#F0A6B0' }, by: 'Luca' },
    { title: 'Halflight', artist: 'Iso Tide', dur: '3:30', kind: 'triangles', palette: { bg:'#1A1A1A', fg:'#FF4D2E' }, by: 'Marta' },
    { title: 'Petals', artist: 'Lana Verdier', dur: '2:55', kind: 'arc', palette: { a:'#FF6B5B', b:'#3A1F8A' }, by: 'Marta' },
    { title: 'Burnt Letters', artist: 'The Tessera', dur: '5:01', kind: 'grid', palette: { a:'#0E1F3A', b: T.ACCENT }, by: 'Davide' },
    { title: 'Slow Driver', artist: 'Iso Tide', dur: '4:20', kind: 'duotone', palette: { a:'#3A0CA3', b:'#F72585' }, by: 'Luca' },
    { title: 'Open Window', artist: 'Mira Holt', dur: '2:51', kind: 'dot', palette: { bg:'#0B3D2E', fg: T.ACCENT }, by: 'Marta' },
  ];
  const isOwner = role === 'owner';
  return (
    <MHScreen navActive="library" gradient={`linear-gradient(180deg, #2A4615 0%, ${T.BG_BOTTOM} 280px)`}>
      <div style={{ padding: '0 16px 12px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <button style={{ background: 'rgba(0,0,0,0.3)', border: 'none', borderRadius: 999, width: 36, height: 36, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.Back/></button>
        <button style={{ background: 'rgba(0,0,0,0.3)', border: 'none', borderRadius: 999, width: 36, height: 36, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.More/></button>
      </div>

      <div style={{ padding: '0 16px 20px', display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
        <div style={{ width: 180, height: 180, marginBottom: 14, boxShadow: '0 24px 48px rgba(0,0,0,0.5)' }}>
          <MHCover kind="grid" palette={{ a:'#0E1F3A', b: T.ACCENT }} radius={10}/>
        </div>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 4 }}>// PLAYLIST · COLLABORATIVA</div>
        <div style={{ fontSize: 26, fontWeight: 800, letterSpacing: -0.5 }}>Casa · Cena</div>
        <div style={{ fontSize: 12.5, color: T.TEXT_LO, marginTop: 4, fontFamily: T.MONO }}>62 brani · 4h 12m</div>
      </div>

      {/* Members strip */}
      <div style={{ padding: '0 16px 14px' }}>
        <div style={{ background: T.CARD, borderRadius: 14, padding: '12px 14px', display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ display: 'flex' }}>
            {[
              { c: '#5C2D8C', l: 'L' }, { c: '#FF6B5B', l: 'M' }, { c: '#06B6D4', l: 'D' }, { c: '#FFC857', l: 'A' },
            ].map((a, i) => (
              <div key={i} style={{ width: 28, height: 28, borderRadius: 999, background: a.c, border: '2px solid #181818', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 700, color: '#0A0A0A', marginLeft: i === 0 ? 0 : -8 }}>
                {a.l}
              </div>
            ))}
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 13, fontWeight: 600 }}>
              {isOwner ? 'Condivisa con 4 persone' : 'Condivisa da Luca'}
            </div>
            <div style={{ fontSize: 11, color: T.TEXT_LO, fontFamily: T.MONO }}>{isOwner ? '3 collaboratori attivi' : '+ tu, Marta, Davide, Anna'}</div>
          </div>
          {isOwner ? (
            <button style={{ padding: '7px 14px', borderRadius: 999, background: T.ACCENT, color: '#0A0A0A', border: 'none', fontWeight: 700, fontSize: 12, cursor: 'pointer', fontFamily: T.FONT }}>Gestisci</button>
          ) : (
            <I.Chevron size={20}/>
          )}
        </div>
      </div>

      {/* Auto-sync card */}
      <div style={{ padding: '0 16px 14px' }}>
        <div style={{ background: T.CARD, borderRadius: 14, padding: '14px 14px', display: 'flex', alignItems: 'center', gap: 12 }}>
          <div style={{ width: 36, height: 36, borderRadius: 10, background: 'rgba(168,224,78,0.12)', display: 'flex', alignItems: 'center', justifyContent: 'center' }}>
            <I.Download size={18} color={T.ACCENT}/>
          </div>
          <div style={{ flex: 1, minWidth: 0 }}>
            <div style={{ fontSize: 13.5, fontWeight: 600 }}>Sincronizzazione automatica</div>
            <div style={{ fontSize: 11.5, color: T.TEXT_LO, marginTop: 2 }}>Solo Wi-Fi · 8 nuovi brani</div>
          </div>
          <div style={{ width: 44, height: 26, borderRadius: 13, background: T.ACCENT, position: 'relative', flexShrink: 0 }}>
            <div style={{ position: 'absolute', top: 3, left: 21, width: 20, height: 20, borderRadius: 10, background: '#0A0A0A' }}/>
          </div>
        </div>
      </div>

      {/* CTAs */}
      <div style={{ padding: '0 16px 12px', display: 'flex', alignItems: 'center', gap: 8 }}>
        <button style={{ flex: 1, padding: '12px', borderRadius: 999, border: 'none', background: T.ACCENT, color: '#0A0A0A', fontWeight: 700, fontSize: 14, fontFamily: T.FONT, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8 }}>
          <I.Play size={14} color="#0A0A0A"/> Riproduci
        </button>
        <button style={{ width: 44, height: 44, borderRadius: 999, border: 'none', background: T.CARD, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.Shuffle size={20} color={T.ACCENT}/></button>
        {isOwner && <button style={{ width: 44, height: 44, borderRadius: 999, border: 'none', background: T.CARD, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><I.Share size={20} color={T.ACCENT}/></button>}
      </div>

      {!isOwner && (
        <div style={{ padding: '6px 16px 12px' }}>
          <button style={{ width: '100%', padding: '11px', borderRadius: 999, background: 'rgba(255,255,255,0.06)', border: 'none', color: T.TEXT_LO, fontWeight: 600, fontSize: 13, fontFamily: T.FONT, cursor: 'pointer' }}>
            Rimuovi dalla libreria
          </button>
        </div>
      )}

      <div style={{ padding: '12px 16px 4px', fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.TEXT_LO, textTransform: 'uppercase' }}>// BRANI</div>
      <div style={{ padding: '0 16px' }}>
        {tracks.map((t, i) => (
          <div key={i} style={{ display: 'flex', alignItems: 'center', gap: 12, padding: '8px 0' }}>
            <div style={{ width: 44, height: 44, flexShrink: 0 }}><MHCover kind={t.kind} palette={t.palette} radius={4}/></div>
            <div style={{ flex: 1, minWidth: 0 }}>
              <div style={{ fontSize: 14, fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{t.title}</div>
              <div style={{ fontSize: 12, color: T.TEXT_LO, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                {t.artist} · <span style={{ color: T.ACCENT, fontFamily: T.MONO, fontSize: 10, fontWeight: 700 }}>{t.by.toUpperCase()}</span>
              </div>
            </div>
            <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 6 }}><I.More/></button>
          </div>
        ))}
      </div>
    </MHScreen>
  );
}

// ── 6.3 PlaylistShareDialog ─────────────────────────────
function PlaylistShareDialog() {
  return (
    <div style={{ width: '100%', height: '100%', background: 'rgba(0,0,0,0.55)', display: 'flex', alignItems: 'flex-end', fontFamily: T.FONT }}>
      <div style={{ width: '100%', borderRadius: '20px 20px 0 0', background: '#181818', color: T.TEXT_HI, padding: '14px 0 32px' }}>
        <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 6 }}>
          <div style={{ width: 36, height: 4, borderRadius: 2, background: 'rgba(255,255,255,0.18)' }}/>
        </div>
        <div style={{ padding: '10px 24px 4px' }}>
          <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase', marginBottom: 4 }}>// CONDIVIDI · COLLABORATIVA</div>
          <div style={{ fontSize: 22, fontWeight: 800, letterSpacing: -0.4 }}>Casa · Cena</div>
          <div style={{ fontSize: 12.5, color: T.TEXT_LO, marginTop: 4, lineHeight: 1.5 }}>
            Chi apre il link entra come <b style={{ color: T.TEXT_HI }}>collaboratore</b> della stessa playlist — non viene creata una copia. Solo tu, come creatore, puoi generare nuovi link.
          </div>
        </div>

        {/* Link box */}
        <div style={{ margin: '16px 24px', padding: '12px 14px', background: '#0A0A0A', border: '1px solid rgba(168,224,78,0.2)', borderRadius: 12, display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{ flex: 1, minWidth: 0, fontFamily: T.MONO, fontSize: 12, color: T.TEXT_HI, overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            <span style={{ color: T.TEXT_LO }}>https://mh.duckdns.org/p/</span>x7Q-9FN
          </div>
          <button style={{ padding: '7px 12px', background: T.ACCENT, border: 'none', borderRadius: 999, color: '#0A0A0A', fontWeight: 700, fontSize: 12, cursor: 'pointer', fontFamily: T.FONT }}>Copia</button>
        </div>

        <div style={{ padding: '0 24px 4px' }}>
          <button style={{ width: '100%', padding: '14px', background: 'rgba(255,255,255,0.06)', border: 'none', borderRadius: 12, color: T.TEXT_HI, fontWeight: 600, fontSize: 14, fontFamily: T.FONT, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 10 }}>
            <I.Share size={18} color={T.TEXT_HI}/> Condividi via sistema
          </button>
        </div>

        <div style={{ padding: '14px 24px 0', display: 'flex', alignItems: 'center', gap: 10 }}>
          <div style={{ flex: 1, fontSize: 12, color: T.TEXT_LO, fontFamily: T.MONO }}>4 membri attivi · revoca in Gestisci</div>
          <button style={{ padding: '6px 12px', background: 'transparent', border: 'none', color: '#FF7A7A', fontSize: 12, fontWeight: 600, cursor: 'pointer', fontFamily: T.FONT }}>Revoca link</button>
        </div>
      </div>
    </div>
  );
}

// ── 6.4 PlaylistShareImporter ───────────────────────────
function PlaylistShareImporter({ state = 'first' }) {
  // states: first | member | owner
  return (
    <div style={{ width: '100%', height: '100%', background: `linear-gradient(180deg, #2A4615 0%, ${T.BG_BOTTOM} 60%)`, color: T.TEXT_HI, fontFamily: T.FONT, display: 'flex', flexDirection: 'column' }}>
      <div style={{ padding: '52px 16px 8px', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
        <button style={{ background: 'transparent', border: 'none', cursor: 'pointer', padding: 4 }}><I.X/></button>
        <div style={{ fontFamily: T.MONO, fontSize: 10, fontWeight: 600, letterSpacing: 1.5, color: T.ACCENT, textTransform: 'uppercase' }}>// LINK CONDIVISO</div>
        <div style={{ width: 30 }}/>
      </div>

      <div style={{ flex: 1, padding: '8px 28px 24px', display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center' }}>
        <div style={{ width: 220, height: 220, marginBottom: 22, boxShadow: '0 24px 56px rgba(0,0,0,0.6)' }}>
          <MHCover kind="grid" palette={{ a:'#0E1F3A', b: T.ACCENT }} radius={12}/>
        </div>
        <div style={{ fontSize: 30, fontWeight: 800, letterSpacing: -0.7, textAlign: 'center', lineHeight: 1.05 }}>Casa · Cena</div>
        <div style={{ fontSize: 13, color: T.TEXT_LO, marginTop: 8, textAlign: 'center' }}>Playlist collaborativa di <b style={{ color: T.TEXT_HI }}>Luca R.</b></div>
        <div style={{ fontSize: 12, color: T.TEXT_LO2, marginTop: 6, fontFamily: T.MONO }}>62 BRANI · 4 MEMBRI</div>

        {/* members preview */}
        <div style={{ display: 'flex', marginTop: 18 }}>
          {[{ c: '#5C2D8C', l: 'L' }, { c: '#FF6B5B', l: 'M' }, { c: '#06B6D4', l: 'D' }, { c: '#FFC857', l: 'A' }].map((a, i) => (
            <div key={i} style={{ width: 28, height: 28, borderRadius: 999, background: a.c, border: '2px solid #181818', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: 11, fontWeight: 700, color: '#0A0A0A', marginLeft: i === 0 ? 0 : -8 }}>
              {a.l}
            </div>
          ))}
        </div>

        {state === 'first' && (
          <div style={{ marginTop: 28, fontSize: 13, color: T.TEXT_LO, textAlign: 'center', maxWidth: 280, lineHeight: 1.5 }}>
            Aggiungendola alla libreria potrai aggiungere brani e tutti i collaboratori vedranno le tue modifiche.
          </div>
        )}
        {state === 'member' && (
          <div style={{ marginTop: 28, padding: '8px 14px', borderRadius: 999, background: 'rgba(168,224,78,0.12)', color: T.ACCENT, fontFamily: T.MONO, fontSize: 11, fontWeight: 700, letterSpacing: 1 }}>
            // GIÀ NELLA TUA LIBRERIA
          </div>
        )}
      </div>

      <div style={{ padding: '12px 24px 36px' }}>
        <button style={{ width: '100%', padding: '15px', borderRadius: 999, border: 'none', background: T.ACCENT, color: '#0A0A0A', fontWeight: 700, fontSize: 15, fontFamily: T.FONT, cursor: 'pointer' }}>
          {state === 'first' ? 'Aggiungi alla mia libreria' : 'Apri playlist'}
        </button>
        {state === 'first' && (
          <div style={{ marginTop: 12, fontSize: 11, color: T.TEXT_LO2, textAlign: 'center', fontFamily: T.MONO, letterSpacing: 0.3 }}>
            mh.duckdns.org/p/x7Q-9FN
          </div>
        )}
      </div>
    </div>
  );
}

window.MHLibrary = {
  AlbumListScreen, ArtistListScreen, LikedScreen, GenreDetailScreen,
  CollabPlaylistOwner, CollabPlaylistMember, PlaylistShareDialog, PlaylistShareImporter,
};
