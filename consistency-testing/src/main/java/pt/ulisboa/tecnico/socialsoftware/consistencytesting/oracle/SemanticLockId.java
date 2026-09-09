package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Objects;
import java.util.Arrays;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

/**
 * Stable identifier for an application semantic-lock state. State names are
 * scoped to their aggregate's state enum.
 *
 * @param sagaStateClassName fully qualified name of the {@link SagaState}
 *                           implementation that owns the state
 * @param stateName          state name returned by
 *                           {@link SagaState#getStateName()}
 */
public record SemanticLockId(String sagaStateClassName, String stateName) {

    public SemanticLockId {
        requireNonBlank(sagaStateClassName, "sagaStateClassName");
        requireNonBlank(stateName, "stateName");
    }

    /**
     * Creates an identifier from a {@link SagaState} Enum.
     *
     * @throws IllegalArgumentException if {@code sagaState} is not an enum
     */
    public static SemanticLockId from(SagaState sagaState) {
        Objects.requireNonNull(sagaState);

        if (!(sagaState instanceof Enum<?> enumState)) {
            throw new IllegalArgumentException(
                    "Semantic-lock state must be an enum, got [%s]".formatted(sagaState.getClass().getName()));
        }

        String className = enumState.getDeclaringClass().getName();

        return new SemanticLockId(className, sagaState.getStateName());
    }

    /**
     * Parses a command/config selector in {@code fully.qualified.Enum#STATE} form.
     */
    public static SemanticLockId parse(String selector) {
        if (selector == null) {
            throw new IllegalArgumentException("Semantic-lock selector cannot be null");
        }

        int separator = selector.lastIndexOf('#');
        if (separator <= 0 || separator == selector.length() - 1) {
            throw new IllegalArgumentException(
                    "Invalid semantic-lock selector '%s'; expected fully.qualified.Enum#STATE".formatted(selector));
        }

        return new SemanticLockId(selector.substring(0, separator), selector.substring(separator + 1));
    }

    /** Returns this identifier in the configuration selector form. */
    public String toSelector() {
        return sagaStateClassName + "#" + stateName;
    }

    /**
     * Validates this identifier before the oracle starts an application.
     * <p>
     * The {@code sagaStateClassName} component is loaded with the supplied
     * class loader and must name an enum implementing {@link SagaState}. The
     * {@code stateName} component is then matched against
     * {@link SagaState#getStateName()} for the enum's constants. Restricting
     * selectors to enums makes the valid state names checkable without starting
     * application code.
     *
     * @param classLoader class loader that can load the application's saga-state
     *                    class
     * @throws IllegalArgumentException if the class cannot be loaded, does not
     *                                  implement {@link SagaState}, is not an enum,
     *                                  or has no state whose
     *                                  name equals {@code stateName}
     */
    public void validateAgainst(ClassLoader classLoader) {
        Class<?> sagaStateClass;
        try {
            sagaStateClass = Class.forName(sagaStateClassName, false, classLoader);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(
                    "Unknown semantic-lock state class '%s'".formatted(sagaStateClassName), e);
        }

        if (!SagaState.class.isAssignableFrom(sagaStateClass)) {
            throw new IllegalArgumentException(
                    "Semantic-lock state class '%s' does not implement %s"
                            .formatted(sagaStateClassName, SagaState.class.getName()));
        }

        if (!sagaStateClass.isEnum()) {
            throw new IllegalArgumentException(
                    "Semantic-lock state class '%s' is not an enum and cannot be validated before startup"
                            .formatted(sagaStateClassName));
        }

        boolean knownState = Arrays.stream(sagaStateClass.getEnumConstants())
                .map(SagaState.class::cast)
                .anyMatch(state -> stateName.equals(state.getStateName()));
        if (!knownState) {
            throw new IllegalArgumentException(
                    "Unknown semantic-lock state '%s' in '%s'".formatted(stateName, sagaStateClassName));
        }
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " cannot be blank");
        }
    }
}
