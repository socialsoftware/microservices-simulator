package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;
import java.io.Serializable;

public class UserDto implements Serializable {
    private Integer aggregateId;
    private Integer key;
    private String name;
    private String username;
    private String role;
    private Boolean active;
    private Long version;
    private AggregateState state;

    public UserDto() {
    }

    public UserDto(User user) {
        setAggregateId(user.getAggregateId());
        setKey(user.getKey());
        setName(user.getName());
        setUsername(user.getUsername());
        setRole(user.getRole() != null ? user.getRole().toString() : null);
        setActive(user.isActive());
        setVersion(user.getVersion());
        setState(user.getState());
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Integer getKey() {
        return key;
    }

    public void setKey(Integer key) {
        this.key = key;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }
}
