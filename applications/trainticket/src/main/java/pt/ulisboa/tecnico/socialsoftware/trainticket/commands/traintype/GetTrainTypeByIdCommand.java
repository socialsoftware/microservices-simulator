package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetTrainTypeByIdCommand extends Command {
    private Integer trainTypeAggregateId;

    public GetTrainTypeByIdCommand(UnitOfWork unitOfWork, String serviceName, Integer trainTypeAggregateId) {
        super(unitOfWork, serviceName, trainTypeAggregateId);
        this.trainTypeAggregateId = trainTypeAggregateId;
    }

    public Integer getTrainTypeAggregateId() {
        return trainTypeAggregateId;
    }
}
