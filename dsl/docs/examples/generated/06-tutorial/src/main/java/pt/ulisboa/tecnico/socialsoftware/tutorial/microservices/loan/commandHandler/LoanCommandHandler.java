package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.tutorial.command.loan.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.service.LoanService;

import java.util.logging.Logger;

@Component
public class LoanCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(LoanCommandHandler.class.getName());

    @Autowired
    private LoanService loanService;

    @Override
    public String getAggregateTypeName() {
        return "Loan";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateLoanCommand cmd -> handleCreateLoan(cmd);
            case GetLoanByIdCommand cmd -> handleGetLoanById(cmd);
            case GetAllLoansCommand cmd -> handleGetAllLoans(cmd);
            case UpdateLoanCommand cmd -> handleUpdateLoan(cmd);
            case DeleteLoanCommand cmd -> handleDeleteLoan(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateLoan(CreateLoanCommand cmd) {
        logger.info("handleCreateLoan");
        try {
            return loanService.createLoan(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetLoanById(GetLoanByIdCommand cmd) {
        logger.info("handleGetLoanById");
        try {
            return loanService.getLoanById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllLoans(GetAllLoansCommand cmd) {
        logger.info("handleGetAllLoans");
        try {
            return loanService.getAllLoans(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateLoan(UpdateLoanCommand cmd) {
        logger.info("handleUpdateLoan");
        try {
            return loanService.updateLoan(cmd.getLoanDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteLoan(DeleteLoanCommand cmd) {
        logger.info("handleDeleteLoan");
        try {
            loanService.deleteLoan(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
