package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

class OrchestratorSemanticLockFaultTest {
    private static final String IGNORED_SEMANTIC_LOCKS_PROPERTY = "consistency.ignoredSemanticLocks";

    private String originalIgnoredSemanticLocks;

    @BeforeEach
    void clearIgnoredSemanticLocksProperty() {
        originalIgnoredSemanticLocks = System.getProperty(IGNORED_SEMANTIC_LOCKS_PROPERTY);
        System.clearProperty(IGNORED_SEMANTIC_LOCKS_PROPERTY);
    }

    @AfterEach
    void restoreIgnoredSemanticLocksProperty() {
        if (originalIgnoredSemanticLocks == null) {
            System.clearProperty(IGNORED_SEMANTIC_LOCKS_PROPERTY);
        } else {
            System.setProperty(IGNORED_SEMANTIC_LOCKS_PROPERTY, originalIgnoredSemanticLocks);
        }
    }

    @Test
    void readsFaultSelectorsFromTestOnlySystemProperty() {
        System.setProperty(IGNORED_SEMANTIC_LOCKS_PROPERTY, TestSagaState.class.getName() + "#LOCKED");

        assertDoesNotThrow(() -> Orchestrator.of(getClass()));
    }

    @Test
    void rejectsInvalidSystemPropertyDuringOrchestratorCreation() {
        System.setProperty(IGNORED_SEMANTIC_LOCKS_PROPERTY, TestSagaState.class.getName() + "#MISSING");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class, () -> Orchestrator.of(getClass()));

        assertEquals("Unknown semantic-lock state 'MISSING' in '" + TestSagaState.class.getName() + "'",
                error.getMessage());
    }

    @Test
    void acceptsValidatedTestOnlyLockSelectors() {
        Orchestrator orchestrator = Orchestrator.of(getClass());

        assertDoesNotThrow(() -> orchestrator.withIgnoredSemanticLockSelectors(
                List.of(TestSagaState.class.getName() + "#LOCKED")));
    }

    @Test
    void returnsSameOrchestratorFromFluentConfiguration() {
        Orchestrator orchestrator = Orchestrator.of(getClass());

        assertSame(orchestrator, orchestrator.withIgnoredSemanticLockSelectors(
                List.of(TestSagaState.class.getName() + "#LOCKED")));
    }

    @Test
    void rejectsInvalidLockSelectorDuringConfiguration() {
        Orchestrator orchestrator = Orchestrator.of(getClass());

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> orchestrator
                        .withIgnoredSemanticLockSelectors(List.of(TestSagaState.class.getName() + "#MISSING")));

        assertEquals("Unknown semantic-lock state 'MISSING' in '" + TestSagaState.class.getName() + "'",
                error.getMessage());
    }

    private enum TestSagaState implements SagaState {
        LOCKED;

        @Override
        public String getStateName() {
            return "LOCKED";
        }
    }
}
