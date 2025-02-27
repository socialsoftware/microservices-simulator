package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Transactional
@Repository
public interface TopicRepository extends JpaRepository<Topic, Integer> {
}
