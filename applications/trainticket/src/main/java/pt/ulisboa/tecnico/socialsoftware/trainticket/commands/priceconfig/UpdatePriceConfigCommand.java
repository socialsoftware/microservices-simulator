package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto;

public class UpdatePriceConfigCommand extends Command {
    private Integer priceConfigAggregateId;
    private PriceConfigDto priceConfigDto;

    public UpdatePriceConfigCommand(UnitOfWork unitOfWork, String serviceName,
                                    Integer priceConfigAggregateId, PriceConfigDto priceConfigDto) {
        super(unitOfWork, serviceName, priceConfigAggregateId);
        this.priceConfigAggregateId = priceConfigAggregateId;
        this.priceConfigDto = priceConfigDto;
    }

    public Integer getPriceConfigAggregateId() {
        return priceConfigAggregateId;
    }

    public PriceConfigDto getPriceConfigDto() {
        return priceConfigDto;
    }
}
