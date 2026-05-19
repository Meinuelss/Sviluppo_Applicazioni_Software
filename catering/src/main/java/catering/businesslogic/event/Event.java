package catering.businesslogic.event;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import catering.businesslogic.user.User;
import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

/**
 * Represents an event in the catering system.
 */
public class Event {

    static int counter = 1;
    private int id = counter;
    private String name;
    private Date dateStart;
    private Date dateEnd;
    private User chef;
    private ArrayList<Service> services;

    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //attributi per il test1
    private String status;

    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //attributi per il test2
    private String clientData;
    private String location;
    private int numParticipants;
    private String notes;
    private Recurrence recurrenceObj;
    private boolean typeEvent; //true = complesso, false = semplice

    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //attributi per il test3
    private ArrayList<Modification> modifications = new ArrayList<>();
    private int chef_id = 0;

    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //attributi per il test4
    private boolean penalty;
    private String waiverReason;

    public Event() {
        services = new ArrayList<>();
        this.id = counter;
        counter++;
    }

    public Event(String name) {
        this();
        this.name = name;
        this.id = counter;
        counter++;
    }

    // Basic getters and setters
    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDateStart() {
        return dateStart;
    }

    public void setDateStart(Date dateStart) {
        this.dateStart = dateStart;
    }

    public Date getDateEnd() {
        return dateEnd;
    }

    public void setDateEnd(Date dateEnd) {
        this.dateEnd = dateEnd;
    }

    public User getChef() {
        return chef;
    }

    public int getChefId() {
        return chef != null ? chef.getId() : 0;
    }

    public void setChef(User chef) {
        this.chef = chef;
    }

    public void setChefId(int chefId) {
        this.chef = User.load(chefId);
    }

    public void setTypeEvent(boolean typeEvent) {
        this.typeEvent = typeEvent;
    }

    public boolean getTypeEvent() {
        return typeEvent;
    }

    public ArrayList<Service> getServices() {
        return services;
    }

    public void setServices(ArrayList<Service> services) {
        this.services = services;
    }

    // Service management
    public void addService(Service service) {
        if (services == null) {
            services = new ArrayList<>();
        }
        services.add(service);
    }

    public void removeService(Service service) {
        if (services != null) {
            services.remove(service);
        }
    }

    public boolean containsService(Service service) {
        if (services != null) {
            return services.contains(service);
        }
        return false;
    }

    // Database operations
    public void saveNewEvent() {

        String query = "INSERT INTO Events (name, date_start, date_end, chef_id, type_event, status, client_data, location, num_participants, notes, penalty, waiver_reason) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        Long startTimestamp = (dateStart != null) ? dateStart.getTime() : null;
        Long endTimestamp = (dateEnd != null) ? dateEnd.getTime() : null;

        PersistenceManager.executeUpdate(query, name, startTimestamp, endTimestamp, getChefId(), getTypeEvent(),
                                         status, clientData, location, numParticipants, notes, penalty, waiverReason);

        // Get the ID of the newly inserted event
        id = PersistenceManager.getLastId();

    }

    public void updateEvent() {
        String query = "UPDATE Events SET name = ?, date_start = ?, date_end = ?, chef_id = ?, type_event = ?, status = ?, client_data = ?, location = ?, num_participants = ?, notes = ?, penalty = ?, waiver_reason = ? WHERE id = ?";
        Long startTimestamp = (dateStart != null) ? dateStart.getTime() : null;
        Long endTimestamp = (dateEnd != null) ? dateEnd.getTime() : null;

        PersistenceManager.executeUpdate(query, name, startTimestamp, endTimestamp, getChefId(), getTypeEvent(), status, clientData, location, numParticipants, notes, penalty, waiverReason, id);

    }

    public boolean deleteEvent() {
        // Delete all services first
        for (Service service : services) {
            service.deleteService();
        }
        services.clear();

        // Delete the event
        String query = "DELETE FROM Events WHERE id = ?";
        boolean success = PersistenceManager.executeUpdate(query, id) > 0;

        if (success) {
        }

        return success;
    }

