package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.coordination.book;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.service.BookService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllBooksFunctionalitySagas extends WorkflowFunctionality {
    private List<BookDto> books;
    private final BookService bookService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllBooksFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, BookService bookService) {
        this.bookService = bookService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllBooksStep = new SagaSyncStep("getAllBooksStep", () -> {
            List<BookDto> books = bookService.getAllBooks(unitOfWork);
            setBooks(books);
        });

        workflow.addStep(getAllBooksStep);
    }
    public List<BookDto> getBooks() {
        return books;
    }

    public void setBooks(List<BookDto> books) {
        this.books = books;
    }
}
