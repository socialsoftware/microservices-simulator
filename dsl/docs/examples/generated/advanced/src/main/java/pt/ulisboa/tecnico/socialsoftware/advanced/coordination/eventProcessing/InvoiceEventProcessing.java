package pt.ulisboa.tecnico.socialsoftware.advanced.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.service.InvoiceService;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.publish.OrderDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.events.publish.CustomerDeletedEvent;

@Service
public class InvoiceEventProcessing {
    @Autowired
    private InvoiceService invoiceService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public InvoiceEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processOrderDeletedEvent(Integer aggregateId, OrderDeletedEvent orderDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }

    public void processCustomerDeletedEvent(Integer aggregateId, CustomerDeletedEvent customerDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}