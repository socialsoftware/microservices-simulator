package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.QuizEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.TopicUpdatedEvent;

public class TopicUpdatedEventHandler extends QuizEventHandler {
    public TopicUpdatedEventHandler(QuizRepository quizRepository, QuizEventProcessing quizEventProcessing) {
        super(quizRepository, quizEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.quizEventProcessing.processTopicUpdatedEvent(subscriberAggregateId, (TopicUpdatedEvent) event);
    }
}
