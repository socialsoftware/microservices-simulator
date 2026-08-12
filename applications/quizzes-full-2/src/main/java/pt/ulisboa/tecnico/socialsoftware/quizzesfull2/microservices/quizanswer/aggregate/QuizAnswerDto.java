package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class QuizAnswerDto {
    private Integer aggregateId;
    private Long version;
    private LocalDateTime creationDate;
    private LocalDateTime answerDate;
    private Boolean completed;
    private Integer quizAggregateId;
    private Long quizVersion;
    private Integer userAggregateId;
    private String userName;
    private Long userVersion;
    private Integer executionAggregateId;
    private Long executionVersion;
    private List<QuestionAnswerDto> questionAnswers = new ArrayList<>();

    public QuizAnswerDto() {
    }

    public QuizAnswerDto(Integer aggregateId, Long version, LocalDateTime creationDate, LocalDateTime answerDate,
                         Boolean completed, Integer quizAggregateId, Long quizVersion, Integer userAggregateId,
                         String userName, Long userVersion, Integer executionAggregateId, Long executionVersion,
                         List<QuestionAnswerDto> questionAnswers) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.creationDate = creationDate;
        this.answerDate = answerDate;
        this.completed = completed;
        this.quizAggregateId = quizAggregateId;
        this.quizVersion = quizVersion;
        this.userAggregateId = userAggregateId;
        this.userName = userName;
        this.userVersion = userVersion;
        this.executionAggregateId = executionAggregateId;
        this.executionVersion = executionVersion;
        this.questionAnswers = questionAnswers;
    }

    public QuizAnswerDto(QuizAnswer quizAnswer) {
        this.aggregateId = quizAnswer.getAggregateId();
        this.version = quizAnswer.getVersion();
        this.creationDate = quizAnswer.getCreationDate();
        this.answerDate = quizAnswer.getAnswerDate();
        this.completed = quizAnswer.getCompleted();
        this.quizAggregateId = quizAnswer.getQuiz().getQuizAggregateId();
        this.quizVersion = quizAnswer.getQuiz().getQuizVersion();
        this.userAggregateId = quizAnswer.getStudent().getUserAggregateId();
        this.userName = quizAnswer.getStudent().getUserName();
        this.userVersion = quizAnswer.getStudent().getUserVersion();
        this.executionAggregateId = quizAnswer.getExecution().getExecutionAggregateId();
        this.executionVersion = quizAnswer.getExecution().getExecutionVersion();
        this.questionAnswers = quizAnswer.getQuestionAnswers().stream()
                .map(QuestionAnswerDto::new)
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

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }

    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }

    public Long getQuizVersion() {
        return quizVersion;
    }

    public void setQuizVersion(Long quizVersion) {
        this.quizVersion = quizVersion;
    }

    public Integer getUserAggregateId() {
        return userAggregateId;
    }

    public void setUserAggregateId(Integer userAggregateId) {
        this.userAggregateId = userAggregateId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public Long getUserVersion() {
        return userVersion;
    }

    public void setUserVersion(Long userVersion) {
        this.userVersion = userVersion;
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

    public List<QuestionAnswerDto> getQuestionAnswers() {
        return questionAnswers;
    }

    public void setQuestionAnswers(List<QuestionAnswerDto> questionAnswers) {
        this.questionAnswers = questionAnswers;
    }
}
