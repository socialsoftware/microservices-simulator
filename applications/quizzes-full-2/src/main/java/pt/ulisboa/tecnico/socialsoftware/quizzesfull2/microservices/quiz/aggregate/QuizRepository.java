package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.util.List;

@Repository
public interface QuizRepository extends AggregateRepository {
    @Query("select q from Quiz q " +
            "where q.state = 'ACTIVE' " +
            "and q.execution.executionAggregateId = :executionAggregateId " +
            "and q.version = (select max(q2.version) from Quiz q2 where q2.aggregateId = q.aggregateId)")
    List<Quiz> findAllLatestActiveByExecution(@Param("executionAggregateId") Integer executionAggregateId);
}
