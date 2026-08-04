package pt.ulisboa.tecnico.socialsoftware.trainticket.command.priceconfig;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigDto;

public class UpdatePriceConfigCommand extends Command {
    private final PriceConfigDto priceconfigDto;

    public UpdatePriceConfigCommand(UnitOfWork unitOfWork, String serviceName, PriceConfigDto priceconfigDto) {
        super(unitOfWork, serviceName, null);
        this.priceconfigDto = priceconfigDto;
    }

    public PriceConfigDto getPriceconfigDto() { return priceconfigDto; }
}
