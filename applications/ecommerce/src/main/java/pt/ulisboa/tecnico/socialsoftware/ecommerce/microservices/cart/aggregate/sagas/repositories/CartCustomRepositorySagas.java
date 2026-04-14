package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.cart.aggregate.sagas.SagaCart;

@Repository
public interface CartCustomRepositorySagas extends JpaRepository<SagaCart, Integer> {
}