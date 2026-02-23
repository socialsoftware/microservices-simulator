package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.coordination.book;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.service.BookService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.webapi.requestDtos.CreateBookRequestDto;

public class CreateBookFunctionalitySagas extends WorkflowFunctionality {
    private BookDto createdBookDto;
    private final BookService bookService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateBookFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, BookService bookService, CreateBookRequestDto createRequest) {
        this.bookService = bookService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateBookRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createBookStep = new SagaSyncStep("createBookStep", () -> {
            BookDto createdBookDto = bookService.createBook(createRequest, unitOfWork);
            setCreatedBookDto(createdBookDto);
        });

        workflow.addStep(createBookStep);
    }
    public BookDto getCreatedBookDto() {
        return createdBookDto;
    }

    public void setCreatedBookDto(BookDto createdBookDto) {
        this.createdBookDto = createdBookDto;
    }
}
