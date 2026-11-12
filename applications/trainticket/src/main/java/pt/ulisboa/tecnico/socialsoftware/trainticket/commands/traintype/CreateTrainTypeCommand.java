package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;

public class CreateTrainTypeCommand extends Command {
    private TrainTypeDto trainTypeDto;

    public CreateTrainTypeCommand(UnitOfWork unitOfWork, String serviceName, TrainTypeDto trainTypeDto) {
        super(unitOfWork, serviceName, null);
        this.trainTypeDto = trainTypeDto;
    }

    public TrainTypeDto getTrainTypeDto() {
        return trainTypeDto;
    }
}
