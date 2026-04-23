package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.OrderCustomer;
import pt.ulisboa.tecnico.socialsoftware.advanced.events.CustomerDeletedEvent;


public class OrderSubscribesCustomerDeletedCustomerRef extends EventSubscription {
    public OrderSubscribesCustomerDeletedCustomerRef(OrderCustomer customer) {
        super(customer.getCustomerAggregateId(),
                customer.getCustomerVersion(),
                CustomerDeletedEvent.class.getSimpleName());
    }

    @Override
    public boolean subscribesEvent(Event event) {
         return super.subscribesEvent(event);
    }

}
