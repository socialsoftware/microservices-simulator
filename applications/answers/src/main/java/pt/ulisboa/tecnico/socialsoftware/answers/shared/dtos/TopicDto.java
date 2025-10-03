package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


public class TopicDto implements Serializable {
    
    // Standard aggregate fields
    private Integer aggregateId;
    private Integer version;
    private String state;

    // Fields from TopicDetails
    private Integer id;
    private String name;
    private LocalDateTime creationDate;
    
    public TopicDto() {
    }
    
    public TopicDto(pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.Topic topic) {
        // Standard aggregate fields
        setAggregateId(topic.getAggregateId());
        setVersion(topic.getVersion());
        setState(topic.getState().toString());

        // Fields from TopicDetails
        setId(topic.getTopicDetails().getId());
        setName(topic.getTopicDetails().getCourseName());
        setCreationDate(topic.getTopicDetails().getCreationDate());

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

    public String getState() {
        return state;
    }
    
    public void setState(String state) {
        this.state = state;
    }

    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
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