package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.sagas.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.sagas.SagaLoan;

@Repository
public interface LoanCustomRepositorySagas extends JpaRepository<SagaLoan, Integer> {
}