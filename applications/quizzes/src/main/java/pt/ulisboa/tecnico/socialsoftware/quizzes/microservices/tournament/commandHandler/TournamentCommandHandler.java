package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.AbortCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.CommitCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.PrepareCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.GetConcurrentAggregateCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command.AbortSagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command.CommitSagaCommand;
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

    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;

    @Override
    public Object handle(Command command) {
        if (command.getForbiddenStates() != null && !command.getForbiddenStates().isEmpty()) {
            sagaUnitOfWorkService.verifySagaState(command.getRootAggregateId(), command.getForbiddenStates());
        }
        Object returnObject;
        switch (command) {
            case GetTournamentByIdCommand getTournamentByIdCommand ->
                returnObject = handleGetTournamentById(getTournamentByIdCommand);
            case AddParticipantCommand addParticipantCommand ->
                returnObject = handleAddParticipant(addParticipantCommand);
            case CreateTournamentCommand createTournamentCommand ->
                returnObject = handleCreateTournament(createTournamentCommand);
            case GetTournamentsByCourseExecutionIdCommand getTournamentsByCourseExecutionIdCommand ->
                returnObject = handleGetTournamentsByCourseExecutionId(getTournamentsByCourseExecutionIdCommand);
            case GetOpenedTournamentsForCourseExecutionCommand getOpenedTournamentsForCourseExecutionCommand ->
                returnObject = handleGetOpenedTournamentsForCourseExecution(
                        getOpenedTournamentsForCourseExecutionCommand);
            case GetClosedTournamentsForCourseExecutionCommand getClosedTournamentsForCourseExecutionCommand ->
                returnObject = handleGetClosedTournamentsForCourseExecution(
                        getClosedTournamentsForCourseExecutionCommand);
            case LeaveTournamentCommand leaveTournamentCommand ->
                returnObject = handleLeaveTournament(leaveTournamentCommand);
            case SolveQuizCommand solveQuizCommand -> returnObject = handleSolveQuiz(solveQuizCommand);
            case RemoveTournamentCommand removeTournamentCommand ->
                returnObject = handleRemoveTournament(removeTournamentCommand);
            case UpdateTournamentCommand updateTournamentCommand ->
                returnObject = handleUpdateTournament(updateTournamentCommand);
            case CancelTournamentCommand cancelTournamentCommand ->
                returnObject = handleCancelTournament(cancelTournamentCommand);
            case AnonymizeUserCommand anonymizeUserCommand -> returnObject = handleAnonymizeUser(anonymizeUserCommand);
            case UpdateUserNameCommand updateUserNameCommand ->
                returnObject = handleUpdateUserName(updateUserNameCommand);
            case RemoveCourseExecutionCommand removeCourseExecutionCommand ->
                returnObject = handleRemoveCourseExecution(removeCourseExecutionCommand);
            case UpdateTopicCommand updateTopicCommand -> returnObject = handleUpdateTopic(updateTopicCommand);
            case RemoveTopicCommand removeTopicCommand -> returnObject = handleRemoveTopic(removeTopicCommand);
            case UpdateParticipantAnswerCommand updateParticipantAnswerCommand ->
                returnObject = handleUpdateParticipantAnswer(updateParticipantAnswerCommand);
            case RemoveUserCommand removeUserCommand -> returnObject = handleRemoveUser(removeUserCommand);
            case InvalidateQuizCommand invalidateQuizCommand ->
                returnObject = handleInvalidateQuiz(invalidateQuizCommand);
            case CommitCausalCommand commitCausalCommand -> returnObject = handleCommitCausal(commitCausalCommand);
            case PrepareCausalCommand prepareCausalCommand -> returnObject = handlePrepareCausal(prepareCausalCommand);
            case AbortCausalCommand abortCausalCommand -> returnObject = handleAbortCausal(abortCausalCommand);
            case GetConcurrentAggregateCommand getConcurrentAggregateCommand -> returnObject = handleGetConcurrentAggregate(getConcurrentAggregateCommand);
            case CommitSagaCommand commitSagaCommand -> returnObject = handleCommitSaga(commitSagaCommand);
            case AbortSagaCommand abortSagaCommand -> returnObject = handleAbortSaga(abortSagaCommand);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                returnObject = null;
            }
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
                    command.getUserDto(),
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

    private Object handleRemoveCourseExecution(RemoveCourseExecutionCommand command) {
        logger.info("Removing course execution from tournament: " + command.getTournamentAggregateId());
        try {
            return tournamentService.removeCourseExecution(
                    command.getTournamentAggregateId(),
                    command.getCourseExecutionId(),
                    command.getEventVersion(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to remove course execution from tournament: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateTopic(UpdateTopicCommand command) {
        logger.info("Updating tournament topic: tournament=" + command.getTournamentAggregateId() + ", topic="
                + command.getTopicAggregateId());
        try {
            return tournamentService.updateTopic(
                    command.getTournamentAggregateId(),
                    command.getTopicAggregateId(),
                    command.getTopicName(),
                    command.getEventVersion(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to update tournament topic: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveTopic(RemoveTopicCommand command) {
        logger.info("Removing tournament topic: tournament=" + command.getTournamentAggregateId() + ", topic="
                + command.getTopicAggregateId());
        try {
            return tournamentService.removeTopic(
                    command.getTournamentAggregateId(),
                    command.getTopicAggregateId(),
                    command.getEventVersion(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to remove tournament topic: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateParticipantAnswer(UpdateParticipantAnswerCommand command) {
        logger.info("Updating participant answer: tournament=" + command.getTournamentAggregateId() + ", student="
                + command.getStudentAggregateId());
        try {
            return tournamentService.updateParticipantAnswer(
                    command.getTournamentAggregateId(),
                    command.getStudentAggregateId(),
                    command.getQuizAnswerAggregateId(),
                    command.getQuestionAggregateId(),
                    command.isCorrect(),
                    command.getEventVersion(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to update participant answer: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveUser(RemoveUserCommand command) {
        logger.info("Removing user " + command.getUserAggregateId() + " from tournament "
                + command.getTournamentAggregateId());
        try {
            return tournamentService.removeUser(
                    command.getTournamentAggregateId(),
                    command.getCourseExecutionAggregateId(),
                    command.getUserAggregateId(),
                    command.getEventVersion(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to remove user from tournament: " + e.getMessage());
            return e;
        }
    }

    private Object handleInvalidateQuiz(InvalidateQuizCommand command) {
        logger.info("Invalidating quiz for tournament: " + command.getTournamentAggregateId());
        try {
            return tournamentService.invalidateQuiz(
                    command.getTournamentAggregateId(),
                    command.getAggregateId(),
                    command.getAggregateVersion(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to invalidate quiz for tournament: " + e.getMessage());
            return e;
        }
    }

    private Object handleCommitCausal(CommitCausalCommand command) {
        logger.info("Committing causal for aggregate: " + command.getRootAggregateId());
        try {
            causalUnitOfWorkService.commitCausal(command.getAggregate());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to commit causal: " + e.getMessage());
            return e;
        }
    }

    private Object handlePrepareCausal(PrepareCausalCommand command) {
        logger.info("Preparing causal for aggregate: " + command.getRootAggregateId());
        try {
            causalUnitOfWorkService.prepareCausal(command.getAggregate());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to prepare causal: " + e.getMessage());
            return e;
        }
    }

    private Object handleAbortCausal(AbortCausalCommand command) {
        logger.info("Aborting causal for aggregate: " + command.getRootAggregateId());
        try {
            causalUnitOfWorkService.abortCausal(command.getRootAggregateId());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to abort causal: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetConcurrentAggregate(GetConcurrentAggregateCommand command) {
        return causalUnitOfWorkService.getConcurrentAggregate(command.getRootAggregateId(), command.getVersion(), "Tournament");
    }

    private Object handleCommitSaga(CommitSagaCommand command) {
        logger.info("Committing saga for aggregate: " + command.getAggregateId());
        try {
            sagaUnitOfWorkService.commitAggregate(command.getAggregateId());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to commit saga: " + e.getMessage());
            return e;
        }
    }

    private Object handleAbortSaga(AbortSagaCommand command) {
        logger.info("Aborting saga for aggregate: " + command.getAggregateId());
        try {
            sagaUnitOfWorkService.abortAggregate(command.getAggregateId(), command.getPreviousState());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to abort saga: " + e.getMessage());
            return e;
        }
    }

}
