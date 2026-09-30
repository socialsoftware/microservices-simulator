package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ReadsFromRelation;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ReadsFromTarget;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepEffect;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;

/** Selects group-local reads-from targets from observed aggregate accesses. */
final class ReadsFromGuidance {
    static final double RANDOM_SCHEDULE_PROBABILITY = 0.20;
    private static final int MAX_UNOBSERVED_ATTEMPTS = 3;

    private final Set<ReadsFromTarget> candidates = new HashSet<>();
    private final Map<ReadsFromTarget, Integer> observedRuns = new HashMap<>();
    private final Map<ReadsFromTarget, Integer> attempts = new HashMap<>();

    /** Returns a target to seek, or null to run an unconstrained random schedule. */
    @Nullable ReadsFromTarget nextTarget(Random random) {
        List<ReadsFromTarget> available = candidates.stream()
                .filter(target -> observedRuns.getOrDefault(target, 0) > 0
                        || attempts.getOrDefault(target, 0) < MAX_UNOBSERVED_ATTEMPTS)
                .toList();

        if (available.isEmpty() || random.nextDouble() < RANDOM_SCHEDULE_PROBABILITY) {
            return null; // use random ready-step selection
        }

        ReadsFromTarget target = chooseLowestPriorityTarget(available, random);
        attempts.merge(target, 1, Integer::sum);
        return target;
    }

    /** Chooses randomly among targets with the lowest exploration score. */
    private ReadsFromTarget chooseLowestPriorityTarget(List<ReadsFromTarget> available, Random random) {
        int minimum = available.stream().mapToInt(this::priority).min().orElseThrow();
        List<ReadsFromTarget> leastExplored = available.stream()
                .filter(target -> priority(target) == minimum)
                .sorted(Comparator.comparing((ReadsFromTarget target) -> target.writer().toString())
                        .thenComparing(target -> target.reader().toString())
                        .thenComparing(ReadsFromTarget::aggregateType))
                .toList();
        return leastExplored.get(random.nextInt(leastExplored.size()));
    }

    /** Learns same-object cross-saga targets and counts relations actually observed in a conclusive run. */
    void observe(TestResult result) {
        if (result.statuses().contains(TestStatus.INTERDEPENDENCY_RESOLUTION_FAILED)
                || result.statuses().contains(TestStatus.EXECUTION_LIMIT_EXCEEDED)) {
            return; // skip inconclusive runs
        }
        Map<ObjectKey, Accesses> accessesByObject = new HashMap<>();
        for (StepEffect effect : result.effectSequence()) {
            if (effect.aggregateId() == null || !effect.stepId().isIdentityStableAcrossRuns()) {
                continue;
            }
            Accesses accesses = accessesByObject.computeIfAbsent(
                    new ObjectKey(effect.aggregateType(), effect.aggregateId()), ignored -> new Accesses());
            (effect.isWrite() ? accesses.writers : accesses.readers).add(effect.stepId());
        }
        // Each observed writer/reader of one object is a possible future reads-from
        // target; only cross-saga pairs can be reordered by this scheduler.
        for (Map.Entry<ObjectKey, Accesses> entry : accessesByObject.entrySet()) {
            for (StepId writer : entry.getValue().writers) {
                for (StepId reader : entry.getValue().readers) {
                    if (!writer.getFunctionalityId().equals(reader.getFunctionalityId())) {
                        candidates.add(new ReadsFromTarget(writer, reader, entry.getKey().aggregateType()));
                    }
                }
            }
        }
        for (ReadsFromRelation relation : result.readsFromRelations()) {
            ReadsFromTarget target = new ReadsFromTarget(
                    relation.writer(), relation.reader(), relation.aggregateType());
            if (candidates.contains(target)) {
                observedRuns.merge(target, 1, Integer::sum);
            }
        }
    }

    /** Lower scores get priority; observed successes cost more than failed-attempt. */
    private int priority(ReadsFromTarget target) {
        return 3 * observedRuns.getOrDefault(target, 0) + attempts.getOrDefault(target, 0);
    }

    private record ObjectKey(String aggregateType, Integer aggregateId) {
    }

    private static final class Accesses {
        private final Set<StepId> writers = new HashSet<>();
        private final Set<StepId> readers = new HashSet<>();
    }
}
