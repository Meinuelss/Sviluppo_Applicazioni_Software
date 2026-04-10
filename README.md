### Modifiche Glossario e conseguenze
- 3. Eliminazione del "Goldplating" (Termini inventati/non richiesti)
Un professore di ingegneria del software penalizza fortemente l'inserimento di requisiti non chiesti dal cliente (sovra-ingegnerizzazione).

Rimozione di "Foglio Riepilogativo": Nel tuo glossario c'era questo termine, ma in tutto il testo del progetto e nelle user stories non viene mai menzionato un "foglio". Si parla solo di chef che assegnano "compiti".

Rimozione di "Tabellone dei turni": Anche questo non è mai citato. Il testo parla genericamente di "calendario dei turni".
4. Correzioni di precisione e refusi
Correzione di "Lavori in cucina": Nel tuo avevi incollato per sbaglio la definizione di "Evento capofila" ("L’Evento originale di una ricorrenza..."). L'ho corretta con la definizione reale (la fase in cui lo chef assegna i compiti).
2. Aggiunta dei Meccanismi di Business (Mancanti nel tuo originale)
Il testo originale descriveva alcune funzionalità chiave dell'applicazione che nel tuo glossario non avevano una voce dedicata:

Disponibilità e Convocazione: Nel tuo mancava il meccanismo di interazione del personale. Ho aggiunto questi termini per spiegare la differenza tra "dire che ci sono" (Disponibilità, ritirabile) e "essere chiamato a lavorare" (Convocazione, vincolante).

Raggruppamento di Turni: Il testo dedicava un lungo paragrafo a questa logica (singoli o ricorrenti). L'ho inserito perché è una regola di dominio complessa.

Stato di Pubblicazione (Bozza/Pubblicata): Nel tuo mancava il ciclo di vita della singola ricetta (il fatto che per essere usata debba passare da bozza a pubblicata).

Tag: Il testo cita esplicitamente i tag (es. crudo, vegetariano), li ho aggiunti per completezza.

Modifica a cascata: Ho aggiunto una voce per spiegare l'operazione di modifica della ricorrenza (il modello Google Calendar).

### UC Bervi
Confusione tra "Mondo Reale" e "Sistema Informatico":
Hai scritto: "Gestire eventi significa valutare la fattibilità di una richiesta verificando lo stato della cucina e accettare la commessa". Questo lo fa l'organizzatore nel mondo reale (magari parlando a voce con lo chef, come dice Pierpaolo nelle User Stories). Ma il Caso d'Uso modella solo l'interazione con il software. Il software non "valuta la fattibilità a voce", il software permette semplicemente di creare/modificare la scheda evento.
??????
Inquinamento da Regole di Business (L'errore più grave): Nelle descrizioni brevi hai inserito percentuali (30%), penali, deroghe, tag, dosi e tempistiche. Questo è un errore concettuale. La descrizione breve serve per tracciare i confini del sistema. I dettagli su come si calcola una penale o quali attributi ha una ricetta appartengono al Glossario e alla Specifica Estesa (o Completamente Vestita) del Caso d'Uso.

Perché è un errore? Se domani l'azienda decide che la penale scatta al 20% invece che al 30%, non devi dover aggiornare il diagramma dei casi d'uso o la sua descrizione breve.

### UC Dettagliati
controllare estenzioni e eccezioni e collegamenti tra di esse
