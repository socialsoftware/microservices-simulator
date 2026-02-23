package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.coordination.book;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.service.BookService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteBookFunctionalitySagas extends WorkflowFunctionality {
    private final BookService bookService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeleteBookFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, BookService bookService, Integer bookAggregateId) {
        this.bookService = bookService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(bookAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer bookAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep deleteBookStep = new SagaSyncStep("deleteBookStep", () -> {
            bookService.deleteBook(bookAggregateId, unitOfWork);
        });

        workflow.addStep(deleteBookStep);
    }
}
