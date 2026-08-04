package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.contacts.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.service.ContactsService;

import java.util.logging.Logger;

@Component
public class ContactsCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(ContactsCommandHandler.class.getName());

    @Autowired
    private ContactsService contactsService;

    @Override
    protected String getAggregateTypeName() {
        return "Contacts";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateContactsCommand cmd -> handleCreateContacts(cmd);
            case GetContactsByIdCommand cmd -> handleGetContactsById(cmd);
            case GetAllContactssCommand cmd -> handleGetAllContactss(cmd);
            case UpdateContactsCommand cmd -> handleUpdateContacts(cmd);
            case DeleteContactsCommand cmd -> handleDeleteContacts(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateContacts(CreateContactsCommand cmd) {
        logger.info("handleCreateContacts");
        try {
            return contactsService.createContacts(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetContactsById(GetContactsByIdCommand cmd) {
        logger.info("handleGetContactsById");
        try {
            return contactsService.getContactsById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllContactss(GetAllContactssCommand cmd) {
        logger.info("handleGetAllContactss");
        try {
            return contactsService.getAllContactss(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateContacts(UpdateContactsCommand cmd) {
        logger.info("handleUpdateContacts");
        try {
            return contactsService.updateContacts(cmd.getContactsDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteContacts(DeleteContactsCommand cmd) {
        logger.info("handleDeleteContacts");
        try {
            contactsService.deleteContacts(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
