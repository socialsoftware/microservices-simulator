package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;

@Entity
public class ExecutionStudent {
    @Id
    @GeneratedValue
    private Integer studentAggregateId;
    private String studentName;
    private String studentUsername;
    private String studentEmail;
    private LocalDateTime enrollmentDate;
    @OneToOne
    private Execution execution; 

    public ExecutionStudent() {
    }

    public ExecutionStudent(ExecutionDto executionDto) {
        setStudentName(executionDto.getStudentName());
        setStudentUsername(executionDto.getStudentUsername());
        setStudentEmail(executionDto.getStudentEmail());
        setEnrollmentDate(executionDto.getEnrollmentDate());
    }

    public ExecutionStudent(ExecutionStudent other) {
        setStudentName(other.getStudentName());
        setStudentUsername(other.getStudentUsername());
        setStudentEmail(other.getStudentEmail());
        setEnrollmentDate(other.getEnrollmentDate());
    }


    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }

    public void setStudentAggregateId(Integer studentAggregateId) {
        this.studentAggregateId = studentAggregateId;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentUsername() {
        return studentUsername;
    }

    public void setStudentUsername(String studentUsername) {
        this.studentUsername = studentUsername;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public void setStudentEmail(String studentEmail) {
        this.studentEmail = studentEmail;
    }

    public LocalDateTime getEnrollmentDate() {
        return enrollmentDate;
    }

    public void setEnrollmentDate(LocalDateTime enrollmentDate) {
        this.enrollmentDate = enrollmentDate;
    }

    public Execution getExecution() {
        return execution;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }


}