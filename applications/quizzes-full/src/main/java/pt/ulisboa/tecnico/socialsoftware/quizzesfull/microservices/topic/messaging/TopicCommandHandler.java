package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.topic.CreateTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.topic.DeleteTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.service.TopicService;

import java.util.logging.Logger;

@Service
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
            case GetTopicByIdCommand cmd -> handleGetTopicById(cmd);
            case CreateTopicCommand cmd -> handleCreateTopic(cmd);
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

    private Object handleDeleteTopic(DeleteTopicCommand command) {
        topicService.deleteTopic(command.getTopicAggregateId(), command.getUnitOfWork());
        return null;
    }
}
