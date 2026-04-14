package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.command.author.*;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateAuthorFunctionalitySagas extends WorkflowFunctionality {
    private AuthorDto updatedAuthorDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateAuthorFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, AuthorDto authorDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(authorDto, unitOfWork);
    }

    public void buildWorkflow(AuthorDto authorDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateAuthorStep = new SagaStep("updateAuthorStep", () -> {
            UpdateAuthorCommand cmd = new UpdateAuthorCommand(unitOfWork, ServiceMapping.AUTHOR.getServiceName(), authorDto);
            AuthorDto updatedAuthorDto = (AuthorDto) commandGateway.send(cmd);
            setUpdatedAuthorDto(updatedAuthorDto);
        });

        workflow.addStep(updateAuthorStep);
    }
    public AuthorDto getUpdatedAuthorDto() {
        return updatedAuthorDto;
    }

    public void setUpdatedAuthorDto(AuthorDto updatedAuthorDto) {
        this.updatedAuthorDto = updatedAuthorDto;
    }
}
