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

public class AddQuestionOptionFunctionalitySagas extends WorkflowFunctionality {
    private OptionDto addedOptionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddQuestionOptionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer questionId, Integer key, OptionDto optionDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionId, key, optionDto, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, Integer key, OptionDto optionDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addOptionStep = new SagaStep("addOptionStep", () -> {
            AddQuestionOptionCommand cmd = new AddQuestionOptionCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionId, key, optionDto);
            OptionDto addedOptionDto = (OptionDto) commandGateway.send(cmd);
            setAddedOptionDto(addedOptionDto);
        });

        workflow.addStep(addOptionStep);
    }
    public OptionDto getAddedOptionDto() {
        return addedOptionDto;
    }

    public void setAddedOptionDto(OptionDto addedOptionDto) {
        this.addedOptionDto = addedOptionDto;
    }
}
