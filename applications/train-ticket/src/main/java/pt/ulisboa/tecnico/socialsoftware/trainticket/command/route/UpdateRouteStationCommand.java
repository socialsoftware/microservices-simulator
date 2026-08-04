package pt.ulisboa.tecnico.socialsoftware.trainticket.command.route;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteStationDto;

public class UpdateRouteStationCommand extends Command {
    private final Integer routeId;
    private final Integer stationAggregateId;
    private final RouteStationDto stationDto;

    public UpdateRouteStationCommand(UnitOfWork unitOfWork, String serviceName, Integer routeId, Integer stationAggregateId, RouteStationDto stationDto) {
        super(unitOfWork, serviceName, null);
        this.routeId = routeId;
        this.stationAggregateId = stationAggregateId;
        this.stationDto = stationDto;
    }

    public Integer getRouteId() { return routeId; }
    public Integer getStationAggregateId() { return stationAggregateId; }
    public RouteStationDto getStationDto() { return stationDto; }
}
