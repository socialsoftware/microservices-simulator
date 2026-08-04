package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas.SagaContacts;

@Repository
public interface ContactsCustomRepositorySagas extends JpaRepository<SagaContacts, Integer> {
}