package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.aggregate.Loan;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;

@Entity
public class SagaLoan extends Loan implements SagaAggregate {
    private SagaState sagaState;

    public SagaLoan() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaLoan(SagaLoan other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaLoan(Integer aggregateId, LoanDto loanDto) {
        super(aggregateId, loanDto);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = state;
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }
}