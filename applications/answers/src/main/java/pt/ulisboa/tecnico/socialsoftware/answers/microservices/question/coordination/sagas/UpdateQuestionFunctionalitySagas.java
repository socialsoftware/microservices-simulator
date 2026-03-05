package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.question.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.QuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateQuestionFunctionalitySagas extends WorkflowFunctionality {
    private QuestionDto updatedQuestionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, QuestionDto questionDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionDto, unitOfWork);
    }

    public void buildWorkflow(QuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateQuestionStep = new SagaStep("updateQuestionStep", () -> {
            UpdateQuestionCommand cmd = new UpdateQuestionCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionDto);
            QuestionDto updatedQuestionDto = (QuestionDto) commandGateway.send(cmd);
            setUpdatedQuestionDto(updatedQuestionDto);
        });

        workflow.addStep(updateQuestionStep);
    }
    public QuestionDto getUpdatedQuestionDto() {
        return updatedQuestionDto;
    }

    public void setUpdatedQuestionDto(QuestionDto updatedQuestionDto) {
        this.updatedQuestionDto = updatedQuestionDto;
    }
}
