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

    // attributi aggiunti
    private String status;
    private String clientData;
    private String location;
    private int numParticipants;
    private String notes;
    private Recurrence recurrenceObj;
    private boolean typeEvent; // true = complesso, false = semplice
    private ArrayList<Modification> modifications = new ArrayList<>();
    private int chef_id = 0;
    private boolean penalty;
    private String waiverReason;
    private ArrayList<Documentation> documentations = new ArrayList<>();

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
        if (chef != null && !chef.isChef()) {
            throw new IllegalArgumentException("L'utente assegnato non ha il ruolo di Chef");
        }
        this.chef = chef;
        this.chef_id = chef != null ? chef.getId() : 0;
    }

    public void setChefId(int chefId) {
        User user = User.load(chefId);
        if (user != null && !user.isChef()) {
            throw new IllegalArgumentException("L'utente assegnato non ha il ruolo di Chef");
        }
        this.chef = user;
        this.chef_id = chefId;
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
        if (this.recurrenceObj != null && this.recurrenceObj.getIdRecurrence() == 0) {
            this.recurrenceObj.saveNewRecurrence();
        }
        int recId = (this.recurrenceObj != null) ? this.recurrenceObj.getIdRecurrence() : 0;

        String query = "INSERT INTO Events (name, date_start, date_end, chef_id, type_event, status, client_data, location, num_participants, notes, penalty, waiver_reason, recurrence_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        PersistenceManager.executeUpdate(query, name, dateStart, dateEnd,
                getChefId(), getTypeEvent(), status, clientData, location, numParticipants,
                notes, penalty, waiverReason, recId);

        id = PersistenceManager.getLastId();

    }

    public void updateEvent() {
        if (this.recurrenceObj != null) {
            if (this.recurrenceObj.getIdRecurrence() == 0) {
                this.recurrenceObj.saveNewRecurrence();
            } else {
                this.recurrenceObj.updateRecurrence();
            }
        }
        int recId = (this.recurrenceObj != null) ? this.recurrenceObj.getIdRecurrence() : 0;

        String query = "UPDATE Events SET name = ?, date_start = ?, date_end = ?, chef_id = ?, type_event = ?, status = ?, client_data = ?, location = ?, num_participants = ?, notes = ?, penalty = ?, waiver_reason = ?, recurrence_id = ? WHERE id = ?";

        PersistenceManager.executeUpdate(query, name, dateStart, dateEnd,
                getChefId(), getTypeEvent(), status, clientData, location, numParticipants,
                notes, penalty, waiverReason, recId, id);
    }

    public boolean deleteEvent() {
        for (Service service : services) {
            service.deleteService();
        }
        services.clear();

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


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getClientData() {
        return clientData;
    }

    public void setClientData(String clientData) {
        this.clientData = clientData;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getNumParticipants() {
        return numParticipants;
    }

    public void setNumParticipants(int numParticipants) {
        this.numParticipants = numParticipants;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setRecurrenceObj(Recurrence r) {
        this.recurrenceObj = r;
    }

    public Recurrence getRecurrenceObj() {
        return this.recurrenceObj;
    }

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
        return this.chef_id > 0 || this.chef != null;
    }

    // Un evento è valido se ha sia servizi che uno chef
    public boolean isValid() {
        return this.hasServices() && this.hasChef();
    }

    public void setModifications(ArrayList<Modification> modifications) {
        this.modifications = modifications;
    }

    public int getChef_id() {
        return chef_id;
    }

    public void setChef_id(int chef_id) {
        User user = User.load(chef_id);
        if (user != null && !user.isChef()) {
            throw new IllegalArgumentException("L'utente assegnato non ha il ruolo di Chef");
        }
        this.chef_id = chef_id;
        this.chef = user;
    }

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

    // metodo oer verificare se la modifica al numero di partecipanti è valida
    // (entro il 30% del numero attuale)
    public boolean canModifyParticipants(int newNum) {
        if (this.numParticipants == 0) {
            return true;
        }

        double variazioneAssoluta = Math.abs(newNum - this.numParticipants);
        double sogliaMassima = this.numParticipants * 0.30;

        return variazioneAssoluta <= sogliaMassima;
    }

    /**
     * Segna l'evento come chiuso, salvando le note storiche.
     */
    public void markAsClosed(String historicalNotes) {
        this.notes = historicalNotes;
        this.status = "Chiuso";
    }

    /**
     * Allega una documentazione all'evento.
     */
    public void attachDocumentation(Documentation doc) {
        if (this.documentations == null) {
            this.documentations = new ArrayList<>();
        }
        this.documentations.add(doc);
    }

    public ArrayList<Documentation> getDocumentations() {
        return this.documentations;
    }

    public void setDocumentations(ArrayList<Documentation> documentations) {
        this.documentations = documentations;
    }
}