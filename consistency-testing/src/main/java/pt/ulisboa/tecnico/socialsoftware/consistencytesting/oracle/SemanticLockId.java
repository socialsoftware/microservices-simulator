package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Objects;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

/**
 * Stable identifier for an application semantic-lock state. State names are
 * scoped to their aggregate's state enum.
 */
public record SemanticLockId(String sagaStateClassName, String stateName) {

    public SemanticLockId {
        requireNonBlank(sagaStateClassName, "sagaStateClassName");
        requireNonBlank(stateName, "stateName");
    }

    public static SemanticLockId from(SagaState sagaState) {
        Objects.requireNonNull(sagaState);

        String className = sagaState instanceof Enum<?> enumState
                ? enumState.getDeclaringClass().getName()
                : sagaState.getClass().getName();

        return new SemanticLockId(className, sagaState.getStateName());
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
    }
}
