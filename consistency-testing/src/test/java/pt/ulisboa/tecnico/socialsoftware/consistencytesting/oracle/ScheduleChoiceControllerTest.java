package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class ScheduleChoiceControllerTest {

    @Test
    void mapsReplayChoicesIntoEachDynamicReadySet() {
        ScheduleChoiceController choices = new ScheduleChoiceController(42L, List.of(1, 5, -1));

        // Prefix indexes are normalized against each run's current ready-set size:
        // 1 mod 3 = 1, 5 mod 2 = 1, and -1 mod 4 = 3.
        // floorMod maps any prefix value into [0, readySetSize). Ready-set sizes
        // can change when events, compensations, or failures add/remove steps.
        // Negative values are not produced by the corpus; floorMod makes external
        // prefixes safe too: -1 maps to the last index, -2 to the second-to-last.
        assertEquals(1, choices.choose(3));
        assertEquals(1, choices.choose(2));
        assertEquals(3, choices.choose(4));
        assertEquals(
                List.of(
                        new ScheduleDecision(3, 1),
                        new ScheduleDecision(2, 1),
                        new ScheduleDecision(4, 3)),
                choices.trace().decisions());
    }

    @Test
    void fallsBackToReproducibleRandomChoicesAfterPrefixEnds() {
        ScheduleChoiceController first = new ScheduleChoiceController(91L, List.of(2));
        ScheduleChoiceController second = new ScheduleChoiceController(91L, List.of(2));

        List<Integer> firstChoices = List.of(first.choose(4), first.choose(5), first.choose(6));
        List<Integer> secondChoices = List.of(second.choose(4), second.choose(5), second.choose(6));

        // Both controllers share the seed and prefix, so their post-prefix random
        // choices must be identical.
        assertEquals(firstChoices, secondChoices);
    }
}
