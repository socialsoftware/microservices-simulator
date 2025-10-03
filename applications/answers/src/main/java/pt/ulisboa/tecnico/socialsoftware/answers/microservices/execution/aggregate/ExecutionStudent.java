package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;

@Entity
public class ExecutionStudent {
    @Id
    @GeneratedValue
    private Long id;
    private Integer studentAggregateId;
    private Integer studentVersion;
    private String studentName;
    private String studentUsername;
    private Boolean active;
    private AggregateState studentState;
    @OneToOne
    private Execution execution; 

    public ExecutionStudent() {
    }

    public ExecutionStudent(UserDto userdto) {
        setStudentAggregateId(userdto.getId());
        setStudentVersion(userdto.getVersion());
        setStudentName(userdto.getName());
        setStudentUsername(userdto.getUsername());
        setStudentState(userdto.getState());
    }

    public ExecutionStudent(ExecutionStudent other) {
        setStudentAggregateId(other.getStudentAggregateId());
        setStudentVersion(other.getStudentVersion());
        setStudentName(other.getStudentName());
        setStudentUsername(other.getStudentUsername());
        setActive(other.getActive());
        setStudentState(other.getStudentState());
    }


    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getStudentAggregateId() {
        return studentAggregateId;
    }

    public void setStudentAggregateId(Integer studentAggregateId) {
        this.studentAggregateId = studentAggregateId;
    }

    public Integer getStudentVersion() {
        return studentVersion;
    }

    public void setStudentVersion(Integer studentVersion) {
        this.studentVersion = studentVersion;
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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public AggregateState getStudentState() {
        return studentState;
    }

    public void setStudentState(AggregateState studentState) {
        this.studentState = studentState;
    }

    public Execution getExecution() {
        return execution;
    }

    public void setExecution(Execution execution) {
        this.execution = execution;
    }


}