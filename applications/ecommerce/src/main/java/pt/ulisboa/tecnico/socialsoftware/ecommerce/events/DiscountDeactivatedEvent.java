package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class DiscountDeactivatedEvent extends Event {
    private String code;

    public DiscountDeactivatedEvent() {
        super();
    }

    public DiscountDeactivatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public DiscountDeactivatedEvent(Integer aggregateId, String code) {
        super(aggregateId);
        setCode(code);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

}