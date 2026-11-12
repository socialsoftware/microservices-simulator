package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip.GetTripByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip.UpdateTripCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas.states.TripSagaState;

import java.util.ArrayList;
import java.util.Arrays;

public class UpdateTripFunctionalitySagas extends WorkflowFunctionality {
    private TripDto tripDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateTripFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                        Integer tripAggregateId, TripDto updatedTripDto,
                                        SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(tripAggregateId, updatedTripDto, unitOfWork);
    }

    public void buildWorkflow(Integer tripAggregateId, TripDto updatedTripDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTripStep = new SagaStep("getTripStep", () -> {
            GetTripByIdCommand readCmd = new GetTripByIdCommand(
                    unitOfWork, ServiceMapping.TRIP.getServiceName(), tripAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(TripSagaState.IN_UPDATE_TRIP);
            this.tripDto = (TripDto) commandGateway.send(sagaCommand);
        });

        SagaStep updateTripStep = new SagaStep("updateTripStep", () -> {
            UpdateTripCommand cmd = new UpdateTripCommand(
                    unitOfWork, ServiceMapping.TRIP.getServiceName(), tripAggregateId, updatedTripDto);
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTripStep)));

        this.workflow.addStep(getTripStep);
        this.workflow.addStep(updateTripStep);
    }

    public TripDto getTripDto() {
        return tripDto;
    }
}
