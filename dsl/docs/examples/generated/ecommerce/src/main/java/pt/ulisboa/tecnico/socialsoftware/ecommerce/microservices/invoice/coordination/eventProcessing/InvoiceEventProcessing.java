package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.service.InvoiceService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.PaymentAuthorizedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderCancelledEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.UserDeletedEvent;

@Service
public class InvoiceEventProcessing {
    @Autowired
    private InvoiceService invoiceService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public InvoiceEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processPaymentAuthorizedEvent(Integer aggregateId, PaymentAuthorizedEvent paymentAuthorizedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        invoiceService.handlePaymentAuthorizedEvent(aggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processOrderCancelledEvent(Integer aggregateId, OrderCancelledEvent orderCancelledEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        invoiceService.handleOrderCancelledEvent(aggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processOrderDeletedEvent(Integer aggregateId, OrderDeletedEvent orderDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }

    public void processUserDeletedEvent(Integer aggregateId, UserDeletedEvent userDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}