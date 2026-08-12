package pt.ulisboa.tecnico.socialsoftware.answers.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class QuizOptionUpdatedEvent extends Event {
    private Integer questionAggregateId;
    private Integer questionVersion;
    private String optionContent;

    public QuizOptionUpdatedEvent() {
        super();
    }

    public QuizOptionUpdatedEvent(Integer aggregateId) {
        super(aggregateId);
    }

    public QuizOptionUpdatedEvent(Integer aggregateId, Integer questionAggregateId, Integer questionVersion, String optionContent) {
        super(aggregateId);
        setQuestionAggregateId(questionAggregateId);
        setQuestionVersion(questionVersion);
        setOptionContent(optionContent);
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

    public String getOptionContent() {
        return optionContent;
    }

    public void setOptionContent(String optionContent) {
        this.optionContent = optionContent;
    }

}