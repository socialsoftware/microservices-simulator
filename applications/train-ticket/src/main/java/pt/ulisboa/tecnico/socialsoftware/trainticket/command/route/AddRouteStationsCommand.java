package pt.ulisboa.tecnico.socialsoftware.trainticket.command.route;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.RouteStationDto;
import java.util.List;

public class AddRouteStationsCommand extends Command {
    private final Integer routeId;
    private final List<RouteStationDto> stationDtos;

    public AddRouteStationsCommand(UnitOfWork unitOfWork, String serviceName, Integer routeId, List<RouteStationDto> stationDtos) {
        super(unitOfWork, serviceName, null);
        this.routeId = routeId;
        this.stationDtos = stationDtos;
    }

    public Integer getRouteId() { return routeId; }
    public List<RouteStationDto> getStationDtos() { return stationDtos; }
}
