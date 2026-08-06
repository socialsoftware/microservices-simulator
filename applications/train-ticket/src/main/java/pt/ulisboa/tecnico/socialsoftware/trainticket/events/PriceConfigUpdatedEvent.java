package pt.ulisboa.tecnico.socialsoftware.trainticket.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class PriceConfigUpdatedEvent extends Event {
    private Double basicPriceRate;
    private Double firstClassPriceRate;

    public PriceConfigUpdatedEvent() {
        super();
    }

    public PriceConfigUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public PriceConfigUpdatedEvent(Integer aggregateId, Double basicPriceRate, Double firstClassPriceRate) {
        super(aggregateId);
        setBasicPriceRate(basicPriceRate);
        setFirstClassPriceRate(firstClassPriceRate);
    }

    public Double getBasicPriceRate() {
        return basicPriceRate;
    }

    public void setBasicPriceRate(Double basicPriceRate) {
        this.basicPriceRate = basicPriceRate;
    }

    public Double getFirstClassPriceRate() {
        return firstClassPriceRate;
    }

    public void setFirstClassPriceRate(Double firstClassPriceRate) {
        this.firstClassPriceRate = firstClassPriceRate;
    }

}
