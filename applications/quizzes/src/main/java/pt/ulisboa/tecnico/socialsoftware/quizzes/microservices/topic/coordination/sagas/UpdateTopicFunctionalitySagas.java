package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.topic.UpdateTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.sagas.states.TopicSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class UpdateTopicFunctionalitySagas extends WorkflowFunctionality {
    private TopicDto topic;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateTopicFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                         TopicDto topicDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(topicDto, unitOfWork);
    }

    public void buildWorkflow(TopicDto topicDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTopicStep = new SagaStep("getTopicStep", () -> {
            GetTopicByIdCommand getTopicByIdCommand = new GetTopicByIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicDto.getAggregateId());
            getTopicByIdCommand.setSemanticLock(TopicSagaState.READ_TOPIC);
            TopicDto topic = (TopicDto) commandGateway.send(getTopicByIdCommand);
            this.setTopic(topic);
        });

        getTopicStep.registerCompensation(() -> {
            Command command = new Command(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topic.getAggregateId());
            command.setSemanticLock(GenericSagaState.NOT_IN_SAGA);
            commandGateway.send(command);
        }, unitOfWork);

        SagaStep updateTopicStep = new SagaStep("updateTopicStep", () -> {
            UpdateTopicCommand updateTopicCommand = new UpdateTopicCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicDto);
            commandGateway.send(updateTopicCommand);
        }, new ArrayList<>(Arrays.asList(getTopicStep)));

        workflow.addStep(getTopicStep);
        workflow.addStep(updateTopicStep);
    }

    public TopicDto getTopic() {
        return topic;
    }

    public void setTopic(TopicDto topic) {
        this.topic = topic;
    }
}