package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface QuestionRepository extends JpaRepository<Question, Integer> {
    @Query("select q from Question q " +
            "where q.state = 'ACTIVE' " +
            "and q.courseAggregateId = :courseAggregateId " +
            "and q.version = (select max(q2.version) from Question q2 where q2.aggregateId = q.aggregateId)")
    List<Question> findAllLatestActiveByCourse(@Param("courseAggregateId") Integer courseAggregateId);

    @Query(value = "select a1 from Question a1 where a1.aggregateId = :aggregateId AND a1.state = 'ACTIVE' AND a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<Question> findLastAggregateVersion(Integer aggregateId);

    Optional<Question> findTopByOrderByVersionDesc();
}
