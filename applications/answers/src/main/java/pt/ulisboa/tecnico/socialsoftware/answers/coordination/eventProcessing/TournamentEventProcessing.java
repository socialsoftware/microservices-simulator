package pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionUserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.events.publish.TopicUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.events.publish.ExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.events.publish.QuizDeletedEvent;

@Service
public class TournamentEventProcessing {
    @Autowired
    private TournamentService tournamentService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public TournamentEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processExecutionUpdatedEvent(Integer aggregateId, ExecutionUpdatedEvent executionUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.handleExecutionUpdatedEvent(aggregateId, executionUpdatedEvent.getPublisherAggregateId(), executionUpdatedEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processExecutionUserUpdatedEvent(Integer aggregateId, ExecutionUserUpdatedEvent executionUserUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.handleExecutionUserUpdatedEvent(aggregateId, executionUserUpdatedEvent.getPublisherAggregateId(), executionUserUpdatedEvent.getPublisherAggregateVersion(), executionUserUpdatedEvent.getUserName(), executionUserUpdatedEvent.getUserUsername(), executionUserUpdatedEvent.getUserActive(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processTopicUpdatedEvent(Integer aggregateId, TopicUpdatedEvent topicUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.handleTopicUpdatedEvent(aggregateId, topicUpdatedEvent.getPublisherAggregateId(), topicUpdatedEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processQuizUpdatedEvent(Integer aggregateId, QuizUpdatedEvent quizUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.handleQuizUpdatedEvent(aggregateId, quizUpdatedEvent.getPublisherAggregateId(), quizUpdatedEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processExecutionDeletedEvent(Integer aggregateId, ExecutionDeletedEvent executionDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }

    public void processQuizDeletedEvent(Integer aggregateId, QuizDeletedEvent quizDeletedEvent) {
        // Reference constraint event processing - implement constraint logic
    }
}