package pt.ulisboa.tecnico.socialsoftware.teastore.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.events.publish.UserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.events.publish.UserDeletedEvent;

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
        orderService.handleUserUpdatedEvent(aggregateId, userUpdatedEvent.getPublisherAggregateId(), userUpdatedEvent.getPublisherAggregateVersion(), userUpdatedEvent.getUserName(), userUpdatedEvent.getRealName(), userUpdatedEvent.getEmail(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processUserDeletedEvent(Integer aggregateId, UserDeletedEvent userDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}