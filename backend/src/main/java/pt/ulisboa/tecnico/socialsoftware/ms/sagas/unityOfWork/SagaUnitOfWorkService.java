package pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregateRepository;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.version.VersionService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;

import jakarta.persistence.EntityManager;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.*;

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
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SagaUnitOfWork createUnitOfWork(String functionalityName) {
        Integer lastCommittedAggregateVersionNumber = versionService.getVersionNumber();

        SagaUnitOfWork unitOfWork = new SagaUnitOfWork(lastCommittedAggregateVersionNumber+1, functionalityName);

        logger.info("START EXECUTION FUNCTIONALITY: {} with version {}", functionalityName, unitOfWork.getVersion());

        return unitOfWork;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Aggregate aggregateLoadAndRegisterRead(Integer aggregateId, SagaUnitOfWork unitOfWork) {
        Aggregate aggregate = sagaAggregateRepository.findSagaAggregate(aggregateId, unitOfWork.getVersion())
                .orElseThrow(() -> new RuntimeException("Aggregate not found: " + aggregateId));

        // TODO: add register read logic if needed
        logger.info("Loaded and registered read for aggregate ID: {}", aggregateId);
        return aggregate;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Aggregate aggregateLoad(Integer aggregateId, SagaUnitOfWork unitOfWork) {
        Aggregate aggregate = sagaAggregateRepository.findSagaAggregate(aggregateId, unitOfWork.getVersion())
                .orElseThrow(() -> new TutorException(AGGREGATE_NOT_FOUND, aggregateId));

        if (aggregate.getState() == Aggregate.AggregateState.DELETED) {
            throw new TutorException(AGGREGATE_DELETED, aggregate.getAggregateType().toString(), aggregate.getAggregateId());
        }

        return aggregate;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Aggregate registerRead(Aggregate aggregate, SagaUnitOfWork unitOfWork) {
        // TODO: add register read logic if needed
        return aggregate;
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void commit(SagaUnitOfWork unitOfWork) {
        // TODO: add commit logic
        logger.info("END EXECUTION FUNCTIONALITY: {} with version {}", unitOfWork.getFunctionalityName(), unitOfWork.getVersion());
    }


    // Must be serializable in order to ensure no other commits are made between the checking of concurrent versions and the actual persist
    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void commitAllObjects(Integer commitVersion, Map<Integer, Aggregate> aggregateMap) {
        aggregateMap.values().forEach(aggregateToWrite -> {
            aggregateToWrite.setVersion(commitVersion);
            aggregateToWrite.setCreationTs(LocalDateTime.now());
            entityManager.persist(aggregateToWrite);
        });
    }

    public void compensate(SagaUnitOfWork unitOfWork) {
        unitOfWork.compensate();
    }
}
