package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.aggregate.OrderFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

@Service
public class OrderEventProcessing {
    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderFactory orderFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public OrderEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processUserUpdatedEvent(Integer aggregateId, UserUpdatedEvent userUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        orderService.handleUserUpdatedEvent(aggregateId, userUpdatedEvent.getPublisherAggregateId(), userUpdatedEvent.getPublisherAggregateVersion(), userUpdatedEvent.getUsername(), userUpdatedEvent.getEmail(), userUpdatedEvent.getShippingAddress(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processUserDeletedEvent(Integer aggregateId, UserDeletedEvent userDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
        newOrder.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newOrder, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}