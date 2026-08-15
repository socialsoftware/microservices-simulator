package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;

import java.io.Serializable;

public class UserDto implements Serializable {

    private Integer aggregateId;
    private String name;
    private String username;
    private String role;
    private boolean active;
    private Long version;
    private AggregateState state;

    public UserDto() {}

    public UserDto(User user) {
        this.aggregateId = user.getAggregateId();
        this.name = user.getName();
        this.username = user.getUsername();
        this.role = user.getRole().toString();
        this.active = user.isActive();
        this.version = user.getVersion();
        this.state = user.getState();
    }

    public UserDto(Integer aggregateId, String name, String username, String role, boolean active) {
        this.aggregateId = aggregateId;
        this.name = name;
        this.username = username;
        this.role = role;
        this.active = active;
    }

    public Integer getAggregateId() { return aggregateId; }
    public void setAggregateId(Integer aggregateId) { this.aggregateId = aggregateId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public AggregateState getState() { return state; }
    public void setState(AggregateState state) { this.state = state; }
}
