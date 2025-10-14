package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class ExecutionDto implements Serializable {
    
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String acronym;
    private String academicTerm;
    private LocalDateTime endDate;
    private Integer courseAggregateId;
    private String courseName;
    private String courseType;
    private Integer courseVersion;
    private Set<UserDto> users;
    
    public ExecutionDto() {
    }
    
    public ExecutionDto(Execution execution) {
        setAggregateId(execution.getAggregateId());
        setVersion(execution.getVersion());
        setState(execution.getState());
        setAcronym(execution.getAcronym());
        setAcademicTerm(execution.getAcademicTerm());
        setEndDate(execution.getEndDate());
        setCourseAggregateId(execution.getExecutionCourse().getCourseAggregateId());
        setCourseName(execution.getExecutionCourse().getCourseName());
        setCourseType(execution.getExecutionCourse().getCourseType().toString());
        setCourseVersion(execution.getExecutionCourse().getCourseVersion());
        setUsers(execution.getUsers().stream()
            .map(executionuser -> ((java.util.function.Supplier<UserDto>) () -> {
            UserDto userdto = new UserDto();
                userdto.setName(executionuser.getUserName());
                userdto.setUsername(executionuser.getUserUsername());
                userdto.setRole(executionuser.getRole());
                userdto.setActive(executionuser.getActive());
                userdto.setNumberAnswered(executionuser.getNumberAnswered());
                userdto.setNumberCorrect(executionuser.getNumberCorrect());
            return userdto;
        }).get())
            .collect(Collectors.toSet()));
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

    public String getAcronym() {
        return acronym;
    }
    
    public void setAcronym(String acronym) {
        this.acronym = acronym;
    }

    public String getAcademicTerm() {
        return academicTerm;
    }
    
    public void setAcademicTerm(String academicTerm) {
        this.academicTerm = academicTerm;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }
    
    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
    }

    public Integer getCourseAggregateId() {
        return courseAggregateId;
    }
    
    public void setCourseAggregateId(Integer courseAggregateId) {
        this.courseAggregateId = courseAggregateId;
    }

    public String getCourseName() {
        return courseName;
    }
    
    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getCourseType() {
        return courseType;
    }
    
    public void setCourseType(String courseType) {
        this.courseType = courseType;
    }

    public Integer getCourseVersion() {
        return courseVersion;
    }
    
    public void setCourseVersion(Integer courseVersion) {
        this.courseVersion = courseVersion;
    }

    public Set<UserDto> getUsers() {
        return users;
    }
    
    public void setUsers(Set<UserDto> users) {
        this.users = users;
    }
}