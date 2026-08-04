package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.aggregate.sagas.SagaPayment;

@Repository
public interface PaymentCustomRepositorySagas extends JpaRepository<SagaPayment, Integer> {
}