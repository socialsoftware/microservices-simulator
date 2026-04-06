package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import jakarta.persistence.Column;

@Entity
public class QuizQuestionUpdatedEvent extends Event {
    @Column(name = "quiz_question_updated_event_question_aggregate_id")
    private Integer questionAggregateId;
    @Column(name = "quiz_question_updated_event_question_version")
    private Integer questionVersion;
    @Column(name = "quiz_question_updated_event_question_title")
    private String questionTitle;
    @Column(name = "quiz_question_updated_event_question_content")
    private String questionContent;
    @Column(name = "quiz_question_updated_event_question_sequence")
    private Integer questionSequence;

    public QuizQuestionUpdatedEvent() {
        super();
    }

    public QuizQuestionUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public QuizQuestionUpdatedEvent(Integer aggregateId, Integer questionAggregateId, Integer questionVersion, String questionTitle, String questionContent, Integer questionSequence) {
        super(aggregateId);
        setQuestionAggregateId(questionAggregateId);
        setQuestionVersion(questionVersion);
        setQuestionTitle(questionTitle);
        setQuestionContent(questionContent);
        setQuestionSequence(questionSequence);
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public void setQuestionAggregateId(Integer questionAggregateId) {
        this.questionAggregateId = questionAggregateId;
    }

    public Integer getQuestionVersion() {
        return questionVersion;
    }

    public void setQuestionVersion(Integer questionVersion) {
        this.questionVersion = questionVersion;
    }

    public String getQuestionTitle() {
        return questionTitle;
    }

    public void setQuestionTitle(String questionTitle) {
        this.questionTitle = questionTitle;
    }

    public String getQuestionContent() {
        return questionContent;
    }

    public void setQuestionContent(String questionContent) {
        this.questionContent = questionContent;
    }

    public Integer getQuestionSequence() {
        return questionSequence;
    }

    public void setQuestionSequence(Integer questionSequence) {
        this.questionSequence = questionSequence;
    }

}