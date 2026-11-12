package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto;

public class CreatePriceConfigCommand extends Command {
    private PriceConfigDto priceConfigDto;

    public CreatePriceConfigCommand(UnitOfWork unitOfWork, String serviceName, PriceConfigDto priceConfigDto) {
        super(unitOfWork, serviceName, null);
        this.priceConfigDto = priceConfigDto;
    }

    public PriceConfigDto getPriceConfigDto() {
        return priceConfigDto;
    }
}
