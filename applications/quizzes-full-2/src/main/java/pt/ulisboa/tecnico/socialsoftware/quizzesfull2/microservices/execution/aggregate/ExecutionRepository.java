package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface ExecutionRepository extends JpaRepository<Execution, Integer> {
    @Query("select e from Execution e " +
            "where e.state = 'ACTIVE' " +
            "and e.version = (select max(e2.version) from Execution e2 where e2.aggregateId = e.aggregateId)")
    List<Execution> findAllLatestActive();

    @Query("select e from Execution e join e.students s " +
            "where e.state = 'ACTIVE' " +
            "and s.userAggregateId = :userAggregateId " +
            "and e.version = (select max(e2.version) from Execution e2 where e2.aggregateId = e.aggregateId)")
    List<Execution> findAllLatestActiveByStudent(@Param("userAggregateId") Integer userAggregateId);

    @Query(value = "select a1 from Execution a1 where a1.aggregateId = :aggregateId AND a1.state = 'ACTIVE' AND a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<Execution> findLastAggregateVersion(Integer aggregateId);

    Optional<Execution> findTopByOrderByVersionDesc();
}
