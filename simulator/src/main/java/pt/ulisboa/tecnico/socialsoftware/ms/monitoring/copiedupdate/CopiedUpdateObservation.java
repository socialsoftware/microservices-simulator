package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.copiedupdate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Passive hooks shared by the local transport and the constructor agent. */
public final class CopiedUpdateObservation {
    private static volatile CopiedUpdateSession active;
    private static volatile List<Map<String, Object>> contracts = List.of();
    private static volatile String manifestHash;
    private static volatile boolean installed;
    private static final Map<String, String> instrumentationFailures = new ConcurrentHashMap<>();

    private CopiedUpdateObservation() {}

    public static void instrumented(List<Map<String, Object>> values, String hash) {
        contracts = List.copyOf(values);
        manifestHash = hash;
        installed = true;
    }

    public static void instrumentationFailed(String type, String reason) {
        instrumentationFailures.put(type, reason);
        invoke(session -> session.gap("INSTRUMENTATION_FAILED:" + type));
    }

    public static synchronized CopiedUpdateSession start(String attemptId, String expectedHash) {
        if (active != null)
            throw new IllegalStateException("Copied-update observation already active");
        CopiedUpdateSession session = new CopiedUpdateSession(attemptId, contracts);
        if (!installed) session.gap("CONSTRUCTOR_INSTRUMENTATION_UNAVAILABLE");
        if (expectedHash == null || !expectedHash.equals(manifestHash))
            session.gap("COPY_CONTRACT_MANIFEST_MISMATCH");
        if (contracts.isEmpty()) session.gap("NO_SUPPORTED_COPY_CONTRACTS");
        instrumentationFailures.forEach(
                (type, reason) -> session.gap("INSTRUMENTATION_FAILED:" + type + ":" + reason));
        if (!installed
                || expectedHash == null
                || !expectedHash.equals(manifestHash)
                || contracts.isEmpty()
                || !instrumentationFailures.isEmpty()) session.disable();
        active = session;
        return session;
    }

    public static synchronized Map<String, Object> finish(CopiedUpdateSession session) {
        if (active == session) active = null;
        return session.finish();
    }

    public static void begin(Object command) {
        invoke(session -> session.begin(command));
    }

    public static void inbound(Object command) {
        invoke(session -> session.inbound(command));
    }

    public static void end(Object result, Throwable failure) {
        invoke(session -> session.end(result, failure));
    }

    public static void copied(Object target, Object[] arguments) {
        invoke(session -> session.copied(target, arguments));
    }

    public static void registered(Object aggregate) {
        invoke(session -> session.registered(aggregate));
    }

    private static void invoke(Consumer<CopiedUpdateSession> operation) {
        CopiedUpdateSession session = active;
        if (session == null) return;
        try {
            operation.accept(session);
        } catch (RuntimeException | LinkageError failure) {
            session.gap("COPY_OBSERVER_FAILURE:" + failure.getClass().getName());
        }
    }
}
