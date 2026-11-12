package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate;

public class ExecutionStudentDto {
    private Integer userAggregateId;
    private String userName;
    private String userUsername;
    private Long userVersion;
    private Boolean active;

    public ExecutionStudentDto() {
    }

    public ExecutionStudentDto(Integer userAggregateId, String userName, String userUsername, Long userVersion,
                               Boolean active) {
        this.userAggregateId = userAggregateId;
        this.userName = userName;
        this.userUsername = userUsername;
        this.userVersion = userVersion;
        this.active = active;
    }

    public ExecutionStudentDto(ExecutionStudent executionStudent) {
        this.userAggregateId = executionStudent.getUserAggregateId();
        this.userName = executionStudent.getUserName();
        this.userUsername = executionStudent.getUserUsername();
        this.userVersion = executionStudent.getUserVersion();
        this.active = executionStudent.isActive();
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
