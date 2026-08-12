package pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.Category;

public class CategoryDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String name;
    private String description;

    public CategoryDto() {
    }

    public CategoryDto(Category category) {
        this.aggregateId = category.getAggregateId();
        this.version = category.getVersion();
        this.state = category.getState();
        this.name = category.getName();
        this.description = category.getDescription();
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
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
}