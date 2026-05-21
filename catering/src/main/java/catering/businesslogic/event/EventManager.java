package catering.businesslogic.event;

import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;

import catering.businesslogic.CatERing;
import catering.businesslogic.UseCaseLogicException;
import catering.businesslogic.menu.Menu;
import catering.businesslogic.user.User;

public class EventManager {

    private ArrayList<EventReceiver> eventReceivers;
    private Event selectedEvent;

    public EventManager() {
        eventReceivers = new ArrayList<>();
    }

    // -------------------------------------------------------------------------
    // Observer
    // -------------------------------------------------------------------------

    public void addEventReceiver(EventReceiver receiver) {
        if (receiver != null && !eventReceivers.contains(receiver)) {
            eventReceivers.add(receiver);
        }
    }

    public void removeEventReceiver(EventReceiver receiver) {
        eventReceivers.remove(receiver);
    }

    // -------------------------------------------------------------------------
    // Stato corrente
    // -------------------------------------------------------------------------

    public ArrayList<Event> getEvents() {
        return Event.loadAllEvents();
    }

    public Event getSelectedEvent() {
        return selectedEvent;
    }

    public void setSelectedEvent(Event event) {
        this.selectedEvent = event;
    }

    // -------------------------------------------------------------------------
    // DSD: creaSchedaEvento
    // -------------------------------------------------------------------------

