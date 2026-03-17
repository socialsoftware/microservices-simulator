package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.CreateQuizAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.Quiz;

public class QuizSubscribesCreateQuizAnswer extends EventSubscription {
    public QuizSubscribesCreateQuizAnswer(Quiz quiz) {
        super(quiz.getAggregateId(), quiz.getVersion(), CreateQuizAnswerEvent.class.getSimpleName());
    }

    public QuizSubscribesCreateQuizAnswer() {}

    @Override
    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
