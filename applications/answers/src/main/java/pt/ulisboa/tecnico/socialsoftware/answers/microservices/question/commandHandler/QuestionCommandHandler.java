package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.command.question.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService;

import java.util.logging.Logger;

@Component
public class QuestionCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(QuestionCommandHandler.class.getName());

    @Autowired
    private QuestionService questionService;

    @Override
    protected String getAggregateTypeName() {
        return "Question";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateQuestionCommand cmd -> handleCreateQuestion(cmd);
            case GetQuestionByIdCommand cmd -> handleGetQuestionById(cmd);
            case GetAllQuestionsCommand cmd -> handleGetAllQuestions(cmd);
            case UpdateQuestionCommand cmd -> handleUpdateQuestion(cmd);
            case DeleteQuestionCommand cmd -> handleDeleteQuestion(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateQuestion(CreateQuestionCommand cmd) {
        logger.info("handleCreateQuestion");
        try {
            return questionService.createQuestion(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetQuestionById(GetQuestionByIdCommand cmd) {
        logger.info("handleGetQuestionById");
        try {
            return questionService.getQuestionById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllQuestions(GetAllQuestionsCommand cmd) {
        logger.info("handleGetAllQuestions");
        try {
            return questionService.getAllQuestions(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateQuestion(UpdateQuestionCommand cmd) {
        logger.info("handleUpdateQuestion");
        try {
            return questionService.updateQuestion(cmd.getQuestionDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteQuestion(DeleteQuestionCommand cmd) {
        logger.info("handleDeleteQuestion");
        try {
            questionService.deleteQuestion(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
