package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route.CreateRouteCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station.GetStationByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

public class CreateRouteFunctionalitySagas extends WorkflowFunctionality {
    private Set<RouteStationDto> resolvedRouteStations;
    private RouteDto createdRouteDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public CreateRouteFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                         RouteDto routeDto,
                                         SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(routeDto, unitOfWork);
    }

    public void buildWorkflow(RouteDto routeDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getStationsStep = new SagaStep("getStationsStep", () -> {
            // [P4a] STATIONS_EXIST — the fetch throwing is the enforcement; there is no service guard.
            this.resolvedRouteStations = resolveRouteStations(routeDto.getRouteStations(), unitOfWork);
        });

        SagaStep createRouteStep = new SagaStep("createRouteStep", () -> {
            CreateRouteCommand cmd = new CreateRouteCommand(
                    unitOfWork, ServiceMapping.ROUTE.getServiceName(),
                    new RouteDto(routeDto.getStartStationName(), routeDto.getEndStationName(),
                            this.resolvedRouteStations));
            this.createdRouteDto = (RouteDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getStationsStep)));

        this.workflow.addStep(getStationsStep);
        this.workflow.addStep(createRouteStep);
    }

    private Set<RouteStationDto> resolveRouteStations(Set<RouteStationDto> requested, SagaUnitOfWork unitOfWork) {
        Set<RouteStationDto> resolved = new LinkedHashSet<>();
        for (RouteStationDto requestedStation : sortedBySequence(requested)) {
            GetStationByIdCommand cmd = new GetStationByIdCommand(
                    unitOfWork, ServiceMapping.STATION.getServiceName(),
                    requestedStation.getStationAggregateId());
            StationDto stationDto = (StationDto) commandGateway.send(cmd);
            resolved.add(new RouteStationDto(requestedStation.getSequence(),
                    requestedStation.getStationAggregateId(), stationDto.getName(),
                    requestedStation.getDistanceFromStart()));
        }
        return resolved;
    }

    private ArrayList<RouteStationDto> sortedBySequence(Set<RouteStationDto> requested) {
        ArrayList<RouteStationDto> sorted = new ArrayList<>(requested);
        sorted.sort(Comparator.comparing(RouteStationDto::getSequence,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return sorted;
    }

    public RouteDto getCreatedRouteDto() {
        return createdRouteDto;
    }
}
