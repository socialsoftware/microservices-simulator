package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionDto;

@Entity
public class QuizExecution {
    @Id
    @GeneratedValue
    private Integer executionAggregateId;
    private String executionName;
    private String executionAcronym;
    private String executionAcademicTerm;
    @OneToOne
    private Quiz quiz;

    public QuizExecution() {

    }

    public QuizExecution(ExecutionDto executionDto) {
        setAcronym(executionDto.getAcronym());
        setAcademicTerm(executionDto.getAcademicTerm());
        setEndDate(executionDto.getEndDate());
        setUsers(executionDto.getUsers());
    }

    public QuizExecution(QuizExecution other) {
        setExecutionName(other.getExecutionName());
        setExecutionAcronym(other.getExecutionAcronym());
        setExecutionAcademicTerm(other.getExecutionAcademicTerm());
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public String getExecutionName() {
        return executionName;
    }

    public void setExecutionName(String executionName) {
        this.executionName = executionName;
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

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }


    public ExecutionDto buildDto() {
        ExecutionDto dto = new ExecutionDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        return dto;
    }
}