package pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.post.aggregate.PostAuthor;

public class PostAuthorDto implements Serializable {
    private String name;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public PostAuthorDto() {
    }

    public PostAuthorDto(PostAuthor postAuthor) {
        this.name = postAuthor.getAuthorName();
        this.aggregateId = postAuthor.getAuthorAggregateId();
        this.version = postAuthor.getAuthorVersion();
        this.state = postAuthor.getAuthorState() != null ? postAuthor.getAuthorState().name() : null;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}