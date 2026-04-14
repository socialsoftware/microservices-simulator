package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.service.DiscountService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.DiscountDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.coordination.webapi.requestDtos.CreateDiscountRequestDto;
import java.util.List;

@Service
public class DiscountFunctionalities {
    @Autowired
    private DiscountService discountService;

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

    public DiscountDto createDiscount(CreateDiscountRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateDiscountFunctionalitySagas createDiscountFunctionalitySagas = new CreateDiscountFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createDiscountFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createDiscountFunctionalitySagas.getCreatedDiscountDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public DiscountDto getDiscountById(Integer discountAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetDiscountByIdFunctionalitySagas getDiscountByIdFunctionalitySagas = new GetDiscountByIdFunctionalitySagas(
                        sagaUnitOfWorkService, discountAggregateId, sagaUnitOfWork, commandGateway);
                getDiscountByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getDiscountByIdFunctionalitySagas.getDiscountDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public DiscountDto updateDiscount(DiscountDto discountDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(discountDto);
                UpdateDiscountFunctionalitySagas updateDiscountFunctionalitySagas = new UpdateDiscountFunctionalitySagas(
                        sagaUnitOfWorkService, discountDto, sagaUnitOfWork, commandGateway);
                updateDiscountFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateDiscountFunctionalitySagas.getUpdatedDiscountDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteDiscount(Integer discountAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteDiscountFunctionalitySagas deleteDiscountFunctionalitySagas = new DeleteDiscountFunctionalitySagas(
                        sagaUnitOfWorkService, discountAggregateId, sagaUnitOfWork, commandGateway);
                deleteDiscountFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<DiscountDto> getAllDiscounts() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllDiscountsFunctionalitySagas getAllDiscountsFunctionalitySagas = new GetAllDiscountsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllDiscountsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllDiscountsFunctionalitySagas.getDiscounts();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(DiscountDto discountDto) {
        if (discountDto.getCode() == null) {
            throw new EcommerceException(DISCOUNT_MISSING_CODE);
        }
        if (discountDto.getDescription() == null) {
            throw new EcommerceException(DISCOUNT_MISSING_DESCRIPTION);
        }
        if (discountDto.getValidFrom() == null) {
            throw new EcommerceException(DISCOUNT_MISSING_VALIDFROM);
        }
        if (discountDto.getValidUntil() == null) {
            throw new EcommerceException(DISCOUNT_MISSING_VALIDUNTIL);
        }
}

    private void checkInput(CreateDiscountRequestDto createRequest) {
        if (createRequest.getCode() == null) {
            throw new EcommerceException(DISCOUNT_MISSING_CODE);
        }
        if (createRequest.getDescription() == null) {
            throw new EcommerceException(DISCOUNT_MISSING_DESCRIPTION);
        }
        if (createRequest.getValidFrom() == null) {
            throw new EcommerceException(DISCOUNT_MISSING_VALIDFROM);
        }
        if (createRequest.getValidUntil() == null) {
            throw new EcommerceException(DISCOUNT_MISSING_VALIDUNTIL);
        }
}
}