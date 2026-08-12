package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizOption;

public class QuizOptionDto implements Serializable {
    private String content;
    private Integer aggregateId;
    private Integer version;
    private String state;

    public QuizOptionDto() {
    }

    public QuizOptionDto(QuizOption quizOption) {
        this.content = quizOption.getOptionContent();
        this.aggregateId = quizOption.getQuestionAggregateId();
        this.version = quizOption.getQuestionVersion();
        this.state = quizOption.getQuestionState() != null ? quizOption.getQuestionState().name() : null;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}