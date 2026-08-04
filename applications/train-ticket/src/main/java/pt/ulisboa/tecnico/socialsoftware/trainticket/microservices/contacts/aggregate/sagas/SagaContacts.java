package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.sagas;

import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.Contacts;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.ContactsDto;

@Entity
public class SagaContacts extends Contacts implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaContacts() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaContacts(SagaContacts other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaContacts(Integer aggregateId, ContactsDto contactsDto) {
        super(aggregateId, contactsDto);
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