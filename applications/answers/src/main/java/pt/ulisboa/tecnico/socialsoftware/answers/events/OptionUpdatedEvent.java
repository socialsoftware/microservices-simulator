package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OptionUpdatedEvent extends Event {
    private Integer key;

    public OptionUpdatedEvent() {
        super();
    }

    public OptionUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OptionUpdatedEvent(Integer aggregateId, Integer key) {
        super(aggregateId);
        setKey(key);
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
    }

}