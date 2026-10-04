package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.NoveltyMetric;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ReadsFromTarget;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator.AdaptiveGroupBudgetAllocator.BatchFeedback;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.TestDriver;

/** Group-local novelty state used for campaign budget decisions. */
final class GroupBudgetEvidence {

    private final NoveltyMetric noveltyMetric;
    private final Set<String> behaviors = new HashSet<>();
    private final Set<String> features = new HashSet<>();
    private final Set<String> findingFamilies = new HashSet<>();
    private final Set<ReadsFromTarget> readsFrom = new HashSet<>();

    GroupBudgetEvidence(GroupBudgetStrategy strategy) {
        this.noveltyMetric = strategy.noveltyMetric();
    }

    BatchFeedback observe(List<TestResult> results) {
        int newBehaviors = 0;
        int runsAddingFeatures = 0;
        int newFindingFamilies = 0;
        int previousReadsFromCount = readsFrom.size();

        for (TestResult result : results) {
            if (result.statuses().contains(TestStatus.INTERDEPENDENCY_RESOLUTION_FAILED)
                    || result.statuses().contains(TestStatus.EXECUTION_LIMIT_EXCEEDED)
                    || result.hasOnlySemanticLockGuardRejections()) {
                // Incomplete or unresolvable schedules and semantic-lock guard rejections
                // do NOT provide useful budget feedback
                continue;
            }
            NoveltyMetric.Observation novelty = noveltyMetric.measure(result);
            recordReadsFromTargets(result);
            if (behaviors.add(novelty.hash())) {
                newBehaviors++;
            }
            int previousFeatureCount = features.size();
            features.addAll(novelty.features());
            if (features.size() > previousFeatureCount) {
                runsAddingFeatures++;
            }
            if (TestDriver.isFinding(result)
                    && findingFamilies.add(findingFamilyOf(novelty.features()))) {
                newFindingFamilies++;
            }
        }

        return new BatchFeedback(results.size(), newBehaviors, runsAddingFeatures,
                newFindingFamilies, readsFrom.size() - previousReadsFromCount);
    }

    /** Records stable cross-saga reads-from targets, excluding initial-state writes. */
    private void recordReadsFromTargets(TestResult result) {
        result.readsFromRelations().stream()
                .filter(relation -> relation.writer().isIdentityStableAcrossRuns()
                        && relation.reader().isIdentityStableAcrossRuns()
                        && !relation.writer().equals(StepId.forInitialStateSetupStep())
                        && !relation.writer().getFunctionalityId().equals(
                                relation.reader().getFunctionalityId()))
                .map(relation -> new ReadsFromTarget(
                        relation.writer(), relation.reader(), relation.aggregateType()))
                .forEach(readsFrom::add);
    }

    int uniqueFindingFamilies() {
        return findingFamilies.size();
    }

    /** Avoids rewarding concrete IDs or every repeated instance of one bug. */
    private static String findingFamilyOf(List<String> features) {
        // Keep status and finding outcome fields. Ignore step, effect, conflict,
        // reads-from, and semantic-lock features because they describe execution
        // details that can vary between occurrences of the same underlying bug.
        return features.stream()
                .filter(feature -> feature.startsWith("status|")
                        || feature.startsWith("inter-invariant-violation|")
                        || feature.startsWith("exception|"))
                .collect(Collectors.joining("\n"));
    }
}
