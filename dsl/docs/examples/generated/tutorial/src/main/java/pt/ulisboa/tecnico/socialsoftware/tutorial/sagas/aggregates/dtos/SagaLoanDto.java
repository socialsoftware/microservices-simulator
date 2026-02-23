package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.Loan;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.Loan;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.aggregates.SagaLoan;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaLoanDto extends LoanDto {
private SagaState sagaState;

public SagaLoanDto(Loan loan) {
super((Loan) loan);
this.sagaState = ((SagaLoan)loan).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}