package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.order.aggregate.sagas.SagaOrder;

@Repository
public interface OrderCustomRepositorySagas extends JpaRepository<SagaOrder, Integer> {
}