package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.service.OrderService;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.CustomerDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.Order;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.OrderFactory;
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

    public void processCustomerDeletedEvent(Integer aggregateId, CustomerDeletedEvent customerDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Order oldOrder = (Order) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Order newOrder = orderFactory.createOrderFromExisting(oldOrder);
        newOrder.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newOrder, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}