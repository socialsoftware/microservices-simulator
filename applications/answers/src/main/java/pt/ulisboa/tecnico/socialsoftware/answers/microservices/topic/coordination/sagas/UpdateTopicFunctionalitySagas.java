package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.topic.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateTopicFunctionalitySagas extends WorkflowFunctionality {
    private TopicDto updatedTopicDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateTopicFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, TopicDto topicDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(topicDto, unitOfWork);
    }

    public void buildWorkflow(TopicDto topicDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateTopicStep = new SagaStep("updateTopicStep", () -> {
            UpdateTopicCommand cmd = new UpdateTopicCommand(unitOfWork, ServiceMapping.TOPIC.getServiceName(), topicDto);
            TopicDto updatedTopicDto = (TopicDto) commandGateway.send(cmd);
            setUpdatedTopicDto(updatedTopicDto);
        });

        workflow.addStep(updateTopicStep);
    }
    public TopicDto getUpdatedTopicDto() {
        return updatedTopicDto;
    }

    public void setUpdatedTopicDto(TopicDto updatedTopicDto) {
        this.updatedTopicDto = updatedTopicDto;
    }
}
