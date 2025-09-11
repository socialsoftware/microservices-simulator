package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;

import java.util.logging.Logger;

@Component
public class TournamentCommandHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(TournamentCommandHandler.class.getName());

    @Autowired
    private TournamentService tournamentService;

    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Override
    public Object handle(Command command) {
        if (command.getForbiddenStates() != null && !command.getForbiddenStates().isEmpty()) {
            sagaUnitOfWorkService.verifySagaState(command.getRootAggregateId(), command.getForbiddenStates());
        }
        Object returnObject;
        if (command instanceof GetTournamentByIdCommand) {
            returnObject = handleGetTournamentById((GetTournamentByIdCommand) command);
        } else if (command instanceof AddParticipantCommand) {
            returnObject = handleAddParticipant((AddParticipantCommand) command);
        } else if (command instanceof CreateTournamentCommand) {
            returnObject = handleCreateTournament((CreateTournamentCommand) command);
        } else if (command instanceof GetTournamentsByCourseExecutionIdCommand) {
            returnObject = handleGetTournamentsByCourseExecutionId((GetTournamentsByCourseExecutionIdCommand) command);
        } else if (command instanceof GetOpenedTournamentsForCourseExecutionCommand) {
            returnObject = handleGetOpenedTournamentsForCourseExecution(
                    (GetOpenedTournamentsForCourseExecutionCommand) command);
        } else if (command instanceof GetClosedTournamentsForCourseExecutionCommand) {
            returnObject = handleGetClosedTournamentsForCourseExecution(
                    (GetClosedTournamentsForCourseExecutionCommand) command);
        } else if (command instanceof LeaveTournamentCommand) {
            returnObject = handleLeaveTournament((LeaveTournamentCommand) command);
        } else if (command instanceof SolveQuizCommand) {
            returnObject = handleSolveQuiz((SolveQuizCommand) command);
        } else if (command instanceof RemoveTournamentCommand) {
            returnObject = handleRemoveTournament((RemoveTournamentCommand) command);
        } else if (command instanceof UpdateTournamentCommand) {
            returnObject = handleUpdateTournament((UpdateTournamentCommand) command);
        } else if (command instanceof CancelTournamentCommand) {
            returnObject = handleCancelTournament((CancelTournamentCommand) command);
        } else if (command instanceof AnonymizeUserCommand) {
            returnObject = handleAnonymizeUser((AnonymizeUserCommand) command);
        } else if (command instanceof UpdateUserNameCommand) {
            returnObject = handleUpdateUserName((UpdateUserNameCommand) command);
        } else {
            logger.warning("Unknown command type: " + command.getClass().getName());
            returnObject = null;
        }

        if (command.getSemanticLock() != null) {
            sagaUnitOfWorkService.registerSagaState(command.getRootAggregateId(), command.getSemanticLock(),
                    (SagaUnitOfWork) command.getUnitOfWork());
        }

        return returnObject;
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
            return tournamentService.updateTournament(
                    command.getTournamentDto(),
                    command.getTopicDtos(),
                    command.getUnitOfWork());
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

    private Object handleAnonymizeUser(AnonymizeUserCommand command) {
        logger.info("Anonymizing user for tournament: " + command.getTournamentAggregateId());
        try {
            return tournamentService.anonymizeUser(
                    command.getTournamentAggregateId(),
                    command.getExecutionAggregateId(),
                    command.getUserAggregateId(),
                    command.getName(),
                    command.getUsername(),
                    command.getEventVersion(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to anonymize user: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateUserName(UpdateUserNameCommand command) {
        logger.info("Updating user name in tournament: " + command.getTournamentAggregateId());
        try {
            tournamentService.updateUserName(
                    command.getTournamentAggregateId(),
                    command.getExecutionAggregateId(),
                    command.getEventVersion(),
                    command.getUserAggregateId(),
                    command.getName(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to update user name: " + e.getMessage());
            return e;
        }
    }

}
