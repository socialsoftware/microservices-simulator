package pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.question.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.OptionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class AddQuestionOptionsFunctionalitySagas extends WorkflowFunctionality {
    private List<OptionDto> addedOptionDtos;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public AddQuestionOptionsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer questionId, List<OptionDto> optionDtos, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(questionId, optionDtos, unitOfWork);
    }

    public void buildWorkflow(Integer questionId, List<OptionDto> optionDtos, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep addOptionsStep = new SagaStep("addOptionsStep", () -> {
            AddQuestionOptionsCommand cmd = new AddQuestionOptionsCommand(unitOfWork, ServiceMapping.QUESTION.getServiceName(), questionId, optionDtos);
            List<OptionDto> addedOptionDtos = (List<OptionDto>) commandGateway.send(cmd);
            setAddedOptionDtos(addedOptionDtos);
        });

        workflow.addStep(addOptionsStep);
    }
    public List<OptionDto> getAddedOptionDtos() {
        return addedOptionDtos;
    }

    public void setAddedOptionDtos(List<OptionDto> addedOptionDtos) {
        this.addedOptionDtos = addedOptionDtos;
    }
}
