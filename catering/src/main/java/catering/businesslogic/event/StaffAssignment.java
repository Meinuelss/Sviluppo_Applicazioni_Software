//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//File creato nuovo per il test3
package catering.businesslogic.event;

public class StaffAssignment {
    private String role;
    private boolean waitingForMenu;
    private boolean reviewAssignment;
    private StaffMember member;

    public StaffAssignment(String role, StaffMember member, boolean waitingForMenu) {
        this.role = role;
        this.member = member;
        this.waitingForMenu = waitingForMenu;
        this.reviewAssignment = false;
    }

    public String getRole() { return role; }
    public boolean isWaitingForMenu() { return waitingForMenu; }
    public StaffMember getMember() { return member; }

    public void setRole(String role) { 
        this.role = role; 
    }
    
    public void setMember(StaffMember member) { 
        this.member = member; 
    }
    
    public void setWaitingForMenu(boolean waitingForMenu) { 
        this.waitingForMenu = waitingForMenu; 
    }
    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //Codice generato per eliminare warnings

    public boolean needsReview() {
        return reviewAssignment;
    }

    public void setReviewAssignment(boolean reviewAssignment) {
        this.reviewAssignment = reviewAssignment;
    }
}