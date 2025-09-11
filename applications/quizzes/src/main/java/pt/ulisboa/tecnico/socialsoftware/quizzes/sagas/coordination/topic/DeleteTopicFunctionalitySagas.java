package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.topic;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.DeleteTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos.SagaTopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.states.TopicSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteTopicFunctionalitySagas extends WorkflowFunctionality {

    private SagaTopicDto topic;

    private final TopicService topicService;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public DeleteTopicFunctionalitySagas(TopicService topicService, SagaUnitOfWorkService unitOfWorkService,
                                         Integer topicAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.topicService = topicService;
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer topicAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getTopicStep = new SagaSyncStep("getTopicStep", () -> {
//            SagaTopicDto topicDto = (SagaTopicDto) topicService.getTopicById(topicAggregateId, unitOfWork);
//            unitOfWorkService.registerSagaState(topicAggregateId, TopicSagaState.READ_TOPIC, unitOfWork);
            GetTopicByIdCommand getTopicByIdCommand = new GetTopicByIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
            getTopicByIdCommand.setSemanticLock(TopicSagaState.READ_TOPIC);
            SagaTopicDto topicDto = (SagaTopicDto) commandGateway.send(getTopicByIdCommand);
            this.setTopic(topicDto);
        });
    
        getTopicStep.registerCompensation(() -> {
//            unitOfWorkService.registerSagaState(topicAggregateId, GenericSagaState.NOT_IN_SAGA, unitOfWork);
            Command command = new Command(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);
    
        SagaSyncStep deleteTopicStep = new SagaSyncStep("deleteTopicStep", () -> {
//            topicService.deleteTopic(topicAggregateId, unitOfWork);
            DeleteTopicCommand deleteTopicCommand = new DeleteTopicCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
            commandGateway.send(deleteTopicCommand);
        }, new ArrayList<>(Arrays.asList(getTopicStep)));
    
        workflow.addStep(getTopicStep);
        workflow.addStep(deleteTopicStep);
    }
    public SagaTopicDto getTopic() {
        return topic;
    }

    public void setTopic(SagaTopicDto topic) {
        this.topic = topic;
    }
}