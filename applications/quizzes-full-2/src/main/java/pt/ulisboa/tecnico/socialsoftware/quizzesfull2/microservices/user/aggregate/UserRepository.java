package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.util.List;

@Repository
public interface UserRepository extends AggregateRepository {
    @Query("select u from User u " +
            "where u.state = 'ACTIVE' " +
            "and u.role = :role " +
            "and u.version = (select max(u2.version) from User u2 where u2.aggregateId = u.aggregateId)")
    List<User> findAllLatestActiveByRole(@Param("role") Role role);
}
