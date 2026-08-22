package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;

public class UpdateTrainTypeCommand extends Command {
    private Integer trainTypeAggregateId;
    private TrainTypeDto trainTypeDto;

    public UpdateTrainTypeCommand(UnitOfWork unitOfWork, String serviceName,
                                  Integer trainTypeAggregateId, TrainTypeDto trainTypeDto) {
        super(unitOfWork, serviceName, trainTypeAggregateId);
        this.trainTypeAggregateId = trainTypeAggregateId;
        this.trainTypeDto = trainTypeDto;
    }

    public Integer getTrainTypeAggregateId() {
        return trainTypeAggregateId;
    }

    public TrainTypeDto getTrainTypeDto() {
        return trainTypeDto;
    }
}
