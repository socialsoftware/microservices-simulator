package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.shipping.aggregate.sagas.SagaShipping;

@Repository
public interface ShippingCustomRepositorySagas extends JpaRepository<SagaShipping, Integer> {
}