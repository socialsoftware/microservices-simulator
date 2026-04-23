package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.events.QuizDeletedEvent;


public class TournamentSubscribesQuizDeletedQuizRef extends EventSubscription {
    public TournamentSubscribesQuizDeletedQuizRef(TournamentQuiz quiz) {
        super(quiz.getQuizAggregateId(),
                quiz.getQuizVersion(),
                QuizDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
