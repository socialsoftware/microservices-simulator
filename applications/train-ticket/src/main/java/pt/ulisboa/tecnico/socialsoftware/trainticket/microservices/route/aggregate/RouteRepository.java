package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Transactional
public interface RouteRepository extends JpaRepository<Route, Integer> {
    @Query(value = "select a1 from Route a1 where a1.aggregateId = :aggregateId AND a1.state = 'ACTIVE' AND a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<Route> findLastAggregateVersion(Integer aggregateId);

    Optional<Route> findTopByOrderByVersionDesc();
}
