package pt.ulisboa.tecnico.socialsoftware.ecommerce.command.discount;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.DiscountDto;

public class UpdateDiscountCommand extends Command {
    private final DiscountDto discountDto;

    public UpdateDiscountCommand(UnitOfWork unitOfWork, String serviceName, DiscountDto discountDto) {
        super(unitOfWork, serviceName, null);
        this.discountDto = discountDto;
    }

    public DiscountDto getDiscountDto() { return discountDto; }
}
