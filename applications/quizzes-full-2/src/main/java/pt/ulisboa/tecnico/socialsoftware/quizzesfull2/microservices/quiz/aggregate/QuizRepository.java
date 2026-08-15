package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface QuizRepository extends JpaRepository<Quiz, Integer> {
    @Query("select q from Quiz q " +
            "where q.state = 'ACTIVE' " +
            "and q.execution.executionAggregateId = :executionAggregateId " +
            "and q.version = (select max(q2.version) from Quiz q2 where q2.aggregateId = q.aggregateId)")
    List<Quiz> findAllLatestActiveByExecution(@Param("executionAggregateId") Integer executionAggregateId);

    @Query(value = "select a1 from Quiz a1 where a1.aggregateId = :aggregateId AND a1.state = 'ACTIVE' AND a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<Quiz> findLastAggregateVersion(Integer aggregateId);

    Optional<Quiz> findTopByOrderByVersionDesc();
}
