package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate;

import org.springframework.data.jpa.repository.Query;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.time.LocalDate;
import java.util.List;

public interface OrderRepository extends AggregateRepository {
    @Query("select o from Order o " +
            "where o.state = 'ACTIVE' " +
            "and o.version = (select max(o2.version) from Order o2 where o2.aggregateId = o.aggregateId)")
    List<Order> findAllLatestActive();

    @Query("select o from Order o " +
            "where o.state = 'ACTIVE' " +
            "and o.userAggregateId = :userAggregateId " +
            "and o.version = (select max(o2.version) from Order o2 where o2.aggregateId = o.aggregateId)")
    List<Order> findAllLatestActiveByUser(Integer userAggregateId);

    @Query("select o from Order o " +
            "where o.state = 'ACTIVE' " +
            "and o.status <> 'CANCELLED' " +
            "and o.tripAggregateId = :tripAggregateId " +
            "and o.travelDate = :travelDate " +
            "and o.seatClass = :seatClass " +
            "and o.version = (select max(o2.version) from Order o2 where o2.aggregateId = o.aggregateId)")
    List<Order> findAllLatestActiveHoldingSeatOn(Integer tripAggregateId, LocalDate travelDate, SeatClass seatClass);
}
