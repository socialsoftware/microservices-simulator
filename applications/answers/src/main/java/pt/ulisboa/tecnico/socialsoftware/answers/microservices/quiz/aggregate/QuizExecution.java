package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuizExecutionDto;

@Entity
public class QuizExecution {
    @Id
    @GeneratedValue
    private Long id;
    private String executionName;
    private String executionAcronym;
    private String executionAcademicTerm;
    private Integer executionAggregateId;
    private Integer executionVersion;
    private AggregateState executionState;
    @OneToOne
    private Quiz quiz;

    public QuizExecution() {

    }

    public QuizExecution(ExecutionDto executionDto) {
        setExecutionAggregateId(executionDto.getAggregateId());
        setExecutionVersion(executionDto.getVersion());
        setExecutionState(executionDto.getState());
        setExecutionAcronym(executionDto.getAcronym());
        setExecutionAcademicTerm(executionDto.getAcademicTerm());
    }

    public QuizExecution(QuizExecution other) {
        setExecutionAcronym(other.getExecutionAcronym());
        setExecutionAcademicTerm(other.getExecutionAcademicTerm());
        setExecutionAggregateId(other.getExecutionAggregateId());
        setExecutionVersion(other.getExecutionVersion());
        setExecutionState(other.getExecutionState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public AggregateState getExecutionState() {
        return executionState;
    }

    public void setExecutionState(AggregateState executionState) {
        this.executionState = executionState;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
    }



    public QuizExecutionDto buildDto() {
        QuizExecutionDto dto = new QuizExecutionDto();
        dto.setExecutionName(getExecutionName());
        dto.setAcronym(getExecutionAcronym());
        dto.setAcademicTerm(getExecutionAcademicTerm());
        dto.setAggregateId(getExecutionAggregateId());
        dto.setVersion(getExecutionVersion());
        dto.setState(getExecutionState());
        return dto;
    }
}