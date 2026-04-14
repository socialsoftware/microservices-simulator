package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.service.PaymentService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderPlacedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderCancelledEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.Payment;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.aggregate.PaymentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderDeletedEvent;

@Service
public class PaymentEventProcessing {
    @Autowired
    private PaymentService paymentService;

    @Autowired
    private PaymentFactory paymentFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public PaymentEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processOrderPlacedEvent(Integer aggregateId, OrderPlacedEvent orderPlacedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        paymentService.handleOrderPlacedEvent(aggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processOrderCancelledEvent(Integer aggregateId, OrderCancelledEvent orderCancelledEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        paymentService.handleOrderCancelledEvent(aggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processOrderDeletedEvent(Integer aggregateId, OrderDeletedEvent orderDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Payment oldPayment = (Payment) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Payment newPayment = paymentFactory.createPaymentFromExisting(oldPayment);
        newPayment.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newPayment, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}