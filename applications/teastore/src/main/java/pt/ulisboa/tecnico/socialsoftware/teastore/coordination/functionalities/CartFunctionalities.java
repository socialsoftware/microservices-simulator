package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception.TeastoreErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.exception.TeastoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.coordination.cart.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.cart.service.CartService;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.CartDto;

@Service
public class CartFunctionalities {
    @Autowired
    private CartService cartService;

    @Autowired
    private UserService userService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new TeastoreException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CartDto createCart(CartDto cartDto) throws TeastoreException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateCartFunctionalitySagas createCartFunctionalitySagas = new CreateCartFunctionalitySagas(
                        cartService, sagaUnitOfWorkService, cartDto, sagaUnitOfWork);
                createCartFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createCartFunctionalitySagas.getCreatedCart();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CartDto addItem(Long cartAggregateId, Long productId, String productName, Double unitPriceInCents, Integer quantity) throws TeastoreException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddItemFunctionalitySagas addItemFunctionalitySagas = new AddItemFunctionalitySagas(
                        cartService, sagaUnitOfWorkService, cartAggregateId, productId, productName, unitPriceInCents, quantity, sagaUnitOfWork);
                addItemFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addItemFunctionalitySagas.getResult();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CartDto updateItem(Long cartAggregateId, Long productId, Integer quantity) throws TeastoreException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateItemFunctionalitySagas updateItemFunctionalitySagas = new UpdateItemFunctionalitySagas(
                        cartService, sagaUnitOfWorkService, cartAggregateId, productId, quantity, sagaUnitOfWork);
                updateItemFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateItemFunctionalitySagas.getResult();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CartDto removeItem(Long cartAggregateId, Long productId) throws TeastoreException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveItemFunctionalitySagas removeItemFunctionalitySagas = new RemoveItemFunctionalitySagas(
                        cartService, sagaUnitOfWorkService, cartAggregateId, productId, sagaUnitOfWork);
                removeItemFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return removeItemFunctionalitySagas.getResult();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CartDto checkoutCart(Long cartAggregateId) throws TeastoreException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CheckoutCartFunctionalitySagas checkoutCartFunctionalitySagas = new CheckoutCartFunctionalitySagas(
                        cartService, sagaUnitOfWorkService, cartAggregateId, sagaUnitOfWork);
                checkoutCartFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return checkoutCartFunctionalitySagas.getResult();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CartDto findByUserId(Long userId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindByUserIdFunctionalitySagas findByUserIdFunctionalitySagas = new FindByUserIdFunctionalitySagas(
                        cartService, sagaUnitOfWorkService, userId, sagaUnitOfWork);
                findByUserIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findByUserIdFunctionalitySagas.getResult();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}