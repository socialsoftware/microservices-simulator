package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;

import java.util.logging.Logger;

@Service
public class TopicCommandHandler implements CommandHandler {

    @Autowired
    private TopicService topicService;

    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Override
    public Object handle(Command command) {
        if (command.getForbiddenStates() != null && !command.getForbiddenStates().isEmpty()) {
            sagaUnitOfWorkService.verifySagaState(command.getRootAggregateId(), command.getForbiddenStates());
        }
        Object returnObject;
        if (command instanceof GetTopicByIdCommand) {
            returnObject = handleGetTopicById((GetTopicByIdCommand) command);
        } else if (command instanceof CreateTopicCommand) {
            returnObject = handleCreateTopic((CreateTopicCommand) command);
        } else if (command instanceof FindTopicsByCourseIdCommand) {
            returnObject = handleFindTopicsByCourseId((FindTopicsByCourseIdCommand) command);
        } else if (command instanceof UpdateTopicCommand) {
            returnObject = handleUpdateTopic((UpdateTopicCommand) command);
        } else if (command instanceof DeleteTopicCommand) {
            returnObject = handleDeleteTopic((DeleteTopicCommand) command);
        } else {
            Logger.getLogger(TopicCommandHandler.class.getName()).warning("Unknown command type: " + command.getClass().getName());
            returnObject = null;
        }
        if (command.getSemanticLock() != null) {
            sagaUnitOfWorkService.registerSagaState(command.getRootAggregateId(), command.getSemanticLock(),
                    (SagaUnitOfWork) command.getUnitOfWork());
        }
        return returnObject;
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
