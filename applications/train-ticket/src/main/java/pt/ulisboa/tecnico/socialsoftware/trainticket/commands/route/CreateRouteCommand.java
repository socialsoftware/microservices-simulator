package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteStationDto;

import java.util.List;

public class CreateRouteCommand extends Command {
    private List<RouteStationDto> stations;

    protected CreateRouteCommand(){}

    public CreateRouteCommand(UnitOfWork unitOfWork, String serviceName, List<RouteStationDto> stations) {
        super(unitOfWork, serviceName, null);
        this.stations = stations;
    }

    public List<RouteStationDto> getStations() {
        return stations;
    }

    public void setStations(List<RouteStationDto> stations) {
        this.stations = stations;
    }
}
