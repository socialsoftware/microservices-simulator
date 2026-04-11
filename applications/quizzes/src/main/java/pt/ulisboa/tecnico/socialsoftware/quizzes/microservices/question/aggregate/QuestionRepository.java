package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.Set;

@Repository
@Transactional
public interface QuestionRepository extends JpaRepository<Question, Integer> {

    @Query("SELECT q.aggregateId FROM Question q")
    Set<Integer> findAllAggregateIds();

    @Query(value = "select a1 from Question a1 where a1.aggregateId = :aggregateId AND a1.state = 'ACTIVE' AND a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<Question> findLastAggregateVersion(Integer aggregateId);
}
