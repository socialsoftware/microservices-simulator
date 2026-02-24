package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.command.tournament.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;

import java.util.logging.Logger;

@Component
public class TournamentCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(TournamentCommandHandler.class.getName());

    @Autowired
    private TournamentService tournamentService;

    @Override
    protected String getAggregateTypeName() {
        return "Tournament";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateTournamentCommand cmd -> handleCreateTournament(cmd);
            case GetTournamentByIdCommand cmd -> handleGetTournamentById(cmd);
            case GetAllTournamentsCommand cmd -> handleGetAllTournaments(cmd);
            case UpdateTournamentCommand cmd -> handleUpdateTournament(cmd);
            case DeleteTournamentCommand cmd -> handleDeleteTournament(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateTournament(CreateTournamentCommand cmd) {
        logger.info("handleCreateTournament");
        try {
            return tournamentService.createTournament(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetTournamentById(GetTournamentByIdCommand cmd) {
        logger.info("handleGetTournamentById");
        try {
            return tournamentService.getTournamentById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllTournaments(GetAllTournamentsCommand cmd) {
        logger.info("handleGetAllTournaments");
        try {
            return tournamentService.getAllTournaments(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateTournament(UpdateTournamentCommand cmd) {
        logger.info("handleUpdateTournament");
        try {
            return tournamentService.updateTournament(cmd.getTournamentDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteTournament(DeleteTournamentCommand cmd) {
        logger.info("handleDeleteTournament");
        try {
            tournamentService.deleteTournament(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
