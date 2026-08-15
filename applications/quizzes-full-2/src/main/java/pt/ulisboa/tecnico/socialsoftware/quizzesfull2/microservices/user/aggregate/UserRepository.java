package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface UserRepository extends JpaRepository<User, Integer> {
    @Query("select u from User u " +
            "where u.state = 'ACTIVE' " +
            "and u.role = :role " +
            "and u.version = (select max(u2.version) from User u2 where u2.aggregateId = u.aggregateId)")
    List<User> findAllLatestActiveByRole(@Param("role") Role role);

    @Query(value = "select a1 from User a1 where a1.aggregateId = :aggregateId AND a1.state = 'ACTIVE' AND a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<User> findLastAggregateVersion(Integer aggregateId);

    Optional<User> findTopByOrderByVersionDesc();
}
