package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.trainticket.command.payment.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.payment.service.PaymentService;

import java.util.logging.Logger;

@Component
public class PaymentCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(PaymentCommandHandler.class.getName());

    @Autowired
    private PaymentService paymentService;

    @Override
    public String getAggregateTypeName() {
        return "Payment";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreatePaymentCommand cmd -> handleCreatePayment(cmd);
            case GetPaymentByIdCommand cmd -> handleGetPaymentById(cmd);
            case GetAllPaymentsCommand cmd -> handleGetAllPayments(cmd);
            case UpdatePaymentCommand cmd -> handleUpdatePayment(cmd);
            case DeletePaymentCommand cmd -> handleDeletePayment(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreatePayment(CreatePaymentCommand cmd) {
        logger.info("handleCreatePayment");
        try {
            return paymentService.createPayment(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetPaymentById(GetPaymentByIdCommand cmd) {
        logger.info("handleGetPaymentById");
        try {
            return paymentService.getPaymentById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllPayments(GetAllPaymentsCommand cmd) {
        logger.info("handleGetAllPayments");
        try {
            return paymentService.getAllPayments(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdatePayment(UpdatePaymentCommand cmd) {
        logger.info("handleUpdatePayment");
        try {
            return paymentService.updatePayment(cmd.getPaymentDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeletePayment(DeletePaymentCommand cmd) {
        logger.info("handleDeletePayment");
        try {
            paymentService.deletePayment(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
