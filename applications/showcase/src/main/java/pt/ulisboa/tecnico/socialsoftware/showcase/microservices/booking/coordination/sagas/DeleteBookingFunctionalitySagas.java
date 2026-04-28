package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.showcase.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.showcase.command.booking.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.sagas.states.BookingSagaState;

public class DeleteBookingFunctionalitySagas extends WorkflowFunctionality {
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public DeleteBookingFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer bookingAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(bookingAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer bookingAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep deleteBookingStep = new SagaStep("deleteBookingStep", () -> {
            unitOfWorkService.verifySagaState(bookingAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(BookingSagaState.READ_BOOKING, BookingSagaState.UPDATE_BOOKING, BookingSagaState.DELETE_BOOKING)));
            unitOfWorkService.registerSagaState(bookingAggregateId, BookingSagaState.DELETE_BOOKING, unitOfWork);
            DeleteBookingCommand cmd = new DeleteBookingCommand(unitOfWork, ServiceMapping.BOOKING.getServiceName(), bookingAggregateId);
            commandGateway.send(cmd);
        });

        workflow.addStep(deleteBookingStep);
    }
}
