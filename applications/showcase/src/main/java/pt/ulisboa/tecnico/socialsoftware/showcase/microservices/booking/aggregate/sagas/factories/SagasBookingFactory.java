package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.Booking;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.BookingFactory;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.sagas.SagaBooking;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.sagas.dtos.SagaBookingDto;

@Service
@Profile("sagas")
public class SagasBookingFactory implements BookingFactory {
    @Override
    public Booking createBooking(Integer aggregateId, BookingDto bookingDto) {
        return new SagaBooking(aggregateId, bookingDto);
    }

    @Override
    public Booking createBookingFromExisting(Booking existingBooking) {
        return new SagaBooking((SagaBooking) existingBooking);
    }

    @Override
    public BookingDto createBookingDto(Booking booking) {
        return new SagaBookingDto(booking);
    }
}