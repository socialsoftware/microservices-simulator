package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.events.publish.AnonymizeStudentEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;

public class TournamentSubscribesAnonymizeStudent extends EventSubscription {
    private TournamentDto tournamentDto;

    public TournamentSubscribesAnonymizeStudent(Tournament tournament) {
        super(tournament.getTournamentCourseExecution().getCourseExecutionAggregateId(),
                tournament.getTournamentCourseExecution().getCourseExecutionVersion(),
                AnonymizeStudentEvent.class.getSimpleName());
        tournamentDto = new TournamentDto(tournament);
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event) && checkTournamentInfo((AnonymizeStudentEvent)event);
    }

    private boolean checkTournamentInfo(AnonymizeStudentEvent anonymizeStudentEvent) {
        if (tournamentDto.getCreator().getAggregateId().equals(anonymizeStudentEvent.getStudentAggregateId())) {
            return true;
        }

        for (UserDto tournamentParticipant: tournamentDto.getParticipants()) {
            if (tournamentParticipant.getAggregateId().equals(anonymizeStudentEvent.getStudentAggregateId())) {
                return true;
            }
        }

        return false;
    }
}