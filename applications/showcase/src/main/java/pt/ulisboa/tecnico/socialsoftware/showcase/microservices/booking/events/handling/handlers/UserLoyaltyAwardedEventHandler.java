package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.events.handling.handlers;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.eventProcessing.BookingEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.BookingRepository;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.UserLoyaltyAwardedEvent;

public class UserLoyaltyAwardedEventHandler extends BookingEventHandler {
    public UserLoyaltyAwardedEventHandler(BookingRepository bookingRepository, BookingEventProcessing bookingEventProcessing) {
        super(bookingRepository, bookingEventProcessing);
    }

    @Override
    public void handleEvent(Integer subscriberAggregateId, Event event) {
        this.bookingEventProcessing.processUserLoyaltyAwardedEvent(subscriberAggregateId, (UserLoyaltyAwardedEvent) event);
    }
}
