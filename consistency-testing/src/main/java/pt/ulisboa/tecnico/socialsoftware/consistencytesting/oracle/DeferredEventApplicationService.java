package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.jspecify.annotations.Nullable;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;

final class DeferredEventApplicationService extends EventApplicationService {

    private volatile @Nullable CaptureSession activeSession;

    /**
     * Starts a new event capturing session.
     * 
     * @return the initialized capture session instance
     * 
     * @throws IllegalStateException if a session is already active
     */
    public synchronized CaptureSession beginCapture() {
        if (activeSession != null) {
            throw new IllegalStateException("There is already an active capture session");
        }
        activeSession = new CaptureSession(this);
        return activeSession;
    }

    /**
     * Clears the active capture session.
     * Invoked internally by the {@link CaptureSession} when it is closed.
     */
    private synchronized void endCapture(CaptureSession session) {
        if (activeSession == session) {
            activeSession = null;
        }
    }

    /**
     * Intercepts asynchronous and synchronous event dispatches. If a capture
     * session is active, the execution is recorded and deferred; otherwise,
     * it executes normally.
     * 
     * @throws NullPointerException if any argument is null
     */
    @Override
    protected void dispatchToHandler(
            @Nullable EventHandler handler,
            @Nullable Integer subscriberAggregateId,
            @Nullable Event eventToProcess) {

        Objects.requireNonNull(handler, "EventHandler cannot be null");
        Objects.requireNonNull(subscriberAggregateId, "SubscriberAggregateId cannot be null");
        Objects.requireNonNull(eventToProcess, "EventToProcess cannot be null");

        Runnable invocation = () -> super.dispatchToHandler(handler, subscriberAggregateId, eventToProcess);
        CaptureSession session = this.activeSession;

        if (session != null) {
            // Capture the invocation instead of executing it immediately.
            var deferredInvocation = new DeferredEventInvocation(
                    eventToProcess, handler, subscriberAggregateId, invocation);
            session.record(deferredInvocation);
        } else {
            // No active capture session, execute immediately, as it would normally happen.
            invocation.run();
        }
    }

    static final class CaptureSession implements AutoCloseable {

        private final DeferredEventApplicationService owner;
        private final Set<DeferredEventInvocation> currentDeferred = ConcurrentHashMap.newKeySet();
        private final Set<DeferredEventInvocation> allDeferred = ConcurrentHashMap.newKeySet();
        private volatile boolean closed = false;

        private CaptureSession(DeferredEventApplicationService owner) {
            this.owner = owner;
        }

        private void record(DeferredEventInvocation invocation) {
            if (closed) {
                throw new IllegalStateException("Capture session already closed");
            }

            if (allDeferred.add(invocation)) {
                // Track new event handling invocations while avoiding duplication of
                // invocations that were previously recorded on allDeferred registry.
                currentDeferred.add(invocation);
            }
        }

        Set<DeferredEventInvocation> drain() {
            Set<DeferredEventInvocation> result = Set.copyOf(currentDeferred);
            Objects.requireNonNull(result);

            currentDeferred.clear();
            return result;
        }

        @Override
        public void close() {
            closed = true;
            owner.endCapture(this);
        }
    }
}
