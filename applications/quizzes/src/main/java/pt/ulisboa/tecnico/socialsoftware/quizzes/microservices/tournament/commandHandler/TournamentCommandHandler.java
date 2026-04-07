package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.tournament.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;

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
            case GetTournamentByIdCommand cmd -> handleGetTournamentById(cmd);
            case AddParticipantCommand cmd -> handleAddParticipant(cmd);
            case CreateTournamentCommand cmd -> handleCreateTournament(cmd);
            case GetTournamentsByCourseExecutionIdCommand cmd -> handleGetTournamentsByCourseExecutionId(cmd);
            case GetOpenedTournamentsForCourseExecutionCommand cmd -> handleGetOpenedTournamentsForCourseExecution(cmd);
            case GetClosedTournamentsForCourseExecutionCommand cmd -> handleGetClosedTournamentsForCourseExecution(cmd);
            case LeaveTournamentCommand cmd -> handleLeaveTournament(cmd);
            case SolveQuizCommand cmd -> handleSolveQuiz(cmd);
            case RemoveTournamentCommand cmd -> handleRemoveTournament(cmd);
            case UpdateTournamentCommand cmd -> handleUpdateTournament(cmd);
            case CancelTournamentCommand cmd -> handleCancelTournament(cmd);
            case AnonymizeUserCommand cmd -> handleAnonymizeUser(cmd);
            case UpdateUserNameCommand cmd -> handleUpdateUserName(cmd);
            case RemoveCourseExecutionCommand cmd -> handleRemoveCourseExecution(cmd);
            case UpdateTopicCommand cmd -> handleUpdateTopic(cmd);
            case RemoveTopicCommand cmd -> handleRemoveTopic(cmd);
            case UpdateParticipantAnswerCommand cmd -> handleUpdateParticipantAnswer(cmd);
            case RemoveUserCommand cmd -> handleRemoveUser(cmd);
            case InvalidateQuizCommand cmd -> handleInvalidateQuiz(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetTournamentById(GetTournamentByIdCommand command) {
        logger.info("Getting tournament: " + command.getAggregateId());
        return tournamentService.getTournamentById(
                command.getAggregateId(),
                command.getUnitOfWork());
    }

    private Object handleAddParticipant(AddParticipantCommand command) {
        logger.info("Adding participant to tournament: " + command.getTournamentAggregateId());
        tournamentService.addParticipant(
                command.getTournamentAggregateId(),
                command.getUserDto(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleCreateTournament(CreateTournamentCommand command) {
        logger.info("Creating tournament: ");
        return tournamentService.createTournament(
                command.getTournamentDto(),
                command.getCreatorDto(),
                command.getCourseExecutionDto(),
                command.getTopicDtos(),
                command.getQuizDto(),
                command.getUnitOfWork());
    }

    private Object handleGetTournamentsByCourseExecutionId(GetTournamentsByCourseExecutionIdCommand command) {
        logger.info("Getting tournaments by course execution ID: " + command.getExecutionAggregateId());
        return tournamentService.getTournamentsByCourseExecutionId(
                command.getExecutionAggregateId(),
                command.getUnitOfWork());
    }

    private Object handleGetOpenedTournamentsForCourseExecution(GetOpenedTournamentsForCourseExecutionCommand command) {
        logger.info("Getting opened tournaments for course execution ID: " + command.getExecutionAggregateId());
        return tournamentService.getOpenedTournamentsForCourseExecution(
                command.getExecutionAggregateId(),
                command.getUnitOfWork());
    }

    private Object handleGetClosedTournamentsForCourseExecution(GetClosedTournamentsForCourseExecutionCommand command) {
        logger.info("Getting closed tournaments for course execution ID: " + command.getExecutionAggregateId());
        return tournamentService.getClosedTournamentsForCourseExecution(
                command.getExecutionAggregateId(),
                command.getUnitOfWork());
    }

    private Object handleLeaveTournament(LeaveTournamentCommand command) {
        logger.info("Leaving tournament: " + command.getTournamentAggregateId());
        tournamentService.leaveTournament(
                command.getTournamentAggregateId(),
                command.getUserAggregateId(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleSolveQuiz(SolveQuizCommand command) {
        logger.info("Solving quiz for tournament: " + command.getTournamentAggregateId());
        tournamentService.solveQuiz(
                command.getTournamentAggregateId(),
                command.getUserAggregateId(),
                command.getAnswerAggregateId(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleRemoveTournament(RemoveTournamentCommand command) {
        logger.info("Removing tournament: " + command.getTournamentAggregateId());
        tournamentService.removeTournament(
                command.getTournamentAggregateId(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleUpdateTournament(UpdateTournamentCommand command) {
        logger.info("Updating tournament: " + command.getTournamentDto().getAggregateId());
        return tournamentService.updateTournament(
                command.getTournamentDto(),
                command.getTopicDtos(),
                command.getUnitOfWork());
    }

    private Object handleCancelTournament(CancelTournamentCommand command) {
        logger.info("Cancelling tournament: " + command.getTournamentAggregateId());
        tournamentService.cancelTournament(
                command.getTournamentAggregateId(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleAnonymizeUser(AnonymizeUserCommand command) {
        logger.info("Anonymizing user for tournament: " + command.getTournamentAggregateId());
        return tournamentService.anonymizeUser(
                command.getTournamentAggregateId(),
                command.getExecutionAggregateId(),
                command.getUserAggregateId(),
                command.getName(),
                command.getUsername(),
                command.getEventVersion(),
                command.getUnitOfWork());
    }

    private Object handleUpdateUserName(UpdateUserNameCommand command) {
        logger.info("Updating user name in tournament: " + command.getTournamentAggregateId());
        tournamentService.updateUserName(
                command.getTournamentAggregateId(),
                command.getExecutionAggregateId(),
                command.getEventVersion(),
                command.getUserAggregateId(),
                command.getName(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleRemoveCourseExecution(RemoveCourseExecutionCommand command) {
        logger.info("Removing course execution from tournament: " + command.getTournamentAggregateId());
        return tournamentService.removeCourseExecution(
                command.getTournamentAggregateId(),
                command.getCourseExecutionId(),
                command.getEventVersion(),
                command.getUnitOfWork());
    }

    private Object handleUpdateTopic(UpdateTopicCommand command) {
        logger.info("Updating tournament topic: tournament=" + command.getTournamentAggregateId() + ", topic="
                + command.getTopicAggregateId());
        return tournamentService.updateTopic(
                command.getTournamentAggregateId(),
                command.getTopicAggregateId(),
                command.getTopicName(),
                command.getEventVersion(),
                command.getUnitOfWork());
    }

    private Object handleRemoveTopic(RemoveTopicCommand command) {
        logger.info("Removing tournament topic: tournament=" + command.getTournamentAggregateId() + ", topic="
                + command.getTopicAggregateId());
        return tournamentService.removeTopic(
                command.getTournamentAggregateId(),
                command.getTopicAggregateId(),
                command.getEventVersion(),
                command.getUnitOfWork());
    }

    private Object handleUpdateParticipantAnswer(UpdateParticipantAnswerCommand command) {
        logger.info("Updating participant answer: tournament=" + command.getTournamentAggregateId() + ", student="
                + command.getStudentAggregateId());
        return tournamentService.updateParticipantAnswer(
                command.getTournamentAggregateId(),
                command.getStudentAggregateId(),
                command.getQuizAnswerAggregateId(),
                command.getQuestionAggregateId(),
                command.isCorrect(),
                command.getEventVersion(),
                command.getUnitOfWork());
    }

    private Object handleRemoveUser(RemoveUserCommand command) {
        logger.info("Removing user " + command.getUserAggregateId() + " from tournament "
                + command.getTournamentAggregateId());
        return tournamentService.removeUser(
                command.getTournamentAggregateId(),
                command.getCourseExecutionAggregateId(),
                command.getUserAggregateId(),
                command.getEventVersion(),
                command.getUnitOfWork());
    }

    private Object handleInvalidateQuiz(InvalidateQuizCommand command) {
        logger.info("Invalidating quiz for tournament: " + command.getTournamentAggregateId());
        return tournamentService.invalidateQuiz(
                command.getTournamentAggregateId(),
                command.getAggregateId(),
                command.getAggregateVersion(),
                command.getUnitOfWork());
    }
}
