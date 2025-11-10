package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import java.time.LocalDateTime;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionUser;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;

@Entity
public abstract class Execution extends Aggregate {
    private String acronym;
    private String academicTerm;
    private LocalDateTime endDate;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "execution")
    private ExecutionCourse executionCourse;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "execution")
    private Set<ExecutionUser> users = new HashSet<>();

    public Execution() {

    }

    public Execution(Integer aggregateId, ExecutionDto executionDto, ExecutionCourse executionCourse) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setAcronym(executionDto.getAcronym());
        setAcademicTerm(executionDto.getAcademicTerm());
        setEndDate(executionDto.getEndDate());
        setExecutionCourse(executionCourse);
    }

    public Execution(Execution other) {
        super(other);
        setAcronym(other.getAcronym());
        setAcademicTerm(other.getAcademicTerm());
        setEndDate(other.getEndDate());
        setExecutionCourse(new ExecutionCourse(other.getExecutionCourse()));
        setUsers(other.getUsers().stream().map(ExecutionUser::new).collect(Collectors.toSet()));
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

    public ExecutionCourse getExecutionCourse() {
        return executionCourse;
    }

    public void setExecutionCourse(ExecutionCourse executionCourse) {
        this.executionCourse = executionCourse;
        if (this.executionCourse != null) {
            this.executionCourse.setExecution(this);
        }
    }

    public Set<ExecutionUser> getUsers() {
        return users;
    }

    public void setUsers(Set<ExecutionUser> users) {
        this.users = users;
        if (this.users != null) {
            this.users.forEach(item -> item.setExecution(this));
        }
    }

    public void addExecutionUser(ExecutionUser executionUser) {
        if (this.users == null) {
            this.users = new HashSet<>();
        }
        this.users.add(executionUser);
        if (executionUser != null) {
            executionUser.setExecution(this);
        }
    }

    public void removeExecutionUser(Long id) {
        if (this.users != null) {
            this.users.removeIf(item -> 
                item.getId() != null && item.getId().equals(id));
        }
    }

    public boolean containsExecutionUser(Long id) {
        if (this.users == null) {
            return false;
        }
        return this.users.stream().anyMatch(item -> 
            item.getId() != null && item.getId().equals(id));
    }

    public ExecutionUser findExecutionUserById(Long id) {
        if (this.users == null) {
            return null;
        }
        return this.users.stream()
            .filter(item -> item.getId() != null && item.getId().equals(id))
            .findFirst()
            .orElse(null);
    }


}