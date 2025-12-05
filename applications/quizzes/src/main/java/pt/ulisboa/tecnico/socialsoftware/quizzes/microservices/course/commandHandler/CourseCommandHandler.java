package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command.CommitSagaCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetAndOrCreateCourseRemoteCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.course.GetCourseByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;

import java.util.logging.Logger;

@Component
public class CourseCommandHandler implements CommandHandler {
    private static final Logger logger = Logger.getLogger(CourseCommandHandler.class.getName());

    @Autowired
    private CourseService courseService;

    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Override
    public Object handle(Command command) {
        if (command.getForbiddenStates() != null && !command.getForbiddenStates().isEmpty()) {
            sagaUnitOfWorkService.verifySagaState(command.getRootAggregateId(), command.getForbiddenStates());
        }
        Object returnObject;
        if (command instanceof GetCourseByIdCommand) {
            returnObject = handleGetCourseById((GetCourseByIdCommand) command);
        } else if (command instanceof GetAndOrCreateCourseRemoteCommand) {
            returnObject = handleGetAndOrCreateCourseRemote((GetAndOrCreateCourseRemoteCommand) command);
        } else if (command instanceof CommitSagaCommand) {
            returnObject = handleCommitSaga((CommitSagaCommand) command);
        } else {
            logger.warning("Unknown command type: " + command.getClass().getName());
            returnObject = null;
        }
        if (command.getSemanticLock() != null) {
            sagaUnitOfWorkService.registerSagaState(command.getRootAggregateId(), command.getSemanticLock(),
                    (SagaUnitOfWork) command.getUnitOfWork());
        }
        return returnObject;
    }

    private Object handleGetCourseById(GetCourseByIdCommand command) {
        logger.info("Getting course by ID: " + command.getAggregateId());
        try {
            CourseDto courseDto = courseService.getCourseById(
                    command.getAggregateId(),
                    command.getUnitOfWork());
            return courseDto;
        } catch (Exception e) {
            logger.severe("Failed to get course by ID: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAndOrCreateCourseRemote(GetAndOrCreateCourseRemoteCommand command) {
        logger.info("Getting or creating course by ID: " + command.getCourseExecutionDto().getAggregateId());
        try {
            return courseService.getAndOrCreateCourseRemote(command.getCourseExecutionDto(), command.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed to get or create course remote by ID: " + e.getMessage());
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
}
