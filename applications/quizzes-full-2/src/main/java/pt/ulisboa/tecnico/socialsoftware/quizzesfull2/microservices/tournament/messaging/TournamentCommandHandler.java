package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.GetClosedTournamentsForExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.GetOpenedTournamentsForExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.GetTournamentsForExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.service.TournamentService;

import java.util.logging.Logger;

@Component
public class TournamentCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(TournamentCommandHandler.class.getName());

    @Autowired
    private TournamentService tournamentService;

    @Override
    public String getAggregateTypeName() {
        return "Tournament";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetTournamentByIdCommand cmd -> handleGetTournamentById(cmd);
            case GetTournamentsForExecutionCommand cmd -> handleGetTournamentsForExecution(cmd);
            case GetOpenedTournamentsForExecutionCommand cmd -> handleGetOpenedTournamentsForExecution(cmd);
            case GetClosedTournamentsForExecutionCommand cmd -> handleGetClosedTournamentsForExecution(cmd);
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetTournamentById(GetTournamentByIdCommand command) {
        return tournamentService.getTournamentById(command.getTournamentAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetTournamentsForExecution(GetTournamentsForExecutionCommand command) {
        return tournamentService.getTournamentsForExecution(command.getExecutionAggregateId(),
                command.getUnitOfWork());
    }

    private Object handleGetOpenedTournamentsForExecution(GetOpenedTournamentsForExecutionCommand command) {
        return tournamentService.getOpenedTournamentsForExecution(command.getExecutionAggregateId(),
                command.getUnitOfWork());
    }

    private Object handleGetClosedTournamentsForExecution(GetClosedTournamentsForExecutionCommand command) {
        return tournamentService.getClosedTournamentsForExecution(command.getExecutionAggregateId(),
                command.getUnitOfWork());
    }
}
