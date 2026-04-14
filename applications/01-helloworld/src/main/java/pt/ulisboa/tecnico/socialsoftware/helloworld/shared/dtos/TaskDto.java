package pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.Task;

public class TaskDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String title;
    private String description;
    private Boolean done;

    public TaskDto() {
    }

    public TaskDto(Task task) {
        this.aggregateId = task.getAggregateId();
        this.version = task.getVersion();
        this.state = task.getState();
        this.title = task.getTitle();
        this.description = task.getDescription();
        this.done = task.getDone();
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getDone() {
        return done;
    }

    public void setDone(Boolean done) {
        this.done = done;
    }
}