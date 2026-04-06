package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.command.enrollment.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.enrollment.service.EnrollmentService;

import java.util.logging.Logger;

@Component
public class EnrollmentCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(EnrollmentCommandHandler.class.getName());

    @Autowired
    private EnrollmentService enrollmentService;

    @Override
    protected String getAggregateTypeName() {
        return "Enrollment";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateEnrollmentCommand cmd -> handleCreateEnrollment(cmd);
            case GetEnrollmentByIdCommand cmd -> handleGetEnrollmentById(cmd);
            case GetAllEnrollmentsCommand cmd -> handleGetAllEnrollments(cmd);
            case UpdateEnrollmentCommand cmd -> handleUpdateEnrollment(cmd);
            case DeleteEnrollmentCommand cmd -> handleDeleteEnrollment(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateEnrollment(CreateEnrollmentCommand cmd) {
        logger.info("handleCreateEnrollment");
        try {
            return enrollmentService.createEnrollment(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetEnrollmentById(GetEnrollmentByIdCommand cmd) {
        logger.info("handleGetEnrollmentById");
        try {
            return enrollmentService.getEnrollmentById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetAllEnrollments(GetAllEnrollmentsCommand cmd) {
        logger.info("handleGetAllEnrollments");
        try {
            return enrollmentService.getAllEnrollments(cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleUpdateEnrollment(UpdateEnrollmentCommand cmd) {
        logger.info("handleUpdateEnrollment");
        try {
            return enrollmentService.updateEnrollment(cmd.getEnrollmentDto(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleDeleteEnrollment(DeleteEnrollmentCommand cmd) {
        logger.info("handleDeleteEnrollment");
        try {
            enrollmentService.deleteEnrollment(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }
}
