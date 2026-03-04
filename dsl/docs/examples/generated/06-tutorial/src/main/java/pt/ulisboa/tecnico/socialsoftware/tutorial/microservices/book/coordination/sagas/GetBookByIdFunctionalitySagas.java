package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.tutorial.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.tutorial.command.book.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetBookByIdFunctionalitySagas extends WorkflowFunctionality {
    private BookDto bookDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetBookByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer bookAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(bookAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer bookAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getBookStep = new SagaStep("getBookStep", () -> {
            GetBookByIdCommand cmd = new GetBookByIdCommand(unitOfWork, ServiceMapping.BOOK.getServiceName(), bookAggregateId);
            BookDto bookDto = (BookDto) commandGateway.send(cmd);
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
