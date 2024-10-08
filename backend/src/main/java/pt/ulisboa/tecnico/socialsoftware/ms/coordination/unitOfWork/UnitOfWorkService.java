package pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork;

import java.sql.SQLException;
import java.util.Map;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

@Service
public abstract class UnitOfWorkService<U extends UnitOfWork> {
    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public abstract U createUnitOfWork(String functionalityName);

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public abstract Aggregate aggregateLoadAndRegisterRead(Integer aggregateId, U unitOfWork);

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public abstract Aggregate aggregateLoad(Integer aggregateId, U unitOfWork);

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public abstract Aggregate registerRead(Aggregate aggregate, U unitOfWork);

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public abstract void registerChanged(Aggregate aggregate, U unitOfWork);

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public abstract void registerEvent(Event event, U unitOfWork);

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public abstract void commit(U unitOfWork);

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public abstract void abort(U unitOfWork);


    // Must be serializable in order to ensure no other commits are made between the checking of concurrent versions and the actual persist
    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public abstract void commitAllObjects(Integer commitVersion, Map<Integer, Aggregate> aggregateMap);
}
