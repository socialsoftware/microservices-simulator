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
import pt.ulisboa.tecnico.socialsoftware.answers.events.UserDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.TopicDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.events.QuizDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

@Service
public class TournamentEventProcessing {
    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private TournamentFactory tournamentFactory;

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
        tournamentService.handleExecutionUserUpdatedEvent(aggregateId, executionUserUpdatedEvent.getPublisherAggregateId(), executionUserUpdatedEvent.getPublisherAggregateVersion(), executionUserUpdatedEvent.getUserName(), executionUserUpdatedEvent.getUserUsername(), unitOfWork);
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
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        newTournament.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newTournament, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processUserDeletedEvent(Integer aggregateId, UserDeletedEvent userDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        newTournament.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newTournament, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processTopicDeletedEvent(Integer aggregateId, TopicDeletedEvent topicDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        newTournament.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newTournament, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void processQuizDeletedEvent(Integer aggregateId, QuizDeletedEvent quizDeletedEvent) {
        UnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        newTournament.setState(Aggregate.AggregateState.INACTIVE);
        unitOfWorkService.registerChanged(newTournament, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}