    // Static load methods
    @SuppressWarnings("Convert2Lambda")
    public static ArrayList<Event> loadAllEvents() {
        ArrayList<Event> events = new ArrayList<>();
        String query = "SELECT * FROM Events ORDER BY date_start DESC";

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                Event e = new Event();
                e.id = rs.getInt("id");
                e.name = rs.getString("name");
                e.dateStart = Date.valueOf(rs.getString("date_start"));
                e.dateEnd = Date.valueOf(rs.getString("date_end"));
                e.chef = User.load(rs.getInt("chef_id"));
                e.status = rs.getString("status");
                e.clientData = rs.getString("client_data");
                e.location = rs.getString("location");
                e.numParticipants = rs.getInt("num_participants");
                e.notes = rs.getString("notes");
                e.penalty = rs.getBoolean("penalty");
                e.waiverReason = rs.getString("waiver_reason");
                events.add(e);
            }
        });

        // Load services for each event
        for (Event e : events) {
            e.services = Service.loadServicesForEvent(e.id);
        }

        return events;
    }

    public static Event loadById(int id) {
        String query = "SELECT * FROM Events WHERE id = ?";
        return loadEventByQuery(query, id);
    }

    public static Event loadByName(String name) {
        String query = "SELECT * FROM Events WHERE name = ?";
        return loadEventByQuery(query, name);
    }

    @SuppressWarnings("Convert2Lambda")
    private static Event loadEventByQuery(String query, Object param) {
        final Event[] eventHolder = new Event[1];
        final boolean[] eventFound = new boolean[1];

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                eventFound[0] = true;

                Event e = new Event();

                e.id = rs.getInt("id");
                e.name = rs.getString("name");
                e.dateStart = Date.valueOf(rs.getString("date_start"));
                e.dateEnd = Date.valueOf(rs.getString("date_end"));
                e.status = rs.getString("status");
                e.clientData = rs.getString("client_data");
                e.location = rs.getString("location");
                e.numParticipants = rs.getInt("num_participants");
                e.notes = rs.getString("notes");
                e.penalty = rs.getBoolean("penalty");
                e.waiverReason = rs.getString("waiver_reason");
                

                try {
                    e.chef = User.load(rs.getInt("chef_id"));
                } catch (SQLException ex) {
                    e.chef = null;
                }

                eventHolder[0] = e;
            }
        }, param);

        if (!eventFound[0]) {
            return null;
        }

        Event result = eventHolder[0];
        if (result != null) {
            try {
                result.services = Service.loadServicesForEvent(result.id);
            } catch (Exception ex) {
                result.services = new ArrayList<>();
            }
        }

        return result;
    }

    @Override
    public String toString() {
        return "Event [id=" + id + ", name=" + name + ", dateStart=" + dateStart +
                ", services=" + (services != null ? services.size() : 0) + "]";
    }

    //^^^^^^^^^^^^^
    //aggiunta metodi per test1

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    //^^^^^^^^^^^^^
    //aggiunta metodi per test2

    public String getClientData() { return clientData; }
    public void setClientData(String clientData) { this.clientData = clientData; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getNumParticipants() { return numParticipants; }
    public void setNumParticipants(int numParticipants) { this.numParticipants = numParticipants; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public void setRecurrenceObj(Recurrence r) { this.recurrenceObj = r;}
    public Recurrence getRecurrenceObj() { return this.recurrenceObj;}
    public void copyFrom(Event e) {
        this.name = e.name;
        this.clientData = e.clientData;
        this.location = e.location;
        this.numParticipants = e.numParticipants;
        this.notes = e.notes;
    }
    public void setDates(Date startDate, Date endDate) {
        this.dateStart = startDate;
        this.dateEnd = endDate;
    }

     //^^^^^^^^^^^^^
    //aggiunta metodi per test3
    public void addModification(Modification mod) {
        this.modifications.add(mod);
    }
    public ArrayList<Modification> getModifications() {
        return this.modifications;
    }
    public boolean hasServices() {
        return this.services != null && !this.services.isEmpty();
    }
    public boolean hasChef() {
        // Usa chef_id se mappato con l'intero, oppure this.chef != null se usi l'oggetto
        return this.chef_id > 0 || this.chef != null; 
    }
    public boolean isValid() {
        // Un evento è valido se ha sia servizi che uno chef
        return this.hasServices() && this.hasChef();
    }

    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //Codice generato per risolvere i warnings
    public void setModifications(ArrayList<Modification> modifications) {
        this.modifications = modifications;
    }

    public int getChef_id() {
        return chef_id;
    }

    public void setChef_id(int chef_id) {
        this.chef_id = chef_id;
    }

    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //Aggiunta metodi per test4

    public boolean hasPenalty() {
        return penalty;
    }

    public void setPenalty(boolean penalty) {
        this.penalty = penalty;
    }

    public String getWaiverReason() {
        return waiverReason;
    }

    public void setWaiverReason(String waiverReason) {
        this.waiverReason = waiverReason;
    }

    //metodo oer verificare se la modifica al numero di partecipanti è valida (entro il 30% del numero attuale)
    public boolean canModifyParticipants(int newNum) {
        // Se l'evento non aveva ancora partecipanti (es. appena creato), la modifica è sempre valida
        if (this.numParticipants == 0) {
            return true;
        }

        // Calcoliamo la differenza assoluta tra il vecchio e il nuovo numero
        double variazioneAssoluta = Math.abs(newNum - this.numParticipants);
        
        // Calcoliamo la soglia massima consentita (30% del numero attuale)
        double sogliaMassima = this.numParticipants * 0.30;

        // Ritorna true se la variazione è entro la soglia
        return variazioneAssoluta <= sogliaMassima;
    }
}