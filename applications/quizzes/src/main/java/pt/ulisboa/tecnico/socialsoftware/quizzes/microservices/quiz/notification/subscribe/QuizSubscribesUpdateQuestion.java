package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizQuestion;

public class QuizSubscribesUpdateQuestion extends EventSubscription {
    public QuizSubscribesUpdateQuestion(QuizQuestion quizQuestion) {
        super(quizQuestion.getQuestionAggregateId(),
                quizQuestion.getQuestionVersion(),
                UpdateQuestionEvent.class.getSimpleName());
    }

    public QuizSubscribesUpdateQuestion() {}

    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}