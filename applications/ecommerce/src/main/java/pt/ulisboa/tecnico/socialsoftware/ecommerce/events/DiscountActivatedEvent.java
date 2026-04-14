package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class DiscountActivatedEvent extends Event {
    private String code;

    public DiscountActivatedEvent() {
        super();
    }

    public DiscountActivatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public DiscountActivatedEvent(Integer aggregateId, String code) {
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