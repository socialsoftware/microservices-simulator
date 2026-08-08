package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface TopicRepository extends JpaRepository<Topic, Integer> {
    @Query("select t from Topic t " +
            "where t.state = 'ACTIVE' " +
            "and t.courseAggregateId = :courseAggregateId " +
            "and t.version = (select max(t2.version) from Topic t2 where t2.aggregateId = t.aggregateId)")
    List<Topic> findAllLatestActiveByCourse(@Param("courseAggregateId") Integer courseAggregateId);

    @Query(value = "select a1 from Topic a1 where a1.aggregateId = :aggregateId AND a1.state = 'ACTIVE' AND a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<Topic> findLastAggregateVersion(Integer aggregateId);

    Optional<Topic> findTopByOrderByVersionDesc();
}
