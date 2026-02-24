package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.DeleteTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.sagas.states.TopicSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteTopicFunctionalitySagas extends WorkflowFunctionality {
    private TopicDto topic;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public DeleteTopicFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                         Integer topicAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer topicAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTopicStep = new SagaStep("getTopicStep", () -> {
            GetTopicByIdCommand getTopicByIdCommand = new GetTopicByIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
            getTopicByIdCommand.setSemanticLock(TopicSagaState.READ_TOPIC);
            TopicDto topicDto = (TopicDto) commandGateway.send(getTopicByIdCommand);
            this.setTopic(topicDto);
        });

        getTopicStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaStep deleteTopicStep = new SagaStep("deleteTopicStep", () -> {
            DeleteTopicCommand deleteTopicCommand = new DeleteTopicCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
            commandGateway.send(deleteTopicCommand);
        }, new ArrayList<>(Arrays.asList(getTopicStep)));

        workflow.addStep(getTopicStep);
        workflow.addStep(deleteTopicStep);
    }

    public TopicDto getTopic() {
        return topic;
    }

    public void setTopic(TopicDto topic) {
        this.topic = topic;
    }
}