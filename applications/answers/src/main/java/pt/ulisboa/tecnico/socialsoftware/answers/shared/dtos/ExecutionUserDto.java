package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionUser;

public class ExecutionUserDto implements Serializable {
    private String name;
    private String username;
    private Boolean active;
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;

    public ExecutionUserDto() {
    }

    public ExecutionUserDto(ExecutionUser executionUser) {
        this.name = executionUser.getUserName();
        this.username = executionUser.getUserUsername();
        this.active = executionUser.getUserActive();
        this.aggregateId = executionUser.getUserAggregateId();
        this.version = executionUser.getUserVersion();
        this.state = executionUser.getUserState();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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