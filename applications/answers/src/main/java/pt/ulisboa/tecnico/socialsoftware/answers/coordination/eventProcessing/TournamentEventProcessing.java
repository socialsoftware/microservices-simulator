package pt.ulisboa.tecnico.socialsoftware.answers.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.unknown.events.publish.UnknownEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.unknown.events.publish.UnknownEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.unknown.events.publish.UnknownEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.unknown.events.publish.UnknownEvent;

@Service
public class TournamentEventProcessing {
    @Autowired
    private TournamentService tournamentService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public TournamentEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }

    public void processExecutionDeletedEvent(Integer aggregateId, ExecutionDeletedEvent executionDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.executionDeleted(aggregateId, executionDeletedEvent.getPublisherAggregateId(), executionDeletedEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processExecutionUpdatedEvent(Integer aggregateId, ExecutionUpdatedEvent executionUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.executionUpdated(aggregateId, executionUpdatedEvent.getPublisherAggregateId(), executionUpdatedEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processTopicUpdatedEvent(Integer aggregateId, TopicUpdatedEvent topicUpdatedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.topicUpdated(aggregateId, topicUpdatedEvent.getPublisherAggregateId(), topicUpdatedEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processTopicDeletedEvent(Integer aggregateId, TopicDeletedEvent topicDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        tournamentService.topicDeleted(aggregateId, topicDeletedEvent.getPublisherAggregateId(), topicDeletedEvent.getPublisherAggregateVersion(), unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}