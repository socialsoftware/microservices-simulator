package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.category.aggregate.sagas.SagaCategory;

@Repository
public interface CategoryCustomRepositorySagas extends JpaRepository<SagaCategory, Integer> {
}