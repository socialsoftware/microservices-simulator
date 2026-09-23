package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
                runs, behaviors, featureRuns, findingFamilies);
    }
}
