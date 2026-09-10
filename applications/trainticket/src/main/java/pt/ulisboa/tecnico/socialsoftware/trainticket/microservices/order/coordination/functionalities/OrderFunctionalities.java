package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.SeatClass;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.CancelOrderFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.CollectTicketFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.DeleteOrderFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.GetLeftTicketCountFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.GetOrderByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.GetOrdersByAccountFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.GetOrdersFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.PayOrderFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.PreserveTicketFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.UseTicketFunctionalitySagas;

import java.time.LocalDate;
import java.util.List;

@Service
public class OrderFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;

    public OrderDto getOrderById(Integer orderAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getOrderById");
        GetOrderByIdFunctionalitySagas saga = new GetOrderByIdFunctionalitySagas(
                unitOfWorkService, orderAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getOrderDto();
    }

    public List<OrderDto> getOrders() {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getOrders");
        GetOrdersFunctionalitySagas saga = new GetOrdersFunctionalitySagas(
                unitOfWorkService, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getOrders();
    }

    public List<OrderDto> getOrdersByAccount(Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getOrdersByAccount");
        GetOrdersByAccountFunctionalitySagas saga = new GetOrdersByAccountFunctionalitySagas(
                unitOfWorkService, userAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getOrders();
    }

    public Integer getLeftTicketCount(Integer tripAggregateId, LocalDate travelDate, SeatClass seatClass) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getLeftTicketCount");
        GetLeftTicketCountFunctionalitySagas saga = new GetLeftTicketCountFunctionalitySagas(
                unitOfWorkService, tripAggregateId, travelDate, seatClass, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getLeftTicketCount();
    }

    public OrderDto preserveTicket(Integer userAggregateId, Integer contactsAggregateId,
                                   Integer tripAggregateId, LocalDate travelDate, String fromStationName,
                                   String toStationName, SeatClass seatClass) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("preserveTicket");
        PreserveTicketFunctionalitySagas saga = new PreserveTicketFunctionalitySagas(unitOfWorkService,
                bookingRequest(userAggregateId, contactsAggregateId, tripAggregateId, travelDate,
                        fromStationName, toStationName, seatClass),
                unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getCreatedOrderDto();
    }

    public void payOrder(Integer orderAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("payOrder");
        PayOrderFunctionalitySagas saga = new PayOrderFunctionalitySagas(
                unitOfWorkService, orderAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void collectTicket(Integer orderAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("collectTicket");
        CollectTicketFunctionalitySagas saga = new CollectTicketFunctionalitySagas(
                unitOfWorkService, orderAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void useTicket(Integer orderAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("useTicket");
        UseTicketFunctionalitySagas saga = new UseTicketFunctionalitySagas(
                unitOfWorkService, orderAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void cancelOrder(Integer orderAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("cancelOrder");
        CancelOrderFunctionalitySagas saga = new CancelOrderFunctionalitySagas(
                unitOfWorkService, orderAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteOrder(Integer orderAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("deleteOrder");
        DeleteOrderFunctionalitySagas saga = new DeleteOrderFunctionalitySagas(
                unitOfWorkService, orderAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    // The terms a booking request carries; every other term on the order is derived downstream.
    private static OrderDto bookingRequest(Integer userAggregateId, Integer contactsAggregateId,
                                           Integer tripAggregateId, LocalDate travelDate,
                                           String fromStationName, String toStationName,
                                           SeatClass seatClass) {
        OrderDto orderDto = new OrderDto();
        orderDto.setUserAggregateId(userAggregateId);
        orderDto.setContactsAggregateId(contactsAggregateId);
        orderDto.setTripAggregateId(tripAggregateId);
        orderDto.setTravelDate(travelDate);
        orderDto.setFromStationName(fromStationName);
        orderDto.setToStationName(toStationName);
        orderDto.setSeatClass(seatClass);
        return orderDto;
    }
}
