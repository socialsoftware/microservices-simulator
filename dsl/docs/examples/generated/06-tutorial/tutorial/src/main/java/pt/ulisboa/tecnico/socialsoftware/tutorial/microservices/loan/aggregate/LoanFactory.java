package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate;

import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;

public interface LoanFactory {
    Loan createLoan(Integer aggregateId, LoanDto loanDto);
    Loan createLoanFromExisting(Loan existingLoan);
    LoanDto createLoanDto(Loan loan);
}
