package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.util.List;

public interface ContactsRepository extends AggregateRepository {
    @Query("select c from Contacts c " +
            "where c.userAggregateId = :userAggregateId " +
            "and c.state = 'ACTIVE' " +
            "and c.version = (select max(c2.version) from Contacts c2 where c2.aggregateId = c.aggregateId)")
    List<Contacts> findAllLatestActiveByAccount(@Param("userAggregateId") Integer userAggregateId);
}
