package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class StationUpdatedEvent extends Event {
    private String name;
    private Integer stayTime;

    public StationUpdatedEvent() {
        super();
    }

    public StationUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public StationUpdatedEvent(Integer aggregateId, String name, Integer stayTime) {
        super(aggregateId);
        setName(name);
        setStayTime(stayTime);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getStayTime() {
        return stayTime;
    }

    public void setStayTime(Integer stayTime) {
        this.stayTime = stayTime;
    }

}
