package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.invoice.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.service.InvoiceService;

import java.util.logging.Logger;

@Component
public class InvoiceCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(InvoiceCommandHandler.class.getName());

    @Autowired
    private InvoiceService invoiceService;

    @Override
    public String getAggregateTypeName() {
        return "Invoice";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateInvoiceCommand cmd -> handleCreateInvoice(cmd);
            case GetInvoiceByIdCommand cmd -> handleGetInvoiceById(cmd);
            case GetAllInvoicesCommand cmd -> handleGetAllInvoices(cmd);
            case UpdateInvoiceCommand cmd -> handleUpdateInvoice(cmd);
            case DeleteInvoiceCommand cmd -> handleDeleteInvoice(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateInvoice(CreateInvoiceCommand cmd) {
        logger.info("handleCreateInvoice");
        try {
            return invoiceService.createInvoice(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetInvoiceById(GetInvoiceByIdCommand cmd) {
        logger.info("handleGetInvoiceById");
        try {
            return invoiceService.getInvoiceById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllInvoices(GetAllInvoicesCommand cmd) {
        logger.info("handleGetAllInvoices");
        try {
            return invoiceService.getAllInvoices(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateInvoice(UpdateInvoiceCommand cmd) {
        logger.info("handleUpdateInvoice");
        try {
            return invoiceService.updateInvoice(cmd.getInvoiceDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteInvoice(DeleteInvoiceCommand cmd) {
        logger.info("handleDeleteInvoice");
        try {
            invoiceService.deleteInvoice(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
