package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.service.InvoiceService;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.OrderDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.CustomerDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.Invoice;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.aggregate.InvoiceFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

@Service
public class InvoiceEventProcessing {
    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private InvoiceFactory invoiceFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public InvoiceEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processOrderDeletedEvent(Integer aggregateId, OrderDeletedEvent orderDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Invoice oldInvoice = (Invoice) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Invoice newInvoice = invoiceFactory.createInvoiceFromExisting(oldInvoice);
        newInvoice.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newInvoice, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processCustomerDeletedEvent(Integer aggregateId, CustomerDeletedEvent customerDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Invoice oldInvoice = (Invoice) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Invoice newInvoice = invoiceFactory.createInvoiceFromExisting(oldInvoice);
        newInvoice.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newInvoice, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}