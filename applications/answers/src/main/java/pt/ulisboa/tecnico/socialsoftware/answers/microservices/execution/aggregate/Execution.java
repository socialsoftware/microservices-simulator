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
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.subscribe.ExecutionSubscribesCourseDeletedCourseExists;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.subscribe.ExecutionSubscribesUserDeletedUsersExist;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.subscribe.ExecutionSubscribesUserUpdatedUsersExist;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionCourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Execution extends Aggregate {
    private String acronym;
    private String academicTerm;
    private LocalDateTime endDate;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "execution")
    private ExecutionCourse course;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "execution")
    private Set<ExecutionUser> users = new HashSet<>();

    public Execution() {

    }

    public Execution(Integer aggregateId, ExecutionDto executionDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setAcronym(executionDto.getAcronym());
        setAcademicTerm(executionDto.getAcademicTerm());
        setEndDate(executionDto.getEndDate());
        setCourse(executionDto.getCourse() != null ? new ExecutionCourse(executionDto.getCourse()) : null);
        setUsers(executionDto.getUsers() != null ? executionDto.getUsers().stream().map(ExecutionUser::new).collect(Collectors.toSet()) : null);
    }


    public Execution(Execution other) {
        super(other);
        setAcronym(other.getAcronym());
        setAcademicTerm(other.getAcademicTerm());
        setEndDate(other.getEndDate());
        setCourse(new ExecutionCourse(other.getCourse()));
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

    public ExecutionCourse getCourse() {
        return course;
    }

    public void setCourse(ExecutionCourse course) {
        this.course = course;
        if (this.course != null) {
            this.course.setExecution(this);
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


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantCourseExists(eventSubscriptions);
            interInvariantUsersExist(eventSubscriptions);
        }
        return eventSubscriptions;
    }
    private void interInvariantCourseExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new ExecutionSubscribesCourseDeletedCourseExists(this.getCourse()));
    }

    private void interInvariantUsersExist(Set<EventSubscription> eventSubscriptions) {
        for (ExecutionUser item : this.users) {
            eventSubscriptions.add(new ExecutionSubscribesUserDeletedUsersExist(item));
            eventSubscriptions.add(new ExecutionSubscribesUserUpdatedUsersExist(item));
        }
    }

    // ============================================================================
    // INVARIANTS
    // ============================================================================

    private boolean invariantAcronymNotBlank() {
        return this.acronym != null && this.acronym.length() > 0;
    }

    private boolean invariantAcademicTermNotBlank() {
        return this.academicTerm != null && this.academicTerm.length() > 0;
    }

    private boolean invariantCourseNotNull() {
        return this.course != null;
    }

    private boolean invariantUsersNotNull() {
        return this.users != null;
    }
    @Override
    public void verifyInvariants() {
        if (!(invariantAcronymNotBlank()
               && invariantAcademicTermNotBlank()
               && invariantCourseNotNull()
               && invariantUsersNotNull())) {
            throw new SimulatorException(INVARIANT_BREAK, getAggregateId());
        }
    }

    public ExecutionDto buildDto() {
        ExecutionDto dto = new ExecutionDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setAcronym(getAcronym());
        dto.setAcademicTerm(getAcademicTerm());
        dto.setEndDate(getEndDate());
        dto.setCourse(getCourse() != null ? new ExecutionCourseDto(getCourse()) : null);
        dto.setUsers(getUsers() != null ? getUsers().stream().map(ExecutionUser::buildDto).collect(Collectors.toSet()) : null);
        return dto;
    }
}