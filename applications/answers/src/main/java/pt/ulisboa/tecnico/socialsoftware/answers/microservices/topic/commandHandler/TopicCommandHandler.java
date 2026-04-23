package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.answers.command.topic.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService;

import java.util.logging.Logger;

@Component
public class TopicCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(TopicCommandHandler.class.getName());

    @Autowired
    private TopicService topicService;

    @Override
    public String getAggregateTypeName() {
        return "Topic";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateTopicCommand cmd -> handleCreateTopic(cmd);
            case GetTopicByIdCommand cmd -> handleGetTopicById(cmd);
            case GetAllTopicsCommand cmd -> handleGetAllTopics(cmd);
            case UpdateTopicCommand cmd -> handleUpdateTopic(cmd);
            case DeleteTopicCommand cmd -> handleDeleteTopic(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateTopic(CreateTopicCommand cmd) {
        logger.info("handleCreateTopic");
        try {
            return topicService.createTopic(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetTopicById(GetTopicByIdCommand cmd) {
        logger.info("handleGetTopicById");
        try {
            return topicService.getTopicById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllTopics(GetAllTopicsCommand cmd) {
        logger.info("handleGetAllTopics");
        try {
            return topicService.getAllTopics(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateTopic(UpdateTopicCommand cmd) {
        logger.info("handleUpdateTopic");
        try {
            return topicService.updateTopic(cmd.getTopicDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteTopic(DeleteTopicCommand cmd) {
        logger.info("handleDeleteTopic");
        try {
            topicService.deleteTopic(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
