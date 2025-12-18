package pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork;

import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.IVersionService;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregateRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command.AbortSagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.command.CommitSagaCommand;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.*;

@Profile("sagas")
@Service
public class SagaUnitOfWorkService extends UnitOfWorkService<SagaUnitOfWork> {
    private static final Logger logger = LoggerFactory.getLogger(SagaUnitOfWorkService.class);

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private SagaAggregateRepository sagaAggregateRepository;
    @Autowired
    private IVersionService versionService;
    @Autowired
    private EventRepository eventRepository;
    @Autowired
    private CommandGateway commandGateway;
    @Autowired
    private Environment environment;

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public SagaUnitOfWork createUnitOfWork(String functionalityName) {
        Integer lastCommittedAggregateVersionNumber = versionService.getVersionNumber();

        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(lastCommittedAggregateVersionNumber + 1, functionalityName);
        return unitOfWork;
    }

    public Aggregate aggregateLoadAndRegisterRead(Integer aggregateId, SagaUnitOfWork unitOfWork) {
        Aggregate aggregate = sagaAggregateRepository.findNonDeletedSagaAggregate(aggregateId)
                .orElseThrow(() -> new SimulatorException(AGGREGATE_NOT_FOUND, aggregateId));

        logger.info("Loaded and registered read for aggregate ID: {} - {}", aggregateId, aggregate.getAggregateType());
        return aggregate;
    }

    public Aggregate aggregateLoad(Integer aggregateId, SagaUnitOfWork unitOfWork) {
        Aggregate aggregate = sagaAggregateRepository.findNonDeletedSagaAggregate(aggregateId)
                .orElseThrow(() -> new SimulatorException(AGGREGATE_NOT_FOUND, aggregateId));

        if (aggregate.getState() == Aggregate.AggregateState.DELETED) {
            throw new SimulatorException(AGGREGATE_DELETED, aggregate.getAggregateType(), aggregate.getAggregateId());
        }

        return aggregate;
    }

    // used for testing with spock
    public Aggregate aggregateDeletedLoad(Integer aggregateId) {
        Aggregate aggregate = sagaAggregateRepository.findDeletedSagaAggregate(aggregateId)
                .orElseThrow(() -> new SimulatorException(AGGREGATE_NOT_FOUND, aggregateId));

        return aggregate;
    }

    public Aggregate registerRead(Aggregate aggregate, SagaUnitOfWork unitOfWork) {
        return aggregate;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void registerSagaState(Integer aggregateId, SagaState state, SagaUnitOfWork unitOfWork) {
        Aggregate aggregate = sagaAggregateRepository.findNonDeletedSagaAggregate(aggregateId)
                .orElseThrow(() -> new SimulatorException(AGGREGATE_NOT_FOUND, aggregateId));

        SagaAggregate sagaAggregate = (SagaAggregate) aggregate;
        unitOfWork.savePreviousState(aggregateId, sagaAggregate.getSagaState());

        sagaAggregate.setSagaState(state);
        entityManager.merge(aggregate);
        unitOfWork.addToAggregatesInSaga(aggregateId, aggregate.getAggregateType());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void verifySagaState(Integer aggregateId, List<SagaState> forbiddenStates) {
        SagaAggregate aggregate = (SagaAggregate) sagaAggregateRepository.findNonDeletedSagaAggregate(aggregateId)
                .orElseThrow(() -> new SimulatorException(AGGREGATE_NOT_FOUND, aggregateId));

        if (forbiddenStates.contains(aggregate.getSagaState())) {
            throw new SimulatorException(AGGREGATE_BEING_USED_IN_OTHER_SAGA, aggregate.getSagaState().getStateName());
        }
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void commit(SagaUnitOfWork unitOfWork) {
        unitOfWork.getAggregatesInSaga().forEach((aggregateId, aggregateType) -> {
            String serviceName = this.resolveServiceName(aggregateType);
            CommitSagaCommand command = new CommitSagaCommand(aggregateId, serviceName);
            commandGateway.send(command);
        });
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void commitAggregate(Integer aggregateId) {
        SagaAggregate aggregate = (SagaAggregate) sagaAggregateRepository.findAnySagaAggregate(aggregateId)
                .orElseThrow(() -> new SimulatorException(AGGREGATE_NOT_FOUND, aggregateId));
        aggregate.setSagaState(GenericSagaState.NOT_IN_SAGA);
        entityManager.merge(aggregate);
    }

    public void commitAllObjects(Integer commitVersion, List<Aggregate> aggregates) {
        // aggregates are committed at the end of each service
    }

    public void compensate(SagaUnitOfWork unitOfWork) {
        unitOfWork.compensate();
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public void abort(SagaUnitOfWork unitOfWork) {
        for (Map.Entry<Integer, SagaState> entry : unitOfWork.getPreviousStates().entrySet()) {
            Integer aggregateId = entry.getKey();
            SagaState previousState = entry.getValue();

            String aggregateType = unitOfWork.getAggregatesInSaga().get(aggregateId);
            String serviceName = this.resolveServiceName(aggregateType);

            AbortSagaCommand command = new AbortSagaCommand(aggregateId, serviceName, previousState);
            commandGateway.send(command);
        }
        compensate(unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void abortAggregate(Integer aggregateId, SagaState previousState) {
        SagaAggregate aggregate = (SagaAggregate) sagaAggregateRepository.findNonDeletedSagaAggregate(aggregateId)
                .orElseThrow(() -> new SimulatorException(AGGREGATE_NOT_FOUND, aggregateId));
        logger.info("abort: aggregate {}", aggregate.getClass().getSimpleName());
        aggregate.setSagaState(previousState);
        entityManager.merge(aggregate);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public void registerChanged(Aggregate aggregate, SagaUnitOfWork unitOfWork) {
        if (aggregate.getPrev() != null && aggregate.getPrev().getState() == Aggregate.AggregateState.INACTIVE) {
            throw new SimulatorException(CANNOT_MODIFY_INACTIVE_AGGREGATE, aggregate.getAggregateId());
        }

        Integer commitVersion = versionService.incrementAndGetVersionNumber();

        aggregate.verifyInvariants();
        aggregate.setVersion(commitVersion);
        aggregate.setCreationTs(DateHandler.now());
        entityManager.merge(aggregate);

        unitOfWork.setVersion(commitVersion);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public void registerEvent(Event event, SagaUnitOfWork unitOfWork) {
        Integer commitVersion = versionService.incrementAndGetVersionNumber();
        event.setPublisherAggregateVersion(commitVersion);
        // If running with "local" profile, mark event as published immediately
        if (Arrays.asList(environment.getActiveProfiles()).contains("local")) {
            event.setPublished(true);
        }
        eventRepository.save(event);
        unitOfWork.setVersion(unitOfWork.getVersion() + 1);
    }
}
