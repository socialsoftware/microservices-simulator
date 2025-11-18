package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.User;

public class UserDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String name;
    private String username;
    private Integer roleAggregateId;
    private Boolean active;

    public UserDto() {
    }

    public UserDto(User user) {
        this.aggregateId = user.getAggregateId();
        this.version = user.getVersion();
        this.state = user.getState();
        this.name = user.getName();
        this.username = user.getUsername();
        this.roleAggregateId = user.getRole() != null ? user.getRole().getAggregateId() : null;
        this.active = user.isActive();
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public Integer getRoleAggregateId() {
        return roleAggregateId;
    }

    public void setRoleAggregateId(Integer roleAggregateId) {
        this.roleAggregateId = roleAggregateId;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}