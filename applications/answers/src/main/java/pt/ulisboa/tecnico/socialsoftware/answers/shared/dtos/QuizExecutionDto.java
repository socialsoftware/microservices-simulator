package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizExecution;

public class QuizExecutionDto implements Serializable {
    private String executionName;
    private String acronym;
    private String academicTerm;
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;

    public QuizExecutionDto() {
    }

    public QuizExecutionDto(QuizExecution quizExecution) {
        this.executionName = quizExecution.getExecutionName();
        this.acronym = quizExecution.getExecutionAcronym();
        this.academicTerm = quizExecution.getExecutionAcademicTerm();
        this.aggregateId = quizExecution.getExecutionAggregateId();
        this.version = quizExecution.getExecutionVersion();
        this.state = quizExecution.getExecutionState();
    }

    public String getExecutionName() {
        return executionName;
    }

    public void setExecutionName(String executionName) {
        this.executionName = executionName;
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
}