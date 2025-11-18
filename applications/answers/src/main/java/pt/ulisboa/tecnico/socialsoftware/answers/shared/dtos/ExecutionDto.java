package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionUser;

public class ExecutionDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String acronym;
    private String academicTerm;
    private LocalDateTime endDate;
    private Integer courseAggregateId;
    private Set<ExecutionUser> users;

    public ExecutionDto() {
    }

    public ExecutionDto(Execution execution) {
        this.aggregateId = execution.getAggregateId();
        this.version = execution.getVersion();
        this.state = execution.getState();
        this.acronym = execution.getAcronym();
        this.academicTerm = execution.getAcademicTerm();
        this.endDate = execution.getEndDate();
        this.courseAggregateId = execution.getCourse() != null ? execution.getCourse().getAggregateId() : null;
        this.users = execution.getUsers();
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

    public Set<ExecutionUser> getUsers() {
        return users;
    }

    public void setUsers(Set<ExecutionUser> users) {
        this.users = users;
    }
}