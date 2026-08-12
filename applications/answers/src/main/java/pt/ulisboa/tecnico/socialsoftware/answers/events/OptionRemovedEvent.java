package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class OptionRemovedEvent extends Event {
    private Integer key;

    public OptionRemovedEvent() {
        super();
    }

    public OptionRemovedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public OptionRemovedEvent(Integer aggregateId, Integer key) {
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