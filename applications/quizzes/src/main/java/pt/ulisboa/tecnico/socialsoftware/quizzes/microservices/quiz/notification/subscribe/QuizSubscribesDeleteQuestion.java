package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizQuestion;

public class QuizSubscribesDeleteQuestion extends EventSubscription {
    public QuizSubscribesDeleteQuestion(QuizQuestion quizQuestion) {
        super(quizQuestion.getQuestionAggregateId(),
                quizQuestion.getQuestionVersion(),
                DeleteQuestionEvent.class.getSimpleName());
    }

    public QuizSubscribesDeleteQuestion() {}

    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}