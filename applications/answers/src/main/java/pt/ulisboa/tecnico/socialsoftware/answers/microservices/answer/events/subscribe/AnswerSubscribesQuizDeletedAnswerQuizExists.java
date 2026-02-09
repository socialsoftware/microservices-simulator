package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizDeletedEvent;


public class AnswerSubscribesQuizDeletedAnswerQuizExists extends EventSubscription {
    public AnswerSubscribesQuizDeletedAnswerQuizExists(AnswerQuiz quiz) {
        super(quiz.getQuizAggregateId(),
                quiz.getQuizVersion(),
                QuizDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
