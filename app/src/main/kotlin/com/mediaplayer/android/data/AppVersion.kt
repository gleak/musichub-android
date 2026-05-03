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
    const val VERSION = "0.12.1"
}

data class ChangelogEntry(
    val version: String,
    val title: String,
    val highlights: List<String>,
)

object Changelog {
    val entries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            version = "0.12.1",
            title = "Sincronizzazione automatica più chiara",
            highlights = listOf(
                "Nella schermata di una playlist trovi adesso una scheda \"Sincronizzazione automatica\" sotto la copertina, con interruttore esplicito e descrizione (\"Scarica i nuovi brani all'apertura dell'app\"). Prima la stessa funzione era nascosta dietro un'icona di sincronizzazione in alto a destra che pochi notavano.",
                "Comportamento confermato: l'interruttore è disattivato per impostazione predefinita su tutte le playlist. La sincronizzazione automatica parte solo quando lo attivi tu.",
            ),
        ),
        ChangelogEntry(
            version = "0.12.0",
            title = "\"Non consigliarmi\" — escludi brani e artisti dai consigli",
            highlights = listOf(
                "Nuovo: dal menu kebab di un brano puoi scegliere \"Non consigliarmi questo brano\" o \"Non consigliarmi questo artista\". Le playlist generate dal sistema (Discover Daily, On Repeat, Release Radar, Daily Mix 1–6, Time Capsule, Mood, Up Next, Radar) saltano da subito quei brani e quegli artisti, anche quando il segnale di ascolto è forte.",
                "Quando segni un artista come \"Non consigliarmi\", il server toglie automaticamente il \"Segui artista\" se attivo — segnali contraddittori non convivono.",
                "Profilo → Riproduzione → Non consigliarmi: schermata con due tab (Brani / Artisti) per vedere cosa hai escluso e ripristinare con un tap.",
                "On Repeat ora pesa ogni ascolto in base al completion ratio: una traccia ascoltata 50× con il 30% di completamento finisce sotto a una ascoltata 20× fino in fondo. Gli skip pesano in negativo invece di essere semplicemente ignorati.",
                "I dislike vengono inviati al server con la stessa coda offline di Mi piace e Segui — premili senza rete e si sincronizzano da soli quando la connessione torna.",
            ),
        ),
        ChangelogEntry(
            version = "0.11.9",
            title = "Sincronizzazione automatica delle playlist",
            highlights = listOf(
                "Nuova icona di sincronizzazione (in alto a destra nella schermata di una playlist) per attivare la sincronizzazione automatica. Quando è attiva, ogni volta che apri l'app i brani della playlist vengono confrontati con quelli già scaricati sul telefono e i mancanti vengono messi automaticamente in coda di download. La preferenza è salvata sul server, quindi vale per ogni dispositivo collegato allo stesso account.",
                "Comportamento: il download rispetta i toggle esistenti di Profilo → Riproduzione → Download offline (Solo Wi-Fi e Download automatico).",
            ),
        ),
        ChangelogEntry(
            version = "0.11.8",
            title = "Link di condivisione playlist cliccabili",
            highlights = listOf(
                "I link generati da \"Condividi playlist\" sono ora URL https cliccabili nelle chat (WhatsApp, Telegram, Messaggi, Gmail). Prima erano un indirizzo `mediaplayer://...` che le app di messaggistica trattavano come testo semplice e il destinatario non poteva toccare per importare. Adesso il sistema riconosce il link, lo apre direttamente nell'app se installata, altrimenti mostra una pagina con il pulsante \"Apri nell'app\".",
                "I link condivisi prima della 0.11.8 (formato `mediaplayer://share/...`) continuano a funzionare — l'app accetta entrambi i formati.",
            ),
        ),
        ChangelogEntry(
            version = "0.11.6",
            title = "Sincronizzazione offline e rifinitura del precaricamento",
            highlights = listOf(
                "Sincronizzazione offline: i Mi piace, i \"Segui artista\" e le riproduzioni vengono ora salvati sul telefono se la rete cade e inviati al server quando torna la connessione. Niente più ascolti persi quando suoni in metropolitana, in aereo o con il segnale a singhiozzo. La coda è persistente — sopravvive alla chiusura dell'app e al riavvio del telefono.",
                "Schermate consultabili offline: Playlist, dettagli di una playlist, brani preferiti (prima pagina), Ascoltati di recente e elenco artisti seguiti mostrano l'ultimo stato visto anche senza rete. Niente più liste vuote quando entri in un tunnel.",
                "Profilo → Riproduzione → Daily Mix: nuova voce per forzare la rigenerazione del Daily Mix manualmente, senza aspettare il refresh automatico del server.",
                "Profilo → App → Eventi in coda: mostra in tempo reale quanti eventi sono in attesa di essere inviati. Quando il numero scende a zero significa che il server è di nuovo allineato.",
                "Risparmio energetico: rimosso il blocco al precaricamento del prossimo brano introdotto nella 0.11.5. Adesso la transizione tra brani resta istantanea anche con Risparmio batteria attivo (l'unico gate che resta è la rete a consumo, perché lì la bandwidth ha un costo reale).",
            ),
        ),
        ChangelogEntry(
            version = "0.11.5",
            title = "HTTPS, Android Auto sistemato, prestazioni e ritocchi",
            highlights = listOf(
                "Sicurezza: tutto il traffico verso il server passa adesso su HTTPS con certificato Let's Encrypt — niente più dati in chiaro tra app e backend, anche fuori dal Wi-Fi di casa.",
                "Android Auto: la lente di ingrandimento è tornata e ricerca per titolo, artista o album. \"Hey Google, metti <brano> su MediaPlayer\" parte direttamente, senza dover aprire l'app.",
                "Velocità: cache HTTP da 50 MB lato app + cover scaricate alla risoluzione esatta della miniatura. Lo scroll della griglia copertine consuma meno dati e si ferma meno spesso.",
                "Avvio: la prima apertura non blocca più l'interfaccia in attesa della preparazione dell'identità anonima. La schermata Home compare subito.",
                "Risparmio batteria: quando attivi il Risparmio energetico di sistema, l'app sospende il precaricamento del prossimo brano. Riprende da sola appena lo disattivi. La riproduzione del brano corrente non è influenzata.",
                "Errori più chiari: i messaggi di Album, Artisti, Per te e \"Aggiungi a playlist\" usano ora le stesse formule chiare già usate nel resto dell'app (niente più \"Unknown error\" o codici grezzi quando la rete cade).",
                "Vibrazione: i pulsanti play/pausa (mini-player e Now Playing) vibrano al tap, in linea con il cuore di Mi piace.",
                "Playlist generate: ogni famiglia (Discover Daily, On Repeat, Release Radar, Daily Mix, Time Capsule, Mood, Next) ha adesso la sua coppia di colori. Niente più sette tile viola identici di fila.",
                "Playlist generate: nelle playlist \"per te\" sono nascosti la maniglia di trascinamento e lo swipe per rimuovere — il backend non le riordina e non rimuove i brani da queste playlist, le icone fuorvianti non appaiono più.",
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
