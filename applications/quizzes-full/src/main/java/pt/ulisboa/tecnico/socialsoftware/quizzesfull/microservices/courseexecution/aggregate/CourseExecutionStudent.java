package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;

@Entity
public class CourseExecutionStudent {
    @Id
    @GeneratedValue
    private Long id;
    private Integer userAggregateId;
    private Long userVersion;
    private String userName;
    private String userUsername;
    @Column(columnDefinition = "boolean default false")
    private boolean active;
    @Enumerated(EnumType.STRING)
    private AggregateState state;
    @ManyToOne
    @JsonIgnore
    private CourseExecution courseExecution;

    public CourseExecutionStudent() {}

    public CourseExecutionStudent(UserDto userDto) {
        setUserAggregateId(userDto.getAggregateId());
        setUserVersion(userDto.getVersion());
        setUserName(userDto.getName());
        setUserUsername(userDto.getUsername());
        setActive(userDto.getActive() != null && userDto.getActive());
        setState(userDto.getState());
    }

    public CourseExecutionStudent(CourseExecutionStudent other) {
        setUserAggregateId(other.getUserAggregateId());
        setUserVersion(other.getUserVersion());
        setUserName(other.getUserName());
        setUserUsername(other.getUserUsername());
        setActive(other.isActive());
        setState(other.getState());
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Integer getUserAggregateId() { return userAggregateId; }
    public void setUserAggregateId(Integer userAggregateId) { this.userAggregateId = userAggregateId; }

    public Long getUserVersion() { return userVersion; }
    public void setUserVersion(Long userVersion) { this.userVersion = userVersion; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserUsername() { return userUsername; }
    public void setUserUsername(String userUsername) { this.userUsername = userUsername; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public AggregateState getState() { return state; }
    public void setState(AggregateState state) { this.state = state; }

    @JsonIgnore
    public CourseExecution getCourseExecution() { return courseExecution; }
    public void setCourseExecution(CourseExecution courseExecution) { this.courseExecution = courseExecution; }

    public void anonymize() {
        setUserName("ANONYMOUS");
        setUserUsername("ANONYMOUS");
    }

    public UserDto buildDto() {
        UserDto dto = new UserDto();
        dto.setAggregateId(getUserAggregateId());
        dto.setVersion(getUserVersion());
        dto.setName(getUserName());
        dto.setUsername(getUserUsername());
        dto.setActive(isActive());
        dto.setState(getState());
        return dto;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (getUserAggregateId() == null ? 0 : getUserAggregateId());
        hash = 31 * hash + (getUserVersion() == null ? 0 : getUserVersion().hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof CourseExecutionStudent)) {
            return false;
        }
        CourseExecutionStudent other = (CourseExecutionStudent) obj;
        return getUserAggregateId() != null && getUserAggregateId().equals(other.getUserAggregateId())
                && getUserVersion() != null && getUserVersion().equals(other.getUserVersion());
    }
}
