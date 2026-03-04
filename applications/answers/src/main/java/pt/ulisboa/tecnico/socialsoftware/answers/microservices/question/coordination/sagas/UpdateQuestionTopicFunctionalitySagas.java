package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.question.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionTopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateQuestionTopicFunctionalitySagas extends WorkflowFunctionality {
    private QuestionTopicDto updatedTopicDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateQuestionTopicFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer questionId, Integer topicAggregateId, QuestionTopicDto topicDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionId, topicAggregateId, topicDto, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, Integer topicAggregateId, QuestionTopicDto topicDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateTopicStep = new SagaStep("updateTopicStep", () -> {
            UpdateQuestionTopicCommand cmd = new UpdateQuestionTopicCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionId, topicAggregateId, topicDto);
            QuestionTopicDto updatedTopicDto = (QuestionTopicDto) commandGateway.send(cmd);
            setUpdatedTopicDto(updatedTopicDto);
        });

        workflow.addStep(updateTopicStep);
    }
    public QuestionTopicDto getUpdatedTopicDto() {
        return updatedTopicDto;
    }

    public void setUpdatedTopicDto(QuestionTopicDto updatedTopicDto) {
        this.updatedTopicDto = updatedTopicDto;
    }
}
