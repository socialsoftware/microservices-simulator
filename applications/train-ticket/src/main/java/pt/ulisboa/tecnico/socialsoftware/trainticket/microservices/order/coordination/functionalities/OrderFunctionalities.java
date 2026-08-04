package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.webapi.requestDtos.CreateOrderRequestDto;
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
            throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
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
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
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
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
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
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
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
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
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
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(OrderDto orderDto) {
        if (orderDto.getBoughtDate() == null) {
            throw new TrainTicketException(ORDER_MISSING_BOUGHTDATE);
        }
        if (orderDto.getTravelDate() == null) {
            throw new TrainTicketException(ORDER_MISSING_TRAVELDATE);
        }
        if (orderDto.getTravelTime() == null) {
            throw new TrainTicketException(ORDER_MISSING_TRAVELTIME);
        }
        if (orderDto.getSeatNumber() == null) {
            throw new TrainTicketException(ORDER_MISSING_SEATNUMBER);
        }
}

    private void checkInput(CreateOrderRequestDto createRequest) {
        if (createRequest.getBoughtDate() == null) {
            throw new TrainTicketException(ORDER_MISSING_BOUGHTDATE);
        }
        if (createRequest.getTravelDate() == null) {
            throw new TrainTicketException(ORDER_MISSING_TRAVELDATE);
        }
        if (createRequest.getTravelTime() == null) {
            throw new TrainTicketException(ORDER_MISSING_TRAVELTIME);
        }
        if (createRequest.getSeatNumber() == null) {
            throw new TrainTicketException(ORDER_MISSING_SEATNUMBER);
        }
}
}