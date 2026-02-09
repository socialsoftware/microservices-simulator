package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate.QuizQuestion;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionDeletedEvent;


public class QuizSubscribesQuestionDeletedQuizQuestionsExist extends EventSubscription {
    public QuizSubscribesQuestionDeletedQuizQuestionsExist(QuizQuestion questions) {
        super(questions.getQuestionAggregateId(),
                questions.getQuestionVersion(),
                QuestionDeletedEvent.class);
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
