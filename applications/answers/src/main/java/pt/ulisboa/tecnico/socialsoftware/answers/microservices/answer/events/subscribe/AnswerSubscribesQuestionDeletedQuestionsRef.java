package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.aggregate.AnswerQuestion;
import pt.ulisboa.tecnico.socialsoftware.answers.events.QuestionDeletedEvent;


public class AnswerSubscribesQuestionDeletedQuestionsRef extends EventSubscription {
    public AnswerSubscribesQuestionDeletedQuestionsRef(AnswerQuestion questions) {
        super(questions.getQuestionAggregateId(),
                questions.getQuestionVersion(),
                QuestionDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
