package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Transactional
@Repository
public interface TopicRepository extends JpaRepository<Topic, Integer> {
}
