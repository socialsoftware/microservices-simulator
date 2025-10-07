package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import java.time.LocalDateTime;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class AnswerDto implements Serializable {
    
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;
    private LocalDateTime creationDate;
    private LocalDateTime answerDate;
    private Boolean completed;
    private Integer userAggregateId;
    private String userName;
    private Integer quizAggregateId;
    
    public AnswerDto() {
    }
    
    public AnswerDto(Integer aggregateId, Integer version, AggregateState state, LocalDateTime creationDate, LocalDateTime answerDate, Boolean completed, Integer userAggregateId, String userName, Integer quizAggregateId) {
        setAggregateId(aggregateId);
        setVersion(version);
        setState(state);
        setCreationDate(creationDate);
        setAnswerDate(answerDate);
        setCompleted(completed);
        setUserAggregateId(userAggregateId);
        setUserName(userName);
        setQuizAggregateId(quizAggregateId);
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

    public Integer getQuizAggregateId() {
        return quizAggregateId;
    }
    
    public void setQuizAggregateId(Integer quizAggregateId) {
        this.quizAggregateId = quizAggregateId;
    }
}