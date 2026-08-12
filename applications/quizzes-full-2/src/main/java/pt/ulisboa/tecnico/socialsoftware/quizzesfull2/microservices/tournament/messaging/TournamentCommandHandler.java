package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.AddParticipantCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.CancelTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.CreateTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.DeleteTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.GetClosedTournamentsForExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.GetOpenedTournamentsForExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.GetTournamentsForExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.tournament.UpdateTournamentCommand;
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
            case CreateTournamentCommand cmd -> handleCreateTournament(cmd);
            case AddParticipantCommand cmd -> {
                handleAddParticipant(cmd);
                yield null;
            }
            case UpdateTournamentCommand cmd -> {
                handleUpdateTournament(cmd);
                yield null;
            }
            case CancelTournamentCommand cmd -> {
                handleCancelTournament(cmd);
                yield null;
            }
            case DeleteTournamentCommand cmd -> {
                handleDeleteTournament(cmd);
                yield null;
            }
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

    private Object handleCreateTournament(CreateTournamentCommand command) {
        return tournamentService.createTournament(command.getExecutionDto(), command.getCreatorDto(),
                command.getTopics(), command.getSelectedQuestions(), command.getQuizAggregateId(),
                command.getQuizVersion(), command.getStartTime(), command.getEndTime(),
                command.getNumberOfQuestions(), command.getUnitOfWork());
    }

    private void handleAddParticipant(AddParticipantCommand command) {
        tournamentService.addParticipant(command.getTournamentAggregateId(), command.getUserDto(),
                command.getExecutionDto(), command.getUnitOfWork());
    }

    private void handleUpdateTournament(UpdateTournamentCommand command) {
        tournamentService.updateTournament(command.getTournamentAggregateId(), command.getStartTime(),
                command.getEndTime(), command.getNumberOfQuestions(), command.getTopics(),
                command.getSelectedQuestions(), command.getUnitOfWork());
    }

    private void handleCancelTournament(CancelTournamentCommand command) {
        tournamentService.cancelTournament(command.getTournamentAggregateId(), command.getUnitOfWork());
    }

    private void handleDeleteTournament(DeleteTournamentCommand command) {
        tournamentService.deleteTournament(command.getTournamentAggregateId(), command.getUnitOfWork());
    }
}
