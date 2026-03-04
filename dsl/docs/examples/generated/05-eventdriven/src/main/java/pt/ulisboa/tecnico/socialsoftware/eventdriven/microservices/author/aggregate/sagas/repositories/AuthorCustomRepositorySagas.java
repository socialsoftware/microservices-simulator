package pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.eventdriven.microservices.author.aggregate.sagas.SagaAuthor;

@Repository
public interface AuthorCustomRepositorySagas extends JpaRepository<SagaAuthor, Integer> {
}