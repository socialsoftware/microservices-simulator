package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.RemoveQuestionFromQuizAnswerCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.RemoveUserFromQuizAnswerCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.answer.UpdateUserNameCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DisenrollStudentFromCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.UpdateStudentNameEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.publish.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.events.publish.DeleteUserEvent;

@Service
public class QuizAnswerEventProcessing {
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;
    private final CommandGateway commandGateway;

    @Autowired
    public QuizAnswerEventProcessing(UnitOfWorkService unitOfWorkService, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
    }

    public void processDeleteUserEvent(Integer aggregateId, DeleteUserEvent deleteUserEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        RemoveUserFromQuizAnswerCommand command = new RemoveUserFromQuizAnswerCommand(
                unitOfWork,
                ServiceMapping.ANSWER.getServiceName(),
                aggregateId,
                deleteUserEvent.getPublisherAggregateId(),
                deleteUserEvent.getPublisherAggregateVersion());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processDeleteQuestionEvent(Integer aggregateId, DeleteQuestionEvent deleteQuestionEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        RemoveQuestionFromQuizAnswerCommand command = new RemoveQuestionFromQuizAnswerCommand(
                unitOfWork,
                ServiceMapping.ANSWER.getServiceName(),
                aggregateId,
                deleteQuestionEvent.getPublisherAggregateId(),
                deleteQuestionEvent.getPublisherAggregateVersion());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processDisenrollStudentEvent(Integer aggregateId,
            DisenrollStudentFromCourseExecutionEvent disenrollStudentFromCourseExecutionEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        RemoveUserFromQuizAnswerCommand command = new RemoveUserFromQuizAnswerCommand(
                unitOfWork,
                ServiceMapping.ANSWER.getServiceName(),
                aggregateId,
                disenrollStudentFromCourseExecutionEvent.getPublisherAggregateId(),
                disenrollStudentFromCourseExecutionEvent.getPublisherAggregateVersion());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processUpdateStudentNameEvent(Integer subscriberAggregateId,
            UpdateStudentNameEvent updateStudentNameEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        UpdateUserNameCommand command = new UpdateUserNameCommand(
                unitOfWork,
                ServiceMapping.ANSWER.getServiceName(),
                subscriberAggregateId,
                updateStudentNameEvent.getPublisherAggregateId(),
                updateStudentNameEvent.getPublisherAggregateVersion(),
                updateStudentNameEvent.getStudentAggregateId(),
                updateStudentNameEvent.getUpdatedName());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }
}
