package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.sagas.dtos;

import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.Contact;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.Contact;
import pt.ulisboa.tecnico.socialsoftware.typesenums.shared.dtos.ContactDto;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.aggregate.sagas.SagaContact;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

public class SagaContactDto extends ContactDto {
@Convert(converter = SagaStateConverter.class)
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