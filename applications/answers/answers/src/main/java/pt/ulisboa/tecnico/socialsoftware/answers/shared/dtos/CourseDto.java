package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class CourseDto implements Serializable {
    
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String type;
    private String name;
    private LocalDateTime creationDate;
    
    public CourseDto() {
    }
    
    public CourseDto(Integer aggregateId, Integer version, AggregateState state, String type, String name, LocalDateTime creationDate) {
        setAggregateId(aggregateId);
        setVersion(version);
        setState(state);
        setType(type);
        setName(name);
        setCreationDate(creationDate);
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

    public String getType() {
        return type;
    }
    
    public void setType(String type) {
        this.type = type;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }
    
    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }
}