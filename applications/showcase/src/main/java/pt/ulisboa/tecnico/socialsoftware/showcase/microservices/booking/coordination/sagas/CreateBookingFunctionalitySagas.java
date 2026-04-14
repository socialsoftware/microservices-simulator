package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.showcase.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.showcase.command.booking.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.webapi.requestDtos.CreateBookingRequestDto;

public class CreateBookingFunctionalitySagas extends WorkflowFunctionality {
    private BookingDto createdBookingDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateBookingFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateBookingRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateBookingRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createBookingStep = new SagaStep("createBookingStep", () -> {
            CreateBookingCommand cmd = new CreateBookingCommand(unitOfWork, ServiceMapping.BOOKING.getServiceName(), createRequest);
            BookingDto createdBookingDto = (BookingDto) commandGateway.send(cmd);
            setCreatedBookingDto(createdBookingDto);
        });

        workflow.addStep(createBookingStep);
    }
    public BookingDto getCreatedBookingDto() {
        return createdBookingDto;
    }

    public void setCreatedBookingDto(BookingDto createdBookingDto) {
        this.createdBookingDto = createdBookingDto;
    }
}
