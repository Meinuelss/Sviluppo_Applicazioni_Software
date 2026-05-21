package catering.businesslogic.event;

import catering.businesslogic.menu.Menu;

/**
 * Interface for receiving event-related notifications.
 * Implemented by classes that need to respond to event changes.
 */
public interface EventReceiver {

    void updateEventCreated(Event event);

    void updateEventModified(Event event);

    void updateEventDeleted(Event event);

    void updateEventCancelled(Event event);

    void updateServiceCreated(Event event, Service service);

    void updateServiceModified(Service service);

    void updateServiceDeleted(Service service);

    void updateMenuAssigned(Service service, Menu menu);

    void updateMenuRemoved(Service service);

    //metodi update aggiunti da noi
    void updateMenuApproved(Event e, catering.businesslogic.menu.Menu m);
    void updateMenuMissing(Service service);
    void updateStaffAssigned(Service service, StaffAssignment ap);
    void updateEventConfirmed(Event e);
    void updateModificationProposed(Event e, catering.businesslogic.menu.Menu m);
    void updateEventClosed(Event e);
}
