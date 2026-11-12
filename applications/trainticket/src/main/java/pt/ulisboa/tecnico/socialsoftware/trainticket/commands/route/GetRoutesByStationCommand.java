package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetRoutesByStationCommand extends Command {
    private Integer stationAggregateId;

    public GetRoutesByStationCommand(UnitOfWork unitOfWork, String serviceName, Integer stationAggregateId) {
        super(unitOfWork, serviceName, stationAggregateId);
        this.stationAggregateId = stationAggregateId;
    }

    public Integer getStationAggregateId() {
        return stationAggregateId;
    }
}
