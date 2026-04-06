package pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.typesenums.command.contact.*;
import pt.ulisboa.tecnico.socialsoftware.typesenums.microservices.contact.service.ContactService;

import java.util.logging.Logger;

@Component
public class ContactCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(ContactCommandHandler.class.getName());

    @Autowired
    private ContactService contactService;

    @Override
    protected String getAggregateTypeName() {
        return "Contact";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateContactCommand cmd -> handleCreateContact(cmd);
            case GetContactByIdCommand cmd -> handleGetContactById(cmd);
            case GetAllContactsCommand cmd -> handleGetAllContacts(cmd);
            case UpdateContactCommand cmd -> handleUpdateContact(cmd);
            case DeleteContactCommand cmd -> handleDeleteContact(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateContact(CreateContactCommand cmd) {
        logger.info("handleCreateContact");
        try {
            return contactService.createContact(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetContactById(GetContactByIdCommand cmd) {
        logger.info("handleGetContactById");
        try {
            return contactService.getContactById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllContacts(GetAllContactsCommand cmd) {
        logger.info("handleGetAllContacts");
        try {
            return contactService.getAllContacts(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateContact(UpdateContactCommand cmd) {
        logger.info("handleUpdateContact");
        try {
            return contactService.updateContact(cmd.getContactDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteContact(DeleteContactCommand cmd) {
        logger.info("handleDeleteContact");
        try {
            contactService.deleteContact(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
