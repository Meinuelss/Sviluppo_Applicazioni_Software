package catering.businesslogic.event;

import java.sql.Date;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import catering.businesslogic.CatERing;
import catering.businesslogic.UseCaseLogicException;
import catering.businesslogic.user.User;
import catering.persistence.PersistenceManager;

class GestireEventiTest {

    @BeforeAll
    @SuppressWarnings("unused")
    static void initializeDatabase() {
        PersistenceManager.initializeDatabase("database/catering_init_sqlite.sql");
    }

    @Nested
    @SuppressWarnings("unused")
    class CreationAndData {

        private EventManager eventMgr;

        @BeforeEach
        void setUp() {
            eventMgr = CatERing.getInstance().getEventManager();
        }

        @Test
        //DSD 1: Crea scheda evento (Controllo Organizzatore e Titolo opzionale
        void test1_CreaSchedaEvento() throws Exception {
            CatERing.getInstance().getUserManager().fakeLogin("Luca");

            UseCaseLogicException eccezione = assertThrows(UseCaseLogicException.class, () -> {
                eventMgr.createEventCard("Festa Vietata");
            });
            assertEquals("Utente non autorizzato: devi essere un Organizzatore per creare un evento.",
                    eccezione.getMessage());

            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");

            Event eventoConTitolo = assertDoesNotThrow(() -> eventMgr.createEventCard("Festa di Laurea"));

            assertAll("Verifica evento creato con titolo",
                    () -> assertNotNull(eventoConTitolo),
                    () -> assertEquals("Festa di Laurea", eventoConTitolo.getName()),
                    () -> assertEquals("Preliminare", eventoConTitolo.getStatus()),
                    () -> assertTrue(eventoConTitolo.getId() > 0, "Salvato tramite EventReceiver nel DB"));

            Event eventoSenzaTitolo = assertDoesNotThrow(() -> eventMgr.createEventCard(null));

            assertAll("Verifica evento creato senza titolo",
                    () -> assertNotNull(eventoSenzaTitolo),
                    () -> assertNull(eventoSenzaTitolo.getName()),
                    () -> assertEquals("Preliminare", eventoSenzaTitolo.getStatus()));
        }

