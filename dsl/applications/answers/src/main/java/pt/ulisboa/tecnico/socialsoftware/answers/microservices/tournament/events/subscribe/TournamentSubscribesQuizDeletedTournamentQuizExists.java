package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentQuiz;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizDeletedEvent;


public class TournamentSubscribesQuizDeletedTournamentQuizExists extends EventSubscription {
    public TournamentSubscribesQuizDeletedTournamentQuizExists(TournamentQuiz quiz) {
        super(quiz.getQuizAggregateId(),
                quiz.getQuizVersion(),
                QuizDeletedEvent.class);
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
