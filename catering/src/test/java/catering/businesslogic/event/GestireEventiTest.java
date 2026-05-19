// ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
// file interamente nuovo per i nostri test
package catering.businesslogic.event;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import catering.businesslogic.CatERing;
import catering.businesslogic.UseCaseLogicException;

class GestireEventiTest {

    private EventManager eventMgr;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        // Recuperiamo l'istanza corretta dell'EventManager dal sistema centrale
        eventMgr = CatERing.getInstance().getEventManager();
    }

    // --- DSD 1: CREA SCHEDA EVENTO ---
    @Test
    @DisplayName("DSD 1: Crea scheda evento (Controllo Organizzatore e Titolo opzionale)")
    void test1_CreaSchedaEvento() throws Exception {
        // =========================================================
        // PARTE 1: TEST FALLIMENTO (Utente NON autorizzato)
        // =========================================================
        // ARRANGE: Facciamo il login con un utente che NON è organizzatore (es. Luca è un Cuoco)
        CatERing.getInstance().getUserManager().fakeLogin("Luca");

        // ACT & ASSERT: Proviamo a creare l'evento e ci aspettiamo l'eccezione
        UseCaseLogicException eccezione = assertThrows(UseCaseLogicException.class, () -> {
            eventMgr.createEventCard("Festa Vietata");
        });
        assertEquals("Utente non autorizzato: devi essere un Organizzatore per creare un evento.", eccezione.getMessage());


        // =========================================================
        // PARTE 2: TEST SUCCESSO CON TITOLO (Utente autorizzato)
        // =========================================================
        // ARRANGE: Facciamo il login con un Organizzatore ("Giovanni" è organizzatore nel DB)
        CatERing.getInstance().getUserManager().fakeLogin("Giovanni");

        // ACT: L'organizzatore crea un nuovo evento con titolo
        Event eventoConTitolo = assertDoesNotThrow(() -> eventMgr.createEventCard("Festa di Laurea"));

        // ASSERT: Verifico le post-condizioni
        assertAll("Verifica evento creato con titolo",
            () -> assertNotNull(eventoConTitolo),
            () -> assertEquals("Festa di Laurea", eventoConTitolo.getName()), // Titolo settato
            () -> assertEquals("Preliminare", eventoConTitolo.getStatus()),  // Stato corretto
            () -> assertTrue(eventoConTitolo.getId() > 0, "Salvato tramite EventReceiver nel DB")
        );

        // =========================================================
        // PARTE 3: TEST SUCCESSO SENZA TITOLO (Titolo opzionale)
        // =========================================================
        // ACT: L'organizzatore crea un evento passando null come titolo
        Event eventoSenzaTitolo = assertDoesNotThrow(() -> eventMgr.createEventCard(null));

        // ASSERT: Verifico che funzioni lo stesso
        assertAll("Verifica evento creato senza titolo",
            () -> assertNotNull(eventoSenzaTitolo),
            () -> assertNull(eventoSenzaTitolo.getName()), // Il titolo non è stato settato
            () -> assertEquals("Preliminare", eventoSenzaTitolo.getStatus())
        );
    }

    // --- DSD 2: INSERISCI DATI (Con controllo Organizzatore e Ricorrenza) ---
    @Test
    @DisplayName("DSD 2: Inserisci i dati, verifica permessi e genera Ricorrenza")
    void test2_InserisciDati() throws Exception {
        
        // =========================================================
        // PARTE 1: TEST FALLIMENTO (Utente non Organizzatore)
        // =========================================================
        // ARRANGE: Facciamo il login come ORGANIZZATORE per poter creare l'evento di base
        CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
        eventMgr.createEventCard("Meeting Aziendale");
        
        Date dataInizio = new Date(System.currentTimeMillis() + 86400000L); // Domani
        Date dataFine = new Date(System.currentTimeMillis() + 172800000L);  // Dopodomani
        System.out.println("Data Inizio: " + dataInizio);

        // ORA CAMBIAMO UTENTE: Facciamo il login con un utente NON organizzatore (Luca)
        CatERing.getInstance().getUserManager().fakeLogin("Luca");

        // ACT & ASSERT: Il sistema deve lanciare UseCaseLogicException
        UseCaseLogicException eccezione = assertThrows(UseCaseLogicException.class, () -> {
            eventMgr.insertData("Cliente Srl", dataInizio, dataFine, "Sala A", 100, "Note", false, null, null);
        });
        assertEquals("Utente non autorizzato: devi essere un Organizzatore.", eccezione.getMessage());


        // =========================================================
        // PARTE 2: TEST SUCCESSO CON RICORRENZA (Utente Organizzatore)
        // =========================================================
        // (Lascia il resto del test esattamente com'era prima, partendo dal fakeLogin di Giovanni)
        CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
        
        eventMgr.createEventCard("Congresso Medico Ricorrente");
        Date conclusioneRicorrenza = new Date(System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000)); // Tra un mese


        assertDoesNotThrow(() -> {
            eventMgr.insertData(
                "Ospedale Maggiore", 
                dataInizio, 
                dataFine, 
                "Auditorium Centrale", 
                150, 
                "Richiesto servizio interpretariato", 
                true,                                  
                "Settimanale",                         
                conclusioneRicorrenza                  
            );
        });

        Event eventoCapofila = eventMgr.getSelectedEvent();
        assertAll("Verifica post-condizioni DSD 2",
            () -> assertNotNull(eventoCapofila),
            () -> assertEquals("Ospedale Maggiore", eventoCapofila.getClientData()),
            () -> assertEquals("Auditorium Centrale", eventoCapofila.getLocation()),
            () -> assertEquals(150, eventoCapofila.getNumParticipants()),
            () -> assertEquals("Richiesto servizio interpretariato", eventoCapofila.getNotes()),
            () -> assertNotNull(eventoCapofila.getRecurrenceObj(), "L'oggetto Recurrence deve essere stato creato"),
            () -> assertEquals("Settimanale", eventoCapofila.getRecurrenceObj().getFrequency()),
            () -> assertEquals(conclusioneRicorrenza, eventoCapofila.getRecurrenceObj().getConclusion())
        );
    }

    // --- TEST 3: APPROVA MENU (Estensione 4a) ---
    @Test
    @DisplayName("DSD 3: Approva Menu con controllo Organizzatore, modifiche opzionali e sblocco staff")
    void test3_ApprovaMenu() throws Exception {
        
        // =========================================================
        // PARTE 1: TEST FALLIMENTO (Utente non Organizzatore)
        // =========================================================
        // ARRANGE: Creiamo un evento da organizzatore
        CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
        eventMgr.createEventCard("Cena di Gala del Personale");
        
        // Carichiamo il menu di esempio pre-configurato nel DB (ID 1)
        catering.businesslogic.menu.Menu menu = catering.businesslogic.menu.Menu.load(1);
        
        // FIX: Impostiamo il menu come NON pubblicato (bozza) per testare correttamente l'attesa!
        menu.setPublished(false);
        
        // Cambiamo utente a Luca (Cuoco) per tentare l'approvazione non autorizzata
        CatERing.getInstance().getUserManager().fakeLogin("Luca");
        
        // ACT & ASSERT: Deve lanciare UseCaseLogicException
        assertThrows(UseCaseLogicException.class, () -> {
            eventMgr.approveMenu(menu, "Cambiare il dolce in Tiramisù");
        });

        // =========================================================
        // PARTE 2: TEST SUCCESSO (Utente Organizzatore e sblocco Staff)
        // =========================================================
        // ARRANGE: Torniamo all'utente Organizzatore autorizzato
        CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
        
        // Definiamo un servizio e colleghiamogli il menu
        Service servizio = eventMgr.defineService("20:00 - 23:00", "Cena Elegante");
        servizio.setMenu(menu);
        
        // Assegniamo un membro dello staff disponibile (risulterà in attesa perché il menu non è approvato)
        StaffMember cameriere = new StaffMember("Luigi");
        cameriere.setAvailable(true);
        StaffAssignment ap = eventMgr.assignStaff(cameriere, "Responsabile di Sala", servizio);
        
        // Verifica preliminare: lo staff è effettivamente bloccato in attesa del menu
        assertTrue(ap.isWaitingForMenu(), "Prima dell'approvazione lo staff deve essere in attesa");

        // ACT: L'organizzatore approva ufficialmente il menu inserendo delle modifiche testuali
        assertDoesNotThrow(() -> {
            eventMgr.approveMenu(menu, "Sostituire vino bianco con rosso d'annata");
        });

        // ASSERT: Verifichiamo lo scatto di stato dell'evento e lo sblocco automatico dello staff
        Event e = eventMgr.getSelectedEvent();
        assertAll("Verifica post-condizioni DSD Approva Menu",
            () -> assertNotNull(e),
            () -> assertEquals("InProgress", e.getStatus(), "L'evento deve passare in stato InProgress"),
            () -> assertFalse(ap.isWaitingForMenu(), "Lo staff NON deve più essere in attesa (inAttesaDiMenu = false)"),
            () -> assertEquals(1, e.getModifications().size(), "Deve essere registrata una modifica nel sistema"),
            () -> assertEquals("Sostituire vino bianco con rosso d'annata", e.getModifications().get(0).getContent())
        );
    }

    // --- DSD4: ASSEGNA PERSONALE (DSD 5) ---
    @Test
    @DisplayName("TEST 3: Assegna Personale con controllo Organizzatore, Disponibilità e Menu")
    void test3_AssegnaPersonale() throws Exception {
        
        // =========================================================
        // PARTE 1: TEST FALLIMENTO (Utente non Organizzatore)
        // =========================================================
        CatERing.getInstance().getUserManager().fakeLogin("Giovanni"); // Login come organizzatore
        eventMgr.createEventCard("Buffet Estivo");
        Service servizio = assertDoesNotThrow(() -> eventMgr.defineService("19:00 - 22:00", "Apericena"));
        
        StaffMember cameriere = new StaffMember("Marco"); 
        cameriere.setAvailable(true);

        // Cambiamo utente (Luca è Cuoco, non Organizzatore)
        CatERing.getInstance().getUserManager().fakeLogin("Luca");

        UseCaseLogicException eccezioneOrg = assertThrows(UseCaseLogicException.class, () -> {
            eventMgr.assignStaff(cameriere, "Cameriere", servizio);
        });
        assertEquals("Utente non autorizzato: devi essere un Organizzatore.", eccezioneOrg.getMessage());


        // =========================================================
        // PARTE 2: TEST FALLIMENTO (Membro non disponibile)
        // =========================================================
        CatERing.getInstance().getUserManager().fakeLogin("Giovanni"); // Torniamo Organizzatore
        
        StaffMember cameriereOccupato = new StaffMember("Anna");
        cameriereOccupato.setAvailable(false); // Settiamo il membro come NON disponibile

        UseCaseLogicException eccezioneDisp = assertThrows(UseCaseLogicException.class, () -> {
            eventMgr.assignStaff(cameriereOccupato, "Sommelier", servizio);
        });
        assertEquals("Il membro del personale non è disponibile per questo turno.", eccezioneDisp.getMessage());


        // =========================================================
        // PARTE 3: TEST SUCCESSO (Membro disponibile, Menu mancante)
        // =========================================================
        // ACT: Assegniamo il cameriere che era disponibile
        StaffAssignment assegnamento = assertDoesNotThrow(() -> {
            return eventMgr.assignStaff(cameriere, "Cameriere", servizio);
        });

        // ASSERT: Verifichiamo tutte le post-condizioni
        assertAll("Verifica post-condizioni Assegna Personale",
            () -> assertNotNull(assegnamento),
            () -> assertEquals("Cameriere", assegnamento.getRole()),
            () -> assertEquals("Marco", assegnamento.getMember().getName()),
            // Poiché non abbiamo settato alcun menu per questo servizio, deve risultare in attesa
            () -> assertTrue(assegnamento.isWaitingForMenu(), "Il menu è null, quindi inAttesaDiMenu deve essere true"),
            () -> assertTrue(servizio.getAssignments().contains(assegnamento))
        );
    }
    

    // --- DSD5: CONFERMA EVENTO ---
    @Test
    @DisplayName("TEST 5: Conferma Evento con controlli su stato, servizi e chef")
    void test5_ConfermaEvento() throws Exception {
        
        // =========================================================
        // PARTE 1: TEST FALLIMENTO (Utente non Organizzatore)
        // =========================================================
        CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
        eventMgr.createEventCard("Festa di Fine Anno");
        Event e = eventMgr.getSelectedEvent();
        
        CatERing.getInstance().getUserManager().fakeLogin("Luca"); // Cuoco
        UseCaseLogicException exUser = assertThrows(UseCaseLogicException.class, () -> eventMgr.confirmEvent());
        assertTrue(exUser.getMessage().contains("Organizzatore"));


        // =========================================================
        // PARTE 2: TEST FALLIMENTO (Stato Preliminare)
        // =========================================================
        CatERing.getInstance().getUserManager().fakeLogin("Giovanni"); // Torniamo Organizzatore
        
        UseCaseLogicException exStato = assertThrows(UseCaseLogicException.class, () -> eventMgr.confirmEvent());
        assertTrue(exStato.getMessage().contains("Preliminare"));

        // Forziamo lo stato a InProgress (simulando che l'approvazione del menu sia già avvenuta)
        e.setStatus("InProgress");


        // =========================================================
        // PARTE 3: TEST FALLIMENTO (Nessun Servizio)
        // =========================================================
        UseCaseLogicException exServizi = assertThrows(UseCaseLogicException.class, () -> eventMgr.confirmEvent());
        assertTrue(exServizi.getMessage().contains("servizi"));

        // Aggiungiamo un servizio per superare il blocco
        eventMgr.defineService("12:00 - 15:00", "Pranzo");


        // =========================================================
        // PARTE 4: TEST FALLIMENTO (Nessuno Chef)
        // =========================================================
        UseCaseLogicException exChef = assertThrows(UseCaseLogicException.class, () -> eventMgr.confirmEvent());
        assertTrue(exChef.getMessage().contains("chef"));

        // Assegniamo uno chef (ID 7 è lo Chef Giovanni nel DB) per superare il blocco
        e.setChefId(7); 


        // =========================================================
        // PARTE 5: TEST SUCCESSO (Tutti i requisiti soddisfatti)
        // =========================================================
        assertDoesNotThrow(() -> {
            eventMgr.confirmEvent();
        });

        // ASSERT: L'evento deve aver raggiunto il suo stato finale!
        assertEquals("Confermato", e.getStatus(), "L'evento deve scattare in stato Confermato!");
    }

    // --- ECCEZIONE 7a.1a: INSERISCI DEROGA PENALE ---
    @Test
    @DisplayName("ECC 7a.1a: Inserisci Deroga Penale con annullamento della penale")
    void test_InserisciDerogaPenale() throws Exception {
        
        // =========================================================
        // ARRANGE: Setup dell'evento e dello stato
        // =========================================================
        CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
        eventMgr.createEventCard("Cena di Gala di Prova");
        Event eventoCorrente = eventMgr.getSelectedEvent();
        
        // Forziamo manualmente lo stato dell'evento per simulare che i lavori siano iniziati
        eventoCorrente.setStatus("InCorso");
        
        // =========================================================
        // ACT: Inserimento della deroga
        // =========================================================
        assertDoesNotThrow(() -> {
            eventMgr.inserisciDerogaPenale("Ritardo dovuto a cause di forza maggiore", false);
        });
        
        // =========================================================
        // ASSERT: Verifiche delle post-condizioni
        // =========================================================
        assertAll("Verifica post-condizioni Deroga Penale",
            () -> assertFalse(eventoCorrente.hasPenalty(), "La penale NON deve essere applicata se è stata concessa una deroga."),
            () -> assertEquals("Ritardo dovuto a cause di forza maggiore", eventoCorrente.getWaiverReason(), "La motivazione della deroga deve essere salvata correttamente.")
        );
    }

    // --- ESTENSIONE 2D: MODIFICA DATI (Regola del 30%) ---
    @Test
    @DisplayName("EST 2D: Modifica Dati con applicazione Penale per >30% e blocco In Corso")
    void test_ModificaDatiVincoli() throws Exception {
        
        CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
        
        // Creiamo e impostiamo un evento a 100 partecipanti
        eventMgr.createEventCard("Festa Aziendale 30%");
        Date dataInizio = new Date(System.currentTimeMillis() + 86400000L);
        Date dataFine = new Date(System.currentTimeMillis() + 172800000L);  // Dopodomani
        eventMgr.insertData("Azienda Spa", dataInizio, dataFine, "Sala Centrale", 100, "", false, null, null);
        Event eventoSelezionato = eventMgr.getSelectedEvent();
        
        // =========================================================
        // PARTE 1: MODIFICA VALIDA (entro il 30%) -> NESSUNA PENALE
        // =========================================================
        // Aumento del 20% (da 100 a 120 pax)
        assertDoesNotThrow(() -> {
            eventMgr.modifyEventData("Azienda Spa", dataInizio, dataFine, "Sala Centrale", 120, "");
        });
        assertEquals(120, eventoSelezionato.getNumParticipants());
        assertFalse(eventoSelezionato.hasPenalty(), "La penale NON deve scattare per variazioni <= 30%");

        // =========================================================
        // PARTE 2: MODIFICA OLTRE IL 30% -> SCATTA LA PENALE!
        // =========================================================
        // Aumento drastico (da 120 a 180 pax)
        assertDoesNotThrow(() -> {
            eventMgr.modifyEventData("Azienda Spa", dataInizio, dataFine, "Sala Centrale", 180, "");
        });
        assertEquals(180, eventoSelezionato.getNumParticipants(), "I dati devono essere modificati con successo");
        assertTrue(eventoSelezionato.hasPenalty(), "La penale DEVE scattare automaticamente per variazioni > 30%");

        // =========================================================
        // PARTE 3: MODIFICA SU EVENTO IN CORSO -> ECCEZIONE 2d.1b
        // =========================================================
        // Forziamo lo stato a In Corso
        eventoSelezionato.setStatus("InCorso");
        
        UseCaseLogicException exInCorso = assertThrows(UseCaseLogicException.class, () -> {
            // Cerchiamo di fare una modifica
            eventMgr.modifyEventData("Azienda Spa", dataInizio, dataFine, "Sala Centrale", 120, "");
        });
        assertTrue(exInCorso.getMessage().contains("Impossibile modificare"), "Deve impedire qualsiasi modifica se l'evento è in corso");
    }

    // --- ANNULLAMENTO EVENTO PRELIMINARE (Senza Penale) ---
    @Test
    @DisplayName("Annullamento evento Preliminare: Nessuna penale applicata a prescindere dai parametri")
    void test_AnnullamentoEventoPreliminare() throws Exception {
        CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
        
        eventMgr.createEventCard("Evento da Annullare Subito");
        Event e = eventMgr.getSelectedEvent();
        
        // L'evento è "Preliminare". Proviamo ad annullarlo passando "true" per la penale:
        // Il sistema deve ignorarla perché la penale si applica SOLO se InCorso!
        assertDoesNotThrow(() -> {
            eventMgr.AnnulledEvent(null, true);
        });

        assertAll("Verifica Annullamento Preliminare",
            () -> assertEquals("Annullato", e.getStatus(), "Lo stato deve cambiare in Annullato"),
            () -> assertFalse(e.hasPenalty(), "La penale NON deve scattare in fase preliminare, anche se richiesta"),
            () -> assertNull(e.getWaiverReason())
        );
    }

    // --- ESTENSIONE 7A: ANNULLAMENTO EVENTO IN CORSO (Penale o Deroga) ---
    @Test
    @DisplayName("EST 7a: Annullamento evento In Corso con scelta tra Penale o Deroga")
    void test_AnnullamentoEventoInCorso() throws Exception {
        
        CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
        
        // =========================================================
        // SCENARIO A: Annullamento con PENALE
        // =========================================================
        eventMgr.createEventCard("Pranzo con Penale");
        Event eventoPenale = eventMgr.getSelectedEvent();
        eventoPenale.setStatus("InCorso"); // Simuliamo che sia in corso
        
        assertDoesNotThrow(() -> {
            // Nessuna motivazione (null), applicazione penale = true
            eventMgr.AnnulledEvent(null, true);
        });
        
        assertAll("Verifica Scenario Penale",
            () -> assertEquals("Annullato", eventoPenale.getStatus()),
            () -> assertTrue(eventoPenale.hasPenalty(), "La penale deve essere applicata"),
            () -> assertNull(eventoPenale.getWaiverReason())
        );

        // =========================================================
        // SCENARIO B: Annullamento con DEROGA
        // =========================================================
        eventMgr.createEventCard("Pranzo con Deroga");
        Event eventoDeroga = eventMgr.getSelectedEvent();
        eventoDeroga.setStatus("InCorso"); // Simuliamo che sia in corso
        
        assertDoesNotThrow(() -> {
            // Motivazione presente, penale = false
            eventMgr.AnnulledEvent("Eccezione per maltempo grave", false);
        });
        
        assertAll("Verifica Scenario Deroga",
            () -> assertEquals("Annullato", eventoDeroga.getStatus()),
            () -> assertFalse(eventoDeroga.hasPenalty(), "La penale non deve scattare se c'è la deroga"),
            () -> assertEquals("Eccezione per maltempo grave", eventoDeroga.getWaiverReason())
        );
    }

    @Test
    @DisplayName("Eliminazione evento: Permessa solo in fase Preliminare")
    void test_EliminazioneEvento() throws Exception {
        CatERing.getInstance().getUserManager().fakeLogin("Giovanni");

        // 1. ELIMINAZIONE CONSENTITA (Evento Preliminare)
        eventMgr.createEventCard("Evento da Eliminare");
        assertDoesNotThrow(() -> {
            eventMgr.deleteCurrentEvent();
        });
        assertNull(eventMgr.getSelectedEvent(), "L'evento corrente deve essere null dopo l'eliminazione");

        // 2. ELIMINAZIONE BLOCCATA (Evento InCorso)
        eventMgr.createEventCard("Evento Intoccabile");
        Event eventoIntoccabile = eventMgr.getSelectedEvent();
        
        // Forziamo lo stato per simulare che l'evento sia andato avanti
        eventoIntoccabile.setStatus("InCorso"); 

        UseCaseLogicException ex = assertThrows(UseCaseLogicException.class, () -> {
            eventMgr.deleteCurrentEvent();
        });
        assertTrue(ex.getMessage().contains("solo in fase Preliminare"), 
            "Il sistema DEVE lanciare un'eccezione se si prova a eliminare un evento non preliminare");
    }

}