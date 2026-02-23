package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing.QuizEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizRepository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish.TopicDeletedEvent;

public class TopicDeletedEventHandler extends QuizEventHandler {
    public TopicDeletedEventHandler(QuizRepository quizRepository, QuizEventProcessing quizEventProcessing) {
        super(quizRepository, quizEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.quizEventProcessing.processTopicDeletedEvent(subscriberAggregateId, (TopicDeletedEvent) event);
    }
}
