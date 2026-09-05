package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts.CreateContactsCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts.DeleteContactsCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts.GetContactsByAccountCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts.GetContactsByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts.UpdateContactsCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.service.ContactsService;

import java.util.logging.Logger;

@Component
public class ContactsCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(ContactsCommandHandler.class.getName());

    @Autowired
    private ContactsService contactsService;

    @Override
    public String getAggregateTypeName() {
        return "Contacts";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetContactsByIdCommand cmd -> handleGetContactsById(cmd);
            case GetContactsByAccountCommand cmd -> handleGetContactsByAccount(cmd);
            case CreateContactsCommand cmd -> handleCreateContacts(cmd);
            case UpdateContactsCommand cmd -> handleUpdateContacts(cmd);
            case DeleteContactsCommand cmd -> handleDeleteContacts(cmd);
            default -> {
                logger.warning("Unknown command: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleGetContactsById(GetContactsByIdCommand command) {
        return contactsService.getContactsById(command.getContactsAggregateId(), command.getUnitOfWork());
    }

    private Object handleGetContactsByAccount(GetContactsByAccountCommand command) {
        return contactsService.getContactsByAccount(command.getUserAggregateId(), command.getUnitOfWork());
    }

    private Object handleCreateContacts(CreateContactsCommand command) {
        return contactsService.createContacts(command.getContactsDto(), command.getUnitOfWork());
    }

    private Object handleUpdateContacts(UpdateContactsCommand command) {
        contactsService.updateContacts(command.getContactsAggregateId(), command.getContactsDto(),
                command.getUnitOfWork());
        return null;
    }

    private Object handleDeleteContacts(DeleteContactsCommand command) {
        contactsService.deleteContacts(command.getContactsAggregateId(), command.getUnitOfWork());
        return null;
    }
}
