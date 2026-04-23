package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface ContactRepository extends JpaRepository<Contact, Integer> {

}