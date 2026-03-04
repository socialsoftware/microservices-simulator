package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.product.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;

import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.ProductDto;

@Entity
public abstract class Product extends Aggregate {
    private String name;
    private Double price;
    private Boolean available;

    public Product() {

    }

    public Product(Integer aggregateId, ProductDto productDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(productDto.getName());
        setPrice(productDto.getPrice());
        setAvailable(productDto.getAvailable());
    }


    public Product(Product other) {
        super(other);
        setName(other.getName());
        setPrice(other.getPrice());
        setAvailable(other.getAvailable());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }


    @Override
    public void verifyInvariants() {
    }

    public ProductDto buildDto() {
        ProductDto dto = new ProductDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setName(getName());
        dto.setPrice(getPrice());
        dto.setAvailable(getAvailable());
        return dto;
    }
}