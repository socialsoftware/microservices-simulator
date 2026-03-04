package pt.ulisboa.tecnico.socialsoftware.advanced.command.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;

public class RemoveOrderProductCommand extends Command {
    private final Integer orderId;
    private final Integer productAggregateId;

    public RemoveOrderProductCommand(UnitOfWork unitOfWork, String serviceName, Integer orderId, Integer productAggregateId) {
        super(unitOfWork, serviceName, null);
        this.orderId = orderId;
        this.productAggregateId = productAggregateId;
    }

    public Integer getOrderId() { return orderId; }
    public Integer getProductAggregateId() { return productAggregateId; }
}
