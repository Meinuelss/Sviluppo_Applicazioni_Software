package catering.businesslogic.event;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;

import catering.businesslogic.CatERing;
import catering.businesslogic.UseCaseLogicException;
import catering.businesslogic.menu.Menu;
import catering.businesslogic.user.User;

/**
 * EventManager handles all operations related to events and services in the
 * CatERing system.
 * It manages event creation, modification, and deletion, as well as service
 * management and menu assignments for services.
 */
public class EventManager {

    private ArrayList<EventReceiver> eventReceivers;
    private Event selectedEvent;
    private Service currentService;

    /**
     * Constructor initializes the event receivers list
     */
    public EventManager() {
        eventReceivers = new ArrayList<>();
    }

    /**
     * Adds an event receiver to be notified of events changes
     * 
     * @param receiver The event receiver to add
     */
    public void addEventReceiver(EventReceiver receiver) {
        if (receiver != null && !eventReceivers.contains(receiver)) {
            eventReceivers.add(receiver);
        }
    }

    /**
     * Removes an event receiver
     * 
     * @param receiver The event receiver to remove
     */
    public void removeEventReceiver(EventReceiver receiver) {
        eventReceivers.remove(receiver);
    }

    /**
     * Gets all events in the system
     * 
     * @return List of all events
     */
    public ArrayList<Event> getEvents() {
        return Event.loadAllEvents();
    }

    /**
     * Sets the current service based on service ID
     * 
     * @param serviceId ID of the service to select
     */
    public void setSelectedServiceIndex(int serviceId) {
        if (selectedEvent != null && selectedEvent.getServices() != null) {
            for (Service si : selectedEvent.getServices()) {
                if (si.getId() == serviceId) {
                    currentService = si;
                    return;
                }
            }
        }
        // If service not found, currentService remains unchanged
    }

    /**
     * Sets the current service directly
     * 
     * @param service Service to set as current
     */
    public void setCurrentService(Service service) {
        this.currentService = service;
    }

    /**
     * Gets the current service
     * 
     * @return Current service or null if none selected
     */
    public Service getCurrentService() {
        return this.currentService;
    }

    /**
     * Gets the selected event
     * 
     * @return Selected event or null if none selected
     */
    public Event getSelectedEvent() {
        return selectedEvent;
    }

    /**
     * Sets the selected event
     * 
     * @param event Event to select
     */
    public void setSelectedEvent(Event event) {
        this.selectedEvent = event;
    }

    /**
     * Modifies a service
     * 
     * @param serviceId ID of the service to modify
     * @param name      New name for the service
     * @param date      New date for the service
     * @param location  New location for the service
     * @param menuId    ID of the menu to assign (0 for no menu)
     * @return The modified service, or null if not found
     */
    public Service modifyService(int serviceId, String name, Date date, String location, int menuId) {
        // First try to find service in the current event's services list
        Service service = findServiceById(serviceId);

        if (service != null) {
            // Update service properties
            service.setName(name);
            service.setDate(date);
            service.setLocation(location);

            // Handle menu assignment if needed
            if (menuId > 0 && (service.getMenuId() == 0 || service.getMenuId() != menuId)) {
                try {
                    Menu menu = Menu.load(menuId);
                    if (menu != null) {
                        service.setMenu(menu);
                    }
                } catch (Exception e) {
                    System.err.println("Error loading menu: " + e.getMessage());
                }
            }

            // Notify all receivers
            notifyServiceModified(service);

            // Update current service reference if this is the current service
            if (currentService != null && currentService.getId() == serviceId) {
                currentService = service;
            }
        }

        return service;
    }

