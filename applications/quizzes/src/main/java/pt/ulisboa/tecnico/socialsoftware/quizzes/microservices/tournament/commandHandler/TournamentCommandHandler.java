package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.AddParticipantCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.CancelTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.CreateTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetTournamentByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetClosedTournamentsForCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetOpenedTournamentsForCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.GetTournamentsByCourseExecutionIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.LeaveTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.RemoveTournamentCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.SolveQuizCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.UpdateTournamentCommand;
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
        } else if (command instanceof CreateTournamentCommand) {
            return handleCreateTournament((CreateTournamentCommand) command);
        } else if (command instanceof GetTournamentsByCourseExecutionIdCommand) {
            return handleGetTournamentsByCourseExecutionId((GetTournamentsByCourseExecutionIdCommand) command);
        } else if (command instanceof GetOpenedTournamentsForCourseExecutionCommand) {
            return handleGetOpenedTournamentsForCourseExecution(
                    (GetOpenedTournamentsForCourseExecutionCommand) command);
        } else if (command instanceof GetClosedTournamentsForCourseExecutionCommand) {
            return handleGetClosedTournamentsForCourseExecution(
                    (GetClosedTournamentsForCourseExecutionCommand) command);
        } else if (command instanceof LeaveTournamentCommand) {
            return handleLeaveTournament((LeaveTournamentCommand) command);
        } else if (command instanceof SolveQuizCommand) {
            return handleSolveQuiz((SolveQuizCommand) command);
        } else if (command instanceof RemoveTournamentCommand) {
            return handleRemoveTournament((RemoveTournamentCommand) command);
        } else if (command instanceof UpdateTournamentCommand) {
            return handleUpdateTournament((UpdateTournamentCommand) command);
        } else if (command instanceof CancelTournamentCommand) {
            return handleCancelTournament((CancelTournamentCommand) command);
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

    private Object handleCreateTournament(CreateTournamentCommand command) {
        logger.info("Creating tournament: ");
        try {
            TournamentDto tournamentDto = tournamentService.createTournament(
                    command.getTournamentDto(),
                    command.getCreatorDto(),
                    command.getCourseExecutionDto(),
                    command.getTopicDtos(),
                    command.getQuizDto(),
                    command.getUnitOfWork());
            return tournamentDto;
        } catch (Exception e) {
            logger.severe("Failed to create tournament: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetTournamentsByCourseExecutionId(GetTournamentsByCourseExecutionIdCommand command) {
        logger.info("Getting tournaments by course execution ID: " + command.getExecutionAggregateId());
        try {
            return tournamentService.getTournamentsByCourseExecutionId(
                    command.getExecutionAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get tournaments: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetOpenedTournamentsForCourseExecution(GetOpenedTournamentsForCourseExecutionCommand command) {
        logger.info("Getting opened tournaments for course execution ID: " + command.getExecutionAggregateId());
        try {
            return tournamentService.getOpenedTournamentsForCourseExecution(
                    command.getExecutionAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get opened tournaments: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetClosedTournamentsForCourseExecution(GetClosedTournamentsForCourseExecutionCommand command) {
        logger.info("Getting closed tournaments for course execution ID: " + command.getExecutionAggregateId());
        try {
            return tournamentService.getClosedTournamentsForCourseExecution(
                    command.getExecutionAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get closed tournaments: " + e.getMessage());
            return e;
        }
    }

    private Object handleLeaveTournament(LeaveTournamentCommand command) {
        logger.info("Leaving tournament: " + command.getTournamentAggregateId());
        try {
            tournamentService.leaveTournament(
                    command.getTournamentAggregateId(),
                    command.getUserAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to leave tournament: " + e.getMessage());
            return e;
        }
    }

    private Object handleSolveQuiz(SolveQuizCommand command) {
        logger.info("Solving quiz for tournament: " + command.getTournamentAggregateId());
        try {
            tournamentService.solveQuiz(
                    command.getTournamentAggregateId(),
                    command.getUserAggregateId(),
                    command.getAnswerAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to solve quiz: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveTournament(RemoveTournamentCommand command) {
        logger.info("Removing tournament: " + command.getTournamentAggregateId());
        try {
            tournamentService.removeTournament(
                    command.getTournamentAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to remove tournament: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateTournament(UpdateTournamentCommand command) {
        logger.info("Updating tournament: " + command.getTournamentDto().getAggregateId());
        try {
            tournamentService.updateTournament(
                    command.getTournamentDto(),
                    command.getTopicDtos(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to update tournament: " + e.getMessage());
            return e;
        }
    }

    private Object handleCancelTournament(CancelTournamentCommand command) {
        logger.info("Cancelling tournament: " + command.getTournamentAggregateId());
        try {
            tournamentService.cancelTournament(
                    command.getTournamentAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to cancel tournament: " + e.getMessage());
            return e;
        }
    }

}
