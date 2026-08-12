package pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.product.aggregate.sagas.SagaProduct;

@Repository
public interface ProductCustomRepositorySagas extends JpaRepository<SagaProduct, Integer> {
}