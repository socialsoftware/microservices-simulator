package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public class GetContactsByIdCommand extends Command {
    private Integer contactsAggregateId;

    public GetContactsByIdCommand(UnitOfWork unitOfWork, String serviceName, Integer contactsAggregateId) {
        super(unitOfWork, serviceName, contactsAggregateId);
        this.contactsAggregateId = contactsAggregateId;
    }

    public Integer getContactsAggregateId() {
        return contactsAggregateId;
    }
}
