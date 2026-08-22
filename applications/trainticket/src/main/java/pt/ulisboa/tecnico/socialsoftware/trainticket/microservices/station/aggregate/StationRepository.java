package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate;

import org.springframework.data.jpa.repository.Query;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.util.List;

public interface StationRepository extends AggregateRepository {
    @Query("select s from Station s " +
            "where s.state = 'ACTIVE' " +
            "and s.version = (select max(s2.version) from Station s2 where s2.aggregateId = s.aggregateId)")
    List<Station> findAllLatestActive();
}
