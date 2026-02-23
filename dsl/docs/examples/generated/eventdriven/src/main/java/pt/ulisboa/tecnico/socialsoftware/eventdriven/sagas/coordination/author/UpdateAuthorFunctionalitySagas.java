package pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.coordination.author;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.service.AuthorService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateAuthorFunctionalitySagas extends WorkflowFunctionality {
    private AuthorDto updatedAuthorDto;
    private final AuthorService authorService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateAuthorFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AuthorService authorService, AuthorDto authorDto) {
        this.authorService = authorService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(authorDto, unitOfWork);
    }

    public void buildWorkflow(AuthorDto authorDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateAuthorStep = new SagaSyncStep("updateAuthorStep", () -> {
            AuthorDto updatedAuthorDto = authorService.updateAuthor(authorDto, unitOfWork);
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
