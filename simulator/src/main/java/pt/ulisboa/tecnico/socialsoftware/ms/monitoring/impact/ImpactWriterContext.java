package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/** Attempt/action attribution installed by an executor around application work. */
public final class ImpactWriterContext {
    private static final ThreadLocal<Deque<ImpactEvidence.Writer>> CURRENT =
            ThreadLocal.withInitial(ArrayDeque::new);

    private ImpactWriterContext() {
    }

    public static Scope enter(ImpactEvidence.Writer writer) {
        if (writer == null) throw new IllegalArgumentException("Impact writer cannot be null");
        CURRENT.get().push(writer);
        return new Scope(writer);
    }

    public static Optional<ImpactEvidence.Writer> current() {
        Deque<ImpactEvidence.Writer> stack = CURRENT.get();
        return stack.isEmpty() ? Optional.empty() : Optional.of(stack.peek());
    }

    public static final class Scope implements AutoCloseable {
        private final ImpactEvidence.Writer writer;
        private boolean closed;

        private Scope(ImpactEvidence.Writer writer) { this.writer = writer; }

        @Override public void close() {
            if (closed) return;
            closed = true;
            Deque<ImpactEvidence.Writer> stack = CURRENT.get();
            if (!stack.isEmpty() && stack.peek() == writer) stack.pop(); else stack.remove(writer);
            if (stack.isEmpty()) CURRENT.remove();
        }
    }
}
