package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.quizzes.events.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;

public class TournamentSubscribesUpdateStudentName extends EventSubscription {
    private TournamentDto tournamentDto;

    public TournamentSubscribesUpdateStudentName(Tournament tournament) {
        super(tournament.getTournamentCourseExecution().getCourseExecutionAggregateId(),
                tournament.getTournamentCourseExecution().getCourseExecutionVersion(),
                UpdateStudentNameEvent.class.getSimpleName());
        tournamentDto = new TournamentDto(tournament);
    }

    public TournamentSubscribesUpdateStudentName() {}

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event) && checkTournamentInfo((UpdateStudentNameEvent)event);
    }

    private boolean checkTournamentInfo(UpdateStudentNameEvent updateStudentNameEvent) {
        return tournamentDto.getCreator().getAggregateId().equals(updateStudentNameEvent.getStudentAggregateId()) ||
                tournamentDto.getParticipants().stream()
                        .anyMatch(participant -> participant.getAggregateId().equals(updateStudentNameEvent.getStudentAggregateId()));
//        if (tournamentDto.getCreator().getAggregateId().equals(updateStudentNameEvent.getStudentAggregateId())) {
//            return true;
//        }
//
//        for (UserDto tournamentParticipant : tournamentDto.getParticipants()) {
//            if (tournamentParticipant.getAggregateId().equals(updateStudentNameEvent.getStudentAggregateId())) {
//                return true;
//            }
//        }

//        return false;
    }

}