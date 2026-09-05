package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto;

public class UpdateRouteCommand extends Command {
    private Integer routeAggregateId;
    private RouteDto routeDto;

    public UpdateRouteCommand(UnitOfWork unitOfWork, String serviceName,
                              Integer routeAggregateId, RouteDto routeDto) {
        super(unitOfWork, serviceName, routeAggregateId);
        this.routeAggregateId = routeAggregateId;
        this.routeDto = routeDto;
    }

    public Integer getRouteAggregateId() {
        return routeAggregateId;
    }

    public RouteDto getRouteDto() {
        return routeDto;
    }
}
