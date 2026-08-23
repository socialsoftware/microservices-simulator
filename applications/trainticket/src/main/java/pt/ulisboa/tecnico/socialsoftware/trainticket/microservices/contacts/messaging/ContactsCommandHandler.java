package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts.GetContactsByAccountCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts.GetContactsByIdCommand;
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
}
