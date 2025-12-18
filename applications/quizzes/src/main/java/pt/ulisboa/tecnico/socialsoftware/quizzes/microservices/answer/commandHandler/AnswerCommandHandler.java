package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService;

import java.util.logging.Logger;

@Component
public class AnswerCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(AnswerCommandHandler.class.getName());

    @Autowired
    private QuizAnswerService quizAnswerService;

    @Override
    protected String getAggregateTypeName() {
        return "QuizAnswer";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetQuizAnswerDtoByQuizIdAndUserIdCommand cmd -> handleGetQuizAnswerDtoByQuizIdAndUserId(cmd);
            case StartQuizCommand cmd -> handleStartQuiz(cmd);
            case ConcludeQuizCommand cmd -> handleConcludeQuiz(cmd);
            case AnswerQuestionCommand cmd -> handleAnswerQuestion(cmd);
            case RemoveQuizAnswerCommand cmd -> handleRemoveQuizAnswer(cmd);
            case UpdateUserNameCommand cmd -> handleUpdateUserName(cmd);
            case RemoveUserFromQuizAnswerCommand cmd -> handleRemoveUserFromQuizAnswer(cmd);
            case RemoveQuestionFromQuizAnswerCommand cmd -> handleRemoveQuestionFromQuizAnswer(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
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
}