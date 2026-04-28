package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.topic.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate.sagas.states.TopicSagaState;

public class GetTopicByIdFunctionalitySagas extends WorkflowFunctionality {
    private TopicDto topicDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetTopicByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer topicAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer topicAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTopicStep = new SagaStep("getTopicStep", () -> {
            unitOfWorkService.verifySagaState(topicAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(TopicSagaState.UPDATE_TOPIC, TopicSagaState.DELETE_TOPIC)));
            unitOfWorkService.registerSagaState(topicAggregateId, TopicSagaState.READ_TOPIC, unitOfWork);
            GetTopicByIdCommand cmd = new GetTopicByIdCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
            TopicDto topicDto = (TopicDto) commandGateway.send(cmd);
            setTopicDto(topicDto);
        });

        workflow.addStep(getTopicStep);
    }
    public TopicDto getTopicDto() {
        return topicDto;
    }

    public void setTopicDto(TopicDto topicDto) {
        this.topicDto = topicDto;
    }
}
