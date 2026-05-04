package pt.ulisboa.tecnico.socialsoftware.quizzesfull.events;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;

@Entity
public class UpdateQuestionEvent extends Event {

    private String title;
    private String content;

    public UpdateQuestionEvent() {
        super();
    }

    public UpdateQuestionEvent(Integer questionAggregateId, String title, String content) {
        super(questionAggregateId);
        this.title = title;
        this.content = content;
    }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
