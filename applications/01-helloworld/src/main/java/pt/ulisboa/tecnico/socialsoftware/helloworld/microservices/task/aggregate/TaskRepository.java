package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface TaskRepository extends JpaRepository<Task, Integer> {

}