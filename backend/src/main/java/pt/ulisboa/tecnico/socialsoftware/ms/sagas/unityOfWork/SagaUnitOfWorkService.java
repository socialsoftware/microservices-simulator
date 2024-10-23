package pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.AGGREGATE_BEING_USED_IN_OTHER_SAGA;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.AGGREGATE_DELETED;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.AGGREGATE_NOT_FOUND;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.version.VersionService;
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
        Aggregate aggregate = sagaAggregateRepository.findSagaAggregate(aggregateId)
                .orElseThrow(() -> new TutorException(AGGREGATE_NOT_FOUND, aggregateId));

        logger.info("Loaded and registered read for aggregate ID: {}", aggregateId);
        return aggregate;
    }

    public Aggregate aggregateLoad(Integer aggregateId, SagaUnitOfWork unitOfWork) {
        Aggregate aggregate = sagaAggregateRepository.findSagaAggregate(aggregateId)
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
        SagaAggregate aggregate = (SagaAggregate) sagaAggregateRepository.findSagaAggregate(aggregateId)
                .orElseThrow(() -> new TutorException(AGGREGATE_NOT_FOUND));
        while (!aggregate.getSagaState().equals(GenericSagaState.NOT_IN_SAGA) && !state.equals(GenericSagaState.NOT_IN_SAGA)) {
            aggregate = (SagaAggregate) sagaAggregateRepository.findSagaAggregate(aggregateId)
                .orElseThrow(() -> new TutorException(AGGREGATE_NOT_FOUND));
        }

        unitOfWork.savePreviousState(aggregateId, aggregate.getSagaState());

        aggregate.setSagaState(state);
        entityManager.persist(aggregate);
        unitOfWork.addToAggregatesInSaga(aggregate);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void registerSagaState(Integer aggregateId, SagaState state, ArrayList<SagaState> allowedStates, SagaUnitOfWork unitOfWork) {
        SagaAggregate aggregate = (SagaAggregate) sagaAggregateRepository.findSagaAggregate(aggregateId)
                .orElseThrow(() -> new TutorException(AGGREGATE_NOT_FOUND));
        while (!aggregate.getSagaState().equals(GenericSagaState.NOT_IN_SAGA)  && !state.equals(GenericSagaState.NOT_IN_SAGA) && !allowedStates.contains(aggregate.getSagaState())) {
            aggregate = (SagaAggregate) sagaAggregateRepository.findSagaAggregate(aggregateId)
                .orElseThrow(() -> new TutorException(AGGREGATE_NOT_FOUND));
        }

        unitOfWork.savePreviousState(aggregateId, aggregate.getSagaState());

        aggregate.setSagaState(state);
        entityManager.persist(aggregate);
        unitOfWork.addToAggregatesInSaga(aggregate);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void verifyAndRegisterSagaState(Integer aggregateId, SagaState state, List<SagaState> forbiddenStates, SagaUnitOfWork unitOfWork) {
        SagaAggregate aggregate = (SagaAggregate) sagaAggregateRepository.findSagaAggregate(aggregateId)
                .orElseThrow(() -> new TutorException(AGGREGATE_NOT_FOUND));
        
        if (forbiddenStates.contains(aggregate.getSagaState())) {
            throw new TutorException(AGGREGATE_BEING_USED_IN_OTHER_SAGA, aggregate.getSagaState().getStateName());
        }
        else if (state != null){
            unitOfWork.savePreviousState(aggregateId, aggregate.getSagaState());
            aggregate.setSagaState(state);
            entityManager.persist(aggregate);
            unitOfWork.addToAggregatesInSaga(aggregate);
        }
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void verifySagaState(Integer aggregateId, List<SagaState> forbiddenStates) {
        SagaAggregate aggregate = (SagaAggregate) sagaAggregateRepository.findSagaAggregate(aggregateId)
                .orElseThrow(() -> new TutorException(AGGREGATE_NOT_FOUND));

        if (forbiddenStates.contains(aggregate.getSagaState())) {
            throw new TutorException(AGGREGATE_BEING_USED_IN_OTHER_SAGA, aggregate.getSagaState().getStateName());
        }
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void commit(SagaUnitOfWork unitOfWork) {
        unitOfWork.getAggregatesInSaga().stream().forEach(a -> {
            a.setSagaState(GenericSagaState.NOT_IN_SAGA);
            entityManager.persist(a);
        });

        logger.info("END EXECUTION FUNCTIONALITY: {} with version {}", unitOfWork.getFunctionalityName(), unitOfWork.getVersion());
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    public void commitAllObjects(Integer commitVersion, Map<Integer, Aggregate> aggregateMap) {
        // aggregates are committed at the end of each service
    }

    public void compensate(SagaUnitOfWork unitOfWork) {
        unitOfWork.compensate();
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    @Override
    public void abort(SagaUnitOfWork unitOfWork) {
        for (Map.Entry<Integer, SagaState> entry : unitOfWork.getPreviousStates().entrySet()) {
            Integer aggregateId = entry.getKey();
            SagaState previousState = entry.getValue();
            SagaAggregate aggregate = (SagaAggregate) sagaAggregateRepository.findSagaAggregate(aggregateId)
                .orElseThrow(() -> new TutorException(AGGREGATE_NOT_FOUND));
            aggregate.setSagaState(previousState);
            entityManager.persist(aggregate);
        }
        this.compensate(unitOfWork);
    }

    @Override
    public void registerChanged(Aggregate aggregate, SagaUnitOfWork unitOfWork) {
        if (aggregate.getId() == null) {
            ((SagaAggregate)aggregate).setSagaState(GenericSagaState.NOT_IN_SAGA);
        }

        Integer commitVersion = versionService.incrementAndGetVersionNumber();

        aggregate.verifyInvariants();
        aggregate.setVersion(commitVersion);
        aggregate.setCreationTs(DateHandler.now());
        entityManager.persist(aggregate);
        
        unitOfWork.setVersion(commitVersion);
    }

    @Override
    public void registerEvent(Event event, SagaUnitOfWork unitOfWork) {
        Integer commitVersion = versionService.incrementAndGetVersionNumber();
        event.setPublisherAggregateVersion(commitVersion);
        eventRepository.save(event);
        unitOfWork.setVersion(unitOfWork.getVersion()+1);
    }
}
