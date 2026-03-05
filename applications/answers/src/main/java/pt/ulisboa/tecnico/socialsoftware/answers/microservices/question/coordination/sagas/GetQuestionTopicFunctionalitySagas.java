package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.question.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionTopicDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetQuestionTopicFunctionalitySagas extends WorkflowFunctionality {
    private QuestionTopicDto topicDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetQuestionTopicFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer questionId, Integer topicAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionId, topicAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, Integer topicAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTopicStep = new SagaStep("getTopicStep", () -> {
            GetQuestionTopicCommand cmd = new GetQuestionTopicCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionId, topicAggregateId);
            QuestionTopicDto topicDto = (QuestionTopicDto) commandGateway.send(cmd);
            setTopicDto(topicDto);
        });

        workflow.addStep(getTopicStep);
    }
    public QuestionTopicDto getTopicDto() {
        return topicDto;
    }

    public void setTopicDto(QuestionTopicDto topicDto) {
        this.topicDto = topicDto;
    }
}
