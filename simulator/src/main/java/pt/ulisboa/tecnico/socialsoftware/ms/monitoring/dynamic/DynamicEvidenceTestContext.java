package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import java.util.Optional;

public final class DynamicEvidenceTestContext {
    private static final ThreadLocal<TestIdentity> CURRENT = new ThreadLocal<>();

    private DynamicEvidenceTestContext() {
    }

    public static void set(TestIdentity testIdentity) {
        if (testIdentity == null) {
            clear();
            return;
        }
        CURRENT.set(testIdentity);
    }

    public static Optional<TestIdentity> current() {
        return Optional.ofNullable(CURRENT.get());
    }

    public static void clear() {
        CURRENT.remove();
    }

    public record TestIdentity(String testClassFqn, String testMethodName, String testDisplayName,
                               String testUniqueId) {
    }
}
