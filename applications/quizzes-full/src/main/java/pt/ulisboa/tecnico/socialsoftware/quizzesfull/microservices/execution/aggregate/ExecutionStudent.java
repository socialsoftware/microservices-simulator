package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;

@Entity
public class ExecutionStudent {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer userAggregateId;
    private String userName;
    private String userUsername;
    private Boolean active;

    public ExecutionStudent() {}

    public ExecutionStudent(UserDto userDto) {
        this.userAggregateId = userDto.getAggregateId();
        this.userName = userDto.getName();
        this.userUsername = userDto.getUsername();
        this.active = userDto.isActive();
    }

    public ExecutionStudent(ExecutionStudent other) {
        this.userAggregateId = other.getUserAggregateId();
        this.userName = other.getUserName();
        this.userUsername = other.getUserUsername();
        this.active = other.isActive();
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getUserAggregateId() { return userAggregateId; }
    public void setUserAggregateId(Integer userAggregateId) { this.userAggregateId = userAggregateId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserUsername() { return userUsername; }
    public void setUserUsername(String userUsername) { this.userUsername = userUsername; }

    public Boolean isActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
