package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.util.List;

@Repository
public interface QuestionRepository extends AggregateRepository {
    @Query("select q from Question q " +
            "where q.state = 'ACTIVE' " +
            "and q.courseAggregateId = :courseAggregateId " +
            "and q.version = (select max(q2.version) from Question q2 where q2.aggregateId = q.aggregateId)")
    List<Question> findAllLatestActiveByCourse(@Param("courseAggregateId") Integer courseAggregateId);
}
