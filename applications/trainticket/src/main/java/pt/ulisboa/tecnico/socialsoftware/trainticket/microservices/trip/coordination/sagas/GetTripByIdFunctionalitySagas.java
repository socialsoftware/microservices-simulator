package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip.GetTripByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto;

public class GetTripByIdFunctionalitySagas extends WorkflowFunctionality {
    private TripDto tripDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetTripByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                         Integer tripAggregateId,
                                         SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(tripAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tripAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTripStep = new SagaStep("getTripStep", () -> {
            GetTripByIdCommand cmd = new GetTripByIdCommand(
                    unitOfWork, ServiceMapping.TRIP.getServiceName(), tripAggregateId);
            this.tripDto = (TripDto) commandGateway.send(cmd);
        });

        this.workflow.addStep(getTripStep);
    }

    public TripDto getTripDto() {
        return tripDto;
    }
}
