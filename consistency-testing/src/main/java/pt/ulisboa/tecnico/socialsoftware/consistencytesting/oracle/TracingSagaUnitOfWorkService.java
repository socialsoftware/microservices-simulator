package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.jspecify.annotations.Nullable;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;

public class TracingSagaUnitOfWorkService extends SagaUnitOfWorkService {

    private volatile @Nullable TraceSession activeSession;
    private Set<SemanticLockId> ignoredSemanticLocks = Set.of();

    /** This should be configured before any schedule runs. */
    void configureIgnoredSemanticLocks(Set<SemanticLockId> ignoredSemanticLocks) {
        this.ignoredSemanticLocks = Set.copyOf(ignoredSemanticLocks);
    }

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

    private void traceSemanticLock(SemanticLockId semanticLock, Integer aggregateId, SemanticLockActivity.Outcome outcome) {
        TraceSession session = this.activeSession;
        if (session != null) {
            session.registerSemanticLock(semanticLock, aggregateId, outcome);
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
        super.registerChanged(aggregate, unitOfWork);

        // trace runs after super.registerChanged to only trace if the write really went
        // through (i.e., the write was not stopped by a throw on verifyInvariants())
        traceWrite(aggregate);
    }

    @Override
    public void registerSagaState(Integer aggregateId, SagaState state, SagaUnitOfWork unitOfWork) {
        SemanticLockId semanticLock = SemanticLockId.from(state);
        if (ignoredSemanticLocks.contains(semanticLock)) {
            // Full-state omission models a developer not calling setSemanticLock
            // at this acquisition.
            // TODO Per-call-site omission could also be implemented.
            traceSemanticLock(semanticLock, aggregateId, SemanticLockActivity.Outcome.SKIPPED);
            return;
        }

        super.registerSagaState(aggregateId, state, unitOfWork);
        traceSemanticLock(semanticLock, aggregateId, SemanticLockActivity.Outcome.ACQUIRED);
    }

    static final class TraceSession implements AutoCloseable {

        private final TracingSagaUnitOfWorkService owner;
        private final Queue<Effect> currentEffects = new ConcurrentLinkedQueue<>();
        private final Queue<SemanticLockActivity> semanticLockTrace = new ConcurrentLinkedQueue<>();
        private volatile @Nullable StepId executingStepId;
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

        StepScope beginStep(StepId stepId) {
            if (closed) {
                throw new IllegalStateException("Trace session already closed");
            }
            if (executingStepId != null) {
                throw new IllegalStateException("Trace session is already associated with a step");
            }
            executingStepId = stepId;
            return new StepScope(this, stepId);
        }

        private void registerSemanticLock(
                SemanticLockId semanticLock, Integer aggregateId, SemanticLockActivity.Outcome outcome) {

            if (closed) {
                throw new IllegalStateException("Trace session already closed");
            }
            StepId stepId = executingStepId;
            if (stepId != null) {
                semanticLockTrace.add(new SemanticLockActivity(stepId, semanticLock, aggregateId, outcome));
            }
        }

        List<SemanticLockActivity> getSemanticLockTrace() {
            return List.copyOf(semanticLockTrace);
        }

        private void endStep(StepId stepId) {
            if (!stepId.equals(executingStepId)) {
                throw new IllegalStateException("Trace session step scope closed out of order");
            }
            executingStepId = null;
        }

        @Override
        public void close() {
            closed = true;
            owner.endTrace(this);
        }

        static final class StepScope implements AutoCloseable {
            private final TraceSession owner;
            private final StepId stepId;
            private boolean closed;

            private StepScope(TraceSession owner, StepId stepId) {
                this.owner = owner;
                this.stepId = stepId;
            }

            @Override
            public void close() {
                if (!closed) {
                    closed = true;
                    owner.endStep(stepId);
                }
            }
        }
    }
}