    /**
     * Deletes a service by its ID
     * 
     * @param serviceId ID of the service to delete
     * @return true if deleted successfully, false otherwise
     */
    public boolean deleteService(int serviceId) {
        try {
            if (selectedEvent == null) {
                return false;
            }

            Service serviceToDelete = findServiceById(serviceId);
            if (serviceToDelete == null) {
                return false;
            }

            selectedEvent.removeService(serviceToDelete);

            // Clear current service if it was the one deleted
            if (currentService != null && currentService.getId() == serviceId) {
                currentService = null;
            }

            // Notify all receivers (EventPersistence will delete from DB)
            notifyServiceDeleted(serviceToDelete);

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Elimina l'evento attualmente in gestione.
     * Permesso SOLO se l'evento è in stato "Preliminare".
     */
    public void deleteCurrentEvent() throws UseCaseLogicException {
        deleteCurrentEvent(false);
    }

    public void deleteCurrentEvent(boolean propagate) throws UseCaseLogicException {
        // 1. Controllo permessi utente
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException(
                    "Utente non autorizzato: devi essere un Organizzatore per eliminare un evento.");
        }

        // 2. Controllo che ci sia un evento selezionato
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento attualmente in gestione da eliminare.");
        }

        // 3. REGOLA DI BUSINESS: L'eliminazione è permessa SOLO in fase Preliminare
        if (!"Preliminare".equals(this.selectedEvent.getStatus())) {
            throw new UseCaseLogicException(
                    "Impossibile eliminare l'evento: l'eliminazione è consentita solo in fase Preliminare. Usa la funzione di annullamento.");
        }

        // Salviamo il riferimento prima di svuotarlo
        Event eventToDelete = this.selectedEvent;

        // 4. Svuota lo stato del manager (l'evento sta per essere distrutto)
        this.selectedEvent = null;
        this.currentService = null;

        // Propagazione dell'eliminazione alle altre istanze preliminari della
        // ricorrenza
        if (propagate && eventToDelete.getRecurrenceObj() != null) {
            java.util.ArrayList<Event> generated = eventToDelete.getRecurrenceObj().getGeneratedEvents();
            // Iteriamo a ritroso per poter rimuovere elementi in sicurezza
            for (int i = generated.size() - 1; i >= 0; i--) {
                Event ei = generated.get(i);
                if ("Preliminare".equals(ei.getStatus()) && ei.getId() != eventToDelete.getId()) {
                    generated.remove(i);
                    notifyEventDeleted(ei);
                }
            }
        }

        // 5. Notifica i receiver per procedere con l'eliminazione nel DB
        // (Questo andrà a chiamare in automatico EventPersistence ->
        // Event.deleteEvent())
        notifyEventDeleted(eventToDelete);
    }

    /**
     * Assigns a menu to the current service
     * 
     * @param menu The menu to assign
     * @throws UseCaseLogicException if no event or service is selected
     */
    public void assignMenu(Menu menu) throws UseCaseLogicException {
        if (selectedEvent == null) {
            String msg = "Cannot assign menu: no event selected";
            throw new UseCaseLogicException(msg);
        }

        if (currentService == null) {
            String msg = "Cannot assign menu: no service selected";
            throw new UseCaseLogicException(msg);
        }

        currentService.setMenu(menu);

        // Notify all receivers (EventPersistence will persist)
        notifyMenuAssigned(currentService, menu);
    }

    /**
     * Removes the menu from the current service
     * 
     * @return true if removed successfully, false if no service selected
     */
    public boolean removeMenu() {
        if (currentService == null) {
            return false;
        }

        currentService.removeMenu();

        // Notify all receivers
        notifyMenuRemoved(currentService);

        return true;
    }

    /**
     * Helper method to find a service by ID within the selected event
     */
    private Service findServiceById(int serviceId) {
        if (selectedEvent == null || selectedEvent.getServices() == null) {
            return null;
        }

        for (Service s : selectedEvent.getServices()) {
            if (s.getId() == serviceId) {
                return s;
            }
        }

        return null;
    }

    // Notification methods to avoid code duplication

