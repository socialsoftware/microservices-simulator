package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.topic.DeleteTopicCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.commands.topic.GetTopicByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.sagas.states.TopicSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteTopicFunctionalitySagas extends WorkflowFunctionality {
    private TopicDto topicDto;

    public DeleteTopicFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer topicAggregateId,
                                         SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, topicAggregateId, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, Integer topicAggregateId,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTopicStep = new SagaStep("getTopicStep", () -> {
            GetTopicByIdCommand readCmd = new GetTopicByIdCommand(
                    unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(TopicSagaState.IN_DELETE_TOPIC);
            this.topicDto = (TopicDto) commandGateway.send(sagaCommand);
        });

        SagaStep deleteTopicStep = new SagaStep("deleteTopicStep", () -> {
            DeleteTopicCommand cmd = new DeleteTopicCommand(
                    unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicAggregateId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTopicStep)));

        this.workflow.addStep(getTopicStep);
        this.workflow.addStep(deleteTopicStep);
    }

    public TopicDto getTopicDto() {
        return topicDto;
    }
}
