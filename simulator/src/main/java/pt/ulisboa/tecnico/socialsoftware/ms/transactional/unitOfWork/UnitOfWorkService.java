package pt.ulisboa.tecnico.socialsoftware.ms.transactional.unitOfWork;

import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.Event;

import java.util.List;

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
    public abstract void commitAllObjects(Long commitVersion, List<Aggregate> aggregates);

    protected abstract String resolveServiceName(String aggregateType);
}
