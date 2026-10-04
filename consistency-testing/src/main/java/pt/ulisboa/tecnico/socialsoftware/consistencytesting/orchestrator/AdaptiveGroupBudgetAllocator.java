package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

/**
 * Deterministic campaign-level allocation using either least-sampled balancing
 * or novelty reward with a UCB exploration bonus. Every group receives its
 * minimum before feedback can influence allocation.
 */
final class AdaptiveGroupBudgetAllocator {

    // Scales the UCB-style exploration bonus relative to observed mean reward.
    private static final double EXPLORATION_WEIGHT = 2.0;
    // Gives newly discovered bug families extra weight in the reward.
    private static final double NEW_FINDING_FAMILY_REWARD = 4.0;
    /** Number of recent allocation batches used for reads-from reward. */
    private static final int READS_FROM_RECENT_BATCHES = 3;

    record GroupKey(String catalog, String group) implements Comparable<GroupKey> {
        GroupKey {
            Objects.requireNonNull(catalog);
            Objects.requireNonNull(group);
        }

        @Override
        public int compareTo(GroupKey other) {
            int catalogOrder = catalog.compareTo(other.catalog);
            return catalogOrder != 0 ? catalogOrder : group.compareTo(other.group);
        }
    }

    enum Phase {
        WARMUP,
        ADAPTIVE
    }

    record Allocation(GroupKey group, int requestedRuns, Phase phase, double priorityScore) {
    }

    record BatchFeedback(
            int completedRuns,
            int newBehaviors,
            int runsAddingFeatures,
            int newFindingFamilies,
            int newReadsFromRelations) {
        BatchFeedback {
            if (completedRuns < 1
                    || newBehaviors < 0 || newBehaviors > completedRuns
                    || runsAddingFeatures < 0 || runsAddingFeatures > completedRuns
                    || newFindingFamilies < 0 || newFindingFamilies > completedRuns
                    || newReadsFromRelations < 0) {
                throw new IllegalArgumentException("Invalid adaptive budget batch feedback");
            }
        }

        /** Sum of newly observed novelty signals for this batch, with extra weight for new finding families. */
        private double noveltyReward() {
            return newBehaviors + runsAddingFeatures
                    + NEW_FINDING_FAMILY_REWARD * newFindingFamilies;
        }

        double reward(GroupBudgetStrategy strategy) {
            return switch (strategy) {
                case ADAPTIVE_READS_FROM -> newReadsFromRelations;
                case BALANCED_REDISTRIBUTION -> 0.0;
                case ADAPTIVE_NOVELTY, ADAPTIVE_SIGNALS -> noveltyReward();
                case FIXED_PER_GROUP -> throw new IllegalArgumentException(
                        "Fixed per-group budgeting does not use allocation feedback");
            };
        }
    }

    private static final class GroupState {
        /** Completed runs observed for this group so far. */
        private int runs;
        private double reward;
        private boolean stopped;
        /** Recent feedback retained for reads-from reward. Deque supports append and oldest-first removal. */
        private final Deque<BatchFeedback> recent = new ArrayDeque<>();

        /**
         * Scores a group by its observed average reward plus a bonus for limited sampling.
         * The bonus shrinks as this group gets more runs and grows as the campaign explores more runs.
         *
         * @param campaignRuns number of completed runs across all groups
         * @return a larger score for groups with more reward or less exploration so far
         */
        private double priorityScore(int campaignRuns, GroupBudgetStrategy strategy) {
            if (strategy == GroupBudgetStrategy.BALANCED_REDISTRIBUTION) {
                return -runs; // Serve the least-sampled active group; randomize ties.
            }
            if (runs == 0) {
                // Ensure every group gets its warm-up allocation before adaptive selection.
                return Double.POSITIVE_INFINITY;
            }
            double meanReward = strategy == GroupBudgetStrategy.ADAPTIVE_READS_FROM
                    ? (double) recent.stream().mapToInt(BatchFeedback::newReadsFromRelations).sum()
                            / recent.stream().mapToInt(BatchFeedback::completedRuns).sum()
                    : reward / runs;
            // This bonus favors groups with fewer runs, so they keep a chance to reveal useful behavior.
            double exploration = EXPLORATION_WEIGHT
                    * Math.sqrt(Math.log(Math.max(2, campaignRuns)) / runs);
            return meanReward + exploration;
        }
    }

    private final int totalRunBudget;
    private final int minimumRunsPerGroup;
    private final int maximumRunsPerGroup;
    private final int batchSize;
    private final GroupBudgetStrategy strategy;
    private final Map<GroupKey, GroupState> states;
    private final List<GroupKey> warmupOrder;
    private final Random tieBreaker;
    private int warmupIndex;
    private int completedRuns;
    // Enforces the request -> execute -> observe protocol before another allocation is issued.
    private @Nullable Allocation pending;

    AdaptiveGroupBudgetAllocator(
            List<GroupKey> groups,
            int totalRunBudget,
            int minimumRunsPerGroup,
            int maximumRunsPerGroup,
            int batchSize,
            long seed) {

        this(groups, totalRunBudget, minimumRunsPerGroup, maximumRunsPerGroup,
                batchSize, seed, GroupBudgetStrategy.ADAPTIVE_NOVELTY);
    }

