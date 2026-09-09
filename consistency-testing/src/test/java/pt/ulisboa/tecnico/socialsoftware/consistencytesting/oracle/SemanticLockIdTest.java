package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

class SemanticLockIdTest {

    @Test
    void parsesAndValidatesKnownEnumState() {
        String selector = TestSagaState.class.getName() + "#LOCKED";

        SemanticLockId semanticLock = SemanticLockId.parse(selector);

        assertEquals(new SemanticLockId(TestSagaState.class.getName(), "LOCKED"), semanticLock);
        assertEquals(selector, semanticLock.toSelector());
        assertDoesNotThrow(() -> semanticLock.validateAgainst(getClass().getClassLoader()));
    }

    @Test
    void rejectsNonEnumSagaState() {
        SagaState state = new NonEnumSagaState();

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> SemanticLockId.from(state));

        assertEquals(
                "Semantic-lock state must be an enum, got [%s]".formatted(NonEnumSagaState.class.getName()),
                error.getMessage());
    }

    @Test
    void rejectsMalformedSelector() {
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> SemanticLockId.parse("missing-separator"));

        assertEquals("Invalid semantic-lock selector 'missing-separator'; expected fully.qualified.Enum#STATE",
                error.getMessage());
    }

    @Test
    void rejectsUnknownStateClass() {
        SemanticLockId semanticLock = SemanticLockId.parse("missing.State#LOCKED");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> semanticLock.validateAgainst(getClass().getClassLoader()));

        assertEquals("Unknown semantic-lock state class 'missing.State'", error.getMessage());
    }

    @Test
    void rejectsTypeThatIsNotSagaState() {
        SemanticLockId semanticLock = SemanticLockId.parse(String.class.getName() + "#LOCKED");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> semanticLock.validateAgainst(getClass().getClassLoader()));

        assertEquals(
                "Semantic-lock state class 'java.lang.String' does not implement " + SagaState.class.getName(),
                error.getMessage());
    }

    @Test
    void rejectsUnknownStateName() {
        SemanticLockId semanticLock = SemanticLockId.parse(TestSagaState.class.getName() + "#MISSING");

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> semanticLock.validateAgainst(getClass().getClassLoader()));

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

    private static final class NonEnumSagaState implements SagaState {
        @Override
        public String getStateName() {
            return "NON_ENUM";
        }
    }
}
