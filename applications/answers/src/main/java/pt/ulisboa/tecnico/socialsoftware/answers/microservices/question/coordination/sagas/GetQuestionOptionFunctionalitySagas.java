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

public class GetQuestionOptionFunctionalitySagas extends WorkflowFunctionality {
    private OptionDto optionDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetQuestionOptionFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer questionId, Integer key, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionId, key, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, Integer key, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getOptionStep = new SagaStep("getOptionStep", () -> {
            GetQuestionOptionCommand cmd = new GetQuestionOptionCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionId, key);
            OptionDto optionDto = (OptionDto) commandGateway.send(cmd);
            setOptionDto(optionDto);
        });

        workflow.addStep(getOptionStep);
    }
    public OptionDto getOptionDto() {
        return optionDto;
    }

    public void setOptionDto(OptionDto optionDto) {
        this.optionDto = optionDto;
    }
}
