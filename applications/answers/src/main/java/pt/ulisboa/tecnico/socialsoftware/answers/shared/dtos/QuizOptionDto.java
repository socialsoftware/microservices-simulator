package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizOption;

public class QuizOptionDto implements Serializable {
    private String content;
    private Integer aggregateId;
    private Integer version;
    private AggregateState state;

    public QuizOptionDto() {
    }

    public QuizOptionDto(QuizOption quizOption) {
        this.content = quizOption.getOptionContent();
        this.aggregateId = quizOption.getQuestionAggregateId();
        this.version = quizOption.getQuestionVersion();
        this.state = quizOption.getQuestionState();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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
}