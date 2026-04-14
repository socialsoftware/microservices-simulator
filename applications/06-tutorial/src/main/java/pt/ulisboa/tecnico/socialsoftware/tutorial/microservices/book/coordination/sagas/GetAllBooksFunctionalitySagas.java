package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.tutorial.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.tutorial.command.book.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllBooksFunctionalitySagas extends WorkflowFunctionality {
    private List<BookDto> books;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllBooksFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllBooksStep = new SagaStep("getAllBooksStep", () -> {
            GetAllBooksCommand cmd = new GetAllBooksCommand(unitOfWork, ServiceMapping.BOOK.getServiceName());
            List<BookDto> books = (List<BookDto>) commandGateway.send(cmd);
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
