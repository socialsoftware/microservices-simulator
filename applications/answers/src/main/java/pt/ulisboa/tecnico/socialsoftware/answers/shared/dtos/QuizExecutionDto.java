package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizExecution;

public class QuizExecutionDto implements Serializable {
    private String executionName;
    private Integer aggregateId;
    private String acronym;
    private String academicTerm;

    public QuizExecutionDto() {
    }

    public QuizExecutionDto(QuizExecution quizExecution) {
        this.executionName = quizExecution.getExecutionName();
        this.aggregateId = quizExecution.getExecutionAggregateId();
        this.acronym = quizExecution.getExecutionAcronym();
        this.academicTerm = quizExecution.getExecutionAcademicTerm();
    }

    public String getExecutionName() {
        return executionName;
    }

    public void setExecutionName(String executionName) {
        this.executionName = executionName;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
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
}