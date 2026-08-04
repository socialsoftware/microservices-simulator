package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.trip.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllTripsFunctionalitySagas extends WorkflowFunctionality {
    private List<TripDto> trips;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetAllTripsFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getAllTripsStep = new SagaStep("getAllTripsStep", () -> {
            GetAllTripsCommand cmd = new GetAllTripsCommand(unitOfWork, ServiceMapping.TRIP.getServiceName());
            List<TripDto> trips = (List<TripDto>) commandGateway.send(cmd);
            setTrips(trips);
        });

        workflow.addStep(getAllTripsStep);
    }
    public List<TripDto> getTrips() {
        return trips;
    }

    public void setTrips(List<TripDto> trips) {
        this.trips = trips;
    }
}
