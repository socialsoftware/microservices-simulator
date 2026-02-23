package pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface TopicRepository extends JpaRepository<Topic, Integer> {

}