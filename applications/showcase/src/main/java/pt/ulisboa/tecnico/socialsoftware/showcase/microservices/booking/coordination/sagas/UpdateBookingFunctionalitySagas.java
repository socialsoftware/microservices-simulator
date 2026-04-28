package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.showcase.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.showcase.command.booking.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.sagas.states.BookingSagaState;

public class UpdateBookingFunctionalitySagas extends WorkflowFunctionality {
    private BookingDto updatedBookingDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateBookingFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, BookingDto bookingDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(bookingDto, unitOfWork);
    }

    public void buildWorkflow(BookingDto bookingDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateBookingStep = new SagaStep("updateBookingStep", () -> {
            unitOfWorkService.verifySagaState(bookingDto.getAggregateId(), new java.util.ArrayList<SagaState>(java.util.Arrays.asList(BookingSagaState.READ_BOOKING, BookingSagaState.UPDATE_BOOKING, BookingSagaState.DELETE_BOOKING)));
            unitOfWorkService.registerSagaState(bookingDto.getAggregateId(), BookingSagaState.UPDATE_BOOKING, unitOfWork);
            UpdateBookingCommand cmd = new UpdateBookingCommand(unitOfWork, ServiceMapping.BOOKING.getServiceName(), bookingDto);
            BookingDto updatedBookingDto = (BookingDto) commandGateway.send(cmd);
            setUpdatedBookingDto(updatedBookingDto);
        });

        workflow.addStep(updateBookingStep);
    }
    public BookingDto getUpdatedBookingDto() {
        return updatedBookingDto;
    }

    public void setUpdatedBookingDto(BookingDto updatedBookingDto) {
        this.updatedBookingDto = updatedBookingDto;
    }
}
