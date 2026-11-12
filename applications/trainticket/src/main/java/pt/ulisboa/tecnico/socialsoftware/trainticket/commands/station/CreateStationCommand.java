package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.station;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto;

public class CreateStationCommand extends Command {
    private StationDto stationDto;

    public CreateStationCommand(UnitOfWork unitOfWork, String serviceName, StationDto stationDto) {
        super(unitOfWork, serviceName, null);
        this.stationDto = stationDto;
    }

    public StationDto getStationDto() {
        return stationDto;
    }
}
