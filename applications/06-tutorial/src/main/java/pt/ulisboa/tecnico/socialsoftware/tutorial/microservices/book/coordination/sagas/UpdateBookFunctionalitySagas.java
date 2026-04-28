package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.tutorial.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.tutorial.command.book.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.BookDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.sagas.states.BookSagaState;

public class UpdateBookFunctionalitySagas extends WorkflowFunctionality {
    private BookDto updatedBookDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateBookFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, BookDto bookDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(bookDto, unitOfWork);
    }

    public void buildWorkflow(BookDto bookDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateBookStep = new SagaStep("updateBookStep", () -> {
            unitOfWorkService.verifySagaState(bookDto.getAggregateId(), new java.util.ArrayList<SagaState>(java.util.Arrays.asList(BookSagaState.READ_BOOK, BookSagaState.UPDATE_BOOK, BookSagaState.DELETE_BOOK)));
            unitOfWorkService.registerSagaState(bookDto.getAggregateId(), BookSagaState.UPDATE_BOOK, unitOfWork);
            UpdateBookCommand cmd = new UpdateBookCommand(unitOfWork, ServiceMapping.BOOK.getServiceName(), bookDto);
            BookDto updatedBookDto = (BookDto) commandGateway.send(cmd);
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
