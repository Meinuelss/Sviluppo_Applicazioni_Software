//Classe che rappresenta un membro dello staff, con nome e disponibilità
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

    public void setName(String name) {
        this.name = name;
    }
}
