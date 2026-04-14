package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.service.ShippingService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.PaymentAuthorizedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderCancelledEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.Shipping;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.ShippingFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderDeletedEvent;

@Service
public class ShippingEventProcessing {
    @Autowired
    private ShippingService shippingService;

    @Autowired
    private ShippingFactory shippingFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public ShippingEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processPaymentAuthorizedEvent(Integer aggregateId, PaymentAuthorizedEvent paymentAuthorizedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        shippingService.handlePaymentAuthorizedEvent(aggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processOrderCancelledEvent(Integer aggregateId, OrderCancelledEvent orderCancelledEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        shippingService.handleOrderCancelledEvent(aggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processOrderDeletedEvent(Integer aggregateId, OrderDeletedEvent orderDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Shipping oldShipping = (Shipping) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Shipping newShipping = shippingFactory.createShippingFromExisting(oldShipping);
        newShipping.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newShipping, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}