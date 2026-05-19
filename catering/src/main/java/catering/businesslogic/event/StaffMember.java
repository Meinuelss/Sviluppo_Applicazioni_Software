//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//File creato nuovo per il test3
package catering.businesslogic.event;

public class StaffMember {
    private String name;
    private boolean available;

    public StaffMember(String name) {
        this.name = name;
        this.available = true;
    }

    public String getName() { return name; }
    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }
    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //Codice generato per correggere i warnings
    public void setName(String name) {
        this.name = name;
    }
}
