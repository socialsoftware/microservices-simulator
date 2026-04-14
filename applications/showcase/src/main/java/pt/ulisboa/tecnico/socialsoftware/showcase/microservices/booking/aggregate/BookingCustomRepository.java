package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate;

import java.util.Set;

public interface BookingCustomRepository {
    Set<Integer> findExpensiveBookingIds(Double minPrice);
    Set<Integer> findConfirmedBookingIds();
}