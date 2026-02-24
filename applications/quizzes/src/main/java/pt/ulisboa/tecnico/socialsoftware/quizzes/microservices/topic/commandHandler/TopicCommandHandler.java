package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;

import java.util.logging.Logger;

@Service
public class TopicCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(TopicCommandHandler.class.getName());

    @Autowired
    private TopicService topicService;

    @Override
    protected String getAggregateTypeName() {
        return "Topic";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetTopicByIdCommand cmd -> handleGetTopicById(cmd);
            case CreateTopicCommand cmd -> handleCreateTopic(cmd);
            case FindTopicsByCourseIdCommand cmd -> handleFindTopicsByCourseId(cmd);
            case UpdateTopicCommand cmd -> handleUpdateTopic(cmd);
            case DeleteTopicCommand cmd -> handleDeleteTopic(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetTopicById(GetTopicByIdCommand command) {
        return topicService.getTopicById(command.getTopicAggregateId(), command.getUnitOfWork());
    }

    private Object handleCreateTopic(CreateTopicCommand command) {
        return topicService.createTopic(command.getTopicDto(), command.getCourse(), command.getUnitOfWork());
    }

    private Object handleFindTopicsByCourseId(FindTopicsByCourseIdCommand command) {
        return topicService.findTopicsByCourseId(command.getCourseAggregateId(), command.getUnitOfWork());
    }

    private Object handleUpdateTopic(UpdateTopicCommand command) {
        topicService.updateTopic(command.getTopicDto(), command.getUnitOfWork());
        return null;
    }

    private Object handleDeleteTopic(DeleteTopicCommand command) {
        topicService.deleteTopic(command.getTopicAggregateId(), command.getUnitOfWork());
        return null;
    }
}
