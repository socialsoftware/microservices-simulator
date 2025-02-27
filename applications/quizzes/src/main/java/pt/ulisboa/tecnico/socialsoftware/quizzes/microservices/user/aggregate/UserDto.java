package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate;

import java.io.Serializable;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

public class UserDto implements Serializable {
    private Integer aggregateId;
    private String name;
    private String username;
    private String role;
    private boolean active;
    private Integer version;
    private Integer answerAggregateId;
    private Integer numberAnswered;
    private Integer numberCorrect;
    private AggregateState state;

    public UserDto() {
    }

    public UserDto(User user) {
        this.aggregateId = user.getAggregateId();
        this.name = user.getName();
        setRole(user.getRole().toString());
        this.username = user.getUsername();
        this.version = user.getVersion();
        setActive(user.isActive());
        setState(user.getState());
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Integer getNumberAnswered() {
        return numberAnswered;
    }

    public Integer getAnswerAggregateId() {
        return answerAggregateId;
    }

    public void setAnswerAggregateId(Integer answerAggregateId) {
        this.answerAggregateId = answerAggregateId;
    }

    public void setNumberAnswered(Integer numberAnswered) {
        this.numberAnswered = numberAnswered;
    }

    public Integer getNumberCorrect() {
        return numberCorrect;
    }

    public void setNumberCorrect(Integer numberCorrect) {
        this.numberCorrect = numberCorrect;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState aggregateState) {
        this.state = aggregateState;
    }
}
