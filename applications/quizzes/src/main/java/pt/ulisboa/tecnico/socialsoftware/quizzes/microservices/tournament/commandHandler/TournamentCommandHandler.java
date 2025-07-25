package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.AddParticipantCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.CreateTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;

import java.util.logging.Logger;

@Component
public class TournamentCommandHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(TournamentCommandHandler.class.getName());

    @Autowired
    private TournamentService tournamentService;

    @Override
    public Object handle(Command command) {
        if (command instanceof GetTournamentByIdCommand) {
            return handleGetTournamentById((GetTournamentByIdCommand) command);
        } else if (command instanceof AddParticipantCommand) {
            return handleAddParticipant((AddParticipantCommand) command);
        }

        logger.warning("Unknown command type: " + command.getClass().getName());
        return null;
    }

    private Object handleGetTournamentById(GetTournamentByIdCommand command) {
        logger.info("Getting tournament: " + command.getAggregateId());
        try {
            TournamentDto tournamentDto = tournamentService.getTournamentById(
                    command.getAggregateId(),
                    command.getUnitOfWork());
            return tournamentDto;
        } catch (Exception e) {
            logger.severe("Failed to get tournament: " + e.getMessage());
            return e;
        }
    }

    private Object handleAddParticipant(AddParticipantCommand command) {
        logger.info("Adding participant to tournament: " + command.getTournamentAggregateId());
        try {
            tournamentService.addParticipant(
                    command.getTournamentAggregateId(),
                    command.getParticipant(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to add participant: " + e.getMessage());
            return e;
        }
    }

}
