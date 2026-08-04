package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.topic.GetTopicsByCourseCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.service.TopicService;

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
            case GetTopicByIdCommand cmd -> handleGetTopicById(cmd);
            case GetTopicsByCourseCommand cmd -> handleGetTopicsByCourse(cmd);
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetTopicById(GetTopicByIdCommand command) {
        return topicService.getTopicById(command.getTopicAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetTopicsByCourse(GetTopicsByCourseCommand command) {
        return topicService.getTopicsByCourse(command.getCourseAggregateId(), command.getUnitOfWork());
    }
}
