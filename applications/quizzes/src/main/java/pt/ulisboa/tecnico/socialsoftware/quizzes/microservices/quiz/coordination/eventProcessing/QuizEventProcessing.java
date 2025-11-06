package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.quizzes.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.RemoveCourseExecutionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.UpdateQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.command.quiz.RemoveQuizQuestionCommand;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.events.publish.DeleteCourseExecutionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.publish.DeleteQuestionEvent;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.events.publish.UpdateQuestionEvent;

@Service
public class QuizEventProcessing {
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;
    private final CommandGateway commandGateway;

    @Autowired
    public QuizEventProcessing(UnitOfWorkService unitOfWorkService, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
    }

    public void processDeleteCourseExecutionEvent(Integer aggregateId,
            DeleteCourseExecutionEvent deleteCourseExecutionEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        RemoveCourseExecutionCommand command = new RemoveCourseExecutionCommand(
                unitOfWork,
                ServiceMapping.QUIZ.getServiceName(),
                aggregateId,
                deleteCourseExecutionEvent.getPublisherAggregateId(),
                deleteCourseExecutionEvent.getPublisherAggregateVersion());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processUpdateQuestionEvent(Integer aggregateId, UpdateQuestionEvent updateQuestionEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        UpdateQuestionCommand command = new UpdateQuestionCommand(
                unitOfWork,
                ServiceMapping.QUIZ.getServiceName(),
                aggregateId,
                updateQuestionEvent.getPublisherAggregateId(),
                updateQuestionEvent.getTitle(),
                updateQuestionEvent.getContent(),
                updateQuestionEvent.getPublisherAggregateVersion());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processDeleteQuizQuestionEvent(Integer aggregateId, DeleteQuestionEvent deleteQuestionEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        RemoveQuizQuestionCommand command = new RemoveQuizQuestionCommand(
                unitOfWork,
                ServiceMapping.QUIZ.getServiceName(),
                aggregateId,
                deleteQuestionEvent.getPublisherAggregateId());
        commandGateway.send(command);
        unitOfWorkService.commit(unitOfWork);
    }
}
