package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip.DeleteTripCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip.GetTripByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.states.TripSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class DeleteTripFunctionalitySagas extends WorkflowFunctionality {
    private TripDto tripDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public DeleteTripFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                        Integer tripAggregateId,
                                        SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(tripAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tripAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTripStep = new SagaStep("getTripStep", () -> {
            GetTripByIdCommand readCmd = new GetTripByIdCommand(
                    unitOfWork, ServiceMapping.TRIP.getServiceName(), tripAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(TripSagaState.IN_DELETE_TRIP);
            this.tripDto = (TripDto) commandGateway.send(sagaCommand);
        });

        SagaStep deleteTripStep = new SagaStep("deleteTripStep", () -> {
            DeleteTripCommand cmd = new DeleteTripCommand(
                    unitOfWork, ServiceMapping.TRIP.getServiceName(), tripAggregateId);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTripStep)));

        this.workflow.addStep(getTripStep);
        this.workflow.addStep(deleteTripStep);
    }

    public TripDto getTripDto() {
        return tripDto;
    }
}
