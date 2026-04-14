package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.discount.aggregate.sagas.SagaDiscount;

@Repository
public interface DiscountCustomRepositorySagas extends JpaRepository<SagaDiscount, Integer> {
}