package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.notification.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.eventProcessing.QuestionEventProcessing;

public class UpdateTopicEventHandler extends QuestionEventHandler {
    public UpdateTopicEventHandler(QuestionRepository questionRepository, QuestionEventProcessing questionEventProcessing) {
        super(questionRepository, questionEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.questionEventProcessing.processUpdateTopic(subscriberAggregateId, (UpdateTopicEvent) event);
    }
}
