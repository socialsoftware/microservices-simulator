package pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.AGGREGATE_DELETED;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.AGGREGATE_NOT_FOUND;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.CANNOT_MODIFY_INACTIVE_AGGREGATE;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

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

        // TODO: add register read logic if needed
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
        // TODO: add register read logic if needed
        return aggregate;
    }

    // TODO check this
    public void registerSagaState(SagaAggregate aggregate, SagaState state, SagaUnitOfWork unitOfWork) {
        aggregate.setSagaState(state);
        entityManager.persist(aggregate);
        unitOfWork.addToAggregatesInSaga(aggregate);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    public void commit(SagaUnitOfWork unitOfWork) {
        boolean concurrentAggregates = true;

        // STEP 1 check whether any of the aggregates to write have concurrent versions
        // STEP 2 if so perform any merges necessary
        // STEP 3 performs steps 1 and 2 until step 1 stops holding
        // STEP 4 perform a commit of the aggregates under SERIALIZABLE isolation

        Map<Integer, Aggregate> originalAggregatesToCommit = new HashMap<>(unitOfWork.getAggregatesToCommit());

        // may contain merged aggregates
        // we do not want to compare intermediate merged aggregates with concurrent aggregate, so we separate
        // the comparison is always between the original written by the functionality and the concurrent
        Map<Integer, Aggregate> modifiedAggregatesToCommit = new HashMap<>(unitOfWork.getAggregatesToCommit());

        while (concurrentAggregates) {
            concurrentAggregates = false;
            for (Integer aggregateId : originalAggregatesToCommit.keySet()) {
                Aggregate aggregateToWrite = originalAggregatesToCommit.get(aggregateId);
                if (aggregateToWrite.getPrev() != null && aggregateToWrite.getPrev().getState() == Aggregate.AggregateState.INACTIVE) {
                    throw new TutorException(CANNOT_MODIFY_INACTIVE_AGGREGATE, aggregateToWrite.getAggregateId());
                }
                aggregateToWrite.verifyInvariants();
                Aggregate concurrentAggregate = getConcurrentAggregate(aggregateToWrite);
                // second condition is necessary for when a concurrent version is detected at first and then in the following detections it will have to do
                // this verification in order to not detect the same as a version as concurrent again
                if (concurrentAggregate != null && unitOfWork.getVersion() <= concurrentAggregate.getVersion()) {
                    concurrentAggregates = true;
                    Aggregate newAggregate = ((SagaAggregate) aggregateToWrite).merge(aggregateToWrite, concurrentAggregate); // TODO change this to saga aggregate
                    newAggregate.verifyInvariants();
                    newAggregate.setId(null);
                    modifiedAggregatesToCommit.put(aggregateId, newAggregate);
                }
            }

            if (concurrentAggregates) {
                // because there was a concurrent version we need to get a new version
                // the service to get a new version must also increment it to guarantee two transactions do run with the same version number
                // a number must be requested every time a concurrent version is detected
                unitOfWork.setVersion(versionService.incrementAndGetVersionNumber());
            }
        }

        unitOfWork.getAggregatesInSaga().stream().forEach(a -> {
            a.setSagaState(GenericSagaState.NOT_IN_SAGA);
        });

        // The commit is done with the last commited version plus one
        Integer commitVersion = versionService.incrementAndGetVersionNumber();
        unitOfWork.setVersion(commitVersion);

        modifiedAggregatesToCommit.values().stream().forEach(a -> {
                                if (a.getState() != AggregateState.DELETED && a.getState() != AggregateState.INACTIVE) {
                                    a.setState(AggregateState.ACTIVE);
                                }
                            });

        commitAllObjects(commitVersion, modifiedAggregatesToCommit);

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
        // TODO
        this.compensate(unitOfWork);
    }
}
