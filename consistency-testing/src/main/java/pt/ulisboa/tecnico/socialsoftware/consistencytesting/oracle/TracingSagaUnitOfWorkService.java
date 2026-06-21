package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jspecify.annotations.Nullable;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;

public class TracingSagaUnitOfWorkService extends SagaUnitOfWorkService {

    private volatile @Nullable TraceSession activeSession;

    /**
     * Starts a new read-write effects tracing session.
     * 
     * @return the initialized tracing session instance
     * 
     * @throws IllegalStateException if a session is already active
     */
    public synchronized TraceSession beginTrace() {
        if (activeSession != null) {
            throw new IllegalStateException("There is already an active trace session");
        }
        activeSession = new TraceSession(this);
        return activeSession;
    }

    /**
     * Clears the active tracing session.
     * Invoked internally by the {@link TraceSession} when it is closed.
     */
    private synchronized void endTrace(TraceSession session) {
        if (activeSession == session) {
            activeSession = null;
        }
    }

    private void traceRead(@Nullable Aggregate aggregate) {
        TraceSession session = this.activeSession;
        if (session != null && aggregate != null && aggregate.getAggregateId() != null) {
            session.register(new Effect.Read(aggregate.getAggregateId(), aggregate.getAggregateType()));
        }
    }

    private void traceWrite(@Nullable Aggregate aggregate) {
        TraceSession session = this.activeSession;
        if (session != null && aggregate != null && aggregate.getAggregateId() != null) {
            session.register(new Effect.Write(aggregate.getAggregateId(), aggregate.getAggregateType()));
        }
    }

    @Override
    public @Nullable Aggregate aggregateLoadAndRegisterRead(
            @Nullable Integer aggregateId, @Nullable SagaUnitOfWork unitOfWork) {

        Aggregate aggregate = super.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        traceRead(aggregate);
        return aggregate;
    }

    @Override
    public @Nullable Aggregate aggregateLoad(
            @Nullable Integer aggregateId, @Nullable SagaUnitOfWork unitOfWork) {

        Aggregate aggregate = super.aggregateLoad(aggregateId, unitOfWork);
        traceRead(aggregate);
        return aggregate;
    }

    @Override
    public @Nullable Aggregate registerRead(
            @Nullable Aggregate aggregate, @Nullable SagaUnitOfWork unitOfWork) {

        traceRead(aggregate);
        return super.registerRead(aggregate, unitOfWork);
    }

    @Override
    public void registerChanged(@Nullable Aggregate aggregate, @Nullable SagaUnitOfWork unitOfWork) {
        traceWrite(aggregate);
        super.registerChanged(aggregate, unitOfWork);
    }

    static final class TraceSession implements AutoCloseable {

        private final TracingSagaUnitOfWorkService owner;
        private final Queue<Effect> currentEffects = new ConcurrentLinkedQueue<>();
        private volatile boolean closed = false;

        private TraceSession(TracingSagaUnitOfWorkService owner) {
            this.owner = owner;
        }

        private void register(Effect effect) {
            if (closed) {
                throw new IllegalStateException("Trace session already closed");
            }
            currentEffects.add(effect);
        }

        List<Effect> drain() {
            List<Effect> drained = new ArrayList<>();
            Effect effect;
            while ((effect = currentEffects.poll()) != null) {
                drained.add(effect);
            }
            return drained;
        }

        @Override
        public void close() {
            closed = true;
            owner.endTrace(this);
        }
    }
}
