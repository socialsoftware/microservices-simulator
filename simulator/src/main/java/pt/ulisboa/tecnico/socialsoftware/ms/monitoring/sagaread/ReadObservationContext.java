package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread;

import java.util.ArrayDeque;
import java.util.Deque;

/** Explicit exclusion for diagnostic/probe work, including work inside an application action. */
public final class ReadObservationContext {
    private static final ThreadLocal<Deque<Scope>> EXCLUSIONS = ThreadLocal.withInitial(ArrayDeque::new);

    private ReadObservationContext() { }

    public static Scope exclude(String role) {
        if (role == null || role.isBlank()) throw new IllegalArgumentException("Exclusion role is required");
        Scope scope = new Scope(role);
        EXCLUSIONS.get().push(scope);
        return scope;
    }

    public static String excludedRole() {
        Deque<Scope> stack = EXCLUSIONS.get();
        return stack.isEmpty() ? null : stack.peek().role;
    }

    public static final class Scope implements AutoCloseable {
        private final String role;
        private boolean closed;

        private Scope(String role) { this.role = role; }

        @Override public void close() {
            if (closed) return;
            closed = true;
            EXCLUSIONS.get().remove(this);
            if (EXCLUSIONS.get().isEmpty()) EXCLUSIONS.remove();
        }
    }
}
