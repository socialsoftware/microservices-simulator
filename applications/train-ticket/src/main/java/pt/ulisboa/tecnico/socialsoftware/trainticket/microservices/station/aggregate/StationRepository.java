package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface StationRepository extends JpaRepository<Station, Integer> {
    @Query("select s from Station s " +
            "where s.state = 'ACTIVE' " +
            "and s.name = :name " +
            "and s.version = (select max(s2.version) from Station s2 where s2.aggregateId = s.aggregateId)")
    List<Station> findAllLatestActiveByName(@Param("name") String name);

    @Query(value = "select a1 from Station a1 where a1.aggregateId = :aggregateId AND a1.state = 'ACTIVE' AND a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<Station> findLastAggregateVersion(Integer aggregateId);

    Optional<Station> findTopByOrderByVersionDesc();
}
