package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class UserDto implements Serializable {
    
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String name;
    private String username;
    private String role;
    private Boolean active;
    private Integer numberAnswered;
    private Integer numberCorrect;
    
    public UserDto() {
    }
    
    public UserDto(Integer aggregateId, Integer version, AggregateState state, String name, String username, String role, Boolean active, Integer numberAnswered, Integer numberCorrect) {
        setAggregateId(aggregateId);
        setVersion(version);
        setState(state);
        setName(name);
        setUsername(username);
        setRole(role);
        setActive(active);
        setNumberAnswered(numberAnswered);
        setNumberCorrect(numberCorrect);
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

    public Integer getNumberAnswered() {
        return numberAnswered;
    }
    
    public void setNumberAnswered(Integer numberAnswered) {
        this.numberAnswered = numberAnswered;
    }

    public Integer getNumberCorrect() {
        return numberCorrect;
    }
    
    public void setNumberCorrect(Integer numberCorrect) {
        this.numberCorrect = numberCorrect;
    }
}