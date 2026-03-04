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
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.coordination.webapi.requestDtos.CreateBookRequestDto;

public class CreateBookFunctionalitySagas extends WorkflowFunctionality {
    private BookDto createdBookDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateBookFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateBookRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateBookRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createBookStep = new SagaStep("createBookStep", () -> {
            CreateBookCommand cmd = new CreateBookCommand(unitOfWork, ServiceMapping.BOOK.getServiceName(), createRequest);
            BookDto createdBookDto = (BookDto) commandGateway.send(cmd);
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
