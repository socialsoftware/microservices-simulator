package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.answer.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateAnswerQuestionFunctionalitySagas extends WorkflowFunctionality {
    private AnswerQuestionDto updatedQuestionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateAnswerQuestionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer answerId, Integer questionAggregateId, AnswerQuestionDto questionDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(answerId, questionAggregateId, questionDto, unitOfWork);
    }

    public void buildWorkflow(Integer answerId, Integer questionAggregateId, AnswerQuestionDto questionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateQuestionStep = new SagaStep("updateQuestionStep", () -> {
            UpdateAnswerQuestionCommand cmd = new UpdateAnswerQuestionCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(), answerId, questionAggregateId, questionDto);
            AnswerQuestionDto updatedQuestionDto = (AnswerQuestionDto) commandGateway.send(cmd);
            setUpdatedQuestionDto(updatedQuestionDto);
        });

        workflow.addStep(updateQuestionStep);
    }
    public AnswerQuestionDto getUpdatedQuestionDto() {
        return updatedQuestionDto;
    }

    public void setUpdatedQuestionDto(AnswerQuestionDto updatedQuestionDto) {
        this.updatedQuestionDto = updatedQuestionDto;
    }
}
