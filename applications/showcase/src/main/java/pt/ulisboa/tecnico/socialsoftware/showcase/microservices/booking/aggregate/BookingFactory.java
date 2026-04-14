package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate;

import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingDto;

public interface BookingFactory {
    Booking createBooking(Integer aggregateId, BookingDto bookingDto);
    Booking createBookingFromExisting(Booking existingBooking);
    BookingDto createBookingDto(Booking booking);
}
