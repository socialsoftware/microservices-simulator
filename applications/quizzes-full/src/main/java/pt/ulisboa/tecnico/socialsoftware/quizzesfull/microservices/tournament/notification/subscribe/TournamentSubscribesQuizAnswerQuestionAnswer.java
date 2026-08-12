package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.events.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentParticipantQuizAnswer;

public class TournamentSubscribesQuizAnswerQuestionAnswer extends EventSubscription {

    public TournamentSubscribesQuizAnswerQuestionAnswer(TournamentParticipantQuizAnswer quizAnswer) {
        super(quizAnswer.getQuizAnswerAggregateId(), quizAnswer.getQuizAnswerVersion(),
                QuizAnswerQuestionAnswerEvent.class.getSimpleName());
    }

    public TournamentSubscribesQuizAnswerQuestionAnswer() {}
}
