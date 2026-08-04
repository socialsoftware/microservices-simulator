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
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.webapi.requestDtos.CreateTripRequestDto;

public class CreateTripFunctionalitySagas extends WorkflowFunctionality {
    private TripDto createdTripDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateTripFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateTripRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateTripRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createTripStep = new SagaStep("createTripStep", () -> {
            CreateTripCommand cmd = new CreateTripCommand(unitOfWork, ServiceMapping.TRIP.getServiceName(), createRequest);
            TripDto createdTripDto = (TripDto) commandGateway.send(cmd);
            setCreatedTripDto(createdTripDto);
        });

        workflow.addStep(createTripStep);
    }
    public TripDto getCreatedTripDto() {
        return createdTripDto;
    }

    public void setCreatedTripDto(TripDto createdTripDto) {
        this.createdTripDto = createdTripDto;
    }
}
