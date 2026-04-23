package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.sagas.SagaCustomer;

@Repository
public interface CustomerCustomRepositorySagas extends JpaRepository<SagaCustomer, Integer> {
}