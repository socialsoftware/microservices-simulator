package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaUser;

@Repository
public interface UserCustomRepositorySagas extends JpaRepository<SagaUser, Integer> {
}