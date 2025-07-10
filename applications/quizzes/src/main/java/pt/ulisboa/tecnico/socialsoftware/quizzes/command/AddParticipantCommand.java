package pt.ulisboa.tecnico.socialsoftware.quizzes.command;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

public class AddParticipantCommand implements Command {
    private final UnitOfWork unitOfWork;
    private TournamentService tournamentService;
    private Integer tournamentAggregateId;
    private TournamentParticipant participant;

    public AddParticipantCommand(UnitOfWork unitOfWork, TournamentService tournamentService, Integer tournamentAggregateId, TournamentParticipant participant) {
        this.unitOfWork = unitOfWork;
        this.tournamentService = tournamentService;
        this.tournamentAggregateId = tournamentAggregateId;
        this.participant = participant;
    }

//    @Override
//    public void execute() {
//        tournamentService.addParticipant(tournamentAggregateId, participant, unitOfWork);
//    }
}