    private void notifyEventCreated(Event event) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateEventCreated(event);
        }
    }

    private void notifyEventModified(Event event) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateEventModified(event);
        }
    }

    private void notifyEventDeleted(Event event) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateEventDeleted(event);
        }
    }

    private void notifyEventConfirmed(Event event) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateEventConfirmed(event);
        }
    }

    private void notifyEventCancelled(Event event) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateEventCancelled(event);
        }
    }

    private void notifyServiceCreated(Service service) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateServiceCreated(selectedEvent, service);
        }
    }

    private void notifyServiceModified(Service service) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateServiceModified(service);
        }
    }

    private void notifyServiceDeleted(Service service) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateServiceDeleted(service);
        }
    }

    private void notifyMenuAssigned(Service service, Menu menu) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateMenuAssigned(service, menu);
        }
    }

    private void notifyMenuRemoved(Service service) {
        for (EventReceiver receiver : eventReceivers) {
            receiver.updateMenuRemoved(service);
        }
    }

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // Aggiunta metodi per gestire i nostri casi nel test1

    public Event createEventCard(String title) throws UseCaseLogicException {
        // 1. Il sistema controlla che l'utente sia un organizzatore
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException(
                    "Utente non autorizzato: devi essere un Organizzatore per creare un evento.");
        }

        // 2. Crea l'evento
        Event event = new Event();

        // 3. Opzionalmente setta il titolo
        if (title != null && !title.trim().isEmpty()) {
            event.setName(title); // Usiamo getName/setName per coerenza col codice del prof
        }

        // 4. Viene settato lo stato preliminare
        event.setStatus("Preliminare");

        // 5. Viene salvato in eventReceiver (Observer)
        notifyEventCreated(event);

        // Aggiorniamo lo stato interno del manager
        this.selectedEvent = event;
        this.currentService = null;

        return event;
    }

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // Aggiunta metodi per gestire i nostri casi nel test2

    public void insertData(String clientData, Date startDate, Date endDate, String location, int pax,
            String notes, boolean recurrence, String frequency, Date conclusion) throws UseCaseLogicException {

        // 1. Ottiene l'utente e controlla che sia l'organizzatore [cite: 184, 383-385]
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException("Utente non autorizzato: devi essere un Organizzatore.");
        }

        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento in gestione.");
        }

        // 2. Controlla che lo stato sia "Preliminare" [cite: 200, 386]
        if (!"Preliminare".equals(this.selectedEvent.getStatus())) {
            throw new UseCaseLogicException("Impossibile modificare l'evento: non è in stato Preliminare.");
        }

        // 3. Setta i dati base dell'evento capofila [cite: 202-206, 396]
        this.selectedEvent.setClientData(clientData);
        this.selectedEvent.setDateStart(startDate);
        this.selectedEvent.setDateEnd(endDate);
        this.selectedEvent.setLocation(location);
        this.selectedEvent.setNumParticipants(pax);

        // 4. Opzionale: setta le note se presenti [cite: 207, 393, 397]
        if (notes != null) {
            this.selectedEvent.setNotes(notes);
        }

        // 5. Se c'è una ricorrenza [cite: 208, 394]
        if (recurrence) {
            // Crea la ricorrenza e la setta sul capofila [cite: 212-213, 399-400]
            Recurrence r = new Recurrence(frequency, conclusion);
            this.selectedEvent.setRecurrenceObj(r);

            // Generiamo le istanze future fino alla conclusione [cite: 214, 395]
            java.util.Calendar calStart = java.util.Calendar.getInstance();
            calStart.setTime(startDate);
            java.util.Calendar calEnd = java.util.Calendar.getInstance();
            calEnd.setTime(endDate);

            // Avanziamo alla prima occorrenza
            advanceDate(calStart, frequency);
            advanceDate(calEnd, frequency);

            while (!calStart.getTime().after(conclusion)) {
                Event ei = new Event();

                // Copia i dati dal capofila [cite: 214, 402]
                ei.copyFrom(this.selectedEvent);

                // Setta le date dell'istanza e nuovamente la ricorrenza [cite: 214, 403-404]
                ei.setDates(new Date(calStart.getTimeInMillis()), new Date(calEnd.getTimeInMillis()));
                ei.setRecurrenceObj(r);
                ei.setStatus("Preliminare");

                // Aggiunge l'istanza generata alla ricorrenza
                r.addGeneratedEvent(ei);

                // Salva a DB l'istanza generata (che a sua volta salverà anche la Recurrence se
                // non salvata)
                notifyEventCreated(ei);

                // Avanza alla prossima data
                advanceDate(calStart, frequency);
                advanceDate(calEnd, frequency);
            }
        }

        // 6. Infine fa l'updateEventDataChanged tramite notifica all'Observer [cite:
        // 405, 408]
        notifyEventModified(this.selectedEvent);
    }

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // Aggiunta metodi per gestire i nostri casi nel test3

    private void advanceDate(java.util.Calendar cal, String frequency) {
        if ("Giornaliera".equalsIgnoreCase(frequency) || "Giornaliero".equalsIgnoreCase(frequency)) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        } else if ("Settimanale".equalsIgnoreCase(frequency)) {
            cal.add(java.util.Calendar.WEEK_OF_YEAR, 1);
        } else if ("Mensile".equalsIgnoreCase(frequency)) {
            cal.add(java.util.Calendar.MONTH, 1);
        } else {
            // Default di ripiego
            cal.add(java.util.Calendar.WEEK_OF_YEAR, 1);
        }
    }

    public void approveMenu(Menu menu, String modifications) throws UseCaseLogicException {
        // 1. Il solito controllo sull'organizzatore [cite: 448-449]
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException("Utente non autorizzato: devi essere un Organizzatore.");
        }

        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento in gestione.");
        }

        // Approva il menu impostandolo come pubblicato [cite: 460-461]
        menu.setPublished(true);

        // 2. Opzionalmente propone modifiche [cite: 462-463]
        if (modifications != null && !modifications.trim().isEmpty()) {
            Modification m = new Modification(modifications); // Crea modification [cite: 464]
            m.setContent(modifications); // Setta il contenuto [cite: 465]
            m.linkTo(this.selectedEvent, menu); // Le collega all'evento e al menu [cite: 466]
            this.selectedEvent.addModification(m);
        }

        // 3. Setta lo stato in progress [cite: 468]
        this.selectedEvent.setStatus("In Corso");

        // 4. Per ogni servizio, opzionalmente se il servizio ha il menu [cite: 469-471]
        if (this.selectedEvent.getServices() != null) {
            for (Service service : this.selectedEvent.getServices()) {
                if (service.getMenu() == menu) {
                    // Per ogni assegna personale in service.getAssignments() [cite: 472]
                    if (service.getAssignments() != null) {
                        for (StaffAssignment ap : service.getAssignments()) {
                            // Opzionalmente se ap.isWaitingForMenu() fa setWaitingForMenu(false) [cite:
                            // 473-475]
                            if (ap.isWaitingForMenu()) {
                                ap.setWaitingForMenu(false);
                            }
                        }
                    }
                }
            }
        }

        // 5. Salva in event receiver [cite: 476]
        for (EventReceiver er : eventReceivers) {
            er.updateMenuApproved(this.selectedEvent, menu); // [cite: 479]
        }
    }

    public Service defineService(Time timeStart, Time timeEnd, String type) throws UseCaseLogicException {
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento in gestione.");
        }

        Service service = new Service();
        service.setTimeStart(timeStart);;
        service.setTimeEnd(timeEnd);
        service.setType(type);
        service.setEventId(this.selectedEvent.getId());

        // Aggiungiamo all'evento
        this.selectedEvent.addService(service);
        this.currentService = service;

        // Notifichiamo l'Observer
        notifyServiceCreated(service);

        return service;
    }

    public StaffAssignment assignStaff(StaffMember member, String role, Service service) throws UseCaseLogicException {
        // 1. Solito controllo sull'organizzatore
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException("Utente non autorizzato: devi essere un Organizzatore.");
        }

        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento in gestione.");
        }

        // 2. Controlla se il membro è disponibile da StaffMember
        if (!member.isAvailable()) {
            throw new UseCaseLogicException("Il membro del personale non è disponibile per questo turno.");
        }

        // 3. Crea l'assegnamento e setta membro e ruolo
        StaffAssignment ap = new StaffAssignment(role, member, false);
        ap.setRole(role);
        ap.setMember(member);

        service.addAssignment(ap);

        // 4. Ottiene il menu e fa i controlli
        Menu menu = service.getMenu();

        // N.B: Assumiamo che "approvato" nel DB del prof corrisponda a "isPublished()"
        boolean isApproved = (menu != null && menu.isPublished());

        if (menu == null || !isApproved) {
            // Notifica che il menù manca
            notifyMenuMissing(service);
            ap.setWaitingForMenu(true);
        } else {
            // Altrimenti fa solo setWaitingForMenu. (Messo a FALSE per coerenza logica)
            ap.setWaitingForMenu(false);
        }

        // 5. Infine salva tutto in event receiver
        notifyStaffAssigned(service, ap);

        return ap;
    }

    private void notifyMenuMissing(Service service) {
        for (EventReceiver er : eventReceivers) {
            er.updateMenuMissing(service);
        }
    }

    private void notifyStaffAssigned(Service service, StaffAssignment ap) {
        for (EventReceiver er : eventReceivers) {
            er.updateStaffAssigned(service, ap);
        }
    }

    public void confirmEvent() throws UseCaseLogicException {
        // Controllo sull'organizzatore
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException("Utente non autorizzato: devi essere un Organizzatore.");
        }

        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento in gestione.");
        }

        // Controlla se è in stato preliminare e lancia eccezione
        if ("Preliminare".equals(this.selectedEvent.getStatus())) {
            throw new UseCaseLogicException(
                    "Impossibile confermare: l'evento è ancora in stato Preliminare. Approva prima il menu.");
        }

        // Controlla hasServices()
        if (!this.selectedEvent.hasServices()) {
            throw new UseCaseLogicException("Impossibile confermare: l'evento non ha servizi definiti.");
        }

        // Controlla hasChef()
        if (!this.selectedEvent.hasChef()) {
            throw new UseCaseLogicException("Impossibile confermare: l'evento non ha uno chef assegnato.");
        }

        // Controlla isValid()
        if (!this.selectedEvent.isValid()) {
            throw new UseCaseLogicException("Impossibile confermare: i dati dell'evento non sono validi.");
        }

        // Cambia lo stato in Confermato
        this.selectedEvent.setStatus("Confermato");

        notifyEventConfirmed(this.selectedEvent);
    }

    public void assignChef(User chef) throws UseCaseLogicException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException("Utente non autorizzato: devi essere un Organizzatore.");
        }

        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento in gestione.");
        }

        if (chef == null || !chef.isChef()) {
            throw new UseCaseLogicException("L'utente specificato non ha il ruolo di Chef.");
        }

        this.selectedEvent.setChef(chef);
        notifyEventModified(this.selectedEvent);
    }

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // Aggiunta metodi per gestire i nostri casi nel test4

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // Aggiunta metodi per gestire i nostri casi nel test5

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // codice generato per risolvere i warnings
    public ArrayList<EventReceiver> getEventReceivers() {
        return eventReceivers;
    }

    public void setEventReceivers(ArrayList<EventReceiver> eventReceivers) {
        this.eventReceivers = eventReceivers;
    }

    public void inserisciDerogaPenale(String motivazioneDeroga, boolean penale) throws UseCaseLogicException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();

        if (!user.isOrganizer()) {
            throw new UseCaseLogicException("Solo l'organizzatore può inserire una deroga.");
        }

        // SOSTITUITO currentEvent con selectedEvent
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento attualmente in gestione.");
        }

        // SOSTITUITO currentEvent con selectedEvent
        if (motivazioneDeroga != null && !motivazioneDeroga.trim().isEmpty()) {
            this.selectedEvent.setWaiverReason(motivazioneDeroga);
            this.selectedEvent.setPenalty(false);
        } else {
            this.selectedEvent.setWaiverReason(null);
            this.selectedEvent.setPenalty(penale);
        }

        notifyEventModified(this.selectedEvent);
    }

    // metodo per modificare i dati dell'evento, con i controlli richiesti
    public void modifyEventData(String clientData, Date startDate, Date endDate, String location, int pax, String notes, String deroga, boolean penale)
            throws UseCaseLogicException {
        modifyEventData(clientData, startDate, endDate, location, pax, notes, false, deroga, penale);
    }

    public void modifyEventData(String clientData, Date startDate, Date endDate, String location, int pax, String notes,
            boolean propagate, String deroga, boolean penale)
            throws UseCaseLogicException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();

        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException("Utente non autorizzato: devi essere un Organizzatore.");
        }

        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento in gestione.");
        }

        // 1. Controllo Eccezione 2d.1b: L'evento non può essere "In Corso"
        if ("In Corso".equals(this.selectedEvent.getStatus())) {
            throw new UseCaseLogicException("Impossibile modificare i dati di un evento già in corso.");
        }

        // 2. Controllo Eccezione 2d.1a: Variazione partecipanti oltre il 30%
        if (!this.selectedEvent.canModifyParticipants(pax)) {
            this.inserisciDerogaPenale(deroga, penale);
        }

        // Applichiamo le modifiche ai dati
        this.selectedEvent.setClientData(clientData);
        this.selectedEvent.setDateStart(startDate);
        this.selectedEvent.setDateEnd(endDate);
        this.selectedEvent.setLocation(location);
        this.selectedEvent.setNumParticipants(pax);

        if (notes != null) {
            this.selectedEvent.setNotes(notes);
        }

        // Notifichiamo il database del cambiamento (ora salverà anche la penale a 1 se
        // è scattata)
        notifyEventModified(this.selectedEvent);

        // Propagazione della modifica alle altre istanze preliminari della ricorrenza
        // (ignorando le date)
        if (propagate && this.selectedEvent.getRecurrenceObj() != null) {
            for (Event ei : this.selectedEvent.getRecurrenceObj().getGeneratedEvents()) {
                if ("Preliminare".equals(ei.getStatus()) && ei.getId() != this.selectedEvent.getId()) {
                    ei.setClientData(clientData);
                    ei.setLocation(location);
                    ei.setNumParticipants(pax);
                    if (notes != null) {
                        ei.setNotes(notes);
                    }
                    notifyEventModified(ei);
                }
            }
        }
    }

    public void cancelEvent(String motivazioneDeroga, boolean penale) throws UseCaseLogicException {
        cancelEvent(motivazioneDeroga, penale, false);
    }

    public void cancelEvent(String motivazioneDeroga, boolean penale, boolean propagate) throws UseCaseLogicException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();

        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException("Utente non autorizzato: devi essere un Organizzatore.");
        }

        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento in gestione.");
        }

        // Estensione 7a: Se l'evento è in corso, l'organizzatore sceglie tra Penale o
        // Deroga
        if ("In Corso".equals(this.selectedEvent.getStatus())) {
            if (motivazioneDeroga != null && !motivazioneDeroga.trim().isEmpty()) {
                this.inserisciDerogaPenale(motivazioneDeroga, false);
            } else {
                this.inserisciDerogaPenale(null, penale);
            }
        } else {
            // Se l'evento è Preliminare, si annulla semplicemente (nessuna penale/deroga
            // possibile)
            this.inserisciDerogaPenale(null, false);
        }

        // Cambia lo stato in Annullato
        this.selectedEvent.setStatus("Annullato");

        // Libera il personale assegnato rimuovendo gli assegnamenti dai servizi
        if (this.selectedEvent.getServices() != null) {
            for (Service s : this.selectedEvent.getServices()) {
                if (s.getAssignments() != null) {
                    s.getAssignments().clear();
                }
            }
        }

        notifyEventCancelled(this.selectedEvent);

        // Propagazione dell'annullamento alle altre istanze preliminari della
        // ricorrenza
        if (propagate && this.selectedEvent.getRecurrenceObj() != null) {
            for (Event ei : this.selectedEvent.getRecurrenceObj().getGeneratedEvents()) {
                if ("Preliminare".equals(ei.getStatus()) && ei.getId() != this.selectedEvent.getId()) {
                    ei.setStatus("Annullato");
                    ei.setPenalty(false);
                    ei.setWaiverReason(null);
                    if (ei.getServices() != null) {
                        for (Service s : ei.getServices()) {
                            if (s.getAssignments() != null) {
                                s.getAssignments().clear();
                            }
                        }
                    }
                    notifyEventCancelled(ei);
                }
            }
        }
    }

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // Aggiunta metodi per terminateEvent

    /**
     * Termina l'evento attualmente in gestione.
     * Come da DCD: terminateEvent(historicalNotes: String, documentation: String)
     * 
     * Pre-condizioni:
     * - L'utente deve essere un Organizzatore
     * - Deve esserci un evento selezionato
     * - L'evento deve essere in stato "Confermato" o "In Corso"
     * 
     * Post-condizioni:
     * - e.notes = historicalNotes (il modello ha un singolo attributo notes)
     * - Eventuale documentazione allegata
     * - e.status = "Chiuso"
     */
    public void terminateEvent(String historicalNotes, String documentation) throws UseCaseLogicException {
        // 1. Controllo permessi utente
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException(
                    "Utente non autorizzato: devi essere un Organizzatore per terminare un evento.");
        }

        // 2. Controllo che ci sia un evento selezionato
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento attualmente in gestione da terminare.");
        }

        // 3. REGOLA DI BUSINESS: La terminazione è permessa solo se l'evento è
        // Confermato o In Corso
        String status = this.selectedEvent.getStatus();
        if (!"Confermato".equals(status) && !"In Corso".equals(status)) {
            throw new UseCaseLogicException(
                    "Impossibile terminare l'evento: la terminazione è consentita solo per eventi in stato Confermato o In Corso.");
        }

        // 4. Allega la documentazione, se fornita (come da DCD: attachDocumentation)
        if (documentation != null && !documentation.trim().isEmpty()) {
            Documentation doc = new Documentation(documentation);
            this.selectedEvent.attachDocumentation(doc);
        }

        // 5. Segna l'evento come chiuso con le note storiche (come da DCD:
        // markAsClosed)
        this.selectedEvent.markAsClosed(historicalNotes);

        // 6. Notifica i receiver per la persistenza
        notifyEventClosed(this.selectedEvent);
    }

    private void notifyEventClosed(Event event) {
        for (EventReceiver er : eventReceivers) {
            er.updateEventClosed(event);
        }
    }

    // ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    // Aggiunta metodi per modifica ricorrenza

    /**
     * Modifica la ricorrenza in sé (frequenza e conclusione) di un evento capofila.
     * Riposiziona le date delle istanze figlie in stato Preliminare senza
     * distruggerle.
     */
    public void modifyRecurrence(String newFrequency, Date newConclusion) throws UseCaseLogicException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException("Utente non autorizzato: devi essere un Organizzatore.");
        }

        if (this.selectedEvent == null || this.selectedEvent.getRecurrenceObj() == null) {
            throw new UseCaseLogicException("Nessun evento ricorrente in gestione (selezionare il capofila).");
        }

        Recurrence r = this.selectedEvent.getRecurrenceObj();
        r.setFrequency(newFrequency);
        r.setConclusion(newConclusion);

        java.util.Calendar calStart = java.util.Calendar.getInstance();
        calStart.setTime(this.selectedEvent.getDateStart());
        java.util.Calendar calEnd = java.util.Calendar.getInstance();
        calEnd.setTime(this.selectedEvent.getDateEnd());

        ArrayList<Event> generated = r.getGeneratedEvents();
        int instanceIndex = 0;

        // Avanziamo alla prima occorrenza
        advanceDate(calStart, newFrequency);
        advanceDate(calEnd, newFrequency);

        while (!calStart.getTime().after(newConclusion)) {
            if (instanceIndex < generated.size()) {
                // Abbiamo un'istanza esistente
                Event ei = generated.get(instanceIndex);
                if ("Preliminare".equals(ei.getStatus())) {
                    // RIUSO: aggiorniamo solo le date dell'istanza preliminare esistente
                    ei.setDates(new Date(calStart.getTimeInMillis()), new Date(calEnd.getTimeInMillis()));
                    notifyEventModified(ei);
                }
                // Se non è preliminare (es. Confermato, In Corso), la saltiamo (mantiene le sue
                // vecchie date)
                instanceIndex++;
            } else {
                // Dobbiamo creare nuove istanze (la ricorrenza si è allungata o è più
                // frequente)
                Event ei = new Event();
                ei.copyFrom(this.selectedEvent);
                ei.setDates(new Date(calStart.getTimeInMillis()), new Date(calEnd.getTimeInMillis()));
                ei.setRecurrenceObj(r);
                ei.setStatus("Preliminare");
                r.addGeneratedEvent(ei);
                notifyEventCreated(ei); // salva la nuova istanza nel DB
                instanceIndex++; // incrementiamo per le prossime
            }

            advanceDate(calStart, newFrequency);
            advanceDate(calEnd, newFrequency);
        }

        // Se sono avanzate istanze preliminari che ora cadono oltre la nuova
        // conclusione, le rimuoviamo.
        while (instanceIndex < generated.size()) {
            Event ei = generated.get(instanceIndex);
            if ("Preliminare".equals(ei.getStatus())) {
                generated.remove(instanceIndex);
                notifyEventDeleted(ei); // Rimuove dal DB
                // Non incrementiamo l'indice perché la lista scala a sinistra
            } else {
                // Eventi non preliminari non possono essere eliminati tacitamente
                instanceIndex++;
            }
        }

        notifyEventModified(this.selectedEvent);
    }

}
