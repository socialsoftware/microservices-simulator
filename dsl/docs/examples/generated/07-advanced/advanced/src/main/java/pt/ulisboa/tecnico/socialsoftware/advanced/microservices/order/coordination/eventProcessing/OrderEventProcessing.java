package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.CustomerDeletedEvent;

@Service
public class OrderEventProcessing {
    @Autowired
    private OrderService orderService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public OrderEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processCustomerDeletedEvent(Integer aggregateId, CustomerDeletedEvent customerDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}