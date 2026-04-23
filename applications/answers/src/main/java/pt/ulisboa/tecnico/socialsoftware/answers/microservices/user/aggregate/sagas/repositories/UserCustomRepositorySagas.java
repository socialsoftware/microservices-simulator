package pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.sagas.SagaUser;

@Repository
public interface UserCustomRepositorySagas extends JpaRepository<SagaUser, Integer> {
}