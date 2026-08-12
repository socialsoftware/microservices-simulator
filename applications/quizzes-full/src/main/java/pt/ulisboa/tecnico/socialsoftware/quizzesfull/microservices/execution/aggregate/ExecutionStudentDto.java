package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate;

import java.io.Serializable;

public class ExecutionStudentDto implements Serializable {
    private Integer userAggregateId;
    private String userName;
    private String userUsername;
    private Boolean active;

    public ExecutionStudentDto() {}

    public ExecutionStudentDto(ExecutionStudent student) {
        this.userAggregateId = student.getUserAggregateId();
        this.userName = student.getUserName();
        this.userUsername = student.getUserUsername();
        this.active = student.isActive();
    }

    public Integer getUserAggregateId() { return userAggregateId; }
    public void setUserAggregateId(Integer userAggregateId) { this.userAggregateId = userAggregateId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserUsername() { return userUsername; }
    public void setUserUsername(String userUsername) { this.userUsername = userUsername; }

    public Boolean isActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
}
