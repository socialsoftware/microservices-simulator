package pt.ulisboa.tecnico.socialsoftware.ecommerce.command.shipping;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ShippingDto;

public class UpdateShippingCommand extends Command {
    private final ShippingDto shippingDto;

    public UpdateShippingCommand(UnitOfWork unitOfWork, String serviceName, ShippingDto shippingDto) {
        super(unitOfWork, serviceName, null);
        this.shippingDto = shippingDto;
    }

    public ShippingDto getShippingDto() { return shippingDto; }
}
