package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.events.subscribe;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.Booking;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.UserLoyaltyAwardedEvent;

public class BookingSubscribesUserLoyaltyAwarded extends EventSubscription {
    

    public BookingSubscribesUserLoyaltyAwarded(Booking booking) {
        super(booking.getAggregateId(),
                0,
                UserLoyaltyAwardedEvent.class.getSimpleName());
        
    }

    public BookingSubscribesUserLoyaltyAwarded() {
    }

    public boolean subscribesEvent(Event event) {
        return super.subscribesEvent(event);
    }
}
