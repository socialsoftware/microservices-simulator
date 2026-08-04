package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.util.List;

@Repository
public interface TopicRepository extends AggregateRepository {
    @Query("select t from Topic t " +
            "where t.state = 'ACTIVE' " +
            "and t.courseAggregateId = :courseAggregateId " +
            "and t.version = (select max(t2.version) from Topic t2 where t2.aggregateId = t.aggregateId)")
    List<Topic> findAllLatestActiveByCourse(@Param("courseAggregateId") Integer courseAggregateId);
}
