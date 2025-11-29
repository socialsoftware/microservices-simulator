package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaCourse;

@Repository
public interface CourseCustomRepositorySagas extends JpaRepository<SagaCourse, Integer> {
    // Saga-specific repository methods can be added here
    }