    AdaptiveGroupBudgetAllocator(
            List<GroupKey> groups,
            int totalRunBudget,
            int minimumRunsPerGroup,
            int maximumRunsPerGroup,
            int batchSize,
            long seed,
            GroupBudgetStrategy strategy) {

        if (!Objects.requireNonNull(strategy).sharesCampaignBudget()) {
            throw new IllegalArgumentException("Allocator requires a shared-budget strategy");
        }
        if (groups.isEmpty()) {
            throw new IllegalArgumentException("Adaptive group budgeting requires at least one group");
        }
        if (minimumRunsPerGroup < 1) {
            throw new IllegalArgumentException("minimumRunsPerGroup must be >= 1");
        }
        if (maximumRunsPerGroup < minimumRunsPerGroup) {
            throw new IllegalArgumentException("maximumRunsPerGroup must be >= minimumRunsPerGroup");
        }
        if (batchSize < 1) {
            throw new IllegalArgumentException("batchSize must be >= 1");
        }

        List<GroupKey> distinctGroups = groups.stream().distinct().sorted().toList();
        if (distinctGroups.size() != groups.size()) {
            throw new IllegalArgumentException("Adaptive group keys must be unique");
        }
        long minimumBudget = (long) distinctGroups.size() * minimumRunsPerGroup;
        long maximumBudget = (long) distinctGroups.size() * maximumRunsPerGroup;
        if (totalRunBudget < minimumBudget || totalRunBudget > maximumBudget) {
            throw new IllegalArgumentException(
                    "totalRunBudget must be between %d and %d for %d group(s), got %d"
                            .formatted(minimumBudget, maximumBudget, distinctGroups.size(), totalRunBudget));
        }

        this.totalRunBudget = totalRunBudget;
        this.minimumRunsPerGroup = minimumRunsPerGroup;
        this.maximumRunsPerGroup = maximumRunsPerGroup;
        this.batchSize = batchSize;
        this.strategy = strategy;
        this.warmupOrder = distinctGroups;
        this.states = new LinkedHashMap<>();
        distinctGroups.forEach(group -> states.put(group, new GroupState()));
        this.tieBreaker = new Random(seed);
    }

    boolean hasNext() {
        return completedRuns < totalRunBudget && states.values().stream()
                .anyMatch(state -> !state.stopped && state.runs < maximumRunsPerGroup);
    }

    Allocation nextAllocation() {
        if (pending != null) {
            throw new IllegalStateException("Previous allocation has not been observed");
        }
        if (!hasNext()) {
            throw new IllegalStateException("Adaptive group budget is exhausted");
        }

        // If there are still groups to warm up, allocate their minimum runs.
        if (warmupIndex < warmupOrder.size()) {
            GroupKey group = warmupOrder.get(warmupIndex++);
            pending = new Allocation(group, minimumRunsPerGroup, Phase.WARMUP, 0.0);
            return pending;
        }

        int remainingBudget = totalRunBudget - completedRuns;
        List<GroupKey> eligible = states.entrySet().stream()
                .filter(entry -> !entry.getValue().stopped && entry.getValue().runs < maximumRunsPerGroup)
                .map(Map.Entry::getKey)
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toCollection(ArrayList::new));
        if (eligible.isEmpty()) {
            throw new IllegalStateException("No group can consume remaining adaptive budget");
        }

        // Shuffle first so equal scores don't always favor the same group. The seeded
        // shuffle makes ties reproducible; max selects the first highest-scoring group.
        Collections.shuffle(eligible, tieBreaker);
        GroupKey selected = eligible.stream()
                .max(Comparator.comparingDouble(group -> states.get(group).priorityScore(completedRuns, strategy)))
                .orElseThrow();
        GroupState selectedState = states.get(selected);

        // Cap each allocation by batch size, remaining campaign budget, and this group's remaining allowance.
        int requestedRuns = Math.min(batchSize,
                Math.min(remainingBudget, maximumRunsPerGroup - selectedState.runs));
        pending = new Allocation(
                selected, requestedRuns, Phase.ADAPTIVE, selectedState.priorityScore(completedRuns, strategy));
        return pending;
    }

    /** Adds feedback to the selected group's totals and makes the allocator ready for its next decision. */
    void observe(Allocation allocation, BatchFeedback feedback) {
        observe(allocation, feedback, false);
    }

    /** Records feedback for the pending allocation; a stopped group may complete fewer runs and is retired. */
    void observe(Allocation allocation, BatchFeedback feedback, boolean stopped) {
        if (pending == null || !pending.equals(allocation)) {
            throw new IllegalArgumentException("Feedback does not match pending allocation");
        }
        if (feedback.completedRuns() > allocation.requestedRuns()) {
            throw new IllegalArgumentException("Completed runs exceed requested allocation");
        }
        if (!stopped && feedback.completedRuns() != allocation.requestedRuns()) {
            throw new IllegalArgumentException("Incomplete allocation requires a stopped group");
        }
        GroupState state = states.get(allocation.group());
        state.runs += feedback.completedRuns();
        state.reward += feedback.reward(strategy);
        state.recent.addLast(feedback);
        if (state.recent.size() > READS_FROM_RECENT_BATCHES) {
            state.recent.removeFirst();
        }
        state.stopped = stopped;
        completedRuns += feedback.completedRuns();
        pending = null;
    }

    int completedRuns(GroupKey group) {
        return states.get(group).runs;
    }

    double priorityScore(GroupKey group) {
        return states.get(group).priorityScore(completedRuns, strategy);
    }
}
