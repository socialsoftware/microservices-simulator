package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.events.handling.handlers;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventHandler;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.eventProcessing.BookingEventProcessing;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.Booking;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.BookingRepository;

public abstract class BookingEventHandler extends EventHandler {
    private BookingRepository bookingRepository;
    protected BookingEventProcessing bookingEventProcessing;

    public BookingEventHandler(BookingRepository bookingRepository, BookingEventProcessing bookingEventProcessing) {
        this.bookingRepository = bookingRepository;
        this.bookingEventProcessing = bookingEventProcessing;
    }

    public Set<Integer> getAggregateIds() {
        return bookingRepository.findAll().stream().map(Booking::getAggregateId).collect(Collectors.toSet());
    }

}
