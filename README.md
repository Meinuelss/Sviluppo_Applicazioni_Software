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

