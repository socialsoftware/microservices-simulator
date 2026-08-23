package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto;

public class UpdateContactsCommand extends Command {
    private Integer contactsAggregateId;
    private ContactsDto contactsDto;

    public UpdateContactsCommand(UnitOfWork unitOfWork, String serviceName,
                                 Integer contactsAggregateId, ContactsDto contactsDto) {
        super(unitOfWork, serviceName, contactsAggregateId);
        this.contactsAggregateId = contactsAggregateId;
        this.contactsDto = contactsDto;
    }

    public Integer getContactsAggregateId() {
        return contactsAggregateId;
    }

    public ContactsDto getContactsDto() {
        return contactsDto;
    }
}
