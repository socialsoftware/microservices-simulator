package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.AbortCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.CommitCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.PrepareCausalCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.command.GetConcurrentAggregateCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command.AbortSagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command.CommitSagaCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.courseExecution.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;

import java.util.logging.Logger;

@Component
public class CourseExecutionCommandHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(CourseExecutionCommandHandler.class.getName());

    @Autowired
    private CourseExecutionService courseExecutionService;

    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;

    @Override
    public Object handle(Command command) {
        if (command.getForbiddenStates() != null && !command.getForbiddenStates().isEmpty()) {
            logger.info("VERIFYING SAGA STATE");
            sagaUnitOfWorkService.verifySagaState(command.getRootAggregateId(), command.getForbiddenStates());
        }
        Object returnObject;
        switch (command) {
            case CreateCourseExecutionCommand createCourseExecutionCommand ->
                returnObject = handleCreateCourseExecution(createCourseExecutionCommand);
            case RemoveCourseExecutionCommand removeCourseExecutionCommand ->
                returnObject = handleRemoveCourseExecution(removeCourseExecutionCommand);
            case RemoveStudentFromCourseExecutionCommand removeStudentFromCourseExecutionCommand ->
                returnObject = handleRemoveStudentFromCourseExecution(removeStudentFromCourseExecutionCommand);
            case UpdateExecutionStudentNameCommand updateExecutionStudentNameCommand ->
                returnObject = handleUpdateExecutionStudentName(updateExecutionStudentNameCommand);
            case GetStudentByExecutionIdAndUserIdCommand getStudentByExecutionIdAndUserIdCommand ->
                returnObject = handleGetStudentByExecutionIdAndUserId(getStudentByExecutionIdAndUserIdCommand);
            case GetCourseExecutionsByUserIdCommand getCourseExecutionsByUserIdCommand ->
                returnObject = handleGetCourseExecutionsByUserId(getCourseExecutionsByUserIdCommand);
            case GetCourseExecutionByIdCommand getCourseExecutionByIdCommand ->
                returnObject = handleGetCourseExecutionById(getCourseExecutionByIdCommand);
            case GetAllCourseExecutionsCommand getAllCourseExecutionsCommand ->
                returnObject = handleGetAllCourseExecutions(getAllCourseExecutionsCommand);
            case EnrollStudentCommand enrollStudentCommand -> returnObject = handleEnrollStudent(enrollStudentCommand);
            case AnonymizeStudentCommand anonymizeStudentCommand ->
                returnObject = handleAnonymizeStudent(anonymizeStudentCommand);
            case RemoveUserCommand removeUserCommand -> returnObject = handleRemoveUser(removeUserCommand);
            case CommitSagaCommand commitSagaCommand -> returnObject = handleCommitSaga(commitSagaCommand);
            case AbortSagaCommand abortSagaCommand -> returnObject = handleAbortSaga(abortSagaCommand);
            case CommitCausalCommand commitCausalCommand -> returnObject = handleCommitCausal(commitCausalCommand);
            case PrepareCausalCommand prepareCausalCommand -> returnObject = handlePrepareCausal(prepareCausalCommand);
            case AbortCausalCommand abortCausalCommand -> returnObject = handleAbortCausal(abortCausalCommand);
            case GetConcurrentAggregateCommand getConcurrentAggregateCommand -> returnObject = handleGetConcurrentAggregate(getConcurrentAggregateCommand);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                returnObject = null;
            }
        }
        if (command.getSemanticLock() != null) {
            Logger.getLogger(CourseExecutionCommandHandler.class.getName())
                    .info("Registering saga state: " + command.getSemanticLock());
            sagaUnitOfWorkService.registerSagaState(command.getRootAggregateId(), command.getSemanticLock(),
                    (SagaUnitOfWork) command.getUnitOfWork());
        }

        return returnObject;
    }

    private Object handleCreateCourseExecution(CreateCourseExecutionCommand command) {
        logger.info("Creating course execution: " + command.getCourseExecutionDto());
        try {
            CourseExecutionDto courseExecutionDto = courseExecutionService.createCourseExecution(
                    command.getCourseExecutionDto(),
                    command.getUnitOfWork());
            return courseExecutionDto;
        } catch (Exception e) {
            logger.severe("Failed to create course execution: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveCourseExecution(RemoveCourseExecutionCommand command) {
        logger.info("Removing course execution: " + command.getExecutionAggregateId());
        try {
            courseExecutionService.removeCourseExecution(
                    command.getExecutionAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to remove course execution: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveStudentFromCourseExecution(RemoveStudentFromCourseExecutionCommand command) {
        logger.info("Removing student from course execution: " + command.getCourseExecutionAggregateId() + ", "
                + command.getUserAggregateId());
        try {
            courseExecutionService.removeStudentFromCourseExecution(
                    command.getCourseExecutionAggregateId(),
                    command.getUserAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to remove student from course execution: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateExecutionStudentName(UpdateExecutionStudentNameCommand command) {
        logger.info("Updating execution student name: " + command.getExecutionAggregateId() + ", "
                + command.getUserAggregateId() + ", "
                + command.getName());
        try {
            courseExecutionService.updateExecutionStudentName(
                    command.getExecutionAggregateId(),
                    command.getUserAggregateId(),
                    command.getName(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to update execution student name: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetStudentByExecutionIdAndUserId(GetStudentByExecutionIdAndUserIdCommand command) {
        logger.info("Getting student by execution id and user id: " + command.getExecutionAggregateId() + ", "
                + command.getUserAggregateId());
        try {
            return courseExecutionService.getStudentByExecutionIdAndUserId(
                    command.getExecutionAggregateId(),
                    command.getUserAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get student by execution id and user id: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetCourseExecutionsByUserId(GetCourseExecutionsByUserIdCommand command) {
        logger.info("Getting course executions by user id: " + command.getUserAggregateId());
        try {
            return courseExecutionService.getCourseExecutionsByUserId(
                    command.getUserAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get course executions by user id: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetCourseExecutionById(GetCourseExecutionByIdCommand command) {
        logger.info("Getting course execution by id: " + command.getExecutionAggregateId());
        try {
            return courseExecutionService.getCourseExecutionById(
                    command.getExecutionAggregateId(),
                    command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get course execution by id: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllCourseExecutions(GetAllCourseExecutionsCommand command) {
        logger.info("Getting all course executions");
        try {
            return courseExecutionService.getAllCourseExecutions(command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get all course executions: " + e.getMessage());
            return e;
        }
    }

    private Object handleEnrollStudent(EnrollStudentCommand command) {
        logger.info("Enrolling student: " + command.getUserDto().getAggregateId() + " in course execution: "
                + command.getCourseExecutionAggregateId());
        try {
            courseExecutionService.enrollStudent(
                    command.getCourseExecutionAggregateId(),
                    command.getUserDto(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to enroll student: " + e.getMessage());
            return e;
        }
    }

    private Object handleAnonymizeStudent(AnonymizeStudentCommand command) {
        logger.info(
                "Anonymizing student: " + command.getUserAggregateId() + " in course execution: "
                        + command.getExecutionAggregateId());
        try {
            courseExecutionService.anonymizeStudent(
                    command.getExecutionAggregateId(),
                    command.getUserAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to anonymize student: " + e.getMessage());
            return e;
        }
    }

    private Object handleRemoveUser(RemoveUserCommand command) {
        logger.info(
                "Removing user: " + command.getUserAggregateId() + " in course execution: "
                        + command.getCourseExecutionAggregateId());
        try {
            courseExecutionService.removeUser(
                    command.getCourseExecutionAggregateId(),
                    command.getUserAggregateId(),
                    command.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to anonymize student: " + e.getMessage());
            return e;
        }
    }

    private Object handleCommitSaga(CommitSagaCommand command) {
        logger.info("Committing saga for aggregate: " + command.getAggregateId());
        try {
            sagaUnitOfWorkService.commitAggregate(command.getAggregateId());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to commit saga: " + e.getMessage());
            return e;
        }
    }

    private Object handleAbortSaga(AbortSagaCommand command) {
        logger.info("Aborting saga for aggregate: " + command.getAggregateId());
        try {
            sagaUnitOfWorkService.abortAggregate(command.getAggregateId(), command.getPreviousState());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to abort saga: " + e.getMessage());
            return e;
        }
    }

    private Object handleCommitCausal(CommitCausalCommand command) {
        logger.info("Committing causal for aggregate: " + command.getRootAggregateId());
        try {
            causalUnitOfWorkService.commitCausal(command.getAggregate());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to commit causal: " + e.getMessage());
            return e;
        }
    }

    private Object handlePrepareCausal(PrepareCausalCommand command) {
        logger.info("Preparing causal for aggregate: " + command.getRootAggregateId());
        try {
            causalUnitOfWorkService.prepareCausal(command.getAggregate());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to prepare causal: " + e.getMessage());
            return e;
        }
    }

    private Object handleAbortCausal(AbortCausalCommand command) {
        logger.info("Aborting causal for aggregate: " + command.getRootAggregateId());
        try {
            causalUnitOfWorkService.abortCausal(command.getRootAggregateId());
            return null;
        } catch (Exception e) {
            logger.severe("Failed to abort causal: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetConcurrentAggregate(GetConcurrentAggregateCommand command) {
        return causalUnitOfWorkService.getConcurrentAggregate(command.getRootAggregateId(), command.getVersion(), "CourseExecution");
    }
}
