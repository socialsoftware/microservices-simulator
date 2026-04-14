package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.command.teacher.*;
import pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.teacher.service.TeacherService;

import java.util.logging.Logger;

@Component
public class TeacherCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(TeacherCommandHandler.class.getName());

    @Autowired
    private TeacherService teacherService;

    @Override
    protected String getAggregateTypeName() {
        return "Teacher";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateTeacherCommand cmd -> handleCreateTeacher(cmd);
            case GetTeacherByIdCommand cmd -> handleGetTeacherById(cmd);
            case GetAllTeachersCommand cmd -> handleGetAllTeachers(cmd);
            case UpdateTeacherCommand cmd -> handleUpdateTeacher(cmd);
            case DeleteTeacherCommand cmd -> handleDeleteTeacher(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateTeacher(CreateTeacherCommand cmd) {
        logger.info("handleCreateTeacher");
        try {
            return teacherService.createTeacher(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetTeacherById(GetTeacherByIdCommand cmd) {
        logger.info("handleGetTeacherById");
        try {
            return teacherService.getTeacherById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleGetAllTeachers(GetAllTeachersCommand cmd) {
        logger.info("handleGetAllTeachers");
        try {
            return teacherService.getAllTeachers(cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleUpdateTeacher(UpdateTeacherCommand cmd) {
        logger.info("handleUpdateTeacher");
        try {
            return teacherService.updateTeacher(cmd.getTeacherDto(), cmd.getUnitOfWork());
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }

    private Object handleDeleteTeacher(DeleteTeacherCommand cmd) {
        logger.info("handleDeleteTeacher");
        try {
            teacherService.deleteTeacher(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (RuntimeException e) {
            logger.severe("Failed: " + e.getMessage());
            throw e;
        }
    }
}
