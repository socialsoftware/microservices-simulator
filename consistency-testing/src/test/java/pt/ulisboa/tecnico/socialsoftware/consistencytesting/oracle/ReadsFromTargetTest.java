package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ReadsFromTargetTest {
    private final FunctionalityId readerSaga = FunctionalityId.forSagaFunctionality("reader");
    private final StepId firstRead = StepId.forFunctionalityStep(readerSaga, "first");
    private final StepId lastRead = StepId.forFunctionalityStep(readerSaga, "last");
    private final StepId writer = StepId.forFunctionalityStep(
            FunctionalityId.forSagaFunctionality("writer"), "write");
    private final ReadsFromTarget target = new ReadsFromTarget(writer, lastRead, "Quiz");

    @Test
    void targetRequiresReaderToObserveWriterOnSameObject() {
        assertTrue(target.observedIn(result(List.of(write(0, writer, 1), read(1, lastRead, 1)))));
        assertFalse(target.observedIn(result(List.of(write(0, writer, 2), read(1, lastRead, 1)))));
        assertFalse(target.observedIn(result(List.of(read(0, lastRead, 1), write(1, writer, 1)))));
    }

    private StepEffect read(int sequence, StepId step, int object) {
        return new StepEffect(sequence, step, StepKind.FUNCTIONALITY,
                StepEffect.EffectKind.READ, object, "Quiz");
    }

    private StepEffect write(int sequence, StepId step, int object) {
        return new StepEffect(sequence, step, StepKind.FUNCTIONALITY,
                StepEffect.EffectKind.WRITE, object, "Quiz");
    }

    private TestResult result(List<StepEffect> effects) {
        return new TestResult(new StepDependencies(), new StepDependencies(), Map.of(),
                List.of(firstRead, writer, lastRead), Map.of(), Set.of(), effects,
                ReadsFromRelation.deriveAll(effects), List.of(), List.of(), Map.of());
    }
}
