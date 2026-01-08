package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.Quiz;

public class QuizDto implements Serializable {
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private String title;
    private String quizType;
    private LocalDateTime creationDate;
    private LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private LocalDateTime resultsDate;
    private Integer executionAggregateId;
    private Set<Integer> questionsAggregateIds;

    public QuizDto() {
    }

    public QuizDto(Quiz quiz) {
        this.aggregateId = quiz.getAggregateId();
        this.version = quiz.getVersion();
        this.state = quiz.getState();
        this.title = quiz.getTitle();
        this.quizType = quiz.getQuizType() != null ? quiz.getQuizType().name() : null;
        this.creationDate = quiz.getCreationDate();
        this.availableDate = quiz.getAvailableDate();
        this.conclusionDate = quiz.getConclusionDate();
        this.resultsDate = quiz.getResultsDate();
        this.executionAggregateId = quiz.getExecution() != null ? quiz.getExecution().getExecutionAggregateId() : null;
        this.questionsAggregateIds = quiz.getQuestions() != null ? quiz.getQuestions().stream().map(item -> item.getQuestionAggregateId()).collect(Collectors.toSet()) : null;
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

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getQuizType() {
        return quizType;
    }

    public void setQuizType(String quizType) {
        this.quizType = quizType;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public LocalDateTime getAvailableDate() {
        return availableDate;
    }

    public void setAvailableDate(LocalDateTime availableDate) {
        this.availableDate = availableDate;
    }

    public LocalDateTime getConclusionDate() {
        return conclusionDate;
    }

    public void setConclusionDate(LocalDateTime conclusionDate) {
        this.conclusionDate = conclusionDate;
    }

    public LocalDateTime getResultsDate() {
        return resultsDate;
    }

    public void setResultsDate(LocalDateTime resultsDate) {
        this.resultsDate = resultsDate;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Set<Integer> getQuestionsAggregateIds() {
        return questionsAggregateIds;
    }

    public void setQuestionsAggregateIds(Set<Integer> questionsAggregateIds) {
        this.questionsAggregateIds = questionsAggregateIds;
    }
}