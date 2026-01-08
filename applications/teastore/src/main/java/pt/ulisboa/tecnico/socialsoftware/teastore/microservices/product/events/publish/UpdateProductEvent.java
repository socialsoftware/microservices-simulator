package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class UpdateProductEvent extends Event {
    private String name;
    private String description;
    private Double listPriceInCents;
    private String categoryName;

    public UpdateProductEvent() {
    }

    public UpdateProductEvent(Integer aggregateId, String name, String description, Double listPriceInCents, String categoryName) {
        super(aggregateId);
        setName(name);
        setDescription(description);
        setListPriceInCents(listPriceInCents);
        setCategoryName(categoryName);
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

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

}