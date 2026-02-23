package pt.ulisboa.tecnico.socialsoftware.advanced.sagas.aggregates.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.advanced.sagas.aggregates.SagaCustomer;

@Repository
public interface CustomerCustomRepositorySagas extends JpaRepository<SagaCustomer, Integer> {
}