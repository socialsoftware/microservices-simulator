package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class QuizExecutionUpdatedEvent extends Event {
    private Integer executionAggregateId;
    private Integer executionVersion;
    private String executionAcronym;
    private String executionAcademicTerm;
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