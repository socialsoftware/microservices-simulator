package pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.coordination.author;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.service.AuthorService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetAuthorByIdFunctionalitySagas extends WorkflowFunctionality {
    private AuthorDto authorDto;
    private final AuthorService authorService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAuthorByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AuthorService authorService, Integer authorAggregateId) {
        this.authorService = authorService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(authorAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer authorAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAuthorStep = new SagaSyncStep("getAuthorStep", () -> {
            AuthorDto authorDto = authorService.getAuthorById(authorAggregateId, unitOfWork);
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
