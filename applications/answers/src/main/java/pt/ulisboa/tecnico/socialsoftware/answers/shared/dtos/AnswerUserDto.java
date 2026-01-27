package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerUser;

public class AnswerUserDto implements Serializable {
    private Integer aggregateId;
    private AggregateState state;
    private String name;

    public AnswerUserDto() {
    }

    public AnswerUserDto(AnswerUser answerUser) {
        this.aggregateId = answerUser.getUserAggregateId();
        this.state = answerUser.getUserState();
        this.name = answerUser.getUserName();
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}