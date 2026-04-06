package pt.ulisboa.tecnico.socialsoftware.teastore.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class ProductUpdatedEvent extends Event {
    @Column(name = "product_updated_event_name")
    private String name;
    @Column(name = "product_updated_event_description")
    private String description;
    @Column(name = "product_updated_event_list_price_in_cents")
    private Double listPriceInCents;

    public ProductUpdatedEvent() {
        super();
    }

    public ProductUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
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