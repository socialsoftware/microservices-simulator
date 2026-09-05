package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate;

import org.springframework.data.jpa.repository.Query;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.util.List;

public interface UserRepository extends AggregateRepository {
    @Query("select u from User u " +
            "where u.state = 'ACTIVE' " +
            "and u.version = (select max(u2.version) from User u2 where u2.aggregateId = u.aggregateId)")
    List<User> findAllLatestActive();
}
