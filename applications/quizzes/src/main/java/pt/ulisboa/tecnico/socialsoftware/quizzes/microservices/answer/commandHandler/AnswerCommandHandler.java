package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.service.QuizAnswerService;

import java.util.logging.Logger;

@Component
public class AnswerCommandHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(AnswerCommandHandler.class.getName());

    @Autowired
    private QuizAnswerService quizAnswerService;

    @Override
    public Object handle(Command command) {
        if (command instanceof GetQuizAnswerByQuizIdAndUserIdCommand) {
            return handleGetQuizAnswerByQuizIdAndUserId((GetQuizAnswerByQuizIdAndUserIdCommand) command);
        } else if (command instanceof GetQuizAnswerDtoByQuizIdAndUserIdCommand) {
            return handleGetQuizAnswerDtoByQuizIdAndUserId((GetQuizAnswerDtoByQuizIdAndUserIdCommand) command);
        } else if (command instanceof StartQuizCommand) {
            return handleStartQuiz((StartQuizCommand) command);
        } else if (command instanceof ConcludeQuizCommand) {
            return handleConcludeQuiz((ConcludeQuizCommand) command);
        } else if (command instanceof AnswerQuestionCommand) {
            return handleAnswerQuestion((AnswerQuestionCommand) command);
        } else if (command instanceof RemoveQuizAnswerCommand) {
            return handleRemoveQuizAnswer((RemoveQuizAnswerCommand) command);
        }

        logger.warning("Unknown command type: " + command.getClass().getName());
        return null;
    }

    private Object handleGetQuizAnswerByQuizIdAndUserId(GetQuizAnswerByQuizIdAndUserIdCommand command) {
        logger.info("Getting quiz answer by quiz ID and user ID: " + command.getQuizAggregateId() + ", "
                + command.getUserAggregateId());
        try {
            QuizAnswer quizAnswerDto = quizAnswerService.getQuizAnswerByQuizIdAndUserId(
                    command.getQuizAggregateId(),
                    command.getUserAggregateId(),
                    command.getUnitOfWork());
            return quizAnswerDto;
        } catch (Exception e) {
            logger.severe("Failed to get quiz answer: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetQuizAnswerDtoByQuizIdAndUserId(GetQuizAnswerDtoByQuizIdAndUserIdCommand command) {
        logger.info("Getting quiz answer DTO by quiz ID and user ID: " + command.getQuizAggregateId() + ", "
                + command.getUserAggregateId());
        try {
            QuizAnswerDto quizAnswerDto = quizAnswerService.getQuizAnswerDtoByQuizIdAndUserId(
                    command.getQuizAggregateId(),
                    command.getUserAggregateId(),
                    command.getUnitOfWork());
            return quizAnswerDto;
        } catch (Exception e) {
            logger.severe("Failed to get quiz answer DTO: " + e.getMessage());
            return e;
        }
    }

    private Object handleStartQuiz(StartQuizCommand command) {
        logger.info("Starting quiz: " + command.getQuizAggregateId());
        try {
            QuizAnswerDto quizAnswerDto = quizAnswerService.startQuiz(
                    command.getQuizAggregateId(),
                    command.getCourseExecutionAggregateId(),
                    command.getUserAggregateId(),
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
            quizAnswerService.concludeQuiz(
                    command.getQuizAggregateId(),
                    command.getUserAggregateId(),
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
            quizAnswerService.answerQuestion(
                    command.getQuizAggregateId(),
                    command.getUserAggregateId(),
                    command.getUserAnswerDto(),
                    command.getQuestionDto(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to answer question: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveQuizAnswer(RemoveQuizAnswerCommand command) {
        logger.info("Removing quiz answer: " + command.getQuizAnswerAggregateId());
        try {
            quizAnswerService.removeQuizAnswer(
                    command.getQuizAnswerAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to remove quiz answer: " + e.getMessage());
            return e;
        }
    }

}