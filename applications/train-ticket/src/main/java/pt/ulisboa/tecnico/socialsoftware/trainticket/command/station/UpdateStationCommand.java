package pt.ulisboa.tecnico.socialsoftware.trainticket.command.station;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;

public class UpdateStationCommand extends Command {
    private final StationDto stationDto;

    public UpdateStationCommand(UnitOfWork unitOfWork, String serviceName, StationDto stationDto) {
        super(unitOfWork, serviceName, null);
        this.stationDto = stationDto;
    }

    public StationDto getStationDto() { return stationDto; }
}