    public Event createEventCard(String title) throws UseCaseLogicException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException(
                    "Utente non autorizzato: devi essere un Organizzatore per creare un evento.");
        }

        Event event = new Event();
        if (title != null && !title.trim().isEmpty()) {
            event.setName(title);
        }
        event.setStatus("Preliminare");

        notifyEventCreated(event);
        this.selectedEvent = event;

        return event;
    }

    // -------------------------------------------------------------------------
    // DSD: inserisciDati
    // -------------------------------------------------------------------------

    public void insertData(String clientData, Date startDate, Date endDate, String location, int pax,
            String notes, boolean recurrence, String frequency, Date conclusion) throws UseCaseLogicException {

        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException("Utente non autorizzato: devi essere un Organizzatore.");
        }
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento in gestione.");
        }
        if (!"Preliminare".equals(this.selectedEvent.getStatus())) {
            throw new UseCaseLogicException("Impossibile modificare l'evento: non è in stato Preliminare.");
        }

        this.selectedEvent.setClientData(clientData);
        this.selectedEvent.setDateStart(startDate);
        this.selectedEvent.setDateEnd(endDate);
        this.selectedEvent.setLocation(location);
        this.selectedEvent.setNumParticipants(pax);
        if (notes != null) {
            this.selectedEvent.setNotes(notes);
        }

        if (recurrence) {
            Recurrence r = new Recurrence(frequency, conclusion);
            this.selectedEvent.setRecurrenceObj(r);

            java.util.Calendar calStart = java.util.Calendar.getInstance();
            calStart.setTime(startDate);
            java.util.Calendar calEnd = java.util.Calendar.getInstance();
            calEnd.setTime(endDate);

            advanceDate(calStart, frequency);
            advanceDate(calEnd, frequency);

            while (!calStart.getTime().after(conclusion)) {
                Event ei = new Event();
                ei.copyFrom(this.selectedEvent);
                ei.setDates(new Date(calStart.getTimeInMillis()), new Date(calEnd.getTimeInMillis()));
                ei.setRecurrenceObj(r);
                ei.setStatus("Preliminare");
                r.addGeneratedEvent(ei);
                notifyEventCreated(ei);
                advanceDate(calStart, frequency);
                advanceDate(calEnd, frequency);
            }
        }

        notifyEventModified(this.selectedEvent);
    }

    private void advanceDate(java.util.Calendar cal, String frequency) {
        if ("Giornaliera".equalsIgnoreCase(frequency) || "Giornaliero".equalsIgnoreCase(frequency)) {
            cal.add(java.util.Calendar.DAY_OF_YEAR, 1);
        } else if ("Settimanale".equalsIgnoreCase(frequency)) {
            cal.add(java.util.Calendar.WEEK_OF_YEAR, 1);
        } else if ("Mensile".equalsIgnoreCase(frequency)) {
            cal.add(java.util.Calendar.MONTH, 1);
        } else {
            cal.add(java.util.Calendar.WEEK_OF_YEAR, 1);
        }
    }

    // -------------------------------------------------------------------------
    // DSD: defineService
    // -------------------------------------------------------------------------

    public Service defineService(Time timeStart, Time timeEnd, String type) throws UseCaseLogicException {
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento in gestione.");
        }

        Service service = new Service();
        service.setTimeStart(timeStart);
        service.setTimeEnd(timeEnd);
        service.setType(type);
        service.setEventId(this.selectedEvent.getId());

        this.selectedEvent.addService(service);
        notifyServiceCreated(service);

        return service;
    }

    // -------------------------------------------------------------------------
    // DSD: assignChef
    // -------------------------------------------------------------------------

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

    // -------------------------------------------------------------------------
    // DSD: assignStaff
    // -------------------------------------------------------------------------

    public StaffAssignment assignStaff(StaffMember member, String role, Service service) throws UseCaseLogicException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException("Utente non autorizzato: devi essere un Organizzatore.");
        }
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento in gestione.");
        }
        if (!member.isAvailable()) {
            throw new UseCaseLogicException("Il membro del personale non è disponibile per questo turno.");
        }

        StaffAssignment ap = new StaffAssignment(role, member, false);
        service.addAssignment(ap);

        Menu menu = service.getMenu();
        if (menu == null || !menu.isApproved()) {
            ap.setWaitingForMenu(true);
            notifyMenuMissing(service);
        } else {
            ap.setWaitingForMenu(false);
        }

        notifyStaffAssigned(service, ap);
        return ap;
    }

    // -------------------------------------------------------------------------
    // DSD: consultProposedMenu / approvaMenu / chefAcceptsMenu
    //
    // chefAcceptsMenu è qui (non in UserManager) perché opera su Event, Menu,
    // Service e StaffAssignment — oggetti del dominio eventi. Metterlo in
    // UserManager creerebbe una dipendenza inversa tra i moduli.
    // -------------------------------------------------------------------------

    public Menu consultProposedMenu(Service service) throws UseCaseLogicException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException("Utente non autorizzato: devi essere un Organizzatore.");
        }
        if (service == null) {
            throw new UseCaseLogicException("Servizio non valido.");
        }
        return service.getMenu();
    }

    public void approveMenu(Menu menu, String modifications) throws UseCaseLogicException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException("Utente non autorizzato: devi essere un Organizzatore.");
        }
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento in gestione.");
        }

        if (modifications == null || modifications.trim().isEmpty()) {
            menu.approve();
            this.selectedEvent.setStatus("In Corso");
            for (Service service : this.selectedEvent.getServices()) {
                if (service.getMenu() == menu) {
                    service.updateAssignmentsAfterMenuApproval();
                }
            }
            for (EventReceiver er : eventReceivers) {
                er.updateMenuApproved(this.selectedEvent, menu);
            }
        } else {
            Modification m = new Modification(modifications);
            m.linkTo(this.selectedEvent, menu);
            this.selectedEvent.addModification(m);
            for (Service service : this.selectedEvent.getServices()) {
                if (service.getMenu() == menu) {
                    for (StaffAssignment ap : service.getAssignments()) {
                        if (ap.isWaitingForMenu()) {
                            ap.setReviewAssignment(true);
                        }
                    }
                }
            }
            notifyModificationProposed(this.selectedEvent, menu);
        }
    }

    public void chefAcceptsMenu(Menu menu) throws UseCaseLogicException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isChef()) {
            throw new UseCaseLogicException("Utente non autorizzato: devi essere uno Chef per accettare il menu.");
        }
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento in gestione.");
        }

        menu.approve();
        this.selectedEvent.setStatus("In Corso");
        for (Service service : this.selectedEvent.getServices()) {
            if (service.getMenu() == menu) {
                service.updateAssignmentsAfterMenuApproval();
            }
        }
        for (EventReceiver er : eventReceivers) {
            er.updateMenuApproved(this.selectedEvent, menu);
        }
    }

    // -------------------------------------------------------------------------
    // DSD: confirmEvent
    // -------------------------------------------------------------------------

    public void confirmEvent() throws UseCaseLogicException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException("Utente non autorizzato: devi essere un Organizzatore.");
        }
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento in gestione.");
        }
        if ("Preliminare".equals(this.selectedEvent.getStatus())) {
            throw new UseCaseLogicException(
                    "Impossibile confermare: l'evento è ancora in stato Preliminare. Approva prima il menu.");
        }
        if (!this.selectedEvent.hasServices()) {
            throw new UseCaseLogicException("Impossibile confermare: l'evento non ha servizi definiti.");
        }
        if (!this.selectedEvent.hasChef()) {
            throw new UseCaseLogicException("Impossibile confermare: l'evento non ha uno chef assegnato.");
        }
        if (!this.selectedEvent.isValid()) {
            throw new UseCaseLogicException("Impossibile confermare: i dati dell'evento non sono validi.");
        }

        this.selectedEvent.setStatus("Confermato");
        notifyEventConfirmed(this.selectedEvent);
    }

    // -------------------------------------------------------------------------
    // modifyEventData + inserisciDerogaPenale
    // -------------------------------------------------------------------------

    public void addWaiverPenalty(String motivazioneDeroga, boolean penale) throws UseCaseLogicException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException("Solo l'organizzatore può inserire una deroga.");
        }
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento attualmente in gestione.");
        }

        if (motivazioneDeroga != null && !motivazioneDeroga.trim().isEmpty()) {
            this.selectedEvent.setWaiverReason(motivazioneDeroga);
            this.selectedEvent.setPenalty(false);
        } else {
            this.selectedEvent.setWaiverReason(null);
            this.selectedEvent.setPenalty(penale);
        }

        notifyEventModified(this.selectedEvent);
    }

    public void modifyEventData(String clientData, Date startDate, Date endDate, String location, int pax,
            String notes, String deroga, boolean penale) throws UseCaseLogicException {
        modifyEventData(clientData, startDate, endDate, location, pax, notes, false, deroga, penale);
    }

    public void modifyEventData(String clientData, Date startDate, Date endDate, String location, int pax,
            String notes, boolean propagate, String deroga, boolean penale) throws UseCaseLogicException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException("Utente non autorizzato: devi essere un Organizzatore.");
        }
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento in gestione.");
        }
        if ("In Corso".equals(this.selectedEvent.getStatus())) {
            throw new UseCaseLogicException("Impossibile modificare i dati di un evento già in corso.");
        }

        if (!this.selectedEvent.canModifyParticipants(pax)) {
            this.addWaiverPenalty(deroga, penale);
        }

        this.selectedEvent.setClientData(clientData);
        this.selectedEvent.setDateStart(startDate);
        this.selectedEvent.setDateEnd(endDate);
        this.selectedEvent.setLocation(location);
        this.selectedEvent.setNumParticipants(pax);
        if (notes != null) {
            this.selectedEvent.setNotes(notes);
        }

        notifyEventModified(this.selectedEvent);

        if (propagate && this.selectedEvent.getRecurrenceObj() != null) {
            for (Event ei : this.selectedEvent.getRecurrenceObj().getGeneratedEvents()) {
                if ("Preliminare".equals(ei.getStatus()) && ei.getId() != this.selectedEvent.getId()) {
                    ei.setClientData(clientData);
                    ei.setLocation(location);
                    ei.setNumParticipants(pax);
                    if (notes != null) ei.setNotes(notes);
                    notifyEventModified(ei);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // cancelEvent
    // -------------------------------------------------------------------------

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

        if ("In Corso".equals(this.selectedEvent.getStatus())) {
            if (motivazioneDeroga != null && !motivazioneDeroga.trim().isEmpty()) {
                this.addWaiverPenalty(motivazioneDeroga, false);
            } else {
                this.addWaiverPenalty(null, penale);
            }
        } else {
            this.addWaiverPenalty(null, false);
        }

        this.selectedEvent.setStatus("Annullato");
        for (Service s : this.selectedEvent.getServices()) {
            s.getAssignments().clear();
        }
        notifyEventCancelled(this.selectedEvent);

        if (propagate && this.selectedEvent.getRecurrenceObj() != null) {
            for (Event ei : this.selectedEvent.getRecurrenceObj().getGeneratedEvents()) {
                if ("Preliminare".equals(ei.getStatus()) && ei.getId() != this.selectedEvent.getId()) {
                    ei.setStatus("Annullato");
                    ei.setPenalty(false);
                    ei.setWaiverReason(null);
                    for (Service s : ei.getServices()) {
                        s.getAssignments().clear();
                    }
                    notifyEventCancelled(ei);
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // deleteEvent
    // -------------------------------------------------------------------------

    public void deleteEvent() throws UseCaseLogicException {
        deleteEvent(false);
    }

    public void deleteEvent(boolean propagate) throws UseCaseLogicException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException(
                    "Utente non autorizzato: devi essere un Organizzatore per eliminare un evento.");
        }
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento attualmente in gestione da eliminare.");
        }
        if (!"Preliminare".equals(this.selectedEvent.getStatus())) {
            throw new UseCaseLogicException(
                    "Impossibile eliminare l'evento: l'eliminazione è consentita solo in fase Preliminare. Usa la funzione di annullamento.");
        }

        Event eventToDelete = this.selectedEvent;
        this.selectedEvent = null;

        if (propagate && eventToDelete.getRecurrenceObj() != null) {
            ArrayList<Event> generated = eventToDelete.getRecurrenceObj().getGeneratedEvents();
            for (int i = generated.size() - 1; i >= 0; i--) {
                Event ei = generated.get(i);
                if ("Preliminare".equals(ei.getStatus()) && ei.getId() != eventToDelete.getId()) {
                    generated.remove(i);
                    notifyEventDeleted(ei);
                }
            }
        }

        notifyEventDeleted(eventToDelete);
    }

    // -------------------------------------------------------------------------
    // terminateEvent
    // -------------------------------------------------------------------------

    public void terminateEvent(String historicalNotes, String documentation) throws UseCaseLogicException {
        User user = CatERing.getInstance().getUserManager().getCurrentUser();
        if (user == null || !user.isOrganizer()) {
            throw new UseCaseLogicException(
                    "Utente non autorizzato: devi essere un Organizzatore per terminare un evento.");
        }
        if (this.selectedEvent == null) {
            throw new UseCaseLogicException("Nessun evento attualmente in gestione da terminare.");
        }
        String status = this.selectedEvent.getStatus();
        if (!"Confermato".equals(status) && !"In Corso".equals(status)) {
            throw new UseCaseLogicException(
                    "Impossibile terminare l'evento: la terminazione è consentita solo per eventi in stato Confermato o In Corso.");
        }

        if (documentation != null && !documentation.trim().isEmpty()) {
            this.selectedEvent.attachDocumentation(new Documentation(documentation));
        }
        this.selectedEvent.markAsClosed(historicalNotes);
        notifyEventClosed(this.selectedEvent);
    }

    // -------------------------------------------------------------------------
    // modifyRecurrence
    // -------------------------------------------------------------------------

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

        advanceDate(calStart, newFrequency);
        advanceDate(calEnd, newFrequency);

        while (!calStart.getTime().after(newConclusion)) {
            if (instanceIndex < generated.size()) {
                Event ei = generated.get(instanceIndex);
                if ("Preliminare".equals(ei.getStatus())) {
                    ei.setDates(new Date(calStart.getTimeInMillis()), new Date(calEnd.getTimeInMillis()));
                    notifyEventModified(ei);
                }
                instanceIndex++;
            } else {
                Event ei = new Event();
                ei.copyFrom(this.selectedEvent);
                ei.setDates(new Date(calStart.getTimeInMillis()), new Date(calEnd.getTimeInMillis()));
                ei.setRecurrenceObj(r);
                ei.setStatus("Preliminare");
                r.addGeneratedEvent(ei);
                notifyEventCreated(ei);
                instanceIndex++;
            }
            advanceDate(calStart, newFrequency);
            advanceDate(calEnd, newFrequency);
        }

        while (instanceIndex < generated.size()) {
            Event ei = generated.get(instanceIndex);
            if ("Preliminare".equals(ei.getStatus())) {
                generated.remove(instanceIndex);
                notifyEventDeleted(ei);
            } else {
                instanceIndex++;
            }
        }

        notifyEventModified(this.selectedEvent);
    }

    // -------------------------------------------------------------------------
    // Notifiche private
    // -------------------------------------------------------------------------

    private void notifyEventCreated(Event event) {
        for (EventReceiver r : eventReceivers) r.updateEventCreated(event);
    }

    private void notifyEventModified(Event event) {
        for (EventReceiver r : eventReceivers) r.updateEventModified(event);
    }

    private void notifyEventDeleted(Event event) {
        for (EventReceiver r : eventReceivers) r.updateEventDeleted(event);
    }

    private void notifyEventConfirmed(Event event) {
        for (EventReceiver r : eventReceivers) r.updateEventConfirmed(event);
    }

    private void notifyEventCancelled(Event event) {
        for (EventReceiver r : eventReceivers) r.updateEventCancelled(event);
    }

    private void notifyEventClosed(Event event) {
        for (EventReceiver r : eventReceivers) r.updateEventClosed(event);
    }

    private void notifyServiceCreated(Service service) {
        for (EventReceiver r : eventReceivers) r.updateServiceCreated(selectedEvent, service);
    }

    private void notifyMenuMissing(Service service) {
        for (EventReceiver r : eventReceivers) r.updateMenuMissing(service);
    }

    private void notifyStaffAssigned(Service service, StaffAssignment ap) {
        for (EventReceiver r : eventReceivers) r.updateStaffAssigned(service, ap);
    }

    private void notifyModificationProposed(Event e, Menu m) {
        for (EventReceiver r : eventReceivers) r.updateModificationProposed(e, m);
    }
}
