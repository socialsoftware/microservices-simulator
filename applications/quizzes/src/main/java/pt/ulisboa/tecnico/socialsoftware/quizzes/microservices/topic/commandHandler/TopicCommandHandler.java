package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.CreateTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.FindTopicsByCourseIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.UpdateTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.DeleteTopicCommand;

@Service
public class TopicCommandHandler implements CommandHandler {

    @Autowired
    private TopicService topicService;

    @Override
    public Object handle(Command command) {
        if (command instanceof GetTopicByIdCommand) {
            return handleGetTopicById((GetTopicByIdCommand) command);
        } else if (command instanceof CreateTopicCommand) {
            return handleCreateTopic((CreateTopicCommand) command);
        } else if (command instanceof FindTopicsByCourseIdCommand) {
            return handleFindTopicsByCourseId((FindTopicsByCourseIdCommand) command);
        } else if (command instanceof UpdateTopicCommand) {
            return handleUpdateTopic((UpdateTopicCommand) command);
        } else if (command instanceof DeleteTopicCommand) {
            return handleDeleteTopic((DeleteTopicCommand) command);
        } else {
            throw new UnsupportedOperationException("Command not supported: " + command.getClass().getName());
        }
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
