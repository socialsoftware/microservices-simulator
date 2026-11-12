package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetStationByIdCommand extends Command {
    private Integer stationAggregateId;

    public GetStationByIdCommand(UnitOfWork unitOfWork, String serviceName, Integer stationAggregateId) {
        super(unitOfWork, serviceName, stationAggregateId);
        this.stationAggregateId = stationAggregateId;
    }

    public Integer getStationAggregateId() {
        return stationAggregateId;
    }
}
