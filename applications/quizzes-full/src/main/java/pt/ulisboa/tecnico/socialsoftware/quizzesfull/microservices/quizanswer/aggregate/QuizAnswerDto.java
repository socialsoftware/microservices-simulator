package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class QuizAnswerDto implements Serializable {
    private Integer aggregateId;
    private Long version;
    private AggregateState state;
    private Integer quizAggregateId;
    private Long quizVersion;
    private Integer userAggregateId;
    private Long userVersion;
    private String userName;
    private String userUsername;
    private Integer executionAggregateId;
    private Long executionVersion;
    private LocalDateTime creationDate;
    private LocalDateTime answerDate;
    private Boolean completed;
    private List<Integer> questionAnswerIds = new ArrayList<>();

    public QuizAnswerDto() {}

    public QuizAnswerDto(QuizAnswer quizAnswer) {
        setAggregateId(quizAnswer.getAggregateId());
        setVersion(quizAnswer.getVersion());
        setState(quizAnswer.getState());
        setQuizAggregateId(quizAnswer.getQuizAggregateId());
        setQuizVersion(quizAnswer.getQuizVersion());
        setUserAggregateId(quizAnswer.getUserAggregateId());
        setUserVersion(quizAnswer.getUserVersion());
        setUserName(quizAnswer.getUserName());
        setUserUsername(quizAnswer.getUserUsername());
        setExecutionAggregateId(quizAnswer.getExecutionAggregateId());
        setExecutionVersion(quizAnswer.getExecutionVersion());
        setCreationDate(quizAnswer.getCreationDate());
        setAnswerDate(quizAnswer.getAnswerDate());
        setCompleted(quizAnswer.getCompleted());
        for (QuestionAnswer qa : quizAnswer.getQuestionAnswers()) {
            this.questionAnswerIds.add(qa.getQuestionAggregateId());
        }
    }

    public QuizAnswerDto(Integer aggregateId, Long version, AggregateState state,
                         Integer quizAggregateId, Long quizVersion,
                         Integer userAggregateId, Long userVersion, String userName, String userUsername,
                         Integer executionAggregateId, Long executionVersion,
                         LocalDateTime creationDate, LocalDateTime answerDate, Boolean completed) {
        this.aggregateId = aggregateId;
        this.version = version;
        this.state = state;
        this.quizAggregateId = quizAggregateId;
        this.quizVersion = quizVersion;
        this.userAggregateId = userAggregateId;
        this.userVersion = userVersion;
        this.userName = userName;
        this.userUsername = userUsername;
        this.executionAggregateId = executionAggregateId;
        this.executionVersion = executionVersion;
        this.creationDate = creationDate;
        this.answerDate = answerDate;
        this.completed = completed;
    }

    public Integer getAggregateId() { return aggregateId; }
    public void setAggregateId(Integer aggregateId) { this.aggregateId = aggregateId; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public AggregateState getState() { return state; }
    public void setState(AggregateState state) { this.state = state; }

    public Integer getQuizAggregateId() { return quizAggregateId; }
    public void setQuizAggregateId(Integer quizAggregateId) { this.quizAggregateId = quizAggregateId; }

    public Long getQuizVersion() { return quizVersion; }
    public void setQuizVersion(Long quizVersion) { this.quizVersion = quizVersion; }

    public Integer getUserAggregateId() { return userAggregateId; }
    public void setUserAggregateId(Integer userAggregateId) { this.userAggregateId = userAggregateId; }

    public Long getUserVersion() { return userVersion; }
    public void setUserVersion(Long userVersion) { this.userVersion = userVersion; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public String getUserUsername() { return userUsername; }
    public void setUserUsername(String userUsername) { this.userUsername = userUsername; }

    public Integer getExecutionAggregateId() { return executionAggregateId; }
    public void setExecutionAggregateId(Integer executionAggregateId) { this.executionAggregateId = executionAggregateId; }

    public Long getExecutionVersion() { return executionVersion; }
    public void setExecutionVersion(Long executionVersion) { this.executionVersion = executionVersion; }

    public LocalDateTime getCreationDate() { return creationDate; }
    public void setCreationDate(LocalDateTime creationDate) { this.creationDate = creationDate; }

    public LocalDateTime getAnswerDate() { return answerDate; }
    public void setAnswerDate(LocalDateTime answerDate) { this.answerDate = answerDate; }

    public Boolean getCompleted() { return completed; }
    public void setCompleted(Boolean completed) { this.completed = completed; }

    public List<Integer> getQuestionAnswerIds() { return questionAnswerIds; }
    public void setQuestionAnswerIds(List<Integer> questionAnswerIds) { this.questionAnswerIds = questionAnswerIds; }
}
