package pt.ulisboa.tecnico.socialsoftware.ms.faults;

import java.util.Optional;

public final class FaultVectorProviderHolder {
    private static volatile FaultVectorFaultProvider activeProvider;
    private static final ThreadLocal<FaultVectorBoundaryContext> currentBoundary = new ThreadLocal<>();

    private FaultVectorProviderHolder() {
    }

    public static synchronized Scope install(FaultVectorFaultProvider provider) {
        if (provider == null) {
            throw new IllegalArgumentException("Fault vector provider cannot be null");
        }
        if (activeProvider != null) {
            throw new IllegalStateException("A fault vector provider is already active");
        }
        activeProvider = provider;
        return new Scope(() -> clearProvider(provider));
    }

    public static BoundaryScope enterBoundary(FaultVectorBoundaryContext context) {
        if (context == null) {
            throw new IllegalArgumentException("Fault vector boundary context cannot be null");
        }
        FaultVectorBoundaryContext previous = currentBoundary.get();
        currentBoundary.set(context);
        return new BoundaryScope(previous);
    }

    public static boolean isActive() {
        return activeProvider != null;
    }

    public static Optional<FaultVectorBoundaryContext> currentBoundary() {
        return Optional.ofNullable(currentBoundary.get());
    }

    public static Optional<FaultVectorFault> faultForCurrentBoundary() {
        FaultVectorFaultProvider provider = activeProvider;
        FaultVectorBoundaryContext context = currentBoundary.get();
        if (provider == null || context == null) {
            return Optional.empty();
        }
        if (context.assignedBit() != 1) {
            return Optional.empty();
        }
        return provider.faultFor(context);
    }

    public static synchronized void clear() {
        activeProvider = null;
        currentBoundary.remove();
    }

    private static synchronized void clearProvider(FaultVectorFaultProvider provider) {
        if (activeProvider == provider) {
            activeProvider = null;
        }
        currentBoundary.remove();
    }

    public static final class Scope implements AutoCloseable {
        private final Runnable closeAction;
        private boolean closed;

        private Scope(Runnable closeAction) {
            this.closeAction = closeAction;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                closeAction.run();
            }
        }
    }

    public static final class BoundaryScope implements AutoCloseable {
        private final FaultVectorBoundaryContext previous;
        private boolean closed;

        private BoundaryScope(FaultVectorBoundaryContext previous) {
            this.previous = previous;
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                if (previous == null) {
                    currentBoundary.remove();
                } else {
                    currentBoundary.set(previous);
                }
            }
        }
    }
}
