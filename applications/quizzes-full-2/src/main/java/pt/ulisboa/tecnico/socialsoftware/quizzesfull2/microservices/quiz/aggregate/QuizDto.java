package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QuizDto {
    private Integer aggregateId;
    private Long version;
    private String title;
    private LocalDateTime creationDate;
    private LocalDateTime availableDate;
    private LocalDateTime conclusionDate;
    private LocalDateTime resultsDate;
    private QuizType quizType;
    private Integer executionAggregateId;
    private Long executionVersion;
    private List<QuizQuestionDto> questions = new ArrayList<>();

    public QuizDto() {
    }

    public QuizDto(Integer aggregateId, Long version, String title, LocalDateTime creationDate,
                   LocalDateTime availableDate, LocalDateTime conclusionDate, LocalDateTime resultsDate,
                   QuizType quizType, Integer executionAggregateId, Long executionVersion,
                   List<QuizQuestionDto> questions) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.title = title;
        this.creationDate = creationDate;
        this.availableDate = availableDate;
        this.conclusionDate = conclusionDate;
        this.resultsDate = resultsDate;
        this.quizType = quizType;
        this.executionAggregateId = executionAggregateId;
        this.executionVersion = executionVersion;
        this.questions = questions;
    }

    public QuizDto(Quiz quiz) {
        this.aggregateId = quiz.getAggregateId();
        this.version = quiz.getVersion();
        this.title = quiz.getTitle();
        this.creationDate = quiz.getCreationDate();
        this.availableDate = quiz.getAvailableDate();
        this.conclusionDate = quiz.getConclusionDate();
        this.resultsDate = quiz.getResultsDate();
        this.quizType = quiz.getQuizType();
        this.executionAggregateId = quiz.getExecution().getExecutionAggregateId();
        this.executionVersion = quiz.getExecution().getExecutionVersion();
        this.questions = quiz.getQuestions().stream()
                .map(QuizQuestionDto::new)
                .collect(Collectors.toList());
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

    public QuizType getQuizType() {
        return quizType;
    }

    public void setQuizType(QuizType quizType) {
        this.quizType = quizType;
    }

    public Integer getExecutionAggregateId() {
        return executionAggregateId;
    }

    public void setExecutionAggregateId(Integer executionAggregateId) {
        this.executionAggregateId = executionAggregateId;
    }

    public Long getExecutionVersion() {
        return executionVersion;
    }

    public void setExecutionVersion(Long executionVersion) {
        this.executionVersion = executionVersion;
    }

    public List<QuizQuestionDto> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuizQuestionDto> questions) {
        this.questions = questions;
    }
}
