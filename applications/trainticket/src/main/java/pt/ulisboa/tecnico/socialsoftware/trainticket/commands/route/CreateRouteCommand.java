package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto;

public class CreateRouteCommand extends Command {
    private RouteDto routeDto;

    public CreateRouteCommand(UnitOfWork unitOfWork, String serviceName, RouteDto routeDto) {
        super(unitOfWork, serviceName, null);
        this.routeDto = routeDto;
    }

    public RouteDto getRouteDto() {
        return routeDto;
    }
}
