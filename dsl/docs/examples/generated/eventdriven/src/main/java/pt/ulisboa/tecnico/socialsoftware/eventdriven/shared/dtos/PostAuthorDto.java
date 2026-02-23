package pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.PostAuthor;

public class PostAuthorDto implements Serializable {
    private String name;
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;

    public PostAuthorDto() {
    }

    public PostAuthorDto(PostAuthor postAuthor) {
        this.name = postAuthor.getAuthorName();
        this.aggregateId = postAuthor.getAuthorAggregateId();
        this.version = postAuthor.getAuthorVersion();
        this.state = postAuthor.getAuthorState();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
}