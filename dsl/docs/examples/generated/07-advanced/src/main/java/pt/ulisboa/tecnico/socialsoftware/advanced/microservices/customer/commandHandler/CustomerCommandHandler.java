package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.advanced.command.customer.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.service.CustomerService;

import java.util.logging.Logger;

@Component
public class CustomerCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(CustomerCommandHandler.class.getName());

    @Autowired
    private CustomerService customerService;

    @Override
    public String getAggregateTypeName() {
        return "Customer";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateCustomerCommand cmd -> handleCreateCustomer(cmd);
            case GetCustomerByIdCommand cmd -> handleGetCustomerById(cmd);
            case GetAllCustomersCommand cmd -> handleGetAllCustomers(cmd);
            case UpdateCustomerCommand cmd -> handleUpdateCustomer(cmd);
            case DeleteCustomerCommand cmd -> handleDeleteCustomer(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateCustomer(CreateCustomerCommand cmd) {
        logger.info("handleCreateCustomer");
        try {
            return customerService.createCustomer(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetCustomerById(GetCustomerByIdCommand cmd) {
        logger.info("handleGetCustomerById");
        try {
            return customerService.getCustomerById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllCustomers(GetAllCustomersCommand cmd) {
        logger.info("handleGetAllCustomers");
        try {
            return customerService.getAllCustomers(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateCustomer(UpdateCustomerCommand cmd) {
        logger.info("handleUpdateCustomer");
        try {
            return customerService.updateCustomer(cmd.getCustomerDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteCustomer(DeleteCustomerCommand cmd) {
        logger.info("handleDeleteCustomer");
        try {
            customerService.deleteCustomer(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
