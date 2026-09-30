package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

class ScheduleChoiceControllerTest {

    @Test
    void mapsReplayChoicesIntoEachDynamicReadySet() {
        ScheduleChoiceController choices = new ScheduleChoiceController(42L, List.of(1, 5, -1), null);

        // Prefix indexes are normalized against each run's current ready-set size:
        // 1 mod 3 = 1, 5 mod 2 = 1, and -1 mod 4 = 3.
        // floorMod maps any prefix value into [0, readySetSize). Ready-set sizes
        // can change when events, compensations, or failures add/remove steps.
        // Negative values are not produced by the corpus; floorMod makes external
        // prefixes safe too: -1 maps to the last index, -2 to the second-to-last.
        assertEquals(1, choices.choose(ready(3), List.of(), Set.of()));
        assertEquals(1, choices.choose(ready(2), List.of(), Set.of()));
        assertEquals(3, choices.choose(ready(4), List.of(), Set.of()));
        assertEquals(
                List.of(
                        new ScheduleDecision(3, 1),
                        new ScheduleDecision(2, 1),
                        new ScheduleDecision(4, 3)),
                choices.trace().decisions());
    }

    @Test
    void fallsBackToReproducibleRandomChoicesAfterPrefixEnds() {
        ScheduleChoiceController first = new ScheduleChoiceController(91L, List.of(2), null);
        ScheduleChoiceController second = new ScheduleChoiceController(91L, List.of(2), null);

        List<Integer> firstChoices = List.of(
                first.choose(ready(4), List.of(), Set.of()),
                first.choose(ready(5), List.of(), Set.of()),
                first.choose(ready(6), List.of(), Set.of()));
        List<Integer> secondChoices = List.of(
                second.choose(ready(4), List.of(), Set.of()),
                second.choose(ready(5), List.of(), Set.of()),
                second.choose(ready(6), List.of(), Set.of()));

        // Both controllers share the seed and prefix, so their post-prefix random
        // choices must be identical.
        assertEquals(firstChoices, secondChoices);
    }

    @Test
    void choosesReadyWriterThenReaderForTarget() {
        StepId writer = StepId.forFunctionalityStep(FunctionalityId.forSagaFunctionality("a"), "write");
        StepId reader = StepId.forFunctionalityStep(FunctionalityId.forSagaFunctionality("b"), "read");
        StepId other = StepId.forFunctionalityStep(FunctionalityId.forSagaFunctionality("c"), "other");
        ScheduleChoiceController choices = new ScheduleChoiceController(
                42L, List.of(), new ReadsFromTarget(writer, reader, "Quiz"));

        assertEquals(1, choices.choose(List.of(reader, other), List.of(), Set.of()));
        assertEquals(0, choices.choose(List.of(writer, reader), List.of(), Set.of(other)));
        StepEffect write = new StepEffect(0, writer, StepKind.FUNCTIONALITY,
                StepEffect.EffectKind.WRITE, 1, "Quiz");
        assertEquals(0, choices.choose(List.of(reader, other), List.of(write), Set.of(writer, other)));
    }

    private static List<StepId> ready(int size) {
        FunctionalityId functionality = FunctionalityId.forSagaFunctionality("ready");
        return IntStream.range(0, size)
                .mapToObj(index -> StepId.forFunctionalityStep(functionality, "step" + index))
                .toList();
    }
}
