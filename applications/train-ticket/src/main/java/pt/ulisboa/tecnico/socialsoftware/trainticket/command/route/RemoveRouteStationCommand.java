package pt.ulisboa.tecnico.socialsoftware.trainticket.command.route;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class RemoveRouteStationCommand extends Command {
    private final Integer routeId;
    private final Integer stationAggregateId;

    public RemoveRouteStationCommand(UnitOfWork unitOfWork, String serviceName, Integer routeId, Integer stationAggregateId) {
        super(unitOfWork, serviceName, null);
        this.routeId = routeId;
        this.stationAggregateId = stationAggregateId;
    }

    public Integer getRouteId() { return routeId; }
    public Integer getStationAggregateId() { return stationAggregateId; }
}
