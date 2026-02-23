package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.advanced.microservices.exception.AdvancedErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.exception.AdvancedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.advanced.sagas.coordination.order.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.OrderProductDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.coordination.webapi.requestDtos.CreateOrderRequestDto;
import java.util.List;

@Service
public class OrderFunctionalities {
    @Autowired
    private OrderService orderService;

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
            throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public OrderDto createOrder(CreateOrderRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateOrderFunctionalitySagas createOrderFunctionalitySagas = new CreateOrderFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, orderService, createRequest);
                createOrderFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createOrderFunctionalitySagas.getCreatedOrderDto();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public OrderDto getOrderById(Integer orderAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetOrderByIdFunctionalitySagas getOrderByIdFunctionalitySagas = new GetOrderByIdFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, orderService, orderAggregateId);
                getOrderByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getOrderByIdFunctionalitySagas.getOrderDto();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public OrderDto updateOrder(OrderDto orderDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(orderDto);
                UpdateOrderFunctionalitySagas updateOrderFunctionalitySagas = new UpdateOrderFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, orderService, orderDto);
                updateOrderFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateOrderFunctionalitySagas.getUpdatedOrderDto();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteOrder(Integer orderAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteOrderFunctionalitySagas deleteOrderFunctionalitySagas = new DeleteOrderFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, orderService, orderAggregateId);
                deleteOrderFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<OrderDto> getAllOrders() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllOrdersFunctionalitySagas getAllOrdersFunctionalitySagas = new GetAllOrdersFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, orderService);
                getAllOrdersFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllOrdersFunctionalitySagas.getOrders();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public OrderProductDto addOrderProduct(Integer orderId, Integer productAggregateId, OrderProductDto productDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddOrderProductFunctionalitySagas addOrderProductFunctionalitySagas = new AddOrderProductFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, orderService,
                        orderId, productAggregateId, productDto);
                addOrderProductFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addOrderProductFunctionalitySagas.getAddedProductDto();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<OrderProductDto> addOrderProducts(Integer orderId, List<OrderProductDto> productDtos) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddOrderProductsFunctionalitySagas addOrderProductsFunctionalitySagas = new AddOrderProductsFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, orderService,
                        orderId, productDtos);
                addOrderProductsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addOrderProductsFunctionalitySagas.getAddedProductDtos();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public OrderProductDto getOrderProduct(Integer orderId, Integer productAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetOrderProductFunctionalitySagas getOrderProductFunctionalitySagas = new GetOrderProductFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, orderService,
                        orderId, productAggregateId);
                getOrderProductFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getOrderProductFunctionalitySagas.getProductDto();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public OrderProductDto updateOrderProduct(Integer orderId, Integer productAggregateId, OrderProductDto productDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateOrderProductFunctionalitySagas updateOrderProductFunctionalitySagas = new UpdateOrderProductFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, orderService,
                        orderId, productAggregateId, productDto);
                updateOrderProductFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateOrderProductFunctionalitySagas.getUpdatedProductDto();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeOrderProduct(Integer orderId, Integer productAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveOrderProductFunctionalitySagas removeOrderProductFunctionalitySagas = new RemoveOrderProductFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, orderService,
                        orderId, productAggregateId);
                removeOrderProductFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(OrderDto orderDto) {
}

    private void checkInput(CreateOrderRequestDto createRequest) {
}
}