package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.messaging.SagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route.GetRouteByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route.UpdateRouteCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station.GetStationByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.sagas.states.RouteSagaState;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

public class UpdateRouteFunctionalitySagas extends WorkflowFunctionality {
    private Set<RouteStationDto> resolvedRouteStations;
    private RouteDto routeDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public UpdateRouteFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                         Integer routeAggregateId, RouteDto updatedRouteDto,
                                         SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(routeAggregateId, updatedRouteDto, unitOfWork);
    }

    public void buildWorkflow(Integer routeAggregateId, RouteDto updatedRouteDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getStationsStep = new SagaStep("getStationsStep", () -> {
            // [P4a] STATIONS_EXIST — the fetch throwing is the enforcement; there is no service guard.
            this.resolvedRouteStations = resolveRouteStations(updatedRouteDto.getRouteStations(), unitOfWork);
        });

        SagaStep getRouteStep = new SagaStep("getRouteStep", () -> {
            GetRouteByIdCommand readCmd = new GetRouteByIdCommand(
                    unitOfWork, ServiceMapping.ROUTE.getServiceName(), routeAggregateId);
            SagaCommand sagaCommand = new SagaCommand(readCmd);
            sagaCommand.setSemanticLock(RouteSagaState.IN_UPDATE_ROUTE);
            this.routeDto = (RouteDto) commandGateway.send(sagaCommand);
        });

        SagaStep updateRouteStep = new SagaStep("updateRouteStep", () -> {
            UpdateRouteCommand cmd = new UpdateRouteCommand(
                    unitOfWork, ServiceMapping.ROUTE.getServiceName(), routeAggregateId,
                    new RouteDto(updatedRouteDto.getStartStationName(), updatedRouteDto.getEndStationName(),
                            this.resolvedRouteStations));
            commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getStationsStep, getRouteStep)));

        this.workflow.addStep(getStationsStep);
        this.workflow.addStep(getRouteStep);
        this.workflow.addStep(updateRouteStep);
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

    public RouteDto getRouteDto() {
        return routeDto;
    }
}
