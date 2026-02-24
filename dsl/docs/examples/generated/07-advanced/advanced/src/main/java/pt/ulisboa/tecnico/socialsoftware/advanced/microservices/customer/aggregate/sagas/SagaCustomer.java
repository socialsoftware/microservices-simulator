package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.aggregate.Customer;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;

@Entity
public class SagaCustomer extends Customer implements SagaAggregate {
    private SagaState sagaState;

    public SagaCustomer() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaCustomer(SagaCustomer other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaCustomer(Integer aggregateId, CustomerDto customerDto) {
        super(aggregateId, customerDto);
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