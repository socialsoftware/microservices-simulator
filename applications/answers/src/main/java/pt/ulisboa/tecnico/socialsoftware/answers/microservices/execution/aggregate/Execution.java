package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.CascadeType;
import jakarta.persistence.FetchType;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.HashSet;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;

@Entity
public abstract class Execution extends Aggregate {
    @Id
    private String acronym;
    private String academicTerm;
    private LocalDateTime endDate;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "execution")
    private ExecutionCourse executionCourse;
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, mappedBy = "execution")
    private Set<ExecutionStudent> students = new HashSet<>(); 

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
        setStudents(other.getStudents().stream().map(ExecutionStudent::new).collect(Collectors.toSet()));
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

    public Set<ExecutionStudent> getStudents() {
        return students;
    }

    public void setStudents(Set<ExecutionStudent> students) {
        this.students = students;
        if (this.students != null) {
            this.students.forEach(executionstudent -> executionstudent.setExecution(this));
        }
    }



}