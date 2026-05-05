### Domande
- Manca estensione 6a, da riportare negli SSD e contratti? o visto che la modifica del menù è relativa ad un solo evento non bisogna farlo?
- Manca estensione 4a, da riportare o no
- Gestione preavviso 14 giorni, in ogni file, come dobbiamo gestirlo?
  
Aggiungere la propagazione su ricorrenza in UC, contratti, SSD, DSD, DCD (è il buco più grave: -2/-3 punti da solo)
Aggiungere la modifica della ricorrenza in sé (frequenza/numero ripetizioni)
Sistemare la confusione pubblicato vs approvato sul Menu introducendo un attributo separato o un'associazione qualificata
Allineare nomi tra DSD e DCD (hasApprovedMenu vs isMenuApproved, ServiceConstraintException mancante)
Sistemare l'estensione 1e (rimuovere la deroga inventata o trasformarla in eccezione bloccante)
Separare note da noteStoriche nel modello di dominio
Valorizzare tipologia in creaEvento o rimuoverlo dal modello

GEMINI

UC DETTAGLIATO: 
Nel passo 1c.2 (Modifica dati) indicate "Torna al passo 2". Questo è un po' rigido: se modifico solo il numero di partecipanti (senza violare le soglie) o le note, non è detto che io debba ridefinire tutti i servizi. Sarebbe stato meglio un ritorno opzionale o al termine del caso d'uso.

MODELLO DI DOMINIO: 
Incongruenza Menù-Servizio: Nel testo del progetto è scritto esplicitamente: "Ciascun servizio avrà una precisa fascia oraria e naturalmente un proprio menu". Tuttavia, nel vostro Modello di Dominio, l'associazione "in uso in" collega Menù direttamente a Evento. Questo è un errore grave: un evento complesso (es. 3 giorni di fiera) ha più servizi e più menù. Se collegate il menù all'evento, non sapete quale menù servire al coffee break e quale al pranzo.

Modifiche Locali al Menù: Il testo dice: "l'organizzatore può proporre delle modifiche ai menù... queste proposte restano visibili come aggiunte o eliminazioni limitate all'evento in questione". Nel MD non c'è traccia di un'entità (es. PropostaModifica) o di una classe associativa che leghi il Menù all'Evento/Servizio per memorizzare queste annotazioni.

SSD: 
Nello scenario principale, passo 5, usate l'operazione di sistema consultaMenuProposto(unServizio). Questo è semanticamente corretto rispetto al testo, ma va in crash con il vostro Modello di Dominio! Poiché nel MD avete collegato il Menù all'Evento e non al Servizio, il sistema, ricevendo unServizio, non ha un'associazione diretta per recuperare il menù associato.  

Al passo 6a.1 usate proponiModifiche(unMenu, noteModifica). Il sistema riceve questa stringa di testo, ma, come fatto notare nel MD, non ha un contenitore concettuale in cui registrarla in associazione a quello specifico evento.

CONTRATTI: 
Unico dettaglio formale: nelle firme di Annullamento ed Eliminazione passate l'oggetto unEvento come parametro. Ricordate che, di norma, l'operazione di sistema agisce sull'evento "corrente" già selezionato o richiede un ID, ma concettualmente è accettabile.

DCD: 
Tutto okk

DSD:
Nel DSD assignStaff, l'EventManager valuta service.hasApprovedMenu() e poi passa un boolean al costruttore di StaffAssignment (create(member, role, false/true)). Sarebbe stato ancora più incapsulato far gestire questo flag direttamente al costruttore di StaffAssignment o al metodo addAssignment del Service.

CLAUDE

UC DETTAGLIATO:
a) Estensione 1e (preavviso 14 giorni) — concettualmente sbagliata. Hai modellato il preavviso come estensione del passo 1, con possibilità di "forzare l'inserimento motivando l'emergenza". Ma nei contratti il preavviso è una pre-condizione di creaEvento e non c'è alcuna deroga prevista. Dal testo del progetto (Pierpaolo): "non accetto mai commesse se non ho almeno due settimane di preavviso" — è una regola di business rigida. Non c'è coerenza tra UC e contratti su questo punto. Decidi: o è una regola rigida (e l'estensione 1e va rimossa o trasformata in eccezione che blocca il flusso), o è derogabile (e va aggiunta nei contratti). Probabilmente la prima è più aderente al testo.

b) L'estensione 1d (Ricorrenza) "il flusso riprende dal passo 3" — ma nel contratto confermaDatiCapofila si dice che ogni istanza generata ha già i Servizi clonati e non ha chef/personale. Quindi il flusso dovrebbe riprendere dal passo 3 per ciascuna istanza autonomamente, non per il capofila. Andrebbe chiarito.
c) Manca esplicito nell'UC dettagliato il caso "modifica della ricorrenza in sé" (cambiare frequenza/numero ripetizioni), che invece il testo del progetto richiede esplicitamente: "L'utente deve anche poter modificare la ricorrenza in sé, ossia modificare la frequenza o il numero degli eventi". Questa funzionalità manca completamente in tutti gli artefatti. È una lacuna importante.

d) Manca la propagazione di modifica/annullamento/eliminazione su istanze ricorrenti. Il testo dice esplicitamente: "gli viene chiesto se vuole modificare nello stesso modo (o annullare, o eliminare) l'intero evento ricorrente... oppure solo la singola istanza". Nell'UC dettagliato lo accenni nelle estensioni 1a/1b ("Se l'evento è parte di una ricorrenza, chiede..."), ma non c'è alcun contratto né SSD che gestisca la propagazione. Lacuna importante.

