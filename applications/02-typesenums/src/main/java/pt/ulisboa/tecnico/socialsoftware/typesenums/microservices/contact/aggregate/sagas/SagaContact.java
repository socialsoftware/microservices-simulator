package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.Contact;
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos.ContactDto;

@Entity
public class SagaContact extends Contact implements SagaAggregate {
    private SagaState sagaState;

    public SagaContact() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaContact(SagaContact other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaContact(Integer aggregateId, ContactDto contactDto) {
        super(aggregateId, contactDto);
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