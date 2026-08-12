package pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionUserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.QuestionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.UserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.QuizDeletedEvent;

@Service
public class AnswerEventProcessing {
    @Autowired
    private AnswerService answerService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public AnswerEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processExecutionUserUpdatedEvent(Integer aggregateId, ExecutionUserUpdatedEvent executionUserUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        answerService.handleExecutionUserUpdatedEvent(aggregateId, executionUserUpdatedEvent.getPublisherAggregateId(), executionUserUpdatedEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processQuestionUpdatedEvent(Integer aggregateId, QuestionUpdatedEvent questionUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        answerService.handleQuestionUpdatedEvent(aggregateId, questionUpdatedEvent.getPublisherAggregateId(), questionUpdatedEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processExecutionDeletedEvent(Integer aggregateId, ExecutionDeletedEvent executionDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }

    public void processUserDeletedEvent(Integer aggregateId, UserDeletedEvent userDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }

    public void processQuizDeletedEvent(Integer aggregateId, QuizDeletedEvent quizDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}