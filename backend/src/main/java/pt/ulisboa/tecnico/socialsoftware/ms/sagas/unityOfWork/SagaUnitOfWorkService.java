package pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.AGGREGATE_DELETED;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.AGGREGATE_NOT_FOUND;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.VersionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregateRepository;

@Profile("sagas")
@Service
public class SagaUnitOfWorkService extends UnitOfWorkService<SagaUnitOfWork> {
    private static final Logger logger = LoggerFactory.getLogger(SagaUnitOfWorkService.class);

    @Autowired
    private EntityManager entityManager;
    @Autowired
    private SagaAggregateRepository sagaAggregateRepository;
    @Autowired
    private VersionService versionService;
    @Autowired
    private EventRepository eventRepository;

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    public SagaUnitOfWork createUnitOfWork(String functionalityName) {
        Integer lastCommittedAggregateVersionNumber = versionService.getVersionNumber();

        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(lastCommittedAggregateVersionNumber+1, functionalityName);

        logger.info("START EXECUTION FUNCTIONALITY: {} with version {}", functionalityName, unitOfWork.getVersion());

        return unitOfWork;
    }

    public Aggregate aggregateLoadAndRegisterRead(Integer aggregateId, SagaUnitOfWork unitOfWork) {
        Aggregate aggregate = sagaAggregateRepository.findSagaAggregate(aggregateId, unitOfWork.getVersion())
                .orElseThrow(() -> new TutorException(AGGREGATE_NOT_FOUND));

        logger.info("Loaded and registered read for aggregate ID: {}", aggregateId);
        return aggregate;
    }

    public Aggregate aggregateLoad(Integer aggregateId, SagaUnitOfWork unitOfWork) {
        Aggregate aggregate = sagaAggregateRepository.findSagaAggregate(aggregateId, unitOfWork.getVersion())
                .orElseThrow(() -> new TutorException(AGGREGATE_NOT_FOUND, aggregateId));

        if (aggregate.getState() == Aggregate.AggregateState.DELETED) {
            throw new TutorException(AGGREGATE_DELETED, aggregate.getAggregateType().toString(), aggregate.getAggregateId());
        }

        return aggregate;
    }

    public Aggregate registerRead(Aggregate aggregate, SagaUnitOfWork unitOfWork) {
        return aggregate;
    }

    public void registerSagaState(SagaAggregate aggregate, SagaState state, SagaUnitOfWork unitOfWork) {
        aggregate.setSagaState(state);
        entityManager.persist(aggregate);
        unitOfWork.addToAggregatesInSaga(aggregate);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void registerSagaState(Integer aggregateId, SagaState state, SagaUnitOfWork unitOfWork) {
        SagaAggregate aggregate = (SagaAggregate) sagaAggregateRepository.findSagaAggregate(aggregateId, unitOfWork.getVersion())
                .orElseThrow(() -> new TutorException(AGGREGATE_NOT_FOUND));
        while (!aggregate.getSagaState().equals(GenericSagaState.NOT_IN_SAGA) && !state.equals(GenericSagaState.NOT_IN_SAGA)) {
            aggregate = (SagaAggregate) sagaAggregateRepository.findSagaAggregate(aggregateId, unitOfWork.getVersion())
                .orElseThrow(() -> new TutorException(AGGREGATE_NOT_FOUND));
        }
        aggregate.setSagaState(state);
        entityManager.persist(aggregate);
        unitOfWork.addToAggregatesInSaga(aggregate);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void registerSagaState(Integer aggregateId, SagaState state, ArrayList<SagaState> allowedStates, SagaUnitOfWork unitOfWork) {
        SagaAggregate aggregate = (SagaAggregate) sagaAggregateRepository.findSagaAggregate(aggregateId, unitOfWork.getVersion())
                .orElseThrow(() -> new TutorException(AGGREGATE_NOT_FOUND));
        while (!aggregate.getSagaState().equals(GenericSagaState.NOT_IN_SAGA)  && !state.equals(GenericSagaState.NOT_IN_SAGA) && !allowedStates.contains(aggregate.getSagaState())) {
            aggregate = (SagaAggregate) sagaAggregateRepository.findSagaAggregate(aggregateId, unitOfWork.getVersion())
                .orElseThrow(() -> new TutorException(AGGREGATE_NOT_FOUND));
        }
        aggregate.setSagaState(state);
        entityManager.persist(aggregate);
        unitOfWork.addToAggregatesInSaga(aggregate);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void commit(SagaUnitOfWork unitOfWork) {
        Map<Integer, Aggregate> aggregatesToCommit = new HashMap<>(unitOfWork.getAggregatesToCommit());

        unitOfWork.getAggregatesInSaga().stream().forEach(a -> {
            a.setSagaState(GenericSagaState.NOT_IN_SAGA);
            entityManager.persist(a);
        });

        // The commit is done with the last commited version plus one
        Integer commitVersion = versionService.incrementAndGetVersionNumber();
        unitOfWork.setVersion(commitVersion);

        aggregatesToCommit.values().stream().forEach(a -> {
                                ((SagaAggregate)a).setSagaState(GenericSagaState.NOT_IN_SAGA);
                                if (a.getState() != AggregateState.DELETED && a.getState() != AggregateState.INACTIVE) {
                                    a.setState(AggregateState.ACTIVE);
                                }
                            });

        commitAllObjects(commitVersion, aggregatesToCommit);

        unitOfWork.getEventsToEmit().forEach(e -> {
            /* this is so event detectors can compare this version to those of running transactions */
            e.setPublisherAggregateVersion(commitVersion);
            eventRepository.save(e);
        });
        logger.info("END EXECUTION FUNCTIONALITY: {} with version {}", unitOfWork.getFunctionalityName(), unitOfWork.getVersion());
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    public void commitAllObjects(Integer commitVersion, Map<Integer, Aggregate> aggregateMap) {
        aggregateMap.values().forEach(aggregateToWrite -> {
            aggregateToWrite.setVersion(commitVersion);
            aggregateToWrite.setCreationTs(DateHandler.now());
            entityManager.persist(aggregateToWrite);
        });
    }

    public void compensate(SagaUnitOfWork unitOfWork) {
        unitOfWork.compensate();
    }

    private Aggregate getConcurrentAggregate(Aggregate aggregate) {
        Aggregate concurrentAggregate;

        /* if the prev aggregate is null it means this is a creation functionality*/
        if (aggregate.getPrev() == null) {
            return null;
        }

        concurrentAggregate = sagaAggregateRepository.findConcurrentVersions(aggregate.getAggregateId(), aggregate.getPrev().getVersion())
                .orElse(null);

        // if a concurrent version is deleted it means the object has been deleted in the meanwhile
        if (concurrentAggregate != null && (concurrentAggregate.getState() == Aggregate.AggregateState.DELETED || concurrentAggregate.getState() == Aggregate.AggregateState.INACTIVE)) {
            throw new TutorException(ErrorMessage.AGGREGATE_DELETED, concurrentAggregate.getAggregateType().toString(), concurrentAggregate.getAggregateId());
        }

        return concurrentAggregate;
    }

    @Override
    public void abort(SagaUnitOfWork unitOfWork) {
        this.compensate(unitOfWork);
    }

    public void throwException(Integer aggregateId) {
        throw new TutorException(AGGREGATE_BEING_USED_IN_OTHER_SAGA, aggregateId);
    }

    @Override
    public void registerChanged(Aggregate aggregate, SagaUnitOfWork unitOfWork) {
        // the id set to null to force a new entry in the db
        //aggregate.setId(null);
        unitOfWork.getAggregatesToCommit().put(aggregate.getAggregateId(), aggregate);

        Map<Integer, Aggregate> aggregatesToCommit = new HashMap<>(unitOfWork.getAggregatesToCommit());

        Integer commitVersion = versionService.incrementAndGetVersionNumber();

        aggregatesToCommit.values().forEach(aggregateToWrite -> {
            aggregateToWrite.setVersion(commitVersion);
            aggregateToWrite.setCreationTs(DateHandler.now());
            entityManager.persist(aggregateToWrite);
        });

        unitOfWork.getEventsToEmit().forEach(e -> {
            /* this is so event detectors can compare this version to those of running transactions */
            e.setPublisherAggregateVersion(commitVersion);
            eventRepository.save(e);
        });
        unitOfWork.setVersion(unitOfWork.getVersion()+1);
    }
}
