package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

class AdaptiveGroupBudgetAllocatorTest {

    private static final AdaptiveGroupBudgetAllocator.GroupKey A =
            new AdaptiveGroupBudgetAllocator.GroupKey("catalog", "a");
    private static final AdaptiveGroupBudgetAllocator.GroupKey B =
            new AdaptiveGroupBudgetAllocator.GroupKey("catalog", "b");

    @Test
    void warmsEveryGroupBeforeUsingFeedback() {
        AdaptiveGroupBudgetAllocator allocator = allocator(12, 2, 8, 2);

        AdaptiveGroupBudgetAllocator.Allocation first = allocator.nextAllocation();
        assertEquals(A, first.group());
        assertEquals(AdaptiveGroupBudgetAllocator.Phase.WARMUP, first.phase());
        allocator.observe(first, feedback(2, 2, 2, 0));

        AdaptiveGroupBudgetAllocator.Allocation second = allocator.nextAllocation();
        assertEquals(B, second.group());
        assertEquals(AdaptiveGroupBudgetAllocator.Phase.WARMUP, second.phase());

        allocator.observe(second, feedback(2, 0, 0, 0));
        assertEquals(AdaptiveGroupBudgetAllocator.Phase.ADAPTIVE, allocator.nextAllocation().phase());
    }

    @Test
    void rewardsNovelGroupButStillHonorsExactBudgetAndCaps() {
        AdaptiveGroupBudgetAllocator allocator = allocator(12, 2, 8, 2);
        List<AdaptiveGroupBudgetAllocator.GroupKey> adaptiveSelections = new ArrayList<>();

        while (allocator.hasNext()) {
            AdaptiveGroupBudgetAllocator.Allocation allocation = allocator.nextAllocation();
            if (allocation.phase() == AdaptiveGroupBudgetAllocator.Phase.ADAPTIVE) {
                adaptiveSelections.add(allocation.group());
            }
            boolean novelGroup = allocation.group().equals(A);
            allocator.observe(allocation, feedback(
                    allocation.requestedRuns(),
                    novelGroup ? allocation.requestedRuns() : 0,
                    novelGroup ? allocation.requestedRuns() : 0,
                    0));
        }

        assertEquals(A, adaptiveSelections.getFirst());
        assertEquals(8, allocator.completedRuns(A));
        assertEquals(4, allocator.completedRuns(B));
    }

    @Test
    void sameSeedAndFeedbackProduceSameAllocationSequence() {
        assertEquals(runSequence(42L), runSequence(42L));
    }

    @Test
    void rejectsBudgetThatCannotGiveEveryGroupItsMinimum() {
        assertThrows(IllegalArgumentException.class,
                () -> allocator(3, 2, 8, 2));
    }

    @Test
    void requiresFeedbackBeforeAnotherAllocation() {
        AdaptiveGroupBudgetAllocator allocator = allocator(8, 2, 6, 2);
        allocator.nextAllocation();

        assertThrows(IllegalStateException.class, allocator::nextAllocation);
    }

    @Test
    void stoppedGroupReleasesUnusedRunsToAnActiveGroup() {
        AdaptiveGroupBudgetAllocator allocator = allocator(9, 2, 8, 2);

        AdaptiveGroupBudgetAllocator.Allocation first = allocator.nextAllocation();
        assertEquals(A, first.group());
        allocator.observe(first, feedback(1, 1, 1, 1), true);

        while (allocator.hasNext()) {
            AdaptiveGroupBudgetAllocator.Allocation allocation = allocator.nextAllocation();
            assertEquals(B, allocation.group());
            allocator.observe(allocation, feedback(allocation.requestedRuns(), 0, 0, 0));
        }

        assertEquals(1, allocator.completedRuns(A));
        assertEquals(8, allocator.completedRuns(B));
    }

    @Test
    void stopsCampaignWhenEveryGroupHasAnActionableFinding() {
        AdaptiveGroupBudgetAllocator allocator = allocator(8, 2, 6, 2);
        for (int index = 0; index < 2; index++) {
            AdaptiveGroupBudgetAllocator.Allocation allocation = allocator.nextAllocation();
            allocator.observe(allocation, feedback(1, 1, 1, 1), true);
        }
        assertFalse(allocator.hasNext());
    }

    @Test
    void readsFromStrategyRewardsRelationsRatherThanFingerprintNovelty() {
        AdaptiveGroupBudgetAllocator allocator = new AdaptiveGroupBudgetAllocator(
                List.of(A, B), 12, 2, 12, 2, 42L, GroupBudgetStrategy.ADAPTIVE_READS_FROM);
        allocator.observe(allocator.nextAllocation(), new AdaptiveGroupBudgetAllocator.BatchFeedback(2, 2, 2, 1, 0));
        allocator.observe(allocator.nextAllocation(), new AdaptiveGroupBudgetAllocator.BatchFeedback(2, 0, 0, 0, 2));
        assertEquals(B, allocator.nextAllocation().group());
    }

