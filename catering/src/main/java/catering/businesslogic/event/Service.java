package catering.businesslogic.event;

import java.sql.*;
import java.util.*;
import java.sql.Date;

import catering.businesslogic.menu.Menu;
import catering.businesslogic.menu.MenuItem;
import catering.persistence.PersistenceManager;
import catering.persistence.ResultHandler;

/**
 * Represents a service in an event in the catering system.
 */
public class Service {

    private int id;
    private String name;
    private Date date;
    private Time timeStart;
    private Time timeEnd;
    private String location;
    private int eventId;
    private Menu menu;

    private ArrayList<StaffAssignment> assignments = new ArrayList<StaffAssignment>();
    private String type;

    public Service() {
    }

    public Service(String name) {
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public Time getTimeStart() {
        return timeStart;
    }

    public void setTimeStart(Time timeStart) {
        this.timeStart = timeStart;
    }

    public Time getTimeEnd() {
        return timeEnd;
    }

    public void setTimeEnd(Time timeEnd) {
        this.timeEnd = timeEnd;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getMenuId() {
        return (menu != null) ? menu.getId() : 0;
    }

    public Menu getMenu() {
        return menu;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
    }

    public void proposeMenu(Menu m) {
        this.menu = m;
        if (this.id > 0 && m != null) {
            PersistenceManager.executeUpdate(
                "UPDATE Services SET approved_menu_id = ? WHERE id = ?",
                m.getId(), this.id);
        }
    }

    public void updateAssignmentsAfterMenuApproval() {
        for (StaffAssignment ap : this.assignments) {
            if (ap.isWaitingForMenu()) {
                ap.setWaitingForMenu(false);
            }
            if (ap.needsReview()) {
                ap.setReviewAssignment(false);
            }
        }
    }

    public void removeMenu() {
        this.menu = null;
    }

    public ArrayList<MenuItem> getMenuItems() {
        if (this.menu == null) {
            return new ArrayList<>();
        }
        return this.menu.getItems();
    }

    public void saveNewService() {
        String query = "INSERT INTO Services (event_id, name, service_date, time_start, time_end, location) VALUES (?, ?, ?, ?, ?, ?)";

        Long dateTimestamp = (this.getDate() != null) ? this.getDate().getTime() : null;

        PersistenceManager.executeUpdate(query,
                this.getEventId(),
                this.getName(),
                dateTimestamp,
                this.getTimeStart(),
                this.getTimeEnd(),
                this.getLocation());

        this.setId(PersistenceManager.getLastId());
    }

    public void updateService() {
        String query = "UPDATE Services SET name = ?, service_date = ?, time_start = ?, time_end = ?, location = ? WHERE id = ?";

        Long dateTimestamp = (this.getDate() != null) ? this.getDate().getTime() : null;

        PersistenceManager.executeUpdate(query,
                this.getName(),
                dateTimestamp,
                this.getTimeStart(),
                this.getTimeEnd(),
                this.getLocation(),
                this.getId());
    }

    public boolean deleteService() {
        String query = "DELETE FROM Services WHERE id = ?";
        return PersistenceManager.executeUpdate(query, this.getId()) > 0;
    }

    public void assignMenuToService(Menu menu) {
        this.setMenu(menu);

        String query = "UPDATE Services SET approved_menu_id = ? WHERE id = ?";
        PersistenceManager.executeUpdate(query, menu.getId(), this.getId());
    }

    public void removeMenuFromService() {
        this.removeMenu();

        String query = "UPDATE Services SET approved_menu_id = 0 WHERE id = ?";
        PersistenceManager.executeUpdate(query, this.getId());
    }

    @SuppressWarnings("Convert2Lambda")
    public static ArrayList<Service> loadServicesForEvent(int eventId) {
        ArrayList<Service> services = new ArrayList<>();
        String query = "SELECT * FROM Services WHERE event_id = ? ORDER BY service_date, time_start";

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                Service s = new Service();
                s.id = rs.getInt("id");
                s.name = rs.getString("name");

                try {
                    s.date = Date.valueOf(rs.getString("service_date"));
                    s.timeStart = Time.valueOf(rs.getString("time_start"));
                    s.timeEnd = Time.valueOf(rs.getString("time_end"));
                } catch (IllegalArgumentException ex) {
                }

                s.location = rs.getString("location");
                s.eventId = rs.getInt("event_id");

                int menuId = rs.getInt("approved_menu_id");
                if (menuId > 0)
                    s.menu = Menu.load(menuId);

                services.add(s);
            }
        }, eventId);

