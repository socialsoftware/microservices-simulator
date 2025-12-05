package pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork;

import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateDto;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.Event;

import java.sql.SQLException;
import java.util.Map;

@Service
public abstract class UnitOfWorkService<U extends UnitOfWork> {
    public abstract U createUnitOfWork(String functionalityName);

    public abstract Aggregate aggregateLoadAndRegisterRead(Integer aggregateId, U unitOfWork);

    public abstract Aggregate aggregateLoad(Integer aggregateId, U unitOfWork);

    public abstract Aggregate registerRead(Aggregate aggregate, U unitOfWork);

    public abstract void registerChanged(Aggregate aggregate, U unitOfWork);

    public abstract void registerEvent(Event event, U unitOfWork);

    public abstract void commit(U unitOfWork);

    public abstract void abort(U unitOfWork);

    // Must be serializable in order to ensure no other commits are made between the
    // checking of concurrent versions and the actual persist
    public abstract void commitAllObjects(Integer commitVersion, Map<Integer, Aggregate> aggregateMap);

    protected String resolveServiceName(String aggregateType) {
        String stripped = aggregateType.replaceAll("Saga", "").replaceAll("Causal", "");
        return Character.toLowerCase(stripped.charAt(0)) + stripped.substring(1);
    }
}
