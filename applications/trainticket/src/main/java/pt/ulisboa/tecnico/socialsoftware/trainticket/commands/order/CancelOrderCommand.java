package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class CancelOrderCommand extends Command {
    private Integer orderAggregateId;

    public CancelOrderCommand(UnitOfWork unitOfWork, String serviceName, Integer orderAggregateId) {
        super(unitOfWork, serviceName, orderAggregateId);
        this.orderAggregateId = orderAggregateId;
    }

    public Integer getOrderAggregateId() {
        return orderAggregateId;
    }
}
