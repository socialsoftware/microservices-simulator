package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas.GetPriceConfigByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas.GetPriceConfigByRouteAndTrainTypeFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas.GetPriceConfigsFunctionalitySagas;

import java.util.List;

@Service
public class PriceConfigFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;

    public PriceConfigDto getPriceConfigById(Integer priceConfigAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getPriceConfigById");
        GetPriceConfigByIdFunctionalitySagas saga = new GetPriceConfigByIdFunctionalitySagas(
                unitOfWorkService, priceConfigAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getPriceConfigDto();
    }

    public List<PriceConfigDto> getPriceConfigs() {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getPriceConfigs");
        GetPriceConfigsFunctionalitySagas saga = new GetPriceConfigsFunctionalitySagas(
                unitOfWorkService, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getPriceConfigs();
    }

    public PriceConfigDto getPriceConfigByRouteAndTrainType(Integer routeAggregateId, Integer trainTypeAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getPriceConfigByRouteAndTrainType");
        GetPriceConfigByRouteAndTrainTypeFunctionalitySagas saga = new GetPriceConfigByRouteAndTrainTypeFunctionalitySagas(
                unitOfWorkService, routeAggregateId, trainTypeAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getPriceConfigDto();
    }
}
