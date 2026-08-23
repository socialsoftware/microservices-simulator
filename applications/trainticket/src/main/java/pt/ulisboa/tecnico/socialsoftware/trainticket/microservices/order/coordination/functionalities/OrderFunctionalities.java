package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.SeatClass;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.GetLeftTicketCountFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.GetOrderByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.GetOrdersByAccountFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas.GetOrdersFunctionalitySagas;

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
}
