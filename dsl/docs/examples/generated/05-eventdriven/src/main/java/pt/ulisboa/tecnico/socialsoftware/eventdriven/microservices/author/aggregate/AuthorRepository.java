package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface AuthorRepository extends JpaRepository<Author, Integer> {

}