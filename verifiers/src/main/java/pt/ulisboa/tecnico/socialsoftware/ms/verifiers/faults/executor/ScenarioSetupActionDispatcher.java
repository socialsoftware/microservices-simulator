package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import java.util.List;
import java.util.Map;

/**
 * Application-owned closed dispatch table for source-derived setup actions.
 * Persisted method keys select an entry; they never authorize reflective invocation.
 */
public interface ScenarioSetupActionDispatcher {
    Map<String, SetupMethod> setupMethods();

    record SetupMethod(
            String methodKey,
            String declaredResultTypeFqn,
            boolean voidResult,
            Invocation invocation) {
    }

    @FunctionalInterface
    interface Invocation {
        Object invoke(List<Object> arguments) throws Exception;
    }
}
