package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route.GetRouteByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype.GetTrainTypeByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip.CreateTripCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto;

import java.util.ArrayList;
import java.util.Arrays;

public class CreateTripFunctionalitySagas extends WorkflowFunctionality {
    private TripDto createdTripDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateTripFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                        TripDto tripDto,
                                        SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(tripDto, unitOfWork);
    }

    public void buildWorkflow(TripDto tripDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getRouteStep = new SagaStep("getRouteStep", () -> {
            // [P4a] ROUTE_AND_TRAIN_TYPE_EXIST - the fetch throwing is the enforcement; there is no
            // service guard. Only the id is cached on the Trip; no RouteDto field is copied onto it.
            GetRouteByIdCommand cmd = new GetRouteByIdCommand(
                    unitOfWork, ServiceMapping.ROUTE.getServiceName(), tripDto.getRouteAggregateId());
            commandGateway.send(cmd);
        });

        SagaStep getTrainTypeStep = new SagaStep("getTrainTypeStep", () -> {
            // [P4a] ROUTE_AND_TRAIN_TYPE_EXIST - see getRouteStep.
            GetTrainTypeByIdCommand cmd = new GetTrainTypeByIdCommand(
                    unitOfWork, ServiceMapping.TRAIN_TYPE.getServiceName(),
                    tripDto.getTrainTypeAggregateId());
            commandGateway.send(cmd);
        });

        SagaStep createTripStep = new SagaStep("createTripStep", () -> {
            CreateTripCommand cmd = new CreateTripCommand(
                    unitOfWork, ServiceMapping.TRIP.getServiceName(), tripDto);
            this.createdTripDto = (TripDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getRouteStep, getTrainTypeStep)));

        this.workflow.addStep(getRouteStep);
        this.workflow.addStep(getTrainTypeStep);
        this.workflow.addStep(createTripStep);
    }

    public TripDto getCreatedTripDto() {
        return createdTripDto;
    }
}
