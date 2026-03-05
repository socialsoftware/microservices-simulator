package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.question.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateQuestionOptionFunctionalitySagas extends WorkflowFunctionality {
    private OptionDto updatedOptionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateQuestionOptionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer questionId, Integer key, OptionDto optionDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionId, key, optionDto, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, Integer key, OptionDto optionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateOptionStep = new SagaStep("updateOptionStep", () -> {
            UpdateQuestionOptionCommand cmd = new UpdateQuestionOptionCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionId, key, optionDto);
            OptionDto updatedOptionDto = (OptionDto) commandGateway.send(cmd);
            setUpdatedOptionDto(updatedOptionDto);
        });

        workflow.addStep(updateOptionStep);
    }
    public OptionDto getUpdatedOptionDto() {
        return updatedOptionDto;
    }

    public void setUpdatedOptionDto(OptionDto updatedOptionDto) {
        this.updatedOptionDto = updatedOptionDto;
    }
}
