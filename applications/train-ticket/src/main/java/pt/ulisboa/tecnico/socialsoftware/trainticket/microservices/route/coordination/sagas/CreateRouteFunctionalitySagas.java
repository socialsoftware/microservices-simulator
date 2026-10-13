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
import java.util.List;

public class CreateRouteFunctionalitySagas extends WorkflowFunctionality {
    private List<RouteStationDto> routeStationDtos;
    private RouteDto routeDto;

    public CreateRouteFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, List<RouteStationDto> stops,
                                         SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        buildWorkflow(unitOfWorkService, stops, unitOfWork, commandGateway);
    }

    public void buildWorkflow(SagaUnitOfWorkService unitOfWorkService, List<RouteStationDto> stops,
                              SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        // Station x N snapshot: caches name and version; sequence and distance are the caller's
        SagaStep getStationsStep = new SagaStep("getStationsStep", () -> {
            this.routeStationDtos = new ArrayList<>();
            for (RouteStationDto stop : stops) {
                GetStationByIdCommand cmd = new GetStationByIdCommand(
                        unitOfWork, ServiceMapping.STATION.getServiceName(), stop.getStationAggregateId());
                StationDto stationDto = (StationDto) commandGateway.send(cmd);
                this.routeStationDtos.add(new RouteStationDto(stationDto.getAggregateId(), stationDto.getName(),
                        stationDto.getVersion(), stop.getSequence(), stop.getDistanceFromStart()));
            }
        });

        SagaStep createRouteStep = new SagaStep("createRouteStep", () -> {
            CreateRouteCommand cmd = new CreateRouteCommand(
                    unitOfWork, ServiceMapping.ROUTE.getServiceName(), this.routeStationDtos);
            this.routeDto = (RouteDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getStationsStep)));

        this.workflow.addStep(getStationsStep);
        this.workflow.addStep(createRouteStep);
    }

    public RouteDto getRouteDto() {
        return routeDto;
    }
}
