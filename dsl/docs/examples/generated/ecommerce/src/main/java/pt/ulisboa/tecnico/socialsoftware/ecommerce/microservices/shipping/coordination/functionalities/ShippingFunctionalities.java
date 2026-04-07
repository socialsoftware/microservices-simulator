package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.service.ShippingService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.ShippingDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.webapi.requestDtos.CreateShippingRequestDto;
import java.util.List;

@Service
public class ShippingFunctionalities {
    @Autowired
    private ShippingService shippingService;

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
            throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ShippingDto createShipping(CreateShippingRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateShippingFunctionalitySagas createShippingFunctionalitySagas = new CreateShippingFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createShippingFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createShippingFunctionalitySagas.getCreatedShippingDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ShippingDto getShippingById(Integer shippingAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetShippingByIdFunctionalitySagas getShippingByIdFunctionalitySagas = new GetShippingByIdFunctionalitySagas(
                        sagaUnitOfWorkService, shippingAggregateId, sagaUnitOfWork, commandGateway);
                getShippingByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getShippingByIdFunctionalitySagas.getShippingDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ShippingDto updateShipping(ShippingDto shippingDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(shippingDto);
                UpdateShippingFunctionalitySagas updateShippingFunctionalitySagas = new UpdateShippingFunctionalitySagas(
                        sagaUnitOfWorkService, shippingDto, sagaUnitOfWork, commandGateway);
                updateShippingFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateShippingFunctionalitySagas.getUpdatedShippingDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteShipping(Integer shippingAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteShippingFunctionalitySagas deleteShippingFunctionalitySagas = new DeleteShippingFunctionalitySagas(
                        sagaUnitOfWorkService, shippingAggregateId, sagaUnitOfWork, commandGateway);
                deleteShippingFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<ShippingDto> getAllShippings() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllShippingsFunctionalitySagas getAllShippingsFunctionalitySagas = new GetAllShippingsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllShippingsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllShippingsFunctionalitySagas.getShippings();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(ShippingDto shippingDto) {
        if (shippingDto.getAddress() == null) {
            throw new EcommerceException(SHIPPING_MISSING_ADDRESS);
        }
        if (shippingDto.getCarrier() == null) {
            throw new EcommerceException(SHIPPING_MISSING_CARRIER);
        }
        if (shippingDto.getTrackingNumber() == null) {
            throw new EcommerceException(SHIPPING_MISSING_TRACKINGNUMBER);
        }
}

    private void checkInput(CreateShippingRequestDto createRequest) {
        if (createRequest.getAddress() == null) {
            throw new EcommerceException(SHIPPING_MISSING_ADDRESS);
        }
        if (createRequest.getCarrier() == null) {
            throw new EcommerceException(SHIPPING_MISSING_CARRIER);
        }
        if (createRequest.getTrackingNumber() == null) {
            throw new EcommerceException(SHIPPING_MISSING_TRACKINGNUMBER);
        }
}
}