package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.showcase.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.showcase.command.booking.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.service.BookingService;

public class ConfirmBookingFunctionalitySagas extends WorkflowFunctionality {
    
        private final SagaUnitOfWorkService sagaUnitOfWorkService;
    private final BookingService bookingService;
    private final SagaUnitOfWork unitOfWork;
    private final CommandGateway commandGateway;

    public ConfirmBookingFunctionalitySagas(SagaUnitOfWorkService sagaUnitOfWorkService, BookingService bookingService, Integer bookingId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.sagaUnitOfWorkService = sagaUnitOfWorkService;
        this.bookingService = bookingService;
        this.unitOfWork = unitOfWork;
        this.commandGateway = commandGateway;
        this.buildWorkflow(bookingId);
    }

    public void buildWorkflow(Integer bookingId) {
        this.workflow = new SagaWorkflow(this, this.sagaUnitOfWorkService, this.unitOfWork);
        SagaStep confirmBookingStep = new SagaStep("confirmBookingStep", () -> {
            this.bookingService.confirmBooking(bookingId, this.unitOfWork);
        });
        this.workflow.addStep(confirmBookingStep);
    }

}