e) Estensione 6a (proposta modifiche al menù): ok averla, ma manca un contratto per proponiModifiche.

MODELLO DI DOMINIO:
a) Attributo tipologia su Evento mancante nei contratti. Nel modello di dominio Evento ha tipologia: testo, ma nel contratto creaEvento non viene mai valorizzato né è un parametro. Va aggiunto al contratto o rimosso dal modello.

b) Relazione Menu ↔ Servizio ambigua. Hai "prevede" tra Servizio (0..1) e Menu (0..n), ma nel testo del progetto un servizio ha un menu (la cardinalità 0..n verso Menu sul lato Servizio è strana — un singolo Servizio può avere più menu? Rileggi il testo: "Ciascun servizio avrà... un proprio menu"). Probabilmente intendevi 0..1 da entrambi i lati durante la fase preliminare.

c) "Foglio riepilogativo", "Mansione di cucina", "Compito", "Tabellone dei turni", "Cuoco", "Turno" appaiono nel modello di dominio ma sono fuori scope per l'UC "Gestire gli eventi" (riguardano "Gestire turni" e "Assegnamento compiti cucina"). Per il DCD focalizzato sull'UC vanno bene se servono come contesto, ma assicurati che il professore voglia vederli qui.

d) Documentazione con cardinalità 0..n verso Evento. Una Documentazione si riferisce a un solo evento, quindi 1 va bene; ma Evento contiene 0..n Documentazione significa che un evento può avere zero o più documentazioni. Coerente con "allegando eventuali documentazioni" — ok.

SSD:
l'SSD dello Scenario Principale ha un loop attorno al passo 4-5 che è corretto, e mostra le estensioni 4a/6a inline con OPT. Buono.
Però: non c'è un SSD per la modifica della ricorrenza né per la propagazione modifica/annullamento/eliminazione su istanze ricorrenti (coerente con la lacuna nei contratti).

CONTRATTI:
a) assegnaPersonale — pre-condizione unMembro.disponibile = sì. Ma l'estensione 4b dice che si può assegnare prima che lo chef abbia proposto il menù. Quindi la disponibilità del membro è un check separato dal menù — ok, ma assicurati che disponibile non sia confuso con "ha dato disponibilità per quel turno". Nel modello di dominio è un booleano semplice: questo è troppo semplificato rispetto al testo che parla di "il personale dà la propria disponibilità ai turni". Probabilmente accettabile per restare in scope, ma lo segnalerei come scelta consapevole.

b) approvaMenu post-condizione unMenu.pubblicato = sì. Attenzione: nel modello di dominio pubblicato è un attributo del Menu che ha un significato preciso ("visibile a tutti, usabile o copiabile") legato al ricettario, non all'approvazione per un evento. Sono due concetti diversi. Rischi di confondere "menù pubblicato nel ricettario" con "menù approvato per un evento". Probabilmente serve un altro attributo, tipo approvato: si/no su Menu o sull'associazione Servizio-Menu.

c) 1a.1 Annullamento — manca il caso ricorrenza. La firma è Annullamento(unEvento: Evento) ma se unEvento è parte di una ricorrenza non c'è gestione. Stessa cosa per Eliminazione e modificaDati.

d) chiudiEvento post-condizione e.note = noteStoriche. Sovrascrive le note di scheda con quelle storiche. La NdR lo giustifica con "il modello ha un singolo attributo note", ma è una scelta di modellazione povera: in realtà sarebbero due cose diverse. Lo segnalerei come limite del modello.

DCD:
a) EventManager.currentEvent come attributo singleton. È una scelta classica ma fragile in contesti multi-utente concorrenti. Per un progetto didattico è accettabile, ma vale la pena dichiararlo come limite.

b) Recurrence.generateOccurrences() ritorna ArrayList<Event> ma poi nella relazione c'è generatedEvents 1..n. Coerente, ok.

c) Manca ServiceConstraintException che usi nel DSD modifyEventData — nel DCD c'è solo UseCaseLogicException. Da aggiungere.

d) Su Event.markAsCancelled(): non gestisce la differenza tra "preliminare → annullato" (libera tutto) e "in corso → annullato" (penale). I contratti lo distinguono, il metodo del DCD no. Probabilmente è corretto perché la logica di penale è gestita altrove (applyPenalty), ma nel DSD di annullamento non c'è.

e) Manca completamente nel DCD la gestione della propagazione su ricorrenza (modifica/annulla/elimina "tutte le istanze") — coerente con la lacuna degli altri artefatti.

DSD:
a) DSD approveMenu: chiami setPublished(true) sul menu — ma come detto sopra, "pubblicato" e "approvato" sono concetti diversi. Inoltre il loop for each ap in s.assignments dentro il loop for each s in services con il check s.getMenu() == menu è un po' contorto: bastava iterare solo sui servizi che hanno quel menù.

b) DSD confirmFlagshipData: la chiamata si = s.clone() e poi update attributes è vaga — i contratti dicono che si.fasciaOraria e si.tipologia ereditano dal capofila. Ok, ma andrebbe esplicitato. Inoltre addService(si) è chiamato sull'istanza ei (l'evento generato), non sull'eventMgr — dovrebbe essere ei.addService(si). Verifica la freccia.

c) DSD assignStaff: manca il check di disponibilità del membro nella guard — c'è solo member.isAvailable() && currentEvent.containsService(service) come condizione dell'alt esterno. Però poi al ramo "else" lanci UseCaseLogicException, e questa è la stessa eccezione che usi per altri casi. Forse servirebbe un'eccezione più specifica (MemberNotAvailableException).

