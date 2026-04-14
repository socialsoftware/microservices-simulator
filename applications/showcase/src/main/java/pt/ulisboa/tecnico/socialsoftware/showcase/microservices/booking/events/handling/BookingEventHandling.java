package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.events.handling;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.eventProcessing.BookingEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.BookingRepository;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.events.handling.handlers.UserLoyaltyAwardedEventHandler;
import pt.ulisboa.tecnico.socialsoftware.showcase.events.UserLoyaltyAwardedEvent;

@Component
public class BookingEventHandling {
    @Autowired
    private EventApplicationService eventApplicationService;
    @Autowired
    private BookingEventProcessing bookingEventProcessing;
    @Autowired
    private BookingRepository bookingRepository;

    @Scheduled(fixedDelay = 1000)
    public void handleUserLoyaltyAwardedEventEvents() {
        eventApplicationService.handleSubscribedEvent(UserLoyaltyAwardedEvent.class,
                new UserLoyaltyAwardedEventHandler(bookingRepository, bookingEventProcessing));
    }

}