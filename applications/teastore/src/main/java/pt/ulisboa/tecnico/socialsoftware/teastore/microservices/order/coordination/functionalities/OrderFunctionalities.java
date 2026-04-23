package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.coordination.webapi.requestDtos.CreateOrderRequestDto;
import java.util.List;

@Service
public class OrderFunctionalities {
    @Autowired
    private OrderService orderService;

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
            throw new TeastoreException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public OrderDto createOrder(CreateOrderRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateOrderFunctionalitySagas createOrderFunctionalitySagas = new CreateOrderFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createOrderFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createOrderFunctionalitySagas.getCreatedOrderDto();
            default: throw new TeastoreException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public OrderDto getOrderById(Integer orderAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetOrderByIdFunctionalitySagas getOrderByIdFunctionalitySagas = new GetOrderByIdFunctionalitySagas(
                        sagaUnitOfWorkService, orderAggregateId, sagaUnitOfWork, commandGateway);
                getOrderByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getOrderByIdFunctionalitySagas.getOrderDto();
            default: throw new TeastoreException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public OrderDto updateOrder(OrderDto orderDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(orderDto);
                UpdateOrderFunctionalitySagas updateOrderFunctionalitySagas = new UpdateOrderFunctionalitySagas(
                        sagaUnitOfWorkService, orderDto, sagaUnitOfWork, commandGateway);
                updateOrderFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateOrderFunctionalitySagas.getUpdatedOrderDto();
            default: throw new TeastoreException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteOrder(Integer orderAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteOrderFunctionalitySagas deleteOrderFunctionalitySagas = new DeleteOrderFunctionalitySagas(
                        sagaUnitOfWorkService, orderAggregateId, sagaUnitOfWork, commandGateway);
                deleteOrderFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new TeastoreException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<OrderDto> getAllOrders() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllOrdersFunctionalitySagas getAllOrdersFunctionalitySagas = new GetAllOrdersFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllOrdersFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllOrdersFunctionalitySagas.getOrders();
            default: throw new TeastoreException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(OrderDto orderDto) {
        if (orderDto.getTime() == null) {
            throw new TeastoreException(ORDER_MISSING_TIME);
        }
        if (orderDto.getAddressName() == null) {
            throw new TeastoreException(ORDER_MISSING_ADDRESSNAME);
        }
        if (orderDto.getAddress1() == null) {
            throw new TeastoreException(ORDER_MISSING_ADDRESS1);
        }
        if (orderDto.getAddress2() == null) {
            throw new TeastoreException(ORDER_MISSING_ADDRESS2);
        }
        if (orderDto.getCreditCardCompany() == null) {
            throw new TeastoreException(ORDER_MISSING_CREDITCARDCOMPANY);
        }
        if (orderDto.getCreditCardNumber() == null) {
            throw new TeastoreException(ORDER_MISSING_CREDITCARDNUMBER);
        }
        if (orderDto.getCreditCardExpiryDate() == null) {
            throw new TeastoreException(ORDER_MISSING_CREDITCARDEXPIRYDATE);
        }
}

    private void checkInput(CreateOrderRequestDto createRequest) {
        if (createRequest.getTime() == null) {
            throw new TeastoreException(ORDER_MISSING_TIME);
        }
        if (createRequest.getAddressName() == null) {
            throw new TeastoreException(ORDER_MISSING_ADDRESSNAME);
        }
        if (createRequest.getAddress1() == null) {
            throw new TeastoreException(ORDER_MISSING_ADDRESS1);
        }
        if (createRequest.getAddress2() == null) {
            throw new TeastoreException(ORDER_MISSING_ADDRESS2);
        }
        if (createRequest.getCreditCardCompany() == null) {
            throw new TeastoreException(ORDER_MISSING_CREDITCARDCOMPANY);
        }
        if (createRequest.getCreditCardNumber() == null) {
            throw new TeastoreException(ORDER_MISSING_CREDITCARDNUMBER);
        }
        if (createRequest.getCreditCardExpiryDate() == null) {
            throw new TeastoreException(ORDER_MISSING_CREDITCARDEXPIRYDATE);
        }
}
}