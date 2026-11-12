package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate;

import java.time.LocalDate;
import java.util.List;

public interface OrderCustomRepository {
    List<Order> findAllLatestActive();

    List<Order> findAllLatestActiveByUser(Integer userAggregateId);

    // The seat set shared by GetLeftTicketCount, the booking saga's seat allocation and the
    // SEAT_CAPACITY_NOT_EXCEEDED / SEAT_NUMBER_UNIQUE_PER_DEPARTURE guards: a cancelled order
    // releases its seat, so it is excluded here rather than at each call site.
    List<Order> findAllLatestActiveHoldingSeatOn(Integer tripAggregateId, LocalDate travelDate, SeatClass seatClass);
}
