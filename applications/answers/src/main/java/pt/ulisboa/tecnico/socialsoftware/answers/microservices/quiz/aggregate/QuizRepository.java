package pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.aggregate;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface QuizRepository extends JpaRepository<Quiz, Integer> {
    @Query(value = "select q1.aggregateId from Quiz q1 where q1.aggregateId NOT IN (select q2.aggregateId from Quiz q2 where q2.state = 'DELETED') AND q1.execution.executionAggregateId = :executionAggregateId")
    Set<Integer> findAllQuizIdsByExecution(Integer executionAggregateId);
}