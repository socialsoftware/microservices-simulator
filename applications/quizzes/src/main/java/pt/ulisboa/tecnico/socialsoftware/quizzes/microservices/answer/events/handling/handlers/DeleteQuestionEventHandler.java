package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.eventProcessing.QuizAnswerEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.aggregate.QuizAnswerRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.events.publish.DeleteQuestionEvent;

public class DeleteQuestionEventHandler extends QuizAnswerEventHandler {
    public DeleteQuestionEventHandler(QuizAnswerRepository quizAnswerRepository, QuizAnswerEventProcessing quizAnswerEventProcessing) {
        super(quizAnswerRepository, quizAnswerEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.quizAnswerEventProcessing.processDeleteQuestionEvent(subscriberAggregateId, (DeleteQuestionEvent) event);
    }
}
