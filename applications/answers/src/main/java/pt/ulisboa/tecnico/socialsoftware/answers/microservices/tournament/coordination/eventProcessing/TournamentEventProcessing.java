package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionUserUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TopicUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.QuizUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.ExecutionDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.QuizDeletedEvent;

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
        tournamentService.handleExecutionUpdatedEvent(aggregateId, executionUpdatedEvent.getPublisherAggregateId(), executionUpdatedEvent.getPublisherAggregateVersion(), executionUpdatedEvent.getAcronym(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processExecutionUserUpdatedEvent(Integer aggregateId, ExecutionUserUpdatedEvent executionUserUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.handleExecutionUserUpdatedEvent(aggregateId, executionUserUpdatedEvent.getPublisherAggregateId(), executionUserUpdatedEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processTopicUpdatedEvent(Integer aggregateId, TopicUpdatedEvent topicUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.handleTopicUpdatedEvent(aggregateId, topicUpdatedEvent.getPublisherAggregateId(), topicUpdatedEvent.getPublisherAggregateVersion(), topicUpdatedEvent.getName(), unitOfWork);
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