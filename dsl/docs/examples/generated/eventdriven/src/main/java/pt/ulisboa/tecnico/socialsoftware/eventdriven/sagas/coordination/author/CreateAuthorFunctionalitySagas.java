package pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.coordination.author;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.service.AuthorService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.coordination.webapi.requestDtos.CreateAuthorRequestDto;

public class CreateAuthorFunctionalitySagas extends WorkflowFunctionality {
    private AuthorDto createdAuthorDto;
    private final AuthorService authorService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateAuthorFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AuthorService authorService, CreateAuthorRequestDto createRequest) {
        this.authorService = authorService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateAuthorRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createAuthorStep = new SagaSyncStep("createAuthorStep", () -> {
            AuthorDto createdAuthorDto = authorService.createAuthor(createRequest, unitOfWork);
            setCreatedAuthorDto(createdAuthorDto);
        });

        workflow.addStep(createAuthorStep);
    }
    public AuthorDto getCreatedAuthorDto() {
        return createdAuthorDto;
    }

    public void setCreatedAuthorDto(AuthorDto createdAuthorDto) {
        this.createdAuthorDto = createdAuthorDto;
    }
}
