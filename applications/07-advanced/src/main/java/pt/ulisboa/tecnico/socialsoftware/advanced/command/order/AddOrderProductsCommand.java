package pt.ulisboa.tecnico.socialsoftware.advanced.command.order;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderProductDto;
import java.util.List;

public class AddOrderProductsCommand extends Command {
    private final Integer orderId;
    private final List<OrderProductDto> productDtos;

    public AddOrderProductsCommand(UnitOfWork unitOfWork, String serviceName, Integer orderId, List<OrderProductDto> productDtos) {
        super(unitOfWork, serviceName, null);
        this.orderId = orderId;
        this.productDtos = productDtos;
    }

    public Integer getOrderId() { return orderId; }
    public List<OrderProductDto> getProductDtos() { return productDtos; }
}
