package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.notification.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.eventProcessing.QuizAnswerEventProcessing;

public class AnonymizeStudentEventHandler extends QuizAnswerEventHandler {
    public AnonymizeStudentEventHandler(QuizAnswerRepository quizAnswerRepository,
            QuizAnswerEventProcessing quizAnswerEventProcessing) {
        super(quizAnswerRepository, quizAnswerEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.quizAnswerEventProcessing.processAnonymizeStudentEvent(subscriberAggregateId,
                (AnonymizeStudentEvent) event);
    }
}
