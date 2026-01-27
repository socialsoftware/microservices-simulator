package pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizQuestion;

public class QuizQuestionDto implements Serializable {
    private AggregateState state;
    private Integer questionSequence;
    private String title;
    private String content;
    private Integer aggregateId;
    private Integer version;

    public QuizQuestionDto() {
    }

    public QuizQuestionDto(QuizQuestion quizQuestion) {
        this.state = quizQuestion.getQuestionState();
        this.questionSequence = quizQuestion.getQuestionSequence();
        this.title = quizQuestion.getQuestionTitle();
        this.content = quizQuestion.getQuestionContent();
        this.aggregateId = quizQuestion.getQuestionAggregateId();
        this.version = quizQuestion.getQuestionVersion();
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
    }

    public Integer getQuestionSequence() {
        return questionSequence;
    }

    public void setQuestionSequence(Integer questionSequence) {
        this.questionSequence = questionSequence;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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
}