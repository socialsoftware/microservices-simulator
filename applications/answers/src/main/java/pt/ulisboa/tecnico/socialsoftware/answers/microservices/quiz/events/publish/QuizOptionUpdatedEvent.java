package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Entity
public class QuizOptionUpdatedEvent extends Event {
    private Integer questionAggregateId;
    private Integer questionVersion;
    private Integer optionSequence;
    private Boolean optionCorrect;
    private String optionContent;

    public QuizOptionUpdatedEvent() {
        super();
    }

    public QuizOptionUpdatedEvent(Integer aggregateId, Integer questionAggregateId, Integer questionVersion, Integer optionSequence, Boolean optionCorrect, String optionContent) {
        super(aggregateId);
        setQuestionAggregateId(questionAggregateId);
        setQuestionVersion(questionVersion);
        setOptionSequence(optionSequence);
        setOptionCorrect(optionCorrect);
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

    public Integer getOptionSequence() {
        return optionSequence;
    }

    public void setOptionSequence(Integer optionSequence) {
        this.optionSequence = optionSequence;
    }

    public Boolean getOptionCorrect() {
        return optionCorrect;
    }

    public void setOptionCorrect(Boolean optionCorrect) {
        this.optionCorrect = optionCorrect;
    }

    public String getOptionContent() {
        return optionContent;
    }

    public void setOptionContent(String optionContent) {
        this.optionContent = optionContent;
    }

}