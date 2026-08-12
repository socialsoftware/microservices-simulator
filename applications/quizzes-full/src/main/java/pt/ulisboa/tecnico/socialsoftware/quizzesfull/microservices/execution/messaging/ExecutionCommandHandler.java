package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.messaging;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.AnonymizeStudentInExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.CreateExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.DeleteExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.DisenrollStudentFromExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.EnrollStudentInExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.GetExecutionByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.GetStudentByExecutionIdAndUserIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.UpdateExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.commands.execution.UpdateStudentNameInExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.service.ExecutionService;

import java.util.logging.Logger;

@Component
public class ExecutionCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(ExecutionCommandHandler.class.getName());

    @Autowired
    private ExecutionService executionService;

    @Override
    public String getAggregateTypeName() {
        return "Execution";
    }

    @Override
    public Object handleDomainCommand(Command command) {
        return switch (command) {
            case GetExecutionByIdCommand cmd -> executionService.getExecutionById(
                    cmd.getExecutionAggregateId(), cmd.getUnitOfWork());
            case GetStudentByExecutionIdAndUserIdCommand cmd -> executionService.getStudentByExecutionIdAndUserId(
                    cmd.getExecutionAggregateId(), cmd.getUserId(), cmd.getUnitOfWork());
            case CreateExecutionCommand cmd -> executionService.createExecution(
                    cmd.getAcronym(), cmd.getAcademicTerm(), cmd.getExecutionCourse(), cmd.getUnitOfWork());
            case UpdateExecutionCommand cmd -> {
                executionService.updateExecution(
                        cmd.getExecutionAggregateId(), cmd.getAcronym(), cmd.getAcademicTerm(), cmd.getUnitOfWork());
                yield null;
            }
            case DeleteExecutionCommand cmd -> {
                executionService.deleteExecution(cmd.getExecutionAggregateId(), cmd.getUnitOfWork());
                yield null;
            }
            case EnrollStudentInExecutionCommand cmd -> {
                executionService.enrollStudentInExecution(
                        cmd.getExecutionAggregateId(), cmd.getUserDto(), cmd.getUnitOfWork());
                yield null;
            }
            case DisenrollStudentFromExecutionCommand cmd -> {
                executionService.disenrollStudent(
                        cmd.getExecutionAggregateId(), cmd.getUserId(), cmd.getUnitOfWork());
                yield null;
            }
            case UpdateStudentNameInExecutionCommand cmd -> {
                executionService.updateStudentNameInExecution(
                        cmd.getExecutionAggregateId(), cmd.getUserId(), cmd.getName(), cmd.getUnitOfWork());
                yield null;
            }
            case AnonymizeStudentInExecutionCommand cmd -> {
                executionService.anonymizeStudentInExecution(
                        cmd.getExecutionAggregateId(), cmd.getUserId(), cmd.getUnitOfWork());
                yield null;
            }
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }
}
