package pt.ulisboa.tecnico.socialsoftware.ms.coordination;

import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.*;

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

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public abstract void commit(U unitOfWork);


    // Must be serializable in order to ensure no other commits are made between the checking of concurrent versions and the actual persist
    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public abstract void commitAllObjects(Integer commitVersion, Map<Integer, Aggregate> aggregateMap);
}
