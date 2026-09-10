package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class DeletePriceConfigCommand extends Command {
    private Integer priceConfigAggregateId;

    public DeletePriceConfigCommand(UnitOfWork unitOfWork, String serviceName, Integer priceConfigAggregateId) {
        super(unitOfWork, serviceName, priceConfigAggregateId);
        this.priceConfigAggregateId = priceConfigAggregateId;
    }

    public Integer getPriceConfigAggregateId() {
        return priceConfigAggregateId;
    }
}
