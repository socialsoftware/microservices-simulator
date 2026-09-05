package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto;

public class UpdateStationCommand extends Command {
    private Integer stationAggregateId;
    private StationDto stationDto;

    public UpdateStationCommand(UnitOfWork unitOfWork, String serviceName,
                                Integer stationAggregateId, StationDto stationDto) {
        super(unitOfWork, serviceName, stationAggregateId);
        this.stationAggregateId = stationAggregateId;
        this.stationDto = stationDto;
    }

    public Integer getStationAggregateId() {
        return stationAggregateId;
    }

    public StationDto getStationDto() {
        return stationDto;
    }
}
