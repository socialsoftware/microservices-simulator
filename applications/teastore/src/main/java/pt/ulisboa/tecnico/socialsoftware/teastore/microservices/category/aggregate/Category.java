package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;

import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CategoryDto;

@Entity
public abstract class Category extends Aggregate {
    private String name;
    private String description;

    public Category() {

    }

    public Category(Integer aggregateId, CategoryDto categoryDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(categoryDto.getName());
        setDescription(categoryDto.getDescription());
    }


    public Category(Category other) {
        super(other);
        setName(other.getName());
        setDescription(other.getDescription());
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }


    @Override
    public void verifyInvariants() {
    }

    public CategoryDto buildDto() {
        CategoryDto dto = new CategoryDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setName(getName());
        dto.setDescription(getDescription());
        return dto;
    }
}