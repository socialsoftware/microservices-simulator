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
import java.util.List;

public class AddQuestionTopicsFunctionalitySagas extends WorkflowFunctionality {
    private List<QuestionTopicDto> addedTopicDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddQuestionTopicsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer questionId, List<QuestionTopicDto> topicDtos, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionId, topicDtos, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, List<QuestionTopicDto> topicDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addTopicsStep = new SagaStep("addTopicsStep", () -> {
            AddQuestionTopicsCommand cmd = new AddQuestionTopicsCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionId, topicDtos);
            List<QuestionTopicDto> addedTopicDtos = (List<QuestionTopicDto>) commandGateway.send(cmd);
            setAddedTopicDtos(addedTopicDtos);
        });

        workflow.addStep(addTopicsStep);
    }
    public List<QuestionTopicDto> getAddedTopicDtos() {
        return addedTopicDtos;
    }

    public void setAddedTopicDtos(List<QuestionTopicDto> addedTopicDtos) {
        this.addedTopicDtos = addedTopicDtos;
    }
}
