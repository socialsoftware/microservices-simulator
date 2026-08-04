package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.service.PriceConfigService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.coordination.webapi.requestDtos.CreatePriceConfigRequestDto;
import java.util.List;

@Service
public class PriceConfigFunctionalities {
    @Autowired
    private PriceConfigService priceconfigService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public PriceConfigDto createPriceConfig(CreatePriceConfigRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreatePriceConfigFunctionalitySagas createPriceConfigFunctionalitySagas = new CreatePriceConfigFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createPriceConfigFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createPriceConfigFunctionalitySagas.getCreatedPriceConfigDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public PriceConfigDto getPriceConfigById(Integer priceconfigAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetPriceConfigByIdFunctionalitySagas getPriceConfigByIdFunctionalitySagas = new GetPriceConfigByIdFunctionalitySagas(
                        sagaUnitOfWorkService, priceconfigAggregateId, sagaUnitOfWork, commandGateway);
                getPriceConfigByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getPriceConfigByIdFunctionalitySagas.getPriceConfigDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public PriceConfigDto updatePriceConfig(PriceConfigDto priceconfigDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(priceconfigDto);
                UpdatePriceConfigFunctionalitySagas updatePriceConfigFunctionalitySagas = new UpdatePriceConfigFunctionalitySagas(
                        sagaUnitOfWorkService, priceconfigDto, sagaUnitOfWork, commandGateway);
                updatePriceConfigFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updatePriceConfigFunctionalitySagas.getUpdatedPriceConfigDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deletePriceConfig(Integer priceconfigAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeletePriceConfigFunctionalitySagas deletePriceConfigFunctionalitySagas = new DeletePriceConfigFunctionalitySagas(
                        sagaUnitOfWorkService, priceconfigAggregateId, sagaUnitOfWork, commandGateway);
                deletePriceConfigFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<PriceConfigDto> getAllPriceConfigs() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllPriceConfigsFunctionalitySagas getAllPriceConfigsFunctionalitySagas = new GetAllPriceConfigsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllPriceConfigsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllPriceConfigsFunctionalitySagas.getPriceConfigs();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(PriceConfigDto priceconfigDto) {
}

    private void checkInput(CreatePriceConfigRequestDto createRequest) {
}
}