package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.answer.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.AnswerDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateAnswerFunctionalitySagas extends WorkflowFunctionality {
    private AnswerDto updatedAnswerDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateAnswerFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, AnswerDto answerDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(answerDto, unitOfWork);
    }

    public void buildWorkflow(AnswerDto answerDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateAnswerStep = new SagaStep("updateAnswerStep", () -> {
            UpdateAnswerCommand cmd = new UpdateAnswerCommand(unitOfWork, ServiceMapping.ANSWER.getServiceName(), answerDto);
            AnswerDto updatedAnswerDto = (AnswerDto) commandGateway.send(cmd);
            setUpdatedAnswerDto(updatedAnswerDto);
        });

        workflow.addStep(updateAnswerStep);
    }
    public AnswerDto getUpdatedAnswerDto() {
        return updatedAnswerDto;
    }

    public void setUpdatedAnswerDto(AnswerDto updatedAnswerDto) {
        this.updatedAnswerDto = updatedAnswerDto;
    }
}
