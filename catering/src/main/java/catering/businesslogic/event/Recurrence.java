//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//File creato nuovo per il test2
package catering.businesslogic.event;
import java.sql.Date;
import java.util.ArrayList;

public class Recurrence {
    private int idRecurrence;
    private String frequency;
    private Date conclusion;
    private ArrayList<Event> generatedEvents;

    public Recurrence(String frequency, Date conclusion) {
        this.frequency = frequency;
        this.conclusion = conclusion;
        this.generatedEvents = new ArrayList<>();
    }

    public int getIdRecurrence() { return idRecurrence; }
    public void setIdRecurrence(int id) { this.idRecurrence = id; }

    public String getFrequency() { return frequency; }
    public Date getConclusion() { return conclusion; }

    public void addGeneratedEvent(Event e) {
        if (!this.generatedEvents.contains(e)) {
            this.generatedEvents.add(e);
        }
    }

    public ArrayList<Event> getGeneratedEvents() {
        return this.generatedEvents;
    }

    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //codice generato per correggere i warnings
    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public void setConclusion(Date conclusion) {
        this.conclusion = conclusion;
    }

    // Database operations
    public void saveNewRecurrence() {
        String query = "INSERT INTO Recurrences (frequency, conclusion) VALUES (?, ?)";
        catering.persistence.PersistenceManager.executeUpdate(query, frequency, conclusion);
        this.idRecurrence = catering.persistence.PersistenceManager.getLastId();
    }

    public void updateRecurrence() {
        String query = "UPDATE Recurrences SET frequency = ?, conclusion = ? WHERE id = ?";
        catering.persistence.PersistenceManager.executeUpdate(query, frequency, conclusion, idRecurrence);
    }
}