        @Test
        //DSD 2: Inserisci i dati, verifica permessi e genera Ricorrenza
        void test2_InserisciDati() throws Exception {
            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
            eventMgr.createEventCard("Meeting Aziendale");

            Date dataInizio = new Date(System.currentTimeMillis() + 86400000L);
            Date dataFine = new Date(System.currentTimeMillis() + 172800000L);

            CatERing.getInstance().getUserManager().fakeLogin("Luca");

            UseCaseLogicException eccezione = assertThrows(UseCaseLogicException.class, () -> {
                eventMgr.insertData("Cliente Srl", dataInizio, dataFine, "Sala A", 100, "Note", false, null, null);
            });
            assertEquals("Utente non autorizzato: devi essere un Organizzatore.", eccezione.getMessage());

            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
            eventMgr.createEventCard("Congresso Medico Ricorrente");
            Date conclusioneRicorrenza = new Date(System.currentTimeMillis() + (30L * 24 * 60 * 60 * 1000));

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
                        conclusioneRicorrenza);
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
                    () -> assertEquals(conclusioneRicorrenza, eventoCapofila.getRecurrenceObj().getConclusion()));
        }

        @Test
        //EST 2D: Modifica Dati con applicazione Penale per >30% e blocco In Corso
        void test_ModificaDatiVincoli() throws Exception {
            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");

            eventMgr.createEventCard("Festa Aziendale 30%");
            Date dataInizio = new Date(System.currentTimeMillis() + 86400000L);
            Date dataFine = new Date(System.currentTimeMillis() + 172800000L);
            eventMgr.insertData("Azienda Spa", dataInizio, dataFine, "Sala Centrale", 100, "", false, null, null);
            Event eventoSelezionato = eventMgr.getSelectedEvent();

            assertDoesNotThrow(() -> {
                eventMgr.modifyEventData("Azienda Spa", dataInizio, dataFine, "Sala Centrale", 120, "", "", false);
            });
            assertEquals(120, eventoSelezionato.getNumParticipants());
            assertFalse(eventoSelezionato.hasPenalty(), "La penale NON deve scattare per variazioni <= 30%");

            assertDoesNotThrow(() -> {
                eventMgr.modifyEventData("Azienda Spa", dataInizio, dataFine, "Sala Centrale", 180, "", "", true);
            });
            assertEquals(180, eventoSelezionato.getNumParticipants(), "I dati devono essere modificati con successo");
            assertTrue(eventoSelezionato.hasPenalty(), "La penale DEVE scattare automaticamente per variazioni > 30%");

            eventoSelezionato.setStatus("In Corso");

            UseCaseLogicException exInCorso = assertThrows(UseCaseLogicException.class, () -> {
                eventMgr.modifyEventData("Azienda Spa", dataInizio, dataFine, "Sala Centrale", 120, "", "", false);
            });
            assertTrue(exInCorso.getMessage().contains("Impossibile modificare"),
                    "Deve impedire qualsiasi modifica se l'evento è in corso");
        }
    }

    @Nested
    @SuppressWarnings("unused")
    class MenuAndStaff {

        private EventManager eventMgr;

        @BeforeEach
        void setUp() {
            eventMgr = CatERing.getInstance().getEventManager();
        }

        @Test
        //DSD 3: Approva Menu — percorso diretto (senza modifiche) e con modifiche + accettazione chef
        void test3_ApprovaMenu() throws Exception {
            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
            eventMgr.createEventCard("Cena di Gala del Personale");
            catering.businesslogic.menu.Menu menuTest = catering.businesslogic.menu.Menu.load(1);
            menuTest.setPublished(false);

            CatERing.getInstance().getUserManager().fakeLogin("Luca");
            assertThrows(UseCaseLogicException.class, () -> {
                eventMgr.approveMenu(menuTest, null);
            });

            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
            eventMgr.createEventCard("Cena Approvazione Diretta");

            catering.businesslogic.menu.Menu menuDiretto = catering.businesslogic.menu.Menu.load(1);
            menuDiretto.setPublished(false);

            Service servDiretto = eventMgr.defineService(java.sql.Time.valueOf("20:00:00"), java.sql.Time.valueOf("23:00:00"), "Cena Diretta");
            servDiretto.setName("Cena Diretta");
            servDiretto.updateService();
            servDiretto.proposeMenu(menuDiretto);

            StaffMember cameriere1 = new StaffMember("Luigi");
            cameriere1.setAvailable(true);
            StaffAssignment ap1 = eventMgr.assignStaff(cameriere1, "Responsabile di Sala", servDiretto);
            assertTrue(ap1.isWaitingForMenu(), "Prima dell'approvazione lo staff deve essere in attesa");

            Event eD = eventMgr.getSelectedEvent();
            assertDoesNotThrow(() -> eventMgr.approveMenu(menuDiretto, null));

            assertAll("Percorso diretto: menu approvato subito, staff sbloccato",
                    () -> assertTrue(menuDiretto.isApproved(), "Menu deve essere approvato"),
                    () -> assertEquals("In Corso", eD.getStatus(), "Evento deve passare In Corso"),
                    () -> assertFalse(ap1.isWaitingForMenu(), "Staff deve essere sbloccato"),
                    () -> assertFalse(ap1.needsReview(), "Nessuna review richiesta"));

            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
            eventMgr.createEventCard("Cena con Modifiche Chef");

            catering.businesslogic.menu.Menu menuMod = catering.businesslogic.menu.Menu.load(1);
            menuMod.setPublished(false);

            Service servMod = eventMgr.defineService(java.sql.Time.valueOf("19:00:00"), java.sql.Time.valueOf("22:00:00"), "Cena Modificata");
            servMod.setName("Cena Modificata");
            servMod.updateService();
            servMod.proposeMenu(menuMod);

            StaffMember cameriere2 = new StaffMember("Marco");
            cameriere2.setAvailable(true);
            StaffAssignment ap2 = eventMgr.assignStaff(cameriere2, "Cameriere", servMod);
            assertTrue(ap2.isWaitingForMenu(), "Prima della proposta lo staff deve essere in attesa");

            Event eMod = eventMgr.getSelectedEvent();

            assertDoesNotThrow(() -> eventMgr.approveMenu(menuMod, "Sostituire vino bianco con rosso d'annata"));

            assertAll("Dopo proposta modifiche: menu non approvato, chef deve confermare",
                    () -> assertFalse(menuMod.isApproved(), "Menu NON deve essere approvato ancora"),
                    () -> assertNotEquals("In Corso", eMod.getStatus(), "Evento NON deve essere In Corso ancora"),
                    () -> assertTrue(ap2.isWaitingForMenu(), "Staff è ancora in attesa del menu"),
                    () -> assertTrue(ap2.needsReview(), "Staff deve avere reviewAssignment=true"),
                    () -> assertEquals(1, eMod.getModifications().size(), "Deve esserci una modifica registrata"),
                    () -> assertEquals("Sostituire vino bianco con rosso d'annata",
                            eMod.getModifications().get(0).getContent()));

            CatERing.getInstance().getUserManager().fakeLogin("Chiara");
            assertDoesNotThrow(() -> eventMgr.chefAcceptsMenu(menuMod));

            assertAll("Dopo accettazione chef: menu approvato e staff sbloccato",
                    () -> assertTrue(menuMod.isApproved(), "Menu deve essere approvato"),
                    () -> assertEquals("In Corso", eMod.getStatus(), "Evento deve essere In Corso"),
                    () -> assertFalse(ap2.isWaitingForMenu(), "Staff deve essere sbloccato"),
                    () -> assertFalse(ap2.needsReview(), "Review completata"));
        }

        @Test
        //TEST 3: Assegna Personale con controllo Organizzatore, Disponibilità e Menu
        void test3_AssegnaPersonale() throws Exception {
            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
            eventMgr.createEventCard("Buffet Estivo");
            Service servizio = assertDoesNotThrow(() -> eventMgr.defineService(java.sql.Time.valueOf("19:00:00"), java.sql.Time.valueOf("22:00:00"), "Apericena"));
            servizio.setName("Apericena");
            servizio.updateService();

            StaffMember cameriere = new StaffMember("Marco");
            cameriere.setAvailable(true);

            CatERing.getInstance().getUserManager().fakeLogin("Luca");

            UseCaseLogicException eccezioneOrg = assertThrows(UseCaseLogicException.class, () -> {
                eventMgr.assignStaff(cameriere, "Cameriere", servizio);
            });
            assertEquals("Utente non autorizzato: devi essere un Organizzatore.", eccezioneOrg.getMessage());

            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");

            StaffMember cameriereOccupato = new StaffMember("Anna");
            cameriereOccupato.setAvailable(false);

            UseCaseLogicException eccezioneDisp = assertThrows(UseCaseLogicException.class, () -> {
                eventMgr.assignStaff(cameriereOccupato, "Sommelier", servizio);
            });
            assertEquals("Il membro del personale non è disponibile per questo turno.", eccezioneDisp.getMessage());

            StaffAssignment assegnamento = assertDoesNotThrow(() -> {
                return eventMgr.assignStaff(cameriere, "Cameriere", servizio);
            });

            assertAll("Verifica post-condizioni Assegna Personale",
                    () -> assertNotNull(assegnamento),
                    () -> assertEquals("Cameriere", assegnamento.getRole()),
                    () -> assertEquals("Marco", assegnamento.getMember().getName()),
                    () -> assertTrue(assegnamento.isWaitingForMenu(),
                            "Il menu è null, quindi inAttesaDiMenu deve essere true"),
                    () -> assertTrue(servizio.getAssignments().contains(assegnamento)));
        }
    }

    @Nested
    @SuppressWarnings("unused")
    class LifecycleAndStatus {

        private EventManager eventMgr;

        @BeforeEach
        void setUp() {
            eventMgr = CatERing.getInstance().getEventManager();
        }

        @Test
        //TEST 5: Conferma Evento con controlli su stato, servizi e chef
        void test5_ConfermaEvento() throws Exception {
            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
            eventMgr.createEventCard("Festa di Fine Anno");
            Event e = eventMgr.getSelectedEvent();

            CatERing.getInstance().getUserManager().fakeLogin("Luca");
            UseCaseLogicException exUser = assertThrows(UseCaseLogicException.class, () -> eventMgr.confirmEvent());
            assertTrue(exUser.getMessage().contains("Organizzatore"));

            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");

            UseCaseLogicException exStato = assertThrows(UseCaseLogicException.class, () -> eventMgr.confirmEvent());
            assertTrue(exStato.getMessage().contains("Preliminare"));

            e.setStatus("In Corso");

            UseCaseLogicException exServizi = assertThrows(UseCaseLogicException.class, () -> eventMgr.confirmEvent());
            assertTrue(exServizi.getMessage().contains("servizi"));

            Service serv = eventMgr.defineService(java.sql.Time.valueOf("12:00:00"), java.sql.Time.valueOf("15:00:00"), "Pranzo");
            serv.setName("Pranzo");
            serv.updateService();

            UseCaseLogicException exChef = assertThrows(UseCaseLogicException.class, () -> eventMgr.confirmEvent());
            assertTrue(exChef.getMessage().contains("chef"));

            User chiara = User.load(6);
            assertDoesNotThrow(() -> eventMgr.assignChef(chiara));

            assertDoesNotThrow(() -> {
                eventMgr.confirmEvent();
            });

            assertEquals("Confermato", e.getStatus(), "L'evento deve scattare in stato Confermato!");
        }

        @Test
        //ECC 7a.1a: Inserisci Deroga Penale con annullamento della penale
        void test_InserisciDerogaPenale() throws Exception {
            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
            eventMgr.createEventCard("Cena di Gala di Prova");
            Event eventoCorrente = eventMgr.getSelectedEvent();

            eventoCorrente.setStatus("In Corso");

            assertDoesNotThrow(() -> {
                eventMgr.addWaiverPenalty("Ritardo dovuto a cause di forza maggiore", false);
            });

            assertAll("Verifica post-condizioni Deroga Penale",
                    () -> assertFalse(eventoCorrente.hasPenalty(),
                            "La penale NON deve essere applicata se è stata concessa una deroga."),
                    () -> assertEquals("Ritardo dovuto a cause di forza maggiore", eventoCorrente.getWaiverReason(),
                            "La motivazione della deroga deve essere salvata correttamente."));
        }

        @Test
        //Annullamento evento Preliminare: Nessuna penale applicata a prescindere dai parametri
        void test_AnnullamentoEventoPreliminare() throws Exception {
            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");

            eventMgr.createEventCard("Evento da Annullare Subito");
            Event e = eventMgr.getSelectedEvent();

            assertDoesNotThrow(() -> {
                eventMgr.cancelEvent(null, true);
            });

            assertAll("Verifica Annullamento Preliminare",
                    () -> assertEquals("Annullato", e.getStatus(), "Lo stato deve cambiare in Annullato"),
                    () -> assertFalse(e.hasPenalty(),
                            "La penale NON deve scattare in fase preliminare, anche se richiesta"),
                    () -> assertNull(e.getWaiverReason()));
        }

        @Test
        //EST 7a: Annullamento evento In Corso con scelta tra Penale o Deroga
        void test_AnnullamentoEventoInCorso() throws Exception {
            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");

            eventMgr.createEventCard("Pranzo con Penale");
            Event eventoPenale = eventMgr.getSelectedEvent();
            eventoPenale.setStatus("In Corso");

            assertDoesNotThrow(() -> {
                eventMgr.cancelEvent(null, true);
            });

            assertAll("Verifica Scenario Penale",
                    () -> assertEquals("Annullato", eventoPenale.getStatus()),
                    () -> assertTrue(eventoPenale.hasPenalty(), "La penale deve essere applicata"),
                    () -> assertNull(eventoPenale.getWaiverReason()));

            eventMgr.createEventCard("Pranzo con Deroga");
            Event eventoDeroga = eventMgr.getSelectedEvent();
            eventoDeroga.setStatus("In Corso");

            assertDoesNotThrow(() -> {
                eventMgr.cancelEvent("Eccezione per maltempo grave", false);
            });

            assertAll("Verifica Scenario Deroga",
                    () -> assertEquals("Annullato", eventoDeroga.getStatus()),
                    () -> assertFalse(eventoDeroga.hasPenalty(), "La penale non deve scattare se c'è la deroga"),
                    () -> assertEquals("Eccezione per maltempo grave", eventoDeroga.getWaiverReason()));
        }

        @Test
        //Eliminazione evento: Permessa solo in fase Preliminare
        void test_EliminazioneEvento() throws Exception {
            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");

            eventMgr.createEventCard("Evento da Eliminare");
            assertDoesNotThrow(() -> {
                eventMgr.deleteEvent();
            });
            assertNull(eventMgr.getSelectedEvent(), "L'evento corrente deve essere null dopo l'eliminazione");

            eventMgr.createEventCard("Evento Intoccabile");
            Event eventoIntoccabile = eventMgr.getSelectedEvent();

            eventoIntoccabile.setStatus("In Corso");

            UseCaseLogicException ex = assertThrows(UseCaseLogicException.class, () -> {
                eventMgr.deleteEvent();
            });
            assertTrue(ex.getMessage().contains("solo in fase Preliminare"),
                    "Il sistema DEVE lanciare un'eccezione se si prova a eliminare un evento non preliminare");
        }

        @Test
        //Terminazione Evento: Permessa solo in stato Confermato/In Corso, con note storiche e documentazione
        void test_TerminazioneEvento() throws Exception {
            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
            eventMgr.createEventCard("Evento da Terminare");
            Event e = eventMgr.getSelectedEvent();
            e.setStatus("Confermato");

            CatERing.getInstance().getUserManager().fakeLogin("Luca");
            UseCaseLogicException exUser = assertThrows(UseCaseLogicException.class, () -> {
                eventMgr.terminateEvent("Note storiche", "Documentazione finale");
            });
            assertTrue(exUser.getMessage().contains("Organizzatore"),
                    "Deve impedire a un non-organizzatore di terminare l'evento");

            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");
            eventMgr.createEventCard("Evento Preliminare Non Terminabile");

            UseCaseLogicException exStato = assertThrows(UseCaseLogicException.class, () -> {
                eventMgr.terminateEvent("Note finali", "Report evento");
            });
            assertTrue(exStato.getMessage().contains("Confermato o In Corso"),
                    "Deve impedire la terminazione di un evento in stato Preliminare");

            eventMgr.createEventCard("Congresso Terminato con Successo");
            Event eventoConfermato = eventMgr.getSelectedEvent();
            Service serv = eventMgr.defineService(java.sql.Time.valueOf("12:00:00"), java.sql.Time.valueOf("15:00:00"), "Pranzo");
            serv.setName("Pranzo");
            serv.updateService();

            eventoConfermato.setStatus("Confermato");
            eventoConfermato.updateEvent();

            assertDoesNotThrow(() -> {
                eventMgr.terminateEvent(
                        "L'evento si è svolto regolarmente con 150 partecipanti. Feedback positivo.",
                        "Report finale con foto e valutazioni");
            });

            assertAll("Verifica post-condizioni terminateEvent con documentazione",
                    () -> assertEquals("Chiuso", eventoConfermato.getStatus(),
                            "Lo stato deve cambiare in Chiuso"),
                    () -> assertEquals("L'evento si è svolto regolarmente con 150 partecipanti. Feedback positivo.",
                            eventoConfermato.getNotes(),
                            "Le note devono essere sostituite dalle note storiche"),
                    () -> assertNotNull(eventoConfermato.getDocumentations(),
                            "La lista documentazioni non deve essere null"),
                    () -> assertEquals(1, eventoConfermato.getDocumentations().size(),
                            "Deve essere allegata una documentazione"),
                    () -> assertEquals("Report finale con foto e valutazioni",
                            eventoConfermato.getDocumentations().get(0).getInformation(),
                            "Il contenuto della documentazione deve corrispondere"));

            eventMgr.createEventCard("Evento InProgress da Chiudere");
            Event eventoInProgress = eventMgr.getSelectedEvent();
            eventoInProgress.setStatus("In Corso");

            assertDoesNotThrow(() -> {
                eventMgr.terminateEvent("Chiusura anticipata per completamento lavori.", null);
            });

            assertAll("Verifica post-condizioni terminateEvent senza documentazione",
                    () -> assertEquals("Chiuso", eventoInProgress.getStatus()),
                    () -> assertEquals("Chiusura anticipata per completamento lavori.",
                            eventoInProgress.getNotes()),
                    () -> assertTrue(eventoInProgress.getDocumentations().isEmpty(),
                            "Non deve esserci alcuna documentazione allegata se non fornita"));
        }
    }

    @Nested
    @SuppressWarnings("unused")
    class RecurrenceManagement {

        private EventManager eventMgr;

        @BeforeEach
        void setUp() {
            eventMgr = CatERing.getInstance().getEventManager();
        }

        @Test
        //Modifica Ricorrenza: riutilizza istanze preliminari, crea nuove se necessario, rimuove le eccedenti
        void test_ModificaRicorrenza() throws Exception {
            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");

            java.util.Calendar calStart = java.util.Calendar.getInstance();
            calStart.set(2026, java.util.Calendar.MAY, 1);
            Date startDate = new Date(calStart.getTimeInMillis());

            java.util.Calendar calEnd = java.util.Calendar.getInstance();
            calEnd.set(2026, java.util.Calendar.MAY, 3);
            Date endDate = new Date(calEnd.getTimeInMillis());

            java.util.Calendar calConc = java.util.Calendar.getInstance();
            calConc.set(2026, java.util.Calendar.MAY, 31);
            Date conclusionDate = new Date(calConc.getTimeInMillis());

            eventMgr.createEventCard("Corso di Formazione");
            eventMgr.insertData(
                    "Azienda Cliente SpA",
                    startDate, endDate, "Aule", 20, "Note", true, "Settimanale", conclusionDate);

            Event capofila = eventMgr.getSelectedEvent();
            Recurrence r = capofila.getRecurrenceObj();

            assertNotNull(r, "L'evento capofila deve avere una ricorrenza");
            assertEquals(4, r.getGeneratedEvents().size(), "Settimanale per 1 mese = 4 occorrenze generate");

            Event primaIstanza = r.getGeneratedEvents().get(0);
            primaIstanza.setStatus("Confermato");
            Date vecchieDateInizio = primaIstanza.getDateStart();

            java.util.Calendar calNuovaConc = java.util.Calendar.getInstance();
            calNuovaConc.set(2026, java.util.Calendar.MAY, 10);
            Date nuovaConclusion = new Date(calNuovaConc.getTimeInMillis());

            assertDoesNotThrow(() -> {
                eventMgr.modifyRecurrence("Giornaliera", nuovaConclusion);
            });

            assertEquals("Giornaliera", r.getFrequency());
            assertEquals(9, r.getGeneratedEvents().size(), "Deve aver creato/modificato in totale 9 occorrenze figlie");
            assertEquals("Confermato", r.getGeneratedEvents().get(0).getStatus());
            assertEquals(vecchieDateInizio.toString(), r.getGeneratedEvents().get(0).getDateStart().toString(),
                    "Le date dell'istanza Confermata non devono essere state alterate");

            java.util.Calendar checkUltima = java.util.Calendar.getInstance();
            checkUltima.setTime(r.getGeneratedEvents().get(8).getDateStart());
            assertEquals(10, checkUltima.get(java.util.Calendar.DAY_OF_MONTH));
            assertEquals(java.util.Calendar.MAY, checkUltima.get(java.util.Calendar.MONTH));

            assertDoesNotThrow(() -> {
                eventMgr.modifyRecurrence("Mensile", conclusionDate);
            });

            assertEquals(1, r.getGeneratedEvents().size(), "Deve essere rimasta SOLO l'istanza Confermata");
            assertEquals("Confermato", r.getGeneratedEvents().get(0).getStatus());
        }

        @Test
        //Propagazione Modifiche Ricorrenza (modifyEventData, cancelEvent, deleteCurrentEvent)
        void test9_PropagazioneRicorrenza() throws Exception {
            CatERing.getInstance().getUserManager().fakeLogin("Giovanni");

            java.util.Calendar calStart = java.util.Calendar.getInstance();
            calStart.set(2026, java.util.Calendar.JUNE, 1);
            Date startDate = new Date(calStart.getTimeInMillis());

            java.util.Calendar calEnd = java.util.Calendar.getInstance();
            calEnd.set(2026, java.util.Calendar.JUNE, 1);
            Date endDate = new Date(calEnd.getTimeInMillis());

            java.util.Calendar calConc = java.util.Calendar.getInstance();
            calConc.set(2026, java.util.Calendar.JUNE, 30);
            Date conclusionDate = new Date(calConc.getTimeInMillis());

            eventMgr.createEventCard("Corsi Estivi");
            eventMgr.insertData("Scuola ABC", startDate, endDate, "Aula 1", 30, "Portare quaderni", true, "Settimanale",
                    conclusionDate);

            Event capofila = eventMgr.getSelectedEvent();
            Recurrence r = capofila.getRecurrenceObj();
            assertNotNull(r);
            assertEquals(4, r.getGeneratedEvents().size(), "Deve avere 4 occorrenze preliminari");

            Event istanzaInCorso = r.getGeneratedEvents().get(1);
            istanzaInCorso.setStatus("In Corso");
            String vecchiaLocation = istanzaInCorso.getLocation();

            eventMgr.modifyEventData("Scuola XYZ", startDate, endDate, "Aula 2 (Nuova)", 40, "Niente quaderni", true, "", false);

            assertEquals("Aula 2 (Nuova)", r.getGeneratedEvents().get(0).getLocation(),
                    "La prima istanza Preliminare deve aggiornarsi");
            assertEquals(vecchiaLocation, istanzaInCorso.getLocation(), "L'istanza In Corso NON deve aggiornarsi");
            assertEquals("Aula 2 (Nuova)", r.getGeneratedEvents().get(2).getLocation(),
                    "Anche le altre istanze Preliminari devono aggiornarsi");

            Event primaIstanza = r.getGeneratedEvents().get(0);
            eventMgr.setSelectedEvent(primaIstanza);

            eventMgr.cancelEvent(null, false, true);

            assertEquals("Annullato", primaIstanza.getStatus(), "La prima istanza deve essere annullata");
            assertEquals("In Corso", istanzaInCorso.getStatus(), "L'istanza In Corso NON deve essere annullata");
            assertEquals("Annullato", r.getGeneratedEvents().get(2).getStatus(),
                    "Le altre istanze Preliminari devono essere annullate");

            r.getGeneratedEvents().get(2).setStatus("Preliminare");
            eventMgr.setSelectedEvent(r.getGeneratedEvents().get(2));

            eventMgr.deleteEvent(true);

            for (Event ei : r.getGeneratedEvents()) {
                if (ei.getId() != r.getGeneratedEvents().get(2).getId()) {
                    assertFalse("Preliminare".equals(ei.getStatus()), "Non devono rimanere istanze preliminari propagate");
                }
            }
        }
    }
}
