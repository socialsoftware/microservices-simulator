package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.events.publish;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

import jakarta.persistence.Entity;

@Entity
public class DeleteQuestionEvent extends Event {
    public DeleteQuestionEvent() {
        super();
    }

    public DeleteQuestionEvent(Integer questionAggregateId) {
        super(questionAggregateId);
    }
}
