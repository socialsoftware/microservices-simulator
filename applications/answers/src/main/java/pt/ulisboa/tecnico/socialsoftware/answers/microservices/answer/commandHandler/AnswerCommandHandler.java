package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.command.answer.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;

import java.util.logging.Logger;

@Component
public class AnswerCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(AnswerCommandHandler.class.getName());

    @Autowired
    private AnswerService answerService;

    @Override
    protected String getAggregateTypeName() {
        return "Answer";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateAnswerCommand cmd -> handleCreateAnswer(cmd);
            case GetAnswerByIdCommand cmd -> handleGetAnswerById(cmd);
            case GetAllAnswersCommand cmd -> handleGetAllAnswers(cmd);
            case UpdateAnswerCommand cmd -> handleUpdateAnswer(cmd);
            case DeleteAnswerCommand cmd -> handleDeleteAnswer(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateAnswer(CreateAnswerCommand cmd) {
        logger.info("handleCreateAnswer");
        try {
            return answerService.createAnswer(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAnswerById(GetAnswerByIdCommand cmd) {
        logger.info("handleGetAnswerById");
        try {
            return answerService.getAnswerById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllAnswers(GetAllAnswersCommand cmd) {
        logger.info("handleGetAllAnswers");
        try {
            return answerService.getAllAnswers(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateAnswer(UpdateAnswerCommand cmd) {
        logger.info("handleUpdateAnswer");
        try {
            return answerService.updateAnswer(cmd.getAnswerDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteAnswer(DeleteAnswerCommand cmd) {
        logger.info("handleDeleteAnswer");
        try {
            answerService.deleteAnswer(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
