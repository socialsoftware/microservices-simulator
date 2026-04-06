package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class QuizExecutionUpdatedEvent extends Event {
    @Column(name = "quiz_execution_updated_event_execution_aggregate_id")
    private Integer executionAggregateId;
    @Column(name = "quiz_execution_updated_event_execution_version")
    private Integer executionVersion;
    @Column(name = "quiz_execution_updated_event_execution_acronym")
    private String executionAcronym;
    @Column(name = "quiz_execution_updated_event_execution_academic_term")
    private String executionAcademicTerm;
    @Column(name = "quiz_execution_updated_event_execution_name")
    private String executionName;

    public QuizExecutionUpdatedEvent() {
        super();
    }

    public QuizExecutionUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public QuizExecutionUpdatedEvent(Integer aggregateId, Integer executionAggregateId, Integer executionVersion, String executionAcronym, String executionAcademicTerm, String executionName) {
        super(aggregateId);
        setExecutionAggregateId(executionAggregateId);
        setExecutionVersion(executionVersion);
        setExecutionAcronym(executionAcronym);
        setExecutionAcademicTerm(executionAcademicTerm);
        setExecutionName(executionName);
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Integer getExecutionVersion() {
        return executionVersion;
    }

    public void setExecutionVersion(Integer executionVersion) {
        this.executionVersion = executionVersion;
    }

    public String getExecutionAcronym() {
        return executionAcronym;
    }

    public void setExecutionAcronym(String executionAcronym) {
        this.executionAcronym = executionAcronym;
    }

    public String getExecutionAcademicTerm() {
        return executionAcademicTerm;
    }

    public void setExecutionAcademicTerm(String executionAcademicTerm) {
        this.executionAcademicTerm = executionAcademicTerm;
    }

    public String getExecutionName() {
        return executionName;
    }

    public void setExecutionName(String executionName) {
        this.executionName = executionName;
    }

}