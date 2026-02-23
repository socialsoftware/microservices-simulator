package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.coordination.book;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.service.BookService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateBookFunctionalitySagas extends WorkflowFunctionality {
    private BookDto updatedBookDto;
    private final BookService bookService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateBookFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, BookService bookService, BookDto bookDto) {
        this.bookService = bookService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(bookDto, unitOfWork);
    }

    public void buildWorkflow(BookDto bookDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateBookStep = new SagaSyncStep("updateBookStep", () -> {
            BookDto updatedBookDto = bookService.updateBook(bookDto, unitOfWork);
            setUpdatedBookDto(updatedBookDto);
        });

        workflow.addStep(updateBookStep);
    }
    public BookDto getUpdatedBookDto() {
        return updatedBookDto;
    }

    public void setUpdatedBookDto(BookDto updatedBookDto) {
        this.updatedBookDto = updatedBookDto;
    }
}
