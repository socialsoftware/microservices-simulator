package pt.ulisboa.tecnico.socialsoftware.ecommerce.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class DiscountCreatedEvent extends Event {
    private String code;
    private Double percentageOff;

    public DiscountCreatedEvent() {
        super();
    }

    public DiscountCreatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public DiscountCreatedEvent(Integer aggregateId, String code, Double percentageOff) {
        super(aggregateId);
        setCode(code);
        setPercentageOff(percentageOff);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public Double getPercentageOff() {
        return percentageOff;
    }

    public void setPercentageOff(Double percentageOff) {
        this.percentageOff = percentageOff;
    }

}