package pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionUserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.events.publish.QuestionUpdatedEvent;

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
}