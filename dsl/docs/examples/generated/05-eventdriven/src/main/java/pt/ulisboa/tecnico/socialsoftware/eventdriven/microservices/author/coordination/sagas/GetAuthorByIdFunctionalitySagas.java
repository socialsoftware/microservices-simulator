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

public class GetAuthorByIdFunctionalitySagas extends WorkflowFunctionality {
    private AuthorDto authorDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAuthorByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer authorAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(authorAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer authorAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAuthorStep = new SagaStep("getAuthorStep", () -> {
            GetAuthorByIdCommand cmd = new GetAuthorByIdCommand(unitOfWork, ServiceMapping.AUTHOR.getServiceName(), authorAggregateId);
            AuthorDto authorDto = (AuthorDto) commandGateway.send(cmd);
            setAuthorDto(authorDto);
        });

        workflow.addStep(getAuthorStep);
    }
    public AuthorDto getAuthorDto() {
        return authorDto;
    }

    public void setAuthorDto(AuthorDto authorDto) {
        this.authorDto = authorDto;
    }
}
