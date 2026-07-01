package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Transactional
@Repository
public interface TopicRepository extends JpaRepository<Topic, Integer> {
    Optional<Topic> findTopByOrderByVersionDesc();

    default Optional<Topic> findLatestTopic() {
        return findTopByOrderByVersionDesc();
    }
}
