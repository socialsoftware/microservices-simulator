package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserDeletedEvent;

@Service
public class OrderEventProcessing {
    @Autowired
    private OrderService orderService;
    
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
        // Reference constraint event processing - implement constraint logic
    }
}