package pt.ulisboa.tecnico.socialsoftware.trainticket.command.train;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;

public class UpdateTrainCommand extends Command {
    private final TrainDto trainDto;

    public UpdateTrainCommand(UnitOfWork unitOfWork, String serviceName, TrainDto trainDto) {
        super(unitOfWork, serviceName, null);
        this.trainDto = trainDto;
    }

    public TrainDto getTrainDto() { return trainDto; }
}
