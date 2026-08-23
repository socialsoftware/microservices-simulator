package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas.CreatePriceConfigFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas.DeletePriceConfigFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas.GetPriceConfigByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas.GetPriceConfigByRouteAndTrainTypeFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas.GetPriceConfigsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas.UpdatePriceConfigFunctionalitySagas;

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

    public PriceConfigDto createPriceConfig(PriceConfigDto priceConfigDto) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("createPriceConfig");
        CreatePriceConfigFunctionalitySagas saga = new CreatePriceConfigFunctionalitySagas(
                unitOfWorkService, priceConfigDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getCreatedPriceConfigDto();
    }

    public void updatePriceConfig(Integer priceConfigAggregateId, PriceConfigDto priceConfigDto) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("updatePriceConfig");
        UpdatePriceConfigFunctionalitySagas saga = new UpdatePriceConfigFunctionalitySagas(
                unitOfWorkService, priceConfigAggregateId, priceConfigDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deletePriceConfig(Integer priceConfigAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("deletePriceConfig");
        DeletePriceConfigFunctionalitySagas saga = new DeletePriceConfigFunctionalitySagas(
                unitOfWorkService, priceConfigAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }
}
