package pt.ulisboa.tecnico.socialsoftware.advanced.command.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;

public class GetOrderItemCommand extends Command {
    private final Integer orderId;
    private final Integer key;

    public GetOrderItemCommand(UnitOfWork unitOfWork, String serviceName, Integer orderId, Integer key) {
        super(unitOfWork, serviceName, null);
        this.orderId = orderId;
        this.key = key;
    }

    public Integer getOrderId() { return orderId; }
    public Integer getKey() { return key; }
}
