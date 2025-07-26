package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;

import java.util.logging.Logger;

@Component
public class QuizCommandHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(QuizCommandHandler.class.getName());

    @Autowired
    private QuizService quizService;

    @Override
    public Object handle(Command command) {
        if (command instanceof StartTournamentQuizCommand) {
            return handleStartTournamentQuiz((StartTournamentQuizCommand) command);
        } else if (command instanceof GetQuizByIdCommand) {
            return handleGetQuizById((GetQuizByIdCommand) command);
        } else if (command instanceof GenerateQuizCommand) {
            return handleGenerateQuiz((GenerateQuizCommand) command);
        } else if (command instanceof CreateQuizCommand) {
            return handleCreateQuiz((CreateQuizCommand) command);
        } else if (command instanceof UpdateGeneratedQuizCommand) {
            return handleUpdateGeneratedQuiz((UpdateGeneratedQuizCommand) command);
        } else if (command instanceof UpdateQuizCommand) {
            return handleUpdateQuiz((UpdateQuizCommand) command);
        } else if (command instanceof GetAvailableQuizzesCommand) {
            return handleGetAvailableQuizzes((GetAvailableQuizzesCommand) command);
        }

        logger.warning("Unknown command type: " + command.getClass().getName());
        return null;
    }

    private Object handleStartTournamentQuiz(StartTournamentQuizCommand command) {
        logger.info("Starting tournament quiz: " + command.getQuizAggregateId());
        try {
            return quizService.startTournamentQuiz(
                    command.getUserAggregateId(),
                    command.getQuizAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to start tournament quiz: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetQuizById(GetQuizByIdCommand command) {
        logger.info("Getting quiz by ID: " + command.getAggregateId());
        try {
            return quizService.getQuizById(
                    command.getAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get quiz by ID: " + e.getMessage());
            return e;
        }
    }

    private Object handleGenerateQuiz(GenerateQuizCommand command) {
        logger.info("Generating quiz for course execution: " + command.getCourseExecutionAggregateId());
        try {
            return quizService.generateQuiz(
                    command.getCourseExecutionAggregateId(),
                    command.getQuizDto(),
                    command.getTopicIds(),
                    command.getNumberOfQuestions(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to generate quiz: " + e.getMessage());
            return e;
        }
    }

    private Object handleCreateQuiz(CreateQuizCommand command) {
        logger.info("Creating quiz for course execution: " + command.getQuizCourseExecution());
        try {
            return quizService.createQuiz(
                    command.getQuizCourseExecution(),
                    command.getQuestions(),
                    command.getQuizDto(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to create quiz: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateGeneratedQuiz(UpdateGeneratedQuizCommand command) {
        logger.info("Updating generated quiz: " + command.getQuizDto().getAggregateId());
        try {
            return quizService.updateGeneratedQuiz(
                    command.getQuizDto(),
                    command.getTopicsAggregateIds(),
                    command.getNumberOfQuestions(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to update generated quiz: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateQuiz(UpdateQuizCommand command) {
        logger.info("Updating quiz: " + command.getQuizDto().getAggregateId());
        try {
            return quizService.updateQuiz(
                    command.getQuizDto(),
                    command.getQuizQuestions(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to update quiz: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAvailableQuizzes(GetAvailableQuizzesCommand command) {
        logger.info("Getting available quizzes for course execution: " + command.getCourseExecutionAggregateId());
        try {
            return quizService.getAvailableQuizzes(
                    command.getCourseExecutionAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get available quizzes: " + e.getMessage());
            return e;
        }
    }
}
