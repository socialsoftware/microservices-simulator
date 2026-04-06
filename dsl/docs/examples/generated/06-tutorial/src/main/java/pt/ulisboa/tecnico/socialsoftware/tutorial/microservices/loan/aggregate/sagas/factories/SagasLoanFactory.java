package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.Loan;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.LoanFactory;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.sagas.SagaLoan;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.sagas.dtos.SagaLoanDto;

@Service
@Profile("sagas")
public class SagasLoanFactory implements LoanFactory {
    @Override
    public Loan createLoan(Integer aggregateId, LoanDto loanDto) {
        return new SagaLoan(aggregateId, loanDto);
    }

    @Override
    public Loan createLoanFromExisting(Loan existingLoan) {
        return new SagaLoan((SagaLoan) existingLoan);
    }

    @Override
    public LoanDto createLoanDto(Loan loan) {
        return new SagaLoanDto(loan);
    }
}