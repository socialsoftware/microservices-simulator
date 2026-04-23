package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.sagas.SagaContact;

@Repository
public interface ContactCustomRepositorySagas extends JpaRepository<SagaContact, Integer> {
}