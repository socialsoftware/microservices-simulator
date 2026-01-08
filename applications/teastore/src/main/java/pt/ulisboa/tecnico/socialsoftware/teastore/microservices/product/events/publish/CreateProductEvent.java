package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class CreateProductEvent extends Event {
    private String name;
    private String categoryName;
    private Double listPriceInCents;

    public CreateProductEvent() {
    }

    public CreateProductEvent(Integer aggregateId, String name, String categoryName, Double listPriceInCents) {
        super(aggregateId);
        setName(name);
        setCategoryName(categoryName);
        setListPriceInCents(listPriceInCents);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Double getListPriceInCents() {
        return listPriceInCents;
    }

    public void setListPriceInCents(Double listPriceInCents) {
        this.listPriceInCents = listPriceInCents;
    }

}