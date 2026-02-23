package pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.aggregates.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.sagas.aggregates.SagaPost;

@Repository
public interface PostCustomRepositorySagas extends JpaRepository<SagaPost, Integer> {
}