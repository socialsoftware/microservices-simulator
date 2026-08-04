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

public class GetTripByIdFunctionalitySagas extends WorkflowFunctionality {
    private TripDto tripDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetTripByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer tripAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tripAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer tripAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTripStep = new SagaStep("getTripStep", () -> {
            GetTripByIdCommand cmd = new GetTripByIdCommand(unitOfWork, ServiceMapping.TRIP.getServiceName(), tripAggregateId);
            TripDto tripDto = (TripDto) commandGateway.send(cmd);
            setTripDto(tripDto);
        });

        workflow.addStep(getTripStep);
    }
    public TripDto getTripDto() {
        return tripDto;
    }

    public void setTripDto(TripDto tripDto) {
        this.tripDto = tripDto;
    }
}
