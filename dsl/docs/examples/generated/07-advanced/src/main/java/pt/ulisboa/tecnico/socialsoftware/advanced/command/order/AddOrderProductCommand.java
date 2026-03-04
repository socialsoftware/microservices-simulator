package pt.ulisboa.tecnico.socialsoftware.advanced.command.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderProductDto;

public class AddOrderProductCommand extends Command {
    private final Integer orderId;
    private final Integer productAggregateId;
    private final OrderProductDto productDto;

    public AddOrderProductCommand(UnitOfWork unitOfWork, String serviceName, Integer orderId, Integer productAggregateId, OrderProductDto productDto) {
        super(unitOfWork, serviceName, null);
        this.orderId = orderId;
        this.productAggregateId = productAggregateId;
        this.productDto = productDto;
    }

    public Integer getOrderId() { return orderId; }
    public Integer getProductAggregateId() { return productAggregateId; }
    public OrderProductDto getProductDto() { return productDto; }
}
