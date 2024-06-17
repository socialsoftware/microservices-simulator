package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.events.publish.QuizAnswerQuestionAnswerEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentParticipant;

public class TournamentSubscribesAnswerQuestion extends EventSubscription {
    Integer studentAggregateId;
    public TournamentSubscribesAnswerQuestion(TournamentParticipant tournamentParticipant) {
        super(tournamentParticipant.getParticipantAnswer().getQuizAnswerAggregateId(),
                tournamentParticipant.getParticipantAnswer().getQuizAnswerVersion(),
                QuizAnswerQuestionAnswerEvent.class.getSimpleName());
        this.studentAggregateId = tournamentParticipant.getParticipantAggregateId();
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event) && checkAnswerInfo((QuizAnswerQuestionAnswerEvent)event);
    }

    private boolean checkAnswerInfo(QuizAnswerQuestionAnswerEvent quizAnswerQuestionAnswerEvent) {
        return studentAggregateId.equals(quizAnswerQuestionAnswerEvent.getStudentAggregateId());
    }

}