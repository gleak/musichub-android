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
    const val VERSION = "0.16.13"
}

data class ChangelogEntry(
    val version: String,
    val title: String,
    val highlights: List<String>,
)

object Changelog {
    val entries: List<ChangelogEntry> = listOf(
        ChangelogEntry(
            version = "0.16.13",
            title = "Cuore funzionante — like sincronizzati col backend",
            highlights = listOf(
                "Il cuore \"mi piace\" del player e del mini-player ora invia davvero il like al backend: prima l'icona si accendeva localmente ma la traccia non veniva salvata server-side (bloccata da un accesso al Player fuori dal thread principale, silenzioso). Il timer di sospensione e la modalità \"Fine traccia\" condividevano lo stesso baco e ora rispondono in modo affidabile.",
                "Stabilizzato anche il ticker dei testi su Android Auto e la navigazione cartelle non-coda di AA: gli stessi accessi cross-thread venivano ignorati senza errore. Niente cambia in superficie, ma le carte ora si aggiornano senza salti.",
            ),
        ),
        ChangelogEntry(
            version = "0.16.12",
            title = "\"Controlla aggiornamenti\" — porta dritti al banner",
            highlights = listOf(
                "Profilo → \"Controlla aggiornamenti\": se c'è un nuovo aggiornamento, l'app ti riporta automaticamente in Home con il banner lime sotto gli occhi (prima compariva un toast \"Disponibile in Home\" e dovevi tornarci a mano). Il banner pulsa brevemente all'arrivo per non lasciarti dubbi su cosa guardare.",
                "Quando invece sei già aggiornato (o c'è un errore di rete) il toast resta come prima — non c'è nulla da andare a vedere altrove.",
            ),
        ),
        ChangelogEntry(
            version = "0.16.11",
            title = "Recenti vivi e \"Già escluso\" sul kebab",
            highlights = listOf(
                "Carosello \"Riprodotti di recente\" in Ricerca e riga \"Brani recenti\" in Home ora condividono lo stesso elenco: appena finisci di ascoltare un brano, lo trovi davanti su entrambe le schermate senza dover ricaricare. Prima ogni schermata aveva la sua copia separata.",
                "Sheet kebab \"Non consigliarmi questo brano/artista\": quando il brano (o l'artista) è già nei \"Non consigliati\", la voce diventa \"Brano già escluso\" / \"Artista già escluso\" con tinta lime, così non riclicchi nel vuoto. Stesso valore se ripristini un brano dalla schermata \"Non consigliati\": il prossimo kebab sullo stesso brano torna alla voce attiva.",
            ),
        ),
        ChangelogEntry(
            version = "0.16.10",
            title = "Playlist sincronizzate ovunque",
            highlights = listOf(
                "Crea / rinomina / elimina una playlist o aggiungi un brano dal kebab: Home, Playlist, Dettaglio e tutti gli altri schermi che mostrano la lista si aggiornano insieme. Prima ogni superficie aveva la sua copia locale: aggiungere un brano dal kebab in Ricerca lasciava la card Home con il vecchio conteggio \"3 brani\" finché non facevi pull-to-refresh.",
                "Sheet \"Aggiungi a playlist\": ogni riga ora mostra una spunta lime quando il brano è già presente nella playlist (visibile per le playlist di cui è già stato aperto il dettaglio). Prima nessun indicatore: rischiavi di aggiungere lo stesso brano due volte.",
                "Importazione di una playlist condivisa: la nuova playlist appare immediatamente in Home e nella tab Playlist senza dover ricaricare. Stesso comportamento per il toggle auto-sync, il rinomina e il elimina/lascia.",
            ),
        ),
        ChangelogEntry(
            version = "0.16.9",
            title = "Cuore unico ovunque — like su ogni lista",
            highlights = listOf(
                "Pulsante \"mi piace\" su ogni riga brano: artisti, album, playlist, generi e Home recenti ora mostrano il cuore accanto al kebab (prima era visibile solo nelle liste di Ricerca e Brani preferiti). Tap = aggiunge/rimuove dai preferiti senza dover aprire il kebab.",
                "Nuova voce \"Aggiungi/Rimuovi dai preferiti\" anche dentro il sheet kebab di ogni brano (prima il kebab non aveva il cuore, l'unico modo era riprodurre il brano e usare il player).",
                "Stato sincronizzato in tempo reale: se metti mi piace dal mini-player, la stessa traccia mostra subito il cuore pieno nelle liste; se la togli da una lista, il player aggiorna l'icona istantaneamente. Prima ogni superficie aveva il suo stato locale e i cuori potevano divergere fino al refresh successivo.",
            ),
        ),
        ChangelogEntry(
            version = "0.16.8",
            title = "Cover playlist, durata totale, invita amici",
            highlights = listOf(
                "Le card playlist in Home ora mostrano la copertina reale invece dell'icona generica: griglia auto-playlist con collage 2×2 (stesso pattern di Per te), card utente con cover del brano selezionato come copertina, righe lista e shortcut piccoli con la stessa cover. Fallback al gradient + icona quando la playlist è vuota.",
                "Libreria: \"Brani preferiti\", le card playlist e \"Import from Spotify\" ora si allineano sullo stesso margine sinistro (16dp dal bordo schermo, in linea con top bar e chip filtro). Prima Brani/Spotify erano a 24dp e le card a 16dp — bordo a sinistra disallineato.",
                "Dettaglio playlist: il sottotitolo mostra adesso anche la durata totale (es. \"1 h 23 min\") e il conteggio dei brani scaricati (es. \"12/25 scaricati\" o \"Tutti scaricati\" quando hai tutto offline). Prima il numero di scaricati appariva solo a metà download e la durata non c'era.",
                "Now Playing: il menu kebab in alto a destra include adesso \"Non consigliarmi questo brano\" e \"Non consigliarmi questo artista\" (prima erano disponibili solo dal kebab delle righe brano). Stessa scrittura sul backend, stessa esclusione automatica dalle playlist generate.",
                "Profilo → App → \"Invita un amico\": apre la share sheet di sistema con un messaggio precompilato e il link per scaricare l'APK più recente (preso dall'endpoint /api/updates/latest). Quando l'endpoint non è raggiungibile ricade sul link al backend così l'invito non rimane mai vuoto.",
            ),
        ),
        ChangelogEntry(
            version = "0.16.7",
            title = "Parità mockup — schermate del player",
            highlights = listOf(
                "Mini-player ridisegnato: bordo a gradiente lime intorno alla card, pulsante Play/Pausa pieno in lime invece dell'icona vuota, riga \"Artista · Album\" al posto del solo artista. Quando trascini per chiudere ora compare uno sfondo con scia lime e l'etichetta \"Rilascia per fermare\" oltre il 25% di trascinamento.",
                "Sleep timer: completato il sheet e cablato anche \"Fine traccia\" (vedi 0.16.6). Tap su una preset mentre un timer è attivo ora rimpiazza il timer invece di cancellarlo (prima dovevi annullare prima e poi rimettere la nuova durata).",
                "Dialogo errore di playback ridisegnato: card scura con eyebrow rosso \"// ERRORE PLAYBACK\", titolo specifico (es. \"Codec non supportato\"), pillola mono \"CODE | <code>\" e tre pulsanti footer \"Chiudi / Riprova / Riscarica\". Il pulsante \"Riscarica\" innesca il re-download dalla sorgente (YouTube), \"Riprova\" reinizializza l'item corrente. Prima c'era solo \"OK\".",
                "Conferma \"Brano sbagliato?\" ridisegnata: card con eyebrow rosso \"// SEGNALA · DEFINITIVO\", anteprima brano (cover + titolo + artista) embedded e pulsante \"Segnala\" rosso destrutturato. Stesso visuale ovunque la conferma viene innescata (Now Playing, AddToPlaylist).",
                "AddToPlaylist sheet ridisegnato: eyebrow \"// AGGIUNGI A · LE MIE PLAYLIST\", barra di ricerca per filtrare le tue playlist mentre digiti, copertine reali al posto dell'icona generica (le auto-playlist tengono il loro gradiente), pallino di selezione radio sul lato destro di ogni riga, pulsante sticky \"Crea nuova playlist\" outlined lime in fondo (prima inline tra le righe).",
                "AddSongs sheet ridisegnato: eyebrow \"// AGGIUNGI A · {nome playlist}\" che ti ricorda dove stai aggiungendo, multi-selezione (checkbox a sinistra di ogni brano), durata mostrata a destra di ogni riga e pulsante sticky \"Aggiungi N brani\" che committa la selezione in batch. Prima ogni tap aggiungeva subito un brano alla volta.",
                "QueueSheet ridisegnato: eyebrow \"// CODA\", titolo \"In riproduzione\", chip header Shuffle/Repeat/More che riflettono lo stato del player, etichette mono per ogni sezione (\"// IN RIPRODUZIONE\", \"// IN CODA · UTENTE · N\", \"// SUCCESSIVI · DA \\\"album\\\"\"), pulsante sticky \"Cancella coda\" rosso outlined in fondo che svuota tutto ciò che è davanti al brano corrente.",
                "EqualizerSheet ridisegnato: eyebrow \"// AUDIO\", pillola lime \"ATTIVO\" quando l'EQ è acceso, slider verticali per banda (prima orizzontali) con dB sopra e frequenza sotto, card preset singola tap-aperta con dialog di scelta (prima FilterChip orizzontale), card info \"// SESSIONE AUDIO\" in fondo che mostra `session_id` (hex) e l'output audio attivo (Bluetooth A2DP, USB Headset, Altoparlante interno…).",
                "TrackActionSheet ora include eyebrow \"// AZIONI\", divider prima del gruppo distruttivo e i nuovi callback \"Non consigliarmi questo brano/artista\" + \"Segnala brano sbagliato\" (prima esposti solo via AddToPlaylist).",
                "Tre nuove voci nel design review (`Claude_design_review.md` §6/§7/§8): banda 10-fissa dell'EQ (vincolato all'API android.media.audiofx.Equalizer di sistema), drag-to-reorder della coda (Compose non ha primitiva first-party), annotazione \"// GESTO · Da v0.12.6\" del mini-player (probabile chrome di canvas Figma).",
            ),
        ),
        ChangelogEntry(
            version = "0.16.6",
            title = "Sleep timer — sheet pieno con countdown e Fine traccia",
            highlights = listOf(
                "Il timer di sospensione adesso apre un bottom sheet completo (prima era un dropdown a 3 voci sul tasto comodino del Now Playing). Le preset disponibili passano da 15/30/60 a 5/10/15/30/45/60 minuti, in griglia 3×2, con numero mono grosso e sotto-label \"MIN\".",
                "Quando un timer è attivo, in cima al sheet compare una card lime con eyebrow `// ATTIVO`, countdown live `mm:ss` (tick locale a ogni secondo, sincronizzato col valore di servizio a confine di minuto), riga \"L'audio si fermerà alle hh:mm\" e pillola \"Annulla\" sul lato destro.",
                "Nuova preset full-width \"Fine traccia\" sotto la griglia: arma la modalità end-of-track del servizio (pause sul prossimo passaggio AUTO/REPEAT, già esposta in Android Auto) anche dal telefono. Quando questa modalità è attiva, la card mostra \"Fine traccia\" + \"Si fermerà alla fine del brano corrente\" al posto del countdown.",
                "Tap su una preset mentre un timer è già attivo ora rimpiazza il timer (prima il primo tap cancellava e basta — bisognava toccare due volte per cambiare durata). La pillola Annulla in AA continua a cancellare come prima.",
            ),
        ),
        ChangelogEntry(
            version = "0.16.5",
            title = "Parità mockup — schermate principali",
            highlights = listOf(
                "Indicatore brano in riproduzione nelle liste: ogni riga della libreria, ricerca, playlist, album, artista o genere mostra adesso titolo lime + tre barre lime animate accanto al brano effettivamente in riproduzione (mockup MusicHub `MHPlayingBars`). Prima nessuna riga era marcata e dovevi controllare il mini-player per capire dov'eri.",
                "Logo MusicHub in cima alle schermate Home e Per te (monogramma + wordmark) come da specifica MusicHub. Prima la chrome del top bar era solo il saluto / titolo.",
                "Testi sincronizzati: la riga corrente è ora più grande (titleSmall → headlineSmall) e in lime, le righe già passate sfumano al 35%, quelle future al 50%. Prima il fade era binario (attiva vs tutto il resto): adesso si vede chiaramente dove sei nel testo.",
                "Stato \"Testo non disponibile\" finalmente in italiano (prima diceva \"No lyrics available\" / \"No lyrics found\"). Il pulsante di import era già localizzato.",
                "Saluto in Home: la riga della data ora aggiunge \"· N nuove uscite per te\" quando Release Radar contiene novità (es. \"Ven 6 Mag · 3 nuove uscite per te\"). Quando Release Radar è vuoto la coda non compare. Allineato al mockup MusicHub.",
                "Card playlist nella griglia di Home: contatore brani in italiano (\"3 brani\" invece di \"3 songs\", \"Generata per te\" invece di \"Made for you\").",
                "Sottotitolo delle righe brano: separatore al middle-dot · come da mockup (prima era •).",
                "Per te: badge mono lime \"OGGI\" accanto al titolo \"I tuoi mix giornalieri\", come da specifica MusicHub.",
            ),
        ),
        ChangelogEntry(
            version = "0.16.4",
            title = "Android Auto — preset timer, fine traccia, copertine generi",
            highlights = listOf(
                "Sleep timer in Android Auto: la striscia comandi del now-playing card mostra ora pillole rapide \"Sospendi tra 15m / 30m / 60m\" + \"Fine traccia\" quando nessun timer è attivo, invece dell'unica pillola fissa a 30 minuti. Tap su una preset arma il timer per quella durata senza dover passare dal telefono.",
                "Nuova modalità \"Fine traccia\": l'app mette in pausa quando il brano corrente termina naturalmente (skip manuale non conta). Funziona sia in macchina sia come logica condivisa col telefono.",
                "Quando un timer è in corso le pillole collassano in un'unica chip live \"Annulla · N min\" (o \"Annulla · fine traccia\") che decresce a ogni minuto fino a zero; il tap la annulla. Prima la chip mostrava solo \"Annulla timer\" senza countdown.",
                "I controlli che leggono la sessione (telefono + AA) hanno due nuovi extra: `sleep_remaining_ms` (Long, ms residui) e `sleep_end_of_track` (Boolean), in linea con `sleep_active`. Aggiornati a confine di minuto per non far thrashare il custom layout.",
                "Tile Generi in Android Auto: ogni tile ha ora una copertina a gradiente con la palette del genere (Indie viola→rosa, Elettronica blu→ciano, Hip-hop nero→arancio, Jazz oro→nero, Classica viola→rosa pesca, Ambient verde scuro→lime, Rock corallo→indaco, Pop blu notte→lime), invece del placeholder bianco. Rimosso anche il sottotitolo ridondante \"Genere\" da ogni riga.",
                "Testi sincronizzati in AA: la riga corrente mostra ora il prefisso mono \"// ORA · \" davanti al testo, così si distingue subito da titolo/artista. Le righe lunghe vengono troncate a ~60 caratteri con ellissi (prima la riga si avvolgeva o tagliava in modo diverso da head unit a head unit).",
                "Coda corrente in Android Auto: nuova voce a livello root del browse tree che elenca i brani della coda di riproduzione attuale. La traccia in riproduzione è marcata con una freccia ▸; toccando una riga si salta direttamente a quella posizione senza ricaricare la coda.",
            ),
        ),
        ChangelogEntry(
            version = "0.16.3",
            title = "Taglio — fade in/out davvero udibile",
            highlights = listOf(
                "La pillola \"Fade in/out\" nell'editor di Taglio adesso applica davvero la dissolvenza: 0.5s in entrata e 0.5s in uscita sul brano salvato. Prima il toggle era solo cosmetico — ora il file `(cut)` parte da zero e finisce in silenzio.",
                "Il salvataggio con fade attivo richiede qualche secondo in più (il server passa dal frame-copy lossless alla ricodifica MP3 a ~190 kbps VBR per poter stendere la dissolvenza sui campioni). Il salvataggio senza fade resta veloce come prima.",
                "Su finestre molto corte la durata della dissolvenza si auto-riduce per non sovrapporre fade-in e fade-out, in modo che entrambe vadano a fondo silenzio.",
            ),
        ),
        ChangelogEntry(
            version = "0.16.2",
            title = "Taglio — waveform reale dall'audio",
            highlights = listOf(
                "L'editor di Taglio ora analizza davvero la traccia: scarica lo stream, lo decodifica con MediaCodec e calcola le ampiezze reali. La waveform che vedi (sia nella card di scrub che nella card di taglio) rispecchia il brano vero, non più un disegno sintetico generato dall'id.",
                "Di conseguenza la pillola \"Aggancia al silenzio\" funziona davvero: IN e OUT saltano alle valli reali dell'onda — punti di volume basso, transizioni naturali — invece che a buchi finti.",
                "L'analisi gira in background al primo accesso al brano (~1-3 secondi su un MP3 medio); l'editor è subito utilizzabile con la waveform sintetica e poi si sostituisce automaticamente quando il calcolo finisce.",
                "Risultato cachato su disco per (songId, 96 bin): riaprire l'editor sullo stesso brano è istantaneo.",
            ),
        ),
        ChangelogEntry(
            version = "0.16.1",
            title = "Taglio — zoom, A/B, snap-silenzio, sostituisci nelle playlist",
            highlights = listOf(
                "Long-press su una maniglia IN o OUT attiva lo zoom ×8: la timeline si stringe a 1/8 dell'originale centrata sulla maniglia, perfetto per posizionare il taglio al millisecondo. La maniglia in zoom prende un colore goldenrod e in alto a destra compare il badge `ZOOM ×8 · IN/OUT` — toccalo (o tocca fuori dalla maniglia) per uscire dallo zoom.",
                "Pillola \"Anteprima A/B\" attiva il loop IN→OUT: appena il playhead supera OUT torna automaticamente a IN, così puoi sentire il taglio in loop senza dover scrubare a mano.",
                "Pillola \"Aggancia al silenzio\": sposta IN e OUT al valle più vicino della waveform (entro ±4s), così il taglio cade su un punto di transizione naturale invece che a metà di una nota.",
                "Toast salvataggio rinnovato: dopo il save compaiono inline le scelte \"Sì / No\" — \"Sostituirà l'originale nelle playlist?\". Sì chiama il nuovo endpoint backend e sostituisce ogni riferimento al brano originale nelle tue playlist (incluse quelle condivise di cui sei membro), mantenendo la posizione. No esce e basta.",
                "Backend: nuovo endpoint `POST /api/playlists/replace-song` con swap atomico via update SQL e guardia anti-duplicato.",
            ),
        ),
        ChangelogEntry(
            version = "0.16.0",
            title = "Modalità Taglio — ringtone editor",
            highlights = listOf(
                "Nuova modalità Taglio raggiungibile dal menu kebab del Now Playing → \"Taglia traccia…\". Editor a tutto schermo con due timeline: in alto la card \"01 · Ascolto · scrub libero\" con waveform, playhead giallo in stile mockup, chip ora corrente lime, e transport −5 / Vai a IN / Play / Vai a OUT / +5; in basso la card \"02 · Taglio · sposta i punti IN / OUT\" con barra lime attiva, due maniglie trascinabili e i nudge box mono ±1s / ±.1.",
                "Tasto \"Salva\" lime in alto a destra: invia inMs/outMs al backend, che ritaglia la traccia con ffmpeg `-c copy` (allineamento al frame MP3, nessuna ricodifica) e crea una nuova riga master con titolo \"<originale> (cut)\". Le copertine vengono duplicate; le righe testo dentro alla finestra vengono copiate spostate di -inMs. La traccia tagliata diventa un brano normale: appare in libreria, è riproducibile, può essere salvata come suoneria con il flusso esistente.",
                "Card \"Risultato\" mono lime con la durata del taglio e quanti minuti/secondi sono stati rimossi dall'originale. Hint \"TIENI PREMUTO UN MARCATORE PER ZOOM ×8\" in fondo (la zoom sarà attivata in una versione futura).",
                "Backend: nuovo endpoint POST /api/songs/{id}/cut, dedup automatico per content_hash (riutilizza la riga esistente se la stessa finestra è già stata salvata).",
            ),
        ),
        ChangelogEntry(
            version = "0.15.2",
            title = "Aggiornamenti, novità e diagnostica rinnovati",
            highlights = listOf(
                "Banner aggiornamento: la card lime in Home ora ha tre stati come da specifica MusicHub. \"Disponibile\" mostra eyebrow `// AGGIORNAMENTO`, diff `vX → vY` con la nuova versione in lime monospazio e una pillola lime \"Installa\" esplicita. \"Scaricamento\" prende il posto della stessa card con spinner, percentuale lime in monospazio e barra di progresso da 4dp che si aggiorna ogni mezzo secondo — non serve più scendere nella shade di sistema per vedere a che punto è il download. \"Fallito\" tinta la card di rosa, mostra triangolo di warning e una pillola \"Riprova\" al posto della X.",
                "Cosa c'è di nuovo: il foglio modale è stato rifatto con hero a gradiente lime, eyebrow `// NOVITÀ · vX`, diff `vY → vX` in monospazio e titolo grande della release. Le novità sono numerate `01`, `02`, … in lime monospazio con divisore sottile fra una e l'altra, e in fondo c'è un pulsante lime a piena larghezza \"Continua\" per chiudere senza dover trascinare. Mostra solo le novità dell'ultima versione invece di tutto lo storico.",
                "Eventi in coda: nuova schermata diagnostica raggiungibile da Profilo → App → Eventi in coda (prima era solo una riga con il conteggio). Card hero con il totale in cifre lime 56sp monospazio e didascalia che spiega che si svuotano da soli quando torni online. Sotto, sezione `// DETTAGLIO` con il dettaglio per tipo (Mi piace, Segui artista, Riproduzioni, Non consigliarmi, …) — ogni riga ha icona squircle lime, label, sottotitolo e pillola `×N` lime monospazio. Footer mostra spinner + \"Sincronizzazione in corso…\" quando sei online, oppure \"In attesa di rete\" se sei offline.",
                "Il modale che si apriva all'avvio per gli aggiornamenti è stato rimosso: il banner in Home è l'unico punto di entrata. Da Profilo → \"Controlla aggiornamenti\" ora compare un toast \"Aggiornamento disponibile in Home\" e basta tornare alla Home per installarlo.",
            ),
        ),
        ChangelogEntry(
            version = "0.15.1",
            title = "Impostazioni rinnovate",
            highlights = listOf(
                "Le sotto-pagine di Impostazioni adottano lo stile MusicHub: ogni schermata ha eyebrow lime in monospazio sopra il titolo (\"// IMPOSTAZIONI\", \"// CONSIGLI\") e contenuti scrollabili anziché fissi.",
                "Crossfade: paragrafo introduttivo \"Sovrappone le tracce in transizione…\" sopra la card, valore in lime extra-grande con suffisso \"s\" senza spazio (es. \"6s\"), e tick numerici in monospazio (0/2/4/6/8/10/12) sotto il cursore — i tick già passati si colorano di lime.",
                "Download offline: card \"Spazio usato\" con conteggio reale dei brani scaricati come sub-line in monospazio (es. \"42 brani scaricati\"), barra di progresso più sottile, sezione \"Gestione\" con scorciatoia \"Forza rigenerazione Daily Mix\" in linea, e pulsante distruttivo \"Cancella tutti i download\" finalmente come pillola con bordo + sfondo rossi traslucidi al posto del semplice testo rosso.",
                "Tema: tre card cliccabili a piena larghezza ognuna con tile di anteprima 56dp del tema (chiaro / scuro / sistema con split diagonale), check lime sulla card selezionata e bordo lime inset; al posto della lista radio compatta. L'ordine ora è Chiaro / Scuro / Sistema come da specifica.",
                "Non consigliati: tab a pillola (Brani · N / Artisti · N) al posto delle tab Material, righe con opacità ridotta perché rappresentano elementi rimossi, e pulsante testuale \"Ripristina\" lime al posto della sola icona.",
            ),
        ),
        ChangelogEntry(
            version = "0.15.0",
            title = "Collaborazione playlist al completo",
            highlights = listOf(
                "Le playlist condivise tracciano chi ha aggiunto ogni brano: nelle righe della playlist trovi una pillola lime in monospazio con le iniziali del collaboratore (\"· LUCA\", \"· MARTA\") accanto alla durata. La pillola appare solo per brani aggiunti da membri diversi dal proprietario.",
                "Nuova schermata Membri raggiungibile dal pulsante \"Gestisci\" sulla card membri della playlist condivisa: elenca proprietario + collaboratori con avatar a colori e ruolo. Il proprietario può rimuovere singoli membri con un tasto rosso PersonRemove (con conferma). Chi viene rimosso perde l'accesso ma chi resta non viene toccato.",
                "Il foglio di condivisione playlist ora ha una vera revoca link: tocca \"Revoca link\", conferma e tutti i link attivi smettono di funzionare per chi non li ha ancora aperti. I membri già accettati restano nella playlist.",
            ),
        ),
        ChangelogEntry(
            version = "0.14.0",
            title = "Sfoglia per genere e libreria con filtri",
            highlights = listOf(
                "La schermata Album ora ha un campo di ricerca \"Cerca album…\" e un pulsante che alterna l'ordine tra \"Recenti\" e \"A → Z\". La ricerca filtra in tempo reale gli album già caricati per nome o artista.",
                "Sulla schermata Artisti compare una scorbar verticale A→Z lungo il bordo destro: tocca una lettera per scorrere fino al primo artista che inizia con quella iniziale. Le lettere senza corrispondenze restano in grigio.",
                "I generi nella griglia \"Sfoglia per genere\" della Ricerca aprono ora una pagina dedicata GenreDetailScreen con eyebrow \"// SFOGLIA · GENERE\", pill rimovibile, tasti \"Riproduci tutti\" + Casuale e elenco dei brani del genere.",
                "Su Brani che ti piacciono ogni riga ora mostra un indice numerico in monospazio a sinistra della copertina (1, 2, 3…) e l'header ha un terzo pulsante per scaricare/rimuovere tutti i brani offline accanto a Riproduci e Casuale.",
            ),
        ),
        ChangelogEntry(
            version = "0.13.5",
            title = "Libreria e Condivisione playlist rinnovate",
            highlights = listOf(
                "Album, Artisti e Mi piace adottano lo stile MusicHub: eyebrow lime \"// LIBRERIA\" sopra il titolo, conteggio totale in monospazio accanto al titolo (\"Album · 42\"), sottotitoli in italiano. Su Mi piace ora vedi anche la durata totale dei brani caricati (\"284 brani · 18h 42m\").",
                "Le righe Artisti hanno una freccia chevron a destra e il sottotitolo è in italiano (\"3 album · 47 brani\").",
                "Le playlist condivise mostrano una card Membri sotto la copertina con avatar a stack e un tasto \"Gestisci\" (per il proprietario) o chevron (per i membri). I membri vedono anche un pulsante ghost \"Rimuovi dalla libreria\" inline, oltre alla scorciatoia long-press.",
                "Il tasto Condividi del proprietario apre ora un foglio dedicato: vedi e copi il link \"mh.duckdns.org/p/...\" in monospazio, hai un pulsante \"Condividi via sistema\" e un footer con il numero di membri attivi. La revoca del link arriverà in una versione futura.",
                "Quando ricevi un link condiviso vedi un modal a tutto schermo con copertina 220dp, titolo della playlist, \"Playlist collaborativa di <nome>\" e tasto lime \"Aggiungi alla mia libreria\", al posto del vecchio dialog testuale.",
            ),
        ),
        ChangelogEntry(
            version = "0.13.4",
            title = "Scopri e Importa da Spotify rinnovati",
            highlights = listOf(
                "La schermata Scopri adotta lo stile MusicHub: titolo \"Trova brani\" con eyebrow lime \"// SCOPRI · YT\", barra di ricerca a card con magnifier e tasto X per pulire il testo, invio della ricerca direttamente dalla tastiera (niente più pulsante Cerca separato).",
                "I risultati YouTube ora hanno una piccola pill rossa \"YT\", la durata in monospazio e un tasto lime \"Aggiungi\" sulla destra; mentre un brano è in download la riga si evidenzia con bordo lime e mostra una pill di stato (SBLOCCO / percentuale).",
                "Errori di ricerca compaiono come banner rosso non-bloccante in cima alla lista invece di sostituire l'intera schermata; i caricamenti mostrano righe skeleton al posto del cerchio centrato.",
                "Quando una ricerca termina vedi una schermata di esito dedicata: cerchio verde \"Aggiunto alla libreria\", ambra \"Importato parzialmente\", rossa \"Brano non trovato\" o grigia \"Ricerca annullata\", con riepilogo sorgente/durata e doppio tasto azione.",
                "Importa da Spotify ora ha una stepper a 5 segmenti in alto e ogni passaggio ha la sua label \"// PASSO N / 5\": istruzioni numerate con drop-zone tratteggiata, parsing del CSV con card dedicata, conferma del nome con campo Material 3 + contatore caratteri, e schermata finale con cerchio lime, griglia stat (Importati/Saltati/Errori) e tasti \"Apri <playlist>\" / \"Torna alle playlist\".",
                "Errori di lettura del CSV mostrano un pannello rosso con suggerimenti puntati per ripristinare l'esportazione invece di un semplice messaggio in inglese.",
            ),
        ),
        ChangelogEntry(
            version = "0.13.3",
            title = "Schermata di accesso rinnovata",
            highlights = listOf(
                "La schermata di accesso adotta il marchio MusicHub: monogramma con barre di equalizzatore lime al posto della vecchia \"M\" piatta, alone radiale lime sul bordo superiore e pulsante \"Accedi con Google\" bianco con la G ufficiale Google a colori.",
                "Quando un accesso fallisce ora compare un riquadro rosso con titolo \"Accesso non riuscito\" e codice tecnico in monospazio: capisci subito se è la connessione, le credenziali o il server.",
                "Mentre l'accesso è in corso il pulsante mostra \"Accesso in corso…\" con spinner inline; non vedi più la schermata vuota con il solo cerchio caricamento.",
                "Sotto al pulsante una nota in piccolo conferma che proseguendo accetti i Termini e l'Informativa privacy.",
            ),
        ),
        ChangelogEntry(
            version = "0.13.2",
            title = "Tutto in italiano",
            highlights = listOf(
                "Sweep di localizzazione: le ultime stringhe inglesi sparse nell'interfaccia (descrizioni di accessibilità del player, intestazioni della coda, voci dell'azione \"Aggiungi alla playlist\", \"Scarica testo\" e affini) sono ora in italiano. Per chi usa TalkBack o uno screen reader, ogni icona del player viene letta in italiano.",
            ),
        ),
        ChangelogEntry(
            version = "0.13.1",
            title = "Errori di riproduzione spiegati",
            highlights = listOf(
                "Quando un brano non parte ora compare un dialog che spiega il motivo (file danneggiato, codec non supportato, server irraggiungibile, ecc.) invece di un toast generico. Resta in vista finché non lo chiudi e include il codice di errore tecnico per chi vuole approfondire.",
            ),
        ),
        ChangelogEntry(
            version = "0.13.0",
            title = "Playlist condivise davvero, in tempo reale",
            highlights = listOf(
                "Le playlist condivise sono ora collaborative. Quando qualcuno apre il tuo link e accetta, finisce nella stessa playlist — non in una copia. Le tue modifiche (brani aggiunti, rimossi, riordinati, rinominata) compaiono nella sua libreria al prossimo refresh, e viceversa.",
                "Niente più duplicati: se apri un link di una playlist che è già tua, l'app ti porta direttamente sulla playlist esistente invece di creare una seconda copia. Stessa cosa se il link te l'avevi già accettato.",
                "Indicatore \"Condivisa da <nome>\" sotto la copertina della playlist e nel sottotitolo della riga in libreria, così riconosci subito quali playlist sono tue e quali ti sono state condivise.",
                "La sincronizzazione automatica delle playlist è una preferenza per dispositivo: ogni membro (e il proprietario) decide se il proprio telefono deve scaricare i brani della playlist condivisa, senza coinvolgere gli altri. Se attivi il tuo, il telefono di chi ha condiviso resta com'è.",
                "Il pulsante elimina di una playlist condivisa diventa \"Rimuovi dalla libreria\": rimuove la playlist solo da te, mentre il proprietario e gli altri membri continuano a vederla. Se sei tu il proprietario e la elimini, sparisce per tutti.",
                "Solo il proprietario può generare nuovi link di condivisione. I membri che vogliono invitare un terzo utente devono chiedere al proprietario.",
            ),
        ),
        ChangelogEntry(
            version = "0.12.9",
            title = "Solo accesso autenticato",
            highlights = listOf(
                "L'app richiede ora l'accesso con Google: la modalità ospite è stata rimossa. Apri l'app, premi \"Accedi con Google\" e riprendi da dove avevi lasciato.",
                "Il server ora memorizza l'ultima attività del tuo account a ogni richiesta — utile per capire quando un dispositivo ha smesso di sincronizzare senza dover ispezionare i log.",
            ),
        ),
        ChangelogEntry(
            version = "0.12.8",
            title = "Segnala brano sbagliato",
            highlights = listOf(
                "Nuovo: dal menu kebab di un brano (in ricerca, playlist, album, artisti, Mi piace, coda) e dal menu del player puoi scegliere \"Report wrong song\". Il brano segnalato sparisce subito da ricerche, playlist, Mi piace e cronologia su tutti i dispositivi; il file audio, la copertina e l'eventuale video vengono cancellati dal server. La segnalazione è permanente e protegge anche dai futuri scarichi automatici: il sistema riconosce il contenuto già marcato come sbagliato e si rifiuta di riscaricarlo.",
            ),
        ),
        ChangelogEntry(
            version = "0.12.7",
            title = "Riga brano più leggibile e riordino con tocco prolungato",
            highlights = listOf(
                "La riga di un brano è stata ripulita: nel sottotitolo ora c'è solo il nome dell'artista seguito dalla durata (\"Artista • 3:42\"). Il nome dell'album è stato tolto perché ridondante con la copertina e affollava la riga.",
                "Il pulsante \"mi piace\" è stato spostato accanto al menu kebab, così le azioni della riga sono raggruppate.",
                "Se il titolo del brano è troppo lungo per stare su una riga, adesso scorre orizzontalmente in automatico così puoi leggerlo per intero senza doverlo aprire.",
                "Nei dettagli di una playlist non c'è più la maniglia di trascinamento dedicata: tieni premuto su un brano per iniziare a riordinarlo. Tocco singolo riproduce, kebab apre il menu come prima.",
            ),
        ),
        ChangelogEntry(
            version = "0.12.6",
            title = "Download trasparenti e mini-player chiudibile",
            highlights = listOf(
                "La notifica di download adesso mostra cosa sta scaricando: \"Scarico: <titolo brano>\" invece del generico \"Sto scaricando…\". Quando ci sono più brani in coda compare anche il conteggio (\"+N in coda\").",
                "Profilo → Riproduzione → Download offline → Download automatico è ora disattivato per impostazione predefinita. Prima ogni brano che ascoltavi veniva scaricato in automatico, anche senza nessuna playlist con la sincronizzazione automatica attiva. Adesso i download partono solo se attivi questa opzione o la sincronizzazione automatica di una playlist.",
                "Mini-player chiudibile: trascina il mini-player a destra o a sinistra per fermare la riproduzione e nasconderlo. Prima si poteva solo mettere in pausa, restava sempre visibile.",
            ),
        ),
        ChangelogEntry(
            version = "0.12.5",
            title = "Player video più coerente",
            highlights = listOf(
                "Quando guardi il video di un brano, la barra di avanzamento e i comandi di riproduzione audio (play/pausa, avanti/indietro, shuffle, repeat) spariscono: l'audio è in pausa per far suonare il video, quindi quei controlli erano scollegati dal video stesso. Restano i controlli del player video sopra l'immagine.",
                "L'icona del video nella barra delle azioni adesso indica lo stato: video spento → icona libreria video; video acceso → nota musicale colorata, come scorciatoia per tornare alla copertina audio.",
                "Il pulsante schermo intero dentro al player video adesso porta davvero a tutto schermo. Prima la finestra di dialogo non veniva forzata a coprire l'intero schermo, quindi il video restava grande quanto il riquadro inline.",
            ),
        ),
        ChangelogEntry(
            version = "0.12.3",
            title = "Recupero automatico dei brani danneggiati",
            highlights = listOf(
                "Quando provi a far partire un brano e non parte (file scaricato troncato, container malformato, decoder che si arrende), l'app ora ti dice cosa è successo invece di restare in silenzio: appare un avviso \"<brano> risulta danneggiato. Lo sto riscaricando…\" e la copia locale viene buttata e ripresa dal server in automatico. Quando i nuovi byte arrivano la riproduzione riparte da sola.",
                "Se anche dopo il riscarico automatico il brano continua a non partire (di solito significa che anche il file sul server è guasto), l'app suggerisce di usare \"Riscarica dalla sorgente\" dal menu del brano, che rifà il download da YouTube.",
                "Per gli errori di rete (connessione assente, timeout) l'app mostra il codice di errore senza riscaricare nulla — riscaricare con la rete giù non aiuterebbe.",
            ),
        ),
        ChangelogEntry(
            version = "0.12.2",
            title = "Pull-to-refresh in \"Per te\"",
            highlights = listOf(
                "Nella schermata \"Per te\" puoi adesso trascinare verso il basso per ricaricare le playlist generate dal sistema, come già fai in \"Mi piace\" e nelle altre liste.",
            ),
        ),
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