    @Test
    void readsFromRewardForgetsAnOldBurstAfterThreeBatches() {
        AdaptiveGroupBudgetAllocator allocator = new AdaptiveGroupBudgetAllocator(
                List.of(A), 20, 2, 20, 2, 42L, GroupBudgetStrategy.ADAPTIVE_READS_FROM);
        allocator.observe(allocator.nextAllocation(), new AdaptiveGroupBudgetAllocator.BatchFeedback(2, 0, 0, 0, 100));
        for (int batch = 0; batch < 3; batch++) {
            allocator.observe(allocator.nextAllocation(), feedback(2, 0, 0, 0));
        }
        // The three zero-relation batches evict the initial burst from the recent reward window;
        // the remaining priority score is the UCB exploration bonus.
        assertTrue(allocator.priorityScore(A) < 2.0);
    }

    @Test
    void balancedRedistributionIgnoresNoveltyAndUsesExactOddBudget() {
        AdaptiveGroupBudgetAllocator allocator = new AdaptiveGroupBudgetAllocator(
                List.of(A, B), 11, 2, 11, 1, 42L, GroupBudgetStrategy.BALANCED_REDISTRIBUTION);
        while (allocator.hasNext()) {
            var allocation = allocator.nextAllocation();
            boolean novel = allocation.group().equals(A);
            allocator.observe(allocation, new AdaptiveGroupBudgetAllocator.BatchFeedback(
                    allocation.requestedRuns(), novel ? allocation.requestedRuns() : 0, 0, 0, novel ? 100 : 0));
        }
        assertEquals(11, allocator.completedRuns(A) + allocator.completedRuns(B));
        assertTrue(Math.abs(allocator.completedRuns(A) - allocator.completedRuns(B)) <= 1);
    }

    @Test
    void readsFromPriorityMovesToNewEvidenceAfterOldEvidenceDecays() {
        AdaptiveGroupBudgetAllocator allocator = new AdaptiveGroupBudgetAllocator(
                List.of(A, B), 20, 2, 20, 2, 42L, GroupBudgetStrategy.ADAPTIVE_READS_FROM);
        allocator.observe(allocator.nextAllocation(), new AdaptiveGroupBudgetAllocator.BatchFeedback(2, 0, 0, 0, 12));
        allocator.observe(allocator.nextAllocation(), feedback(2, 0, 0, 0));
        for (int batch = 0; batch < 3; batch++) {
            var allocation = allocator.nextAllocation();
            assertEquals(A, allocation.group());
            allocator.observe(allocation, feedback(2, 0, 0, 0));
        }
        var newEvidence = allocator.nextAllocation();
        assertEquals(B, newEvidence.group());
        allocator.observe(newEvidence, new AdaptiveGroupBudgetAllocator.BatchFeedback(2, 0, 0, 0, 8));
        assertEquals(B, allocator.nextAllocation().group());
    }

    @Test
    void activeGroupConsumesRemainingBudgetAfterAnotherStops() {
        AdaptiveGroupBudgetAllocator allocator = new AdaptiveGroupBudgetAllocator(
                List.of(A, B), 100, 2, 100, 2, 42L, GroupBudgetStrategy.ADAPTIVE_READS_FROM);
        allocator.observe(allocator.nextAllocation(), feedback(1, 0, 0, 0), true);
        while (allocator.hasNext()) {
            var allocation = allocator.nextAllocation();
            assertEquals(B, allocation.group());
            allocator.observe(allocation, feedback(allocation.requestedRuns(), 0, 0, 0));
        }
        assertEquals(99, allocator.completedRuns(B));
    }

    private static List<AdaptiveGroupBudgetAllocator.GroupKey> runSequence(long seed) {
        AdaptiveGroupBudgetAllocator allocator = new AdaptiveGroupBudgetAllocator(
                List.of(A, B), 12, 2, 8, 2, seed);
        List<AdaptiveGroupBudgetAllocator.GroupKey> sequence = new ArrayList<>();
        while (allocator.hasNext()) {
            AdaptiveGroupBudgetAllocator.Allocation allocation = allocator.nextAllocation();
            sequence.add(allocation.group());
            allocator.observe(allocation, feedback(allocation.requestedRuns(), 1, 1, 0));
        }
        return sequence;
    }

    private static AdaptiveGroupBudgetAllocator allocator(
            int total, int minimum, int maximum, int batch) {

        return new AdaptiveGroupBudgetAllocator(
                List.of(B, A), total, minimum, maximum, batch, 42L);
    }

    private static AdaptiveGroupBudgetAllocator.BatchFeedback feedback(
            int runs, int behaviors, int featureRuns, int findingFamilies) {

        return new AdaptiveGroupBudgetAllocator.BatchFeedback(
                runs, behaviors, featureRuns, findingFamilies, 0);
    }
}
