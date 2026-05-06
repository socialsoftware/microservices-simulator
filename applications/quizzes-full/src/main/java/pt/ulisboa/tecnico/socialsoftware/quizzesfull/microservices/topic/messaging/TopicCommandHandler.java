package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.topic.CreateTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.service.TopicService;

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
            case GetTopicByIdCommand cmd -> topicService.getTopicById(cmd.getTopicAggregateId(), cmd.getUnitOfWork());
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateTopic(CreateTopicCommand command) {
        return topicService.createTopic(command.getTopicDto(), command.getUnitOfWork());
    }
}
