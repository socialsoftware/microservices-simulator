package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.eventProcessing.QuestionEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.DeleteTopicEvent;

public class DeleteTopicEventHandler extends QuestionEventHandler {
    public DeleteTopicEventHandler(QuestionRepository questionRepository, QuestionEventProcessing questionEventProcessing) {
        super(questionRepository, questionEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.questionEventProcessing.processDeleteTopic(subscriberAggregateId, (DeleteTopicEvent) event);
    }
}
