package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetPriceConfigByRouteAndTrainTypeCommand extends Command {
    private Integer routeAggregateId;
    private Integer trainTypeAggregateId;

    public GetPriceConfigByRouteAndTrainTypeCommand(UnitOfWork unitOfWork, String serviceName,
                                                    Integer routeAggregateId, Integer trainTypeAggregateId) {
        super(unitOfWork, serviceName, null);
        this.routeAggregateId = routeAggregateId;
        this.trainTypeAggregateId = trainTypeAggregateId;
    }

    public Integer getRouteAggregateId() {
        return routeAggregateId;
    }

    public Integer getTrainTypeAggregateId() {
        return trainTypeAggregateId;
    }
}
