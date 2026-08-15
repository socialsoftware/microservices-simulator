package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class UpdateQuestionEvent extends Event {
    private Integer questionAggregateId;
    private String title;
    private String content;

    protected UpdateQuestionEvent() {}

    public UpdateQuestionEvent(Integer questionAggregateId, String title, String content) {
        super(questionAggregateId);
        this.questionAggregateId = questionAggregateId;
        this.title = title;
        this.content = content;
    }

    public Integer getQuestionAggregateId() {
        return questionAggregateId;
    }

    public String getTitle() {
        return title;
    }

    public String getContent() {
        return content;
    }
}
