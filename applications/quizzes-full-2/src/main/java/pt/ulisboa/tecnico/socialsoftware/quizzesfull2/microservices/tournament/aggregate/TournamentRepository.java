package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface TournamentRepository extends JpaRepository<Tournament, Integer> {
    @Query("select t from Tournament t " +
            "where t.state = 'ACTIVE' " +
            "and t.execution.executionAggregateId = :executionAggregateId " +
            "and t.version = (select max(t2.version) from Tournament t2 where t2.aggregateId = t.aggregateId)")
    List<Tournament> findAllLatestActiveByExecution(@Param("executionAggregateId") Integer executionAggregateId);

    @Query(value = "select a1 from Tournament a1 where a1.aggregateId = :aggregateId AND a1.state = 'ACTIVE' AND a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<Tournament> findLastAggregateVersion(Integer aggregateId);

    Optional<Tournament> findTopByOrderByVersionDesc();
}
