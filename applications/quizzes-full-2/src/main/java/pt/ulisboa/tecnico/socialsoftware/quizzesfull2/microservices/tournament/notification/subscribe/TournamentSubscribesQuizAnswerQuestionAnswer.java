package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.notification.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentParticipant;

// Anchored on the quiz the tournament generated - the event's publisher id - while the version cursor
// is the participant's own quizAnswerVersion, so folding an answer in never advances the quiz snapshot
// past a pending InvalidateQuizEvent, which shares that anchor.
public class TournamentSubscribesQuizAnswerQuestionAnswer extends EventSubscription {
    public TournamentSubscribesQuizAnswerQuestionAnswer(Integer quizAggregateId,
                                                        TournamentParticipant participant) {
        super(quizAggregateId, participant.getQuizAnswer().getQuizAnswerVersion(),
                QuizAnswerQuestionAnswerEvent.class.getSimpleName());
    }

    public TournamentSubscribesQuizAnswerQuestionAnswer() {
    }
}
