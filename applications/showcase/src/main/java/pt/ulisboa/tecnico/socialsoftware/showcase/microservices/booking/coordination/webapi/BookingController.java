package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.functionalities.BookingFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.coordination.webapi.requestDtos.CreateBookingRequestDto;

@RestController
public class BookingController {
    @Autowired
    private BookingFunctionalities bookingFunctionalities;

    @PostMapping("/bookings/create")
    @ResponseStatus(HttpStatus.CREATED)
    public BookingDto createBooking(@RequestBody CreateBookingRequestDto createRequest) {
        return bookingFunctionalities.createBooking(createRequest);
    }

    @GetMapping("/bookings/{bookingAggregateId}")
    public BookingDto getBookingById(@PathVariable Integer bookingAggregateId) {
        return bookingFunctionalities.getBookingById(bookingAggregateId);
    }

    @PutMapping("/bookings")
    public BookingDto updateBooking(@RequestBody BookingDto bookingDto) {
        return bookingFunctionalities.updateBooking(bookingDto);
    }

    @DeleteMapping("/bookings/{bookingAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteBooking(@PathVariable Integer bookingAggregateId) {
        bookingFunctionalities.deleteBooking(bookingAggregateId);
    }

    @GetMapping("/bookings")
    public List<BookingDto> getAllBookings() {
        return bookingFunctionalities.getAllBookings();
    }
}
