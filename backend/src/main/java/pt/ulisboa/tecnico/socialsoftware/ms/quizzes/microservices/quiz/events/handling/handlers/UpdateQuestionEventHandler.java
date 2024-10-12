package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.eventProcessing.QuizEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.question.events.publish.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizRepository;

public class UpdateQuestionEventHandler extends QuizEventHandler {
    public UpdateQuestionEventHandler(QuizRepository quizRepository, QuizEventProcessing quizEventProcessing) {
        super(quizRepository, quizEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.quizEventProcessing.processUpdateQuestionEvent(subscriberAggregateId, (UpdateQuestionEvent) event);
    }
}
