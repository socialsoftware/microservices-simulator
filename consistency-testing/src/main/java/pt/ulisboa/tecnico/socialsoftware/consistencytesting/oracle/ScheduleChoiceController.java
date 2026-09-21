package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * One-run controller that replays a prefix while available, then uses seeded
 * random choices through the rest of the run.
 */
final class ScheduleChoiceController {

    private final Random random;
    private final List<Integer> choicePrefix;
    private final List<ScheduleDecision> decisions = new ArrayList<>();

    ScheduleChoiceController(long schedulerSeed, List<Integer> choicePrefix) {
        this.random = new Random(schedulerSeed);
        this.choicePrefix = List.copyOf(choicePrefix);
    }

    /**
     * Selects one ready-set position and records the decision for the run trace.
     */
    int choose(int readySetSize) {
        if (readySetSize < 1) {
            throw new IllegalArgumentException("readySetSize must be >= 1, got " + readySetSize);
        }

        int decisionIndex = decisions.size();
        int selectedIndex = decisionIndex < choicePrefix.size()
                ? normalizeReplayChoice(choicePrefix.get(decisionIndex), readySetSize)
                : random.nextInt(readySetSize);
        decisions.add(new ScheduleDecision(readySetSize, selectedIndex));
        return selectedIndex;
    }

    /**
     * Maps a replayed choice into the current ready set's valid index range.
     * <p>
     * The ready set can differ between runs because events, compensations, or
     * failures may add or remove executable steps. {@link Math#floorMod(int, int)}
     * wraps the replayed value into the circular range {@code 0..readySetSize - 1}:
     * {@code floorMod(5, 4) == 1}, {@code floorMod(-1, 4) == 3}, and
     * {@code floorMod(-2, 4) == 2}. Therefore every replayed value becomes a valid
     * index; {@code -1} means one position before {@code 0}, namely the last index.
     */
    private static int normalizeReplayChoice(int choice, int readySetSize) {
        return Math.floorMod(choice, readySetSize);
    }

    ScheduleTrace trace() {
        return new ScheduleTrace(decisions);
    }
}
