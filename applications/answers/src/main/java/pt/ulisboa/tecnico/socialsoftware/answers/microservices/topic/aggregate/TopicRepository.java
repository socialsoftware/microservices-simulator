package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface TopicRepository extends JpaRepository<Topic, Integer> {
    @Query(value = "")
    Optional<Integer> findTopicIdByName(String topicName);
}