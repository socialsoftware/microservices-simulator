package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class ProductUpdatedEvent extends Event {
    private String name;
    private String description;
    private Double listPriceInCents;

    public ProductUpdatedEvent() {
    }

    public ProductUpdatedEvent(Integer aggregateId, String name, String description, Double listPriceInCents) {
        super(aggregateId);
        setName(name);
        setDescription(description);
        setListPriceInCents(listPriceInCents);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getListPriceInCents() {
        return listPriceInCents;
    }

    public void setListPriceInCents(Double listPriceInCents) {
        this.listPriceInCents = listPriceInCents;
    }

}