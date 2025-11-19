package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionUser;

public class ExecutionDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String acronym;
    private String academicTerm;
    private LocalDateTime endDate;
    private CourseDto course;
    private Set<UserDto> users;

    public ExecutionDto() {
    }

    public ExecutionDto(Execution execution) {
        this.aggregateId = execution.getAggregateId();
        this.version = execution.getVersion();
        this.state = execution.getState();
        this.acronym = execution.getAcronym();
        this.academicTerm = execution.getAcademicTerm();
        this.endDate = execution.getEndDate();
        this.course = execution.getCourse() != null ? execution.getCourse().buildDto() : null;
        this.users = execution.getUsers() != null ? execution.getUsers().stream().map(ExecutionUser::buildDto).collect(Collectors.toSet()) : null;
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

    public CourseDto getCourse() {
        return course;
    }

    public void setCourse(CourseDto course) {
        this.course = course;
    }

    public Set<UserDto> getUsers() {
        return users;
    }

    public void setUsers(Set<UserDto> users) {
        this.users = users;
    }
}