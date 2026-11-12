package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetRouteByIdCommand extends Command {
    private Integer routeAggregateId;

    public GetRouteByIdCommand(UnitOfWork unitOfWork, String serviceName, Integer routeAggregateId) {
        super(unitOfWork, serviceName, routeAggregateId);
        this.routeAggregateId = routeAggregateId;
    }

    public Integer getRouteAggregateId() {
        return routeAggregateId;
    }
}
