//^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
//File creato nuovo per il test3
package catering.businesslogic.event;

import catering.businesslogic.menu.Menu;

public class Modification {
    private String content;
    private Event event;
    private Menu menu;

    public Modification(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void linkTo(Event e, Menu m) {
        this.event = e;
        this.menu = m;
    }
    //^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
    //codice generato per eliminare warnings
    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public Menu getMenu() {
        return menu;
    }

    public void setMenu(Menu menu) {
        this.menu = menu;
    }
}
