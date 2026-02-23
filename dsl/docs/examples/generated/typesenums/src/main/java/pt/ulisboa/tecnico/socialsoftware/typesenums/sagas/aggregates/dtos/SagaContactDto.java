package pt.ulisboa.tecnico.socialsoftware.typesenums.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.Contact;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.Contact;
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos.ContactDto;
import pt.ulisboa.tecnico.socialsoftware.typesenums.sagas.aggregates.SagaContact;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaContactDto extends ContactDto {
private SagaState sagaState;

public SagaContactDto(Contact contact) {
super((Contact) contact);
this.sagaState = ((SagaContact)contact).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}