package pt.ulisboa.tecnico.socialsoftware.blcm.question.event.publish;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.event.Event;

@Entity
public class UpdateQuestionEvent extends Event {
    private String title;
    private String content;

    public UpdateQuestionEvent() {
        super();
    }
    public UpdateQuestionEvent(Integer questionAggregateId, String questionTitle, String questionContent) {
        super(questionAggregateId);
        setTitle(questionTitle);
        setContent(questionContent);
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
}
