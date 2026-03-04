package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerUser;

public class AnswerUserDto implements Serializable {
    private Integer aggregateId;
    private String name;
    private Integer version;
    private String state;

    public AnswerUserDto() {
    }

    public AnswerUserDto(AnswerUser answerUser) {
        this.aggregateId = answerUser.getUserAggregateId();
        this.name = answerUser.getUserName();
        this.version = answerUser.getUserVersion();
        this.state = answerUser.getUserState() != null ? answerUser.getUserState().name() : null;
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

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}