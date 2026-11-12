package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "execution_students")
public class ExecutionStudent {
    @Id
    @GeneratedValue
    private Integer id;
    private Integer userAggregateId;
    private String userName;
    private String userUsername;
    private Long userVersion;
    private Boolean active;

    public ExecutionStudent() {
    }

    public ExecutionStudent(Integer userAggregateId, String userName, String userUsername, Long userVersion,
                            Boolean active) {
        this.userAggregateId = userAggregateId;
        this.userName = userName;
        this.userUsername = userUsername;
        this.userVersion = userVersion;
        this.active = active;
    }

    public ExecutionStudent(ExecutionStudent other) {
        this.userAggregateId = other.getUserAggregateId();
        this.userName = other.getUserName();
        this.userUsername = other.getUserUsername();
        this.userVersion = other.getUserVersion();
        this.active = other.isActive();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserUsername() {
        return userUsername;
    }

    public void setUserUsername(String userUsername) {
        this.userUsername = userUsername;
    }

    public Long getUserVersion() {
        return userVersion;
    }

    public void setUserVersion(Long userVersion) {
        this.userVersion = userVersion;
    }

    public Boolean isActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}
