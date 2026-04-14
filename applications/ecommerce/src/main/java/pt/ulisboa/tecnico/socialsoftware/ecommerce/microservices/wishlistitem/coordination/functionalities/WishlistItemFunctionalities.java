package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.service.WishlistItemService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.WishlistItemDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.wishlistitem.coordination.webapi.requestDtos.CreateWishlistItemRequestDto;
import java.util.List;

@Service
public class WishlistItemFunctionalities {
    @Autowired
    private WishlistItemService wishlistitemService;

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

    public WishlistItemDto createWishlistItem(CreateWishlistItemRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateWishlistItemFunctionalitySagas createWishlistItemFunctionalitySagas = new CreateWishlistItemFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createWishlistItemFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createWishlistItemFunctionalitySagas.getCreatedWishlistItemDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public WishlistItemDto getWishlistItemById(Integer wishlistitemAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetWishlistItemByIdFunctionalitySagas getWishlistItemByIdFunctionalitySagas = new GetWishlistItemByIdFunctionalitySagas(
                        sagaUnitOfWorkService, wishlistitemAggregateId, sagaUnitOfWork, commandGateway);
                getWishlistItemByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getWishlistItemByIdFunctionalitySagas.getWishlistItemDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public WishlistItemDto updateWishlistItem(WishlistItemDto wishlistitemDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(wishlistitemDto);
                UpdateWishlistItemFunctionalitySagas updateWishlistItemFunctionalitySagas = new UpdateWishlistItemFunctionalitySagas(
                        sagaUnitOfWorkService, wishlistitemDto, sagaUnitOfWork, commandGateway);
                updateWishlistItemFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateWishlistItemFunctionalitySagas.getUpdatedWishlistItemDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteWishlistItem(Integer wishlistitemAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteWishlistItemFunctionalitySagas deleteWishlistItemFunctionalitySagas = new DeleteWishlistItemFunctionalitySagas(
                        sagaUnitOfWorkService, wishlistitemAggregateId, sagaUnitOfWork, commandGateway);
                deleteWishlistItemFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<WishlistItemDto> getAllWishlistItems() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllWishlistItemsFunctionalitySagas getAllWishlistItemsFunctionalitySagas = new GetAllWishlistItemsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllWishlistItemsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllWishlistItemsFunctionalitySagas.getWishlistItems();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(WishlistItemDto wishlistitemDto) {
        if (wishlistitemDto.getAddedAt() == null) {
            throw new EcommerceException(WISHLISTITEM_MISSING_ADDEDAT);
        }
}

    private void checkInput(CreateWishlistItemRequestDto createRequest) {
        if (createRequest.getAddedAt() == null) {
            throw new EcommerceException(WISHLISTITEM_MISSING_ADDEDAT);
        }
}
}