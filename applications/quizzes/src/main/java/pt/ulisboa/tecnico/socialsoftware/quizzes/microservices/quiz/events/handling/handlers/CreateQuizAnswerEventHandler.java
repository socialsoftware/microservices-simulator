package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.CreateQuizAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.eventProcessing.QuizEventProcessing;

public class CreateQuizAnswerEventHandler extends QuizEventHandler {
    public CreateQuizAnswerEventHandler(QuizRepository quizRepository,
            QuizEventProcessing quizEventProcessing) {
        super(quizRepository, quizEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.quizEventProcessing.processCreateQuizAnswerEvent(subscriberAggregateId, (CreateQuizAnswerEvent) event);
    }
}
