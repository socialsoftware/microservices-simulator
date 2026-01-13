package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;

public class AnswerDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private LocalDateTime creationDate;
    private LocalDateTime answerDate;
    private Boolean completed;
    private AnswerExecutionDto execution;
    private AnswerUserDto user;
    private AnswerQuizDto quiz;
    private List<QuestionAnsweredDto> questions;

    public AnswerDto() {
    }

    public AnswerDto(Answer answer) {
        this.aggregateId = answer.getAggregateId();
        this.version = answer.getVersion();
        this.state = answer.getState();
        this.creationDate = answer.getCreationDate();
        this.answerDate = answer.getAnswerDate();
        this.completed = answer.getCompleted();
        this.execution = answer.getExecution() != null ? new AnswerExecutionDto(answer.getExecution()) : null;
        this.user = answer.getUser() != null ? new AnswerUserDto(answer.getUser()) : null;
        this.quiz = answer.getQuiz() != null ? new AnswerQuizDto(answer.getQuiz()) : null;
        this.questions = answer.getQuestions() != null ? answer.getQuestions().stream().map(QuestionAnsweredDto::new).collect(Collectors.toList()) : null;
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

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getAnswerDate() {
        return answerDate;
    }

    public void setAnswerDate(LocalDateTime answerDate) {
        this.answerDate = answerDate;
    }

    public Boolean getCompleted() {
        return completed;
    }

    public void setCompleted(Boolean completed) {
        this.completed = completed;
    }

    public AnswerExecutionDto getExecution() {
        return execution;
    }

    public void setExecution(AnswerExecutionDto execution) {
        this.execution = execution;
    }

    public AnswerUserDto getUser() {
        return user;
    }

    public void setUser(AnswerUserDto user) {
        this.user = user;
    }

    public AnswerQuizDto getQuiz() {
        return quiz;
    }

    public void setQuiz(AnswerQuizDto quiz) {
        this.quiz = quiz;
    }

    public List<QuestionAnsweredDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuestionAnsweredDto> questions) {
        this.questions = questions;
    }
}