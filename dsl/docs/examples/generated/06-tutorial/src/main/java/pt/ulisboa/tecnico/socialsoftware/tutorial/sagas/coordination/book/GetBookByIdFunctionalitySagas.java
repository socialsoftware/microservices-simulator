package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.coordination.book;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.service.BookService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetBookByIdFunctionalitySagas extends WorkflowFunctionality {
    private BookDto bookDto;
    private final BookService bookService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetBookByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, BookService bookService, Integer bookAggregateId) {
        this.bookService = bookService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(bookAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer bookAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getBookStep = new SagaSyncStep("getBookStep", () -> {
            BookDto bookDto = bookService.getBookById(bookAggregateId, unitOfWork);
            setBookDto(bookDto);
        });

        workflow.addStep(getBookStep);
    }
    public BookDto getBookDto() {
        return bookDto;
    }

    public void setBookDto(BookDto bookDto) {
        this.bookDto = bookDto;
    }
}
