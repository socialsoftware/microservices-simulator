package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.command.quiz.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService;

import java.util.logging.Logger;

@Component
public class QuizCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(QuizCommandHandler.class.getName());

    @Autowired
    private QuizService quizService;

    @Override
    public String getAggregateTypeName() {
        return "Quiz";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateQuizCommand cmd -> handleCreateQuiz(cmd);
            case GetQuizByIdCommand cmd -> handleGetQuizById(cmd);
            case GetAllQuizsCommand cmd -> handleGetAllQuizs(cmd);
            case UpdateQuizCommand cmd -> handleUpdateQuiz(cmd);
            case DeleteQuizCommand cmd -> handleDeleteQuiz(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateQuiz(CreateQuizCommand cmd) {
        logger.info("handleCreateQuiz");
        try {
            return quizService.createQuiz(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetQuizById(GetQuizByIdCommand cmd) {
        logger.info("handleGetQuizById");
        try {
            return quizService.getQuizById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllQuizs(GetAllQuizsCommand cmd) {
        logger.info("handleGetAllQuizs");
        try {
            return quizService.getAllQuizs(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateQuiz(UpdateQuizCommand cmd) {
        logger.info("handleUpdateQuiz");
        try {
            return quizService.updateQuiz(cmd.getQuizDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteQuiz(DeleteQuizCommand cmd) {
        logger.info("handleDeleteQuiz");
        try {
            quizService.deleteQuiz(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
