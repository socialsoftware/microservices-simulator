package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Transactional
@Repository
public interface TopicRepository extends JpaRepository<Topic, Integer> {
    @Query(value = "select t1 from Topic t1 where t1.version = (select max(t2.version) from Topic t2)")
    Optional<Topic> findLatestTopic();
}
