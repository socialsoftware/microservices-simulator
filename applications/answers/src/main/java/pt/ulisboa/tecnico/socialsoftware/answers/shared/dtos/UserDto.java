package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class UserDto implements Serializable {
    
    private Integer id;
    private Integer version;
    private String name;
    private String username;
    private AggregateState state;
    
    public UserDto() {
    }
    
    public UserDto(Integer id, Integer version, String name, String username, AggregateState state) {
        setId(id);
        setVersion(version);
        setName(name);
        setUsername(username);
        setState(state);
    }
    
    public Integer getId() {
        return id;
    }
    
    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getVersion() {
        return version;
    }
    
    public void setVersion(Integer version) {
        this.version = version;
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

    public AggregateState getState() {
        return state;
    }
    
    public void setState(AggregateState state) {
        this.state = state;
    }
}