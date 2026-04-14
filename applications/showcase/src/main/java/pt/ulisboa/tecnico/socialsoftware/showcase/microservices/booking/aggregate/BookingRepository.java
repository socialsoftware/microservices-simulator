package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface BookingRepository extends JpaRepository<Booking, Integer> {
    @Query(value = "select b.aggregateId from Booking b where b.totalPrice >= :minPrice")
    Set<Integer> findExpensiveBookingIds(Double minPrice);
    @Query(value = "select b.aggregateId from Booking b where b.confirmed = true and b.state != 'DELETED'")
    Set<Integer> findConfirmedBookingIds();
}