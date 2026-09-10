package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate;

import org.springframework.data.jpa.repository.Query;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.util.List;

public interface TripRepository extends AggregateRepository {
    @Query("select t from Trip t " +
            "where t.state = 'ACTIVE' " +
            "and t.version = (select max(t2.version) from Trip t2 where t2.aggregateId = t.aggregateId)")
    List<Trip> findAllLatestActive();
}
