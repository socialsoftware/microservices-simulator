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

public class AddQuestionTopicFunctionalitySagas extends WorkflowFunctionality {
    private QuestionTopicDto addedTopicDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddQuestionTopicFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer questionId, Integer topicAggregateId, QuestionTopicDto topicDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionId, topicAggregateId, topicDto, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, Integer topicAggregateId, QuestionTopicDto topicDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addTopicStep = new SagaStep("addTopicStep", () -> {
            AddQuestionTopicCommand cmd = new AddQuestionTopicCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionId, topicAggregateId, topicDto);
            QuestionTopicDto addedTopicDto = (QuestionTopicDto) commandGateway.send(cmd);
            setAddedTopicDto(addedTopicDto);
        });

        workflow.addStep(addTopicStep);
    }
    public QuestionTopicDto getAddedTopicDto() {
        return addedTopicDto;
    }

    public void setAddedTopicDto(QuestionTopicDto addedTopicDto) {
        this.addedTopicDto = addedTopicDto;
    }
}
