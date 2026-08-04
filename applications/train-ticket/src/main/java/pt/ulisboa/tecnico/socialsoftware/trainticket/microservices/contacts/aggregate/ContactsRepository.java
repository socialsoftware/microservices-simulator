package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface ContactsRepository extends JpaRepository<Contacts, Integer> {

}