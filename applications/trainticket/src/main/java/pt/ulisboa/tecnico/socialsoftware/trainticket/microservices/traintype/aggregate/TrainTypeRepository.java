package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate;

import org.springframework.data.jpa.repository.Query;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.util.List;

public interface TrainTypeRepository extends AggregateRepository {
    @Query("select t from TrainType t " +
            "where t.state = 'ACTIVE' " +
            "and t.version = (select max(t2.version) from TrainType t2 where t2.aggregateId = t.aggregateId)")
    List<TrainType> findAllLatestActive();
}