        return services;
    }

    public static Service loadById(int id) {
        String query = "SELECT * FROM Services WHERE id = ?";
        return loadServiceByQuery(query, id);
    }

    public static Service loadByName(String name) {
        String query = "SELECT * FROM Services WHERE name = ?";
        return loadServiceByQuery(query, name);
    }

    @SuppressWarnings("Convert2Lambda")
    private static Service loadServiceByQuery(String query, Object param) {
        final Service[] serviceHolder = new Service[1];
        final boolean[] serviceFound = new boolean[1];
        serviceFound[0] = false;

        PersistenceManager.executeQuery(query, new ResultHandler() {
            @Override
            public void handle(ResultSet rs) throws SQLException {
                serviceFound[0] = true;

                Service s = new Service();
                s.id = rs.getInt("id");
                s.name = rs.getString("name");

                try {
                    String dateStr = rs.getString("service_date");
                    String startTimeStr = rs.getString("time_start");
                    String endTimeStr = rs.getString("time_end");

                    if (dateStr != null && !dateStr.isEmpty()) {
                        s.date = Date.valueOf(dateStr);
                    }
                    if (startTimeStr != null && !startTimeStr.isEmpty()) {
                        s.timeStart = Time.valueOf(startTimeStr);
                    }
                    if (endTimeStr != null && !endTimeStr.isEmpty()) {
                        s.timeEnd = Time.valueOf(endTimeStr);
                    }
                } catch (IllegalArgumentException ex) {
                }

                s.location = rs.getString("location");
                s.eventId = rs.getInt("event_id");

                int menuId = rs.getInt("approved_menu_id");
                if (menuId > 0) {
                    try {
                        s.menu = Menu.load(menuId);
                    } catch (Exception e) {
                    }
                }

                serviceHolder[0] = s;
            }
        }, param);

        return serviceFound[0] ? serviceHolder[0] : null;
    }

    @Override
    public String toString() {
        return "Service [id=" + id + ", name=" + name + ", date=" + date + ", location=" + location +
                ", menu=" + (menu != null ? menu.getTitle() : "none") + "]";
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;

        Service other = (Service) obj;

        if (this.id > 0 && other.id > 0) {
            return this.id == other.id;
        }

        boolean nameMatch = (this.name == null && other.name == null) ||
                (this.name != null && this.name.equals(other.name));

        if (!nameMatch)
            return false;

        boolean dateMatch = (this.date == null && other.date == null) ||
                (this.date != null && this.date.equals(other.date));

        if (!dateMatch)
            return false;

        boolean timeStartMatch = (this.timeStart == null && other.timeStart == null) ||
                (this.timeStart != null && this.timeStart.equals(other.timeStart));

        if (!timeStartMatch)
            return false;

        boolean timeEndMatch = (this.timeEnd == null && other.timeEnd == null) ||
                (this.timeEnd != null && this.timeEnd.equals(other.timeEnd));

        if (!timeEndMatch)
            return false;

        boolean locationMatch = (this.location == null && other.location == null) ||
                (this.location != null && this.location.equals(other.location));

        if (!locationMatch)
            return false;

        boolean menuMatch = (this.menu == null && other.menu == null) ||
                (this.menu != null && this.menu.equals(other.menu));

        if (!menuMatch)
            return false;

        if (this.eventId > 0 && other.eventId > 0) {
            return this.eventId == other.eventId;
        }

        return true;
    }


    public ArrayList<StaffAssignment> getAssignments() {
        return this.assignments;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void addAssignment(StaffAssignment ap) {
        this.assignments.add(ap);
    }

    public void setAssignments(ArrayList<StaffAssignment> assignments) {
        this.assignments = assignments;
    }


    public String getType() {
        return type;
    }

}
