package pt.ulisboa.tecnico.socialsoftware.typesenums.sagas.aggregates.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.typesenums.sagas.aggregates.SagaContact;

@Repository
public interface ContactCustomRepositorySagas extends JpaRepository<SagaContact, Integer> {
}