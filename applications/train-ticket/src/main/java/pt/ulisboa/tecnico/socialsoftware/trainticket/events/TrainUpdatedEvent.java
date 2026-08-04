package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

@Entity
public class TrainUpdatedEvent extends Event {
    private String name;
    private Integer economyClass;
    private Integer confortClass;
    private Integer averageSpeed;

    public TrainUpdatedEvent() {
        super();
    }

    public TrainUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public TrainUpdatedEvent(Integer aggregateId, String name, Integer economyClass, Integer confortClass, Integer averageSpeed) {
        super(aggregateId);
        setName(name);
        setEconomyClass(economyClass);
        setConfortClass(confortClass);
        setAverageSpeed(averageSpeed);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getEconomyClass() {
        return economyClass;
    }

    public void setEconomyClass(Integer economyClass) {
        this.economyClass = economyClass;
    }

    public Integer getConfortClass() {
        return confortClass;
    }

    public void setConfortClass(Integer confortClass) {
        this.confortClass = confortClass;
    }

    public Integer getAverageSpeed() {
        return averageSpeed;
    }

    public void setAverageSpeed(Integer averageSpeed) {
        this.averageSpeed = averageSpeed;
    }

}
