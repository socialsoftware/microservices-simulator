package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;

import pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos.TaskDto;

@Entity
public abstract class Task extends Aggregate {
    private String title;
    private String description;
    private Boolean done;

    public Task() {

    }

    public Task(Integer aggregateId, TaskDto taskDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setTitle(taskDto.getTitle());
        setDescription(taskDto.getDescription());
        setDone(taskDto.getDone());
    }


    public Task(Task other) {
        super(other);
        setTitle(other.getTitle());
        setDescription(other.getDescription());
        setDone(other.getDone());
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }


    @Override
    public void verifyInvariants() {
    }

    public TaskDto buildDto() {
        TaskDto dto = new TaskDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setTitle(getTitle());
        dto.setDescription(getDescription());
        dto.setDone(getDone());
        return dto;
    }
}