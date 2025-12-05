package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command.CommitSagaCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService;

import java.util.logging.Logger;

@Component
public class AnswerCommandHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(AnswerCommandHandler.class.getName());

    @Autowired
    private QuizAnswerService quizAnswerService;

    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Override
    public Object handle(Command command) {
        if (command.getForbiddenStates() != null && !command.getForbiddenStates().isEmpty()) {
            sagaUnitOfWorkService.verifySagaState(command.getRootAggregateId(), command.getForbiddenStates());
        }
        Object returnObject;
        switch (command) {
            case GetQuizAnswerDtoByQuizIdAndUserIdCommand getQuizAnswerDtoByQuizIdAndUserIdCommand ->
                returnObject = handleGetQuizAnswerDtoByQuizIdAndUserId(getQuizAnswerDtoByQuizIdAndUserIdCommand);
            case StartQuizCommand startQuizCommand -> returnObject = handleStartQuiz(startQuizCommand);
            case ConcludeQuizCommand concludeQuizCommand -> returnObject = handleConcludeQuiz(concludeQuizCommand);
            case AnswerQuestionCommand answerQuestionCommand ->
                returnObject = handleAnswerQuestion(answerQuestionCommand);
            case RemoveQuizAnswerCommand removeQuizAnswerCommand ->
                returnObject = handleRemoveQuizAnswer(removeQuizAnswerCommand);
            case UpdateUserNameCommand updateUserNameCommand ->
                returnObject = handleUpdateUserName(updateUserNameCommand);
            case RemoveUserFromQuizAnswerCommand removeUserFromQuizAnswerCommand ->
                returnObject = handleRemoveUserFromQuizAnswer(removeUserFromQuizAnswerCommand);
            case RemoveQuestionFromQuizAnswerCommand removeQuestionFromQuizAnswerCommand ->
                returnObject = handleRemoveQuestionFromQuizAnswer(removeQuestionFromQuizAnswerCommand);
            case CommitSagaCommand commitSagaCommand -> returnObject = handleCommitSaga(commitSagaCommand);
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

    private Object handleGetQuizAnswerDtoByQuizIdAndUserId(GetQuizAnswerDtoByQuizIdAndUserIdCommand command) {
        logger.info("Getting quiz answer DTO by quiz ID and user ID: " + command.getQuizAggregateId() + ", "
                + command.getUserAggregateId());
        try {
            QuizAnswerDto quizAnswerDto = quizAnswerService.getQuizAnswerDtoByQuizIdAndUserId(
                    command.getQuizAggregateId(), command.getUserAggregateId(), command.getUnitOfWork());
            return quizAnswerDto;
        } catch (Exception e) {
            logger.severe("Failed to get quiz answer DTO: " + e.getMessage());
            return e;
        }
    }

    private Object handleStartQuiz(StartQuizCommand command) {
        logger.info("Starting quiz: " + command.getQuizAggregateId());
        try {
            QuizAnswerDto quizAnswerDto = quizAnswerService.startQuiz(command.getQuizAggregateId(),
                    command.getCourseExecutionAggregateId(), command.getQuizDto(), command.getUserDto(),
                    command.getUnitOfWork());
            return quizAnswerDto;
        } catch (Exception e) {
            logger.severe("Failed to start quiz: " + e.getMessage());
            return e;
        }
    }

    private Object handleConcludeQuiz(ConcludeQuizCommand command) {
        logger.info("Concluding quiz: " + command.getQuizAggregateId());
        try {
            quizAnswerService.concludeQuiz(command.getQuizAggregateId(), command.getUserAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to conclude quiz: " + e.getMessage());
            return e;
        }
    }

    private Object handleAnswerQuestion(AnswerQuestionCommand command) {
        logger.info("Answering question for quiz: " + command.getQuizAggregateId());
        try {
            quizAnswerService.answerQuestion(command.getQuizAggregateId(), command.getUserAggregateId(),
                    command.getUserAnswerDto(), command.getQuestionDto(), command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to answer question: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveQuizAnswer(RemoveQuizAnswerCommand command) {
        logger.info("Removing quiz answer: " + command.getQuizAnswerAggregateId());
        try {
            quizAnswerService.removeQuizAnswer(command.getQuizAnswerAggregateId(), command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to remove quiz answer: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateUserName(UpdateUserNameCommand command) {
        logger.info("Updating user name in quiz answer: " + command.getAnswerAggregateId());
        try {
            quizAnswerService.updateUserName(
                    command.getAnswerAggregateId(),
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

    private Object handleRemoveUserFromQuizAnswer(RemoveUserFromQuizAnswerCommand command) {
        logger.info("Removing user from quiz answer: " + command.getAnswerAggregateId());
        try {
            quizAnswerService.removeUser(
                    command.getAnswerAggregateId(),
                    command.getUserAggregateId(),
                    command.getAggregateVersion(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to remove user from quiz answer: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveQuestionFromQuizAnswer(RemoveQuestionFromQuizAnswerCommand command) {
        logger.info("Removing question from quiz answer: " + command.getAnswerAggregateId());
        try {
            quizAnswerService.removeQuestion(
                    command.getAnswerAggregateId(),
                    command.getQuestionAggregateId(),
                    command.getAggregateVersion(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to remove question from quiz answer: " + e.getMessage());
            return e;
        }
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

}