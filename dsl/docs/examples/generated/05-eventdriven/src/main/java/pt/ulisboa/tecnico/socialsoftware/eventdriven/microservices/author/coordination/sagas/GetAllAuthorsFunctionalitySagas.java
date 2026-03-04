package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.command.author.*;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllAuthorsFunctionalitySagas extends WorkflowFunctionality {
    private List<AuthorDto> authors;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllAuthorsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllAuthorsStep = new SagaStep("getAllAuthorsStep", () -> {
            GetAllAuthorsCommand cmd = new GetAllAuthorsCommand(unitOfWork, ServiceMapping.AUTHOR.getServiceName());
            List<AuthorDto> authors = (List<AuthorDto>) commandGateway.send(cmd);
            setAuthors(authors);
        });

        workflow.addStep(getAllAuthorsStep);
    }
    public List<AuthorDto> getAuthors() {
        return authors;
    }

    public void setAuthors(List<AuthorDto> authors) {
        this.authors = authors;
    }
}
