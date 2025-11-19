package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.Answer;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerUser;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.QuestionAnswered;

public class AnswerDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private LocalDateTime creationDate;
    private LocalDateTime answerDate;
    private Boolean completed;
    private Integer executionAggregateId;
    private Integer userAggregateId;
    private Integer quizAggregateId;
    private List<QuestionAnsweredDto> question;

    public AnswerDto() {
    }

    public AnswerDto(Answer answer) {
        this.aggregateId = answer.getAggregateId();
        this.version = answer.getVersion();
        this.state = answer.getState();
        this.creationDate = answer.getCreationDate();
        this.answerDate = answer.getAnswerDate();
        this.completed = answer.getCompleted();
        this.executionAggregateId = answer.getExecution() != null ? answer.getExecution().getExecutionAggregateId() : null;
        this.userAggregateId = answer.getUser() != null ? answer.getUser().getUserAggregateId() : null;
        this.quizAggregateId = answer.getQuiz() != null ? answer.getQuiz().getQuizAggregateId() : null;
        this.question = answer.getQuestion() != null ? answer.getQuestion().stream().map(QuestionAnswered::buildDto).collect(Collectors.toList()) : null;
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

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }

    public List<QuestionAnsweredDto> getQuestion() {
        return question;
    }

    public void setQuestion(List<QuestionAnsweredDto> question) {
        this.question = question;
    }
}