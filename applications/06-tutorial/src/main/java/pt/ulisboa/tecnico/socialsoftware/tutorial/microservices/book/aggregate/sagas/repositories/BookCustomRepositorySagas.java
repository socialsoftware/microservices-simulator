package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.book.aggregate.sagas.SagaBook;

@Repository
public interface BookCustomRepositorySagas extends JpaRepository<SagaBook, Integer> {
}