package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * One-run controller that replays a prefix, then prefers a reads-from target
 * when possible or chooses a ready step with seeded randomness.
 */
final class ScheduleChoiceController {

    private final Random random;
    private final List<Integer> choicePrefix;
    private final @Nullable ReadsFromTarget readsFromTarget;
    private final List<ScheduleDecision> decisions = new ArrayList<>();

    ScheduleChoiceController(
            long schedulerSeed, List<Integer> choicePrefix, @Nullable ReadsFromTarget readsFromTarget) {

        this.random = new Random(schedulerSeed);
        this.choicePrefix = List.copyOf(choicePrefix);
        this.readsFromTarget = readsFromTarget;
    }

    /**
     * Selects a ready step. A replay prefix takes precedence; afterward, a
     * configured reads-from target can prefer its writer before its reader.
     * With no usable preference, selection uses seeded randomness.
     *
     * @param readySteps current executable steps, in ready-set order
     * @param effects effects recorded so far in this run
     * @param executed steps already executed in this run
     * @return chosen index from {@code readySteps}
     */
    int choose(List<StepId> readySteps, List<StepEffect> effects, Set<StepId> executed) {
        @Nullable Integer preferred = null;
        if (readsFromTarget != null && !executed.contains(readsFromTarget.reader())) {
            int writer = readySteps.indexOf(readsFromTarget.writer());
            int reader = readySteps.indexOf(readsFromTarget.reader());
            if (!executed.contains(readsFromTarget.writer())) {
                if (writer >= 0) {
                    // Run the target writer as soon as it becomes ready.
                    preferred = writer;
                } else if (reader >= 0 && readySteps.size() > 1) {
                    // Let other ready steps run while the target writer is not ready.
                    preferred = chooseOtherReady(readySteps.size(), reader);
                }
            } else if (reader >= 0 && effects.stream().anyMatch(effect ->
                    effect.stepId().equals(readsFromTarget.writer()) && effect.isWrite()
                            && effect.aggregateType().equals(readsFromTarget.aggregateType()))) {
                // Run the target reader as soon as it becomes ready after/if the target read has been produced.
                preferred = reader;
            }
        }
        return recordChoice(readySteps.size(), preferred);
    }

    /** Draw uniformly from every ready index except the reader's index. */
    private int chooseOtherReady(int readySetSize, int avoided) {
        // Shift sampled indexes at or beyond the omitted slot up by one to skip it.
        int choice = random.nextInt(readySetSize - 1);
        return choice >= avoided ? choice + 1 : choice;
    }

    private int recordChoice(int readySetSize, @Nullable Integer preferred) {
        if (readySetSize < 1) {
            throw new IllegalArgumentException("readySetSize must be >= 1, got " + readySetSize);
        }

        int decisionIndex = decisions.size();
        int selectedIndex = decisionIndex < choicePrefix.size()
                ? normalizeReplayChoice(choicePrefix.get(decisionIndex), readySetSize)
                : preferred != null ? preferred : random.nextInt(readySetSize);
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
