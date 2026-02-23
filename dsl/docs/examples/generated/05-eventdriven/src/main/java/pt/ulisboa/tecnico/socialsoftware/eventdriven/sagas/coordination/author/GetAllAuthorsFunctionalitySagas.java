package pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.coordination.author;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.service.AuthorService;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.shared.dtos.AuthorDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllAuthorsFunctionalitySagas extends WorkflowFunctionality {
    private List<AuthorDto> authors;
    private final AuthorService authorService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllAuthorsFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, AuthorService authorService) {
        this.authorService = authorService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllAuthorsStep = new SagaSyncStep("getAllAuthorsStep", () -> {
            List<AuthorDto> authors = authorService.getAllAuthors(unitOfWork);
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
