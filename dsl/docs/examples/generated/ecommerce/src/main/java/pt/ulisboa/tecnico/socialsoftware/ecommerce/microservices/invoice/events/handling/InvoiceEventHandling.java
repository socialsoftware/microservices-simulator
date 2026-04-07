package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.coordination.eventProcessing.InvoiceEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.aggregate.InvoiceRepository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.events.handling.handlers.PaymentAuthorizedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.PaymentAuthorizedEvent;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.invoice.events.handling.handlers.OrderCancelledEventHandler;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.events.OrderCancelledEvent;

@Component
public class InvoiceEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private InvoiceEventProcessing invoiceEventProcessing;
    @Autowired
    private InvoiceRepository invoiceRepository;

    @Scheduled(fixedDelay = 1000)
    public void handlePaymentAuthorizedEventEvents() {
        eventApplicationService.handleSubscribedEvent(PaymentAuthorizedEvent.class,
                new PaymentAuthorizedEventHandler(invoiceRepository, invoiceEventProcessing));
    }

    @Scheduled(fixedDelay = 1000)
    public void handleOrderCancelledEventEvents() {
        eventApplicationService.handleSubscribedEvent(OrderCancelledEvent.class,
                new OrderCancelledEventHandler(invoiceRepository, invoiceEventProcessing));
    }

}