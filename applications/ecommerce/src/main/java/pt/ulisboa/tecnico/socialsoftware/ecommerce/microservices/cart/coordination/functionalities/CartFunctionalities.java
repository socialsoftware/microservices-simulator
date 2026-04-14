package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.service.CartService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.CartItemDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.coordination.webapi.requestDtos.CreateCartRequestDto;
import java.util.List;

@Service
public class CartFunctionalities {
    @Autowired
    private CartService cartService;

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

    public CartDto createCart(CreateCartRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateCartFunctionalitySagas createCartFunctionalitySagas = new CreateCartFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createCartFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createCartFunctionalitySagas.getCreatedCartDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CartDto getCartById(Integer cartAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCartByIdFunctionalitySagas getCartByIdFunctionalitySagas = new GetCartByIdFunctionalitySagas(
                        sagaUnitOfWorkService, cartAggregateId, sagaUnitOfWork, commandGateway);
                getCartByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getCartByIdFunctionalitySagas.getCartDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CartDto updateCart(CartDto cartDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(cartDto);
                UpdateCartFunctionalitySagas updateCartFunctionalitySagas = new UpdateCartFunctionalitySagas(
                        sagaUnitOfWorkService, cartDto, sagaUnitOfWork, commandGateway);
                updateCartFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateCartFunctionalitySagas.getUpdatedCartDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteCart(Integer cartAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteCartFunctionalitySagas deleteCartFunctionalitySagas = new DeleteCartFunctionalitySagas(
                        sagaUnitOfWorkService, cartAggregateId, sagaUnitOfWork, commandGateway);
                deleteCartFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<CartDto> getAllCarts() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllCartsFunctionalitySagas getAllCartsFunctionalitySagas = new GetAllCartsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllCartsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllCartsFunctionalitySagas.getCarts();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CartItemDto addCartItem(Integer cartId, Integer quantity, CartItemDto itemDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddCartItemFunctionalitySagas addCartItemFunctionalitySagas = new AddCartItemFunctionalitySagas(
                        sagaUnitOfWorkService,
                        cartId, quantity, itemDto,
                        sagaUnitOfWork, commandGateway);
                addCartItemFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addCartItemFunctionalitySagas.getAddedItemDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<CartItemDto> addCartItems(Integer cartId, List<CartItemDto> itemDtos) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddCartItemsFunctionalitySagas addCartItemsFunctionalitySagas = new AddCartItemsFunctionalitySagas(
                        sagaUnitOfWorkService,
                        cartId, itemDtos,
                        sagaUnitOfWork, commandGateway);
                addCartItemsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addCartItemsFunctionalitySagas.getAddedItemDtos();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CartItemDto getCartItem(Integer cartId, Integer quantity) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCartItemFunctionalitySagas getCartItemFunctionalitySagas = new GetCartItemFunctionalitySagas(
                        sagaUnitOfWorkService,
                        cartId, quantity,
                        sagaUnitOfWork, commandGateway);
                getCartItemFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getCartItemFunctionalitySagas.getItemDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CartItemDto updateCartItem(Integer cartId, Integer quantity, CartItemDto itemDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateCartItemFunctionalitySagas updateCartItemFunctionalitySagas = new UpdateCartItemFunctionalitySagas(
                        sagaUnitOfWorkService,
                        cartId, quantity, itemDto,
                        sagaUnitOfWork, commandGateway);
                updateCartItemFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateCartItemFunctionalitySagas.getUpdatedItemDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeCartItem(Integer cartId, Integer quantity) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveCartItemFunctionalitySagas removeCartItemFunctionalitySagas = new RemoveCartItemFunctionalitySagas(
                        sagaUnitOfWorkService,
                        cartId, quantity,
                        sagaUnitOfWork, commandGateway);
                removeCartItemFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(CartDto cartDto) {
}

    private void checkInput(CreateCartRequestDto createRequest) {
}
}