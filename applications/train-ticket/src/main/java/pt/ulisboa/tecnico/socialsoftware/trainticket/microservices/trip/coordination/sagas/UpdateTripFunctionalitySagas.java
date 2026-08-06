package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.trip.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripDto;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;

public class UpdateTripFunctionalitySagas extends WorkflowFunctionality {
    private TripDto updatedTripDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateTripFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, TripDto tripDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(tripDto, unitOfWork);
    }

    public void buildWorkflow(TripDto tripDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateTripStep = new SagaStep("updateTripStep", () -> {
            UpdateTripCommand cmd = new UpdateTripCommand(unitOfWork, ServiceMapping.TRIP.getServiceName(), tripDto);
            TripDto updatedTripDto = (TripDto) commandGateway.send(cmd);
            setUpdatedTripDto(updatedTripDto);
        });

        workflow.addStep(updateTripStep);
    }
    public TripDto getUpdatedTripDto() {
        return updatedTripDto;
    }

    public void setUpdatedTripDto(TripDto updatedTripDto) {
        this.updatedTripDto = updatedTripDto;
    }
}
