//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//File creato nuovo per il test2
package catering.businesslogic.event;
import java.sql.Date;

public class Recurrence {
    private String frequency;
    private Date conclusion;

    public Recurrence(String frequency, Date conclusion) {
        this.frequency = frequency;
        this.conclusion = conclusion;
    }

    public String getFrequency() { return frequency; }
    public Date getConclusion() { return conclusion; }

    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //codice generato per correggere i warnings
    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public void setConclusion(Date conclusion) {
        this.conclusion = conclusion;
    }
}
