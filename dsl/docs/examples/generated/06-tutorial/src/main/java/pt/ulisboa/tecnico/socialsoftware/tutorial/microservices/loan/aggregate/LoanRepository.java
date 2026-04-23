package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface LoanRepository extends JpaRepository<Loan, Integer> {

}