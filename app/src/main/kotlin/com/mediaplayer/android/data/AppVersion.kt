package com.mediaplayer.android.data

/**
 * App-wide version + changelog source of truth.
 *
 * INVARIANT: every new user-visible feature MUST bump [VERSION] and prepend a
 * matching [ChangelogEntry] at the top of [Changelog.entries]. Bug-fix-only
 * commits do not require a bump. Keep [VERSION] aligned with `versionName`
 * in `app/build.gradle.kts` — the Gradle value drives Play Store metadata,
 * this constant drives the in-app changelog gate.
 */
object AppVersion {
    const val VERSION = "0.11.1"
}

data class ChangelogEntry(
    val version: String,
    val title: String,
    val highlights: List<String>,
)

object Changelog {
    val entries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            version = "0.11.1",
            title = "Risparmio batteria: stop al precaricamento quando serve",
            highlights = listOf(
                "Quando attivi il Risparmio energetico di sistema, l'app sospende automaticamente il precaricamento del prossimo brano. Riprende da sola appena disattivi il risparmio. La riproduzione del brano corrente non è influenzata.",
            ),
        ),
        ChangelogEntry(
            version = "0.11.0",
            title = "Connessione cifrata + cover più rapide",
            highlights = listOf(
                "Sicurezza: tutto il traffico verso il server adesso passa su HTTPS con certificato Let's Encrypt — niente più dati in chiaro tra app e backend, anche fuori dal Wi-Fi di casa.",
                "Velocità: cache HTTP da 50 MB lato app + cover scaricate alla risoluzione esatta della miniatura. Lo scroll della griglia copertine consuma meno dati e si ferma meno spesso.",
                "Avvio: la prima apertura non blocca più l'interfaccia in attesa della preparazione dell'identità anonima. La schermata Home compare subito.",
            ),
        ),
        ChangelogEntry(
            version = "0.10.20",
            title = "MusicHub — riepilogo completo + Testi e Generi su Android Auto",
            highlights = listOf(
                // --- Novità di questa release ----------------------------------
                "Novità: testi sincronizzati anche in Android Auto. La riga corrente del testo viene scritta nel campo \"description\" del now-playing card della macchina e si aggiorna ogni secondo seguendo la posizione di riproduzione. Niente scroll (vietato da AA per sicurezza alla guida) — solo la riga giusta al momento giusto. Quando un brano non ha testi sincronizzati, la card resta com'era.",
                "Novità: tile \"Generi\" anche in Android Auto. Gli stessi 8 generi del telefono (Indie / Elettronica / Hip-hop / Jazz / Classica / Ambient / Rock / Pop) ora compaiono nel browse tree dell'auto e aprono la lista di brani filtrata per tag. Tap su un brano espande l'intera coda del genere a partire da quella posizione.",
                "Novità: Download offline collegato davvero ai toggle. \"Solo Wi-Fi\" ora controlla il DownloadManager (prima sempre legato a rete non a consumo); \"Download automatico\" salva ogni brano che ascolti (prima il toggle veniva ignorato e il trigger richiedeva una soglia di ascolto contraria all'etichetta).",
                // --- Account ----------------------------------------------------
                "Account: accesso con Google + modalità ospite con sessione persistente, \"Cambia account\" e dialog di conferma prima della disconnessione.",
                // --- Player -----------------------------------------------------
                "Player: Media3 in foreground service con notifica e controlli da lockscreen; mini-player espandibile in Now Playing con transizione shared-element del cover.",
                "Coda stile Spotify: Now playing / Next in queue / Next up. \"Riproduci dopo\" e \"Aggiungi a coda\" tracciate separatamente dalla source, ordine preservato in shuffle, rimozione singola dalla sheet \"Up next\".",
                "Equalizer 10 bande con preset (l'effetto è applicato a livello di sessione audio, quindi vale anche per l'audio in macchina).",
                "Crossfade 0–12s configurabile, applicato sia su telefono sia su Android Auto via fade-in del volume al cambio traccia.",
                "Sleep timer condiviso telefono+AA: imposta in macchina e vedi in app, e viceversa.",
                "Testi sincronizzati (Lyrics) sotto il Now Playing.",
                "Player video inline + fullscreen per i brani con video, esporta come suoneria, \"Mark broken\" dal kebab e due percorsi distinti di re-download (origine vs cache locale).",
                // --- Libreria ---------------------------------------------------
                "Libreria con 4 tab: Playlist, Album, Artisti, Scaricati. Brani preferiti paginati con conteggio totale reale.",
                "Playlist: crea, riordina con animazione, swipe-to-remove con snackbar di conferma, kebab in ogni riga (Add to playlist / Play next / Coda / Scarica / Mostra testo / Vai all'artista o album / Condividi).",
                "Pagine Album e Artista con tracklist numerata; \"Segui artista\" (campanella) per inserire l'artista in Release Radar.",
                "Pull-to-refresh sulle schermate principali (Home, Liked, Playlists, Playlist Detail, Album, Artist, Find).",
                // --- Per te -----------------------------------------------------
                "Per te: hub con Discover Daily, On Repeat, Release Radar e 6 Daily Mix tematici; refresh strip \"AGGIORNATA / BRANI\" e banner di cadenza nei dettagli.",
                // --- Cerca ------------------------------------------------------
                "Cerca: ricerca testuale + ricerca vocale (riconoscitore di sistema), cronologia query (max 8) con cancellazione singola/totale.",
                "Sfoglia · Tutti i generi: griglia 4×2 colorata; tap = filtro per tag (song_tags), pill rimovibile, digitazione restringe i risultati dentro al genere.",
                // --- Find -------------------------------------------------------
                "Find: tab di scoperta con stati di caricamento ed errore + retry.",
                // --- Spotify ----------------------------------------------------
                "Import playlist Spotify via CSV Exportify (pipeline IT, conferma + risultato).",
                // --- Sharing ----------------------------------------------------
                "Condivisione playlist: link mediaplayer://share/<token>, copia + share di sistema; import one-shot indipendente (le modifiche del mittente non si propagano).",
                // --- Android Auto ----------------------------------------------
                "Android Auto: browse tree IT (Per te, Brani preferiti, Ascoltati di recente, Playlist, Album, Artisti, Generi, Tutti i brani), voice search, ripresa al cold connect, custom layout con Like e Sleep timer, riga di testo sincronizzata sul now-playing card.",
                // --- Aggiornamenti ---------------------------------------------
                "Aggiornamenti: canale self-hosted (APK sul server, niente Play Store), banner Home v<old>→v<new> con install in un tap, voce \"Controlla aggiornamenti\" che bypassa il rate-limit.",
                // --- Tema -------------------------------------------------------
                "Tema: Scuro / Chiaro / Sistema, applicato app-wide via MaterialTheme.",
                // --- Onboarding -------------------------------------------------
                "Onboarding: first-launch tag picker (\"Cosa ascolti?\") + welcome sheet; banner ospite \"Stai ascoltando come ospite. Accedi per sincronizzare la libreria.\".",
                // --- Profilo ----------------------------------------------------
                "Profilo: schermata full-screen con avatar, contatori Brani / Playlist / Artisti tappabili (collegati a /api/auth/stats), sub-pages Crossfade / Tema / Download offline.",
                // --- Polish -----------------------------------------------------
                "Bottom-nav root-return: tap stessa tab → torna al root della tab. Icona Profilo coerente nelle 4 tab (Home / Cerca / Per te / Libreria).",
                "Empty states con icona + messaggio invece di una riga di testo; shimmer skeleton durante il caricamento delle liste.",
            ),
        ),
    )
}
