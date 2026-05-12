package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;
import java.util.UUID;

public final class DynamicEvidenceContext {
    private static final ThreadLocal<Deque<StepContext>> CURRENT = ThreadLocal.withInitial(ArrayDeque::new);

    private DynamicEvidenceContext() {
    }

    public static Scope enterStep(String functionalityName, String stepName, Long unitOfWorkVersion) {
        return enterStep(functionalityName, null, null, stepName, unitOfWorkVersion);
    }

    public static Scope enterStep(String functionalityName, String functionalityClassFqn,
                                  String functionalityClassSimpleName, String stepName, Long unitOfWorkVersion) {
        DynamicInputAttribution attribution = DynamicInputAttributionHolder.resolve(functionalityClassFqn, stepName);
        StepContext context = new StepContext(
                functionalityName,
                functionalityClassFqn,
                functionalityClassSimpleName,
                attribution.inputVariantId(),
                attribution.status(),
                attribution.basis(),
                attribution.candidateInputVariantIds(),
                invocationId(functionalityName, unitOfWorkVersion),
                stepName,
                unitOfWorkVersion,
                System.currentTimeMillis(),
                System.nanoTime());
        CURRENT.get().push(context);
        return new Scope(context);
    }

    public static Optional<StepContext> current() {
        Deque<StepContext> stack = CURRENT.get();
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack.peek());
    }

    public static Optional<String> currentFunctionalityName() {
        return current().map(StepContext::functionalityName);
    }

    public static Optional<String> currentFunctionalityClassFqn() {
        return current().map(StepContext::functionalityClassFqn);
    }

    public static Optional<String> currentInputVariantId() {
        return current().map(StepContext::inputVariantId);
    }

    public static Optional<String> currentStepName() {
        return current().map(StepContext::stepName);
    }

    public static void clear() {
        CURRENT.remove();
    }

    private static String invocationId(String functionalityName, Long unitOfWorkVersion) {
        if (functionalityName != null && unitOfWorkVersion != null) {
            return functionalityName + "-" + unitOfWorkVersion;
        }
        return UUID.randomUUID().toString();
    }

    public record StepContext(String functionalityName, String functionalityClassFqn,
                              String functionalityClassSimpleName, String inputVariantId,
                              String inputVariantAttributionStatus, String inputVariantAttributionBasis,
                              java.util.List<String> candidateInputVariantIds,
                              String functionalityInvocationId, String stepName,
                              Long unitOfWorkVersion, long startedAtMillis, long startedAtNanos) {
        public StepContext(String functionalityName, String functionalityClassFqn,
                           String functionalityClassSimpleName, String functionalityInvocationId, String stepName,
                           Long unitOfWorkVersion, long startedAtMillis, long startedAtNanos) {
            this(functionalityName, functionalityClassFqn, functionalityClassSimpleName, null, null, null,
                    java.util.List.of(), functionalityInvocationId, stepName, unitOfWorkVersion, startedAtMillis,
                    startedAtNanos);
        }

        public StepContext(String functionalityName, String functionalityInvocationId, String stepName,
                           Long unitOfWorkVersion, long startedAtMillis, long startedAtNanos) {
            this(functionalityName, null, null, null, null, null, java.util.List.of(), functionalityInvocationId, stepName, unitOfWorkVersion,
                    startedAtMillis, startedAtNanos);
        }

        public StepContext {
            candidateInputVariantIds = candidateInputVariantIds == null ? java.util.List.of() : java.util.List.copyOf(candidateInputVariantIds);
        }
    }

    public static final class Scope implements AutoCloseable {
        private final StepContext context;
        private boolean closed;

        private Scope(StepContext context) {
            this.context = context;
        }

        public StepContext context() {
            return context;
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            Deque<StepContext> stack = CURRENT.get();
            if (!stack.isEmpty() && stack.peek() == context) {
                stack.pop();
            } else {
                stack.remove(context);
            }
            if (stack.isEmpty()) {
                CURRENT.remove();
            }
        }
    }
}
