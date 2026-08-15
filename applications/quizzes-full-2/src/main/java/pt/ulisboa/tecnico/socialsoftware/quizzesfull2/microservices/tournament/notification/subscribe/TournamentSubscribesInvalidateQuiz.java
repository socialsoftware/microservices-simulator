package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.InvalidateQuizEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentQuiz;

public class TournamentSubscribesInvalidateQuiz extends EventSubscription {
    public TournamentSubscribesInvalidateQuiz(TournamentQuiz quiz) {
        super(quiz.getQuizAggregateId(), quiz.getQuizVersion(),
                InvalidateQuizEvent.class.getSimpleName());
    }

    public TournamentSubscribesInvalidateQuiz() {
    }
}
