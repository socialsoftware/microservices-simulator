package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.eventProcessing;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.RemoveTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.question.UpdateTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.DeleteTopicEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.events.publish.UpdateTopicEvent;

@Service
public class QuestionEventProcessing {
    private static final Logger logger = org.slf4j.LoggerFactory.getLogger(QuestionEventProcessing.class);
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;
    private final CommandGateway commandGateway;

    @Autowired
    public QuestionEventProcessing(UnitOfWorkService unitOfWorkService, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
    }

    /************************************************
     * EVENT PROCESSING
     ************************************************/

    public void processUpdateTopic(Integer aggregateId, UpdateTopicEvent updateTopicEvent) {
        logger.info("Processing UpdateTopicEvent: aggregateId={}, event={}", aggregateId, updateTopicEvent);
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        UpdateTopicCommand command = new UpdateTopicCommand(
                unitOfWork,
                ServiceMapping.QUESTION.getServiceName(),
                aggregateId,
                updateTopicEvent.getPublisherAggregateId(),
                updateTopicEvent.getTopicName(),
                updateTopicEvent.getPublisherAggregateVersion());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processDeleteTopic(Integer aggregateId, DeleteTopicEvent deleteTopicEvent) {
        logger.info("Processing DeleteTopicEvent: aggregateId={}, event={}", aggregateId, deleteTopicEvent);
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        RemoveTopicCommand command = new RemoveTopicCommand(
                unitOfWork,
                ServiceMapping.QUESTION.getServiceName(),
                aggregateId,
                deleteTopicEvent.getPublisherAggregateId(),
                deleteTopicEvent.getPublisherAggregateVersion());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }
}
