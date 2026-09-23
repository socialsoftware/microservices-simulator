package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.BehavioralFingerprint;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator.AdaptiveGroupBudgetAllocator.BatchFeedback;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.TestDriver;

/** Group-local novelty state used for campaign budget decisions. */
final class GroupBudgetEvidence {

    private final Set<String> behaviors = new HashSet<>();
    private final Set<String> features = new HashSet<>();
    private final Set<String> findingFamilies = new HashSet<>();

    BatchFeedback observe(List<TestResult> results) {
        int newBehaviors = 0;
        int runsAddingFeatures = 0;
        int newFindingFamilies = 0;

        for (TestResult result : results) {
            if (result.statuses().contains(TestStatus.INTERDEPENDENCY_RESOLUTION_FAILED)
                    || result.statuses().contains(TestStatus.EXECUTION_LIMIT_EXCEEDED)) {
                continue; // incomplete or unresolvable schedules do not provide useful budget feedback
            }
            BehavioralFingerprint fingerprint = BehavioralFingerprint.from(result);
            if (behaviors.add(fingerprint.hash())) {
                newBehaviors++;
            }
            int previousFeatureCount = features.size();
            features.addAll(fingerprint.features());
            if (features.size() > previousFeatureCount) {
                runsAddingFeatures++;
            }
            if (TestDriver.isFinding(result)
                    && findingFamilies.add(findingFamilyOf(fingerprint))) {
                newFindingFamilies++;
            }
        }

        return new BatchFeedback(results.size(), newBehaviors, runsAddingFeatures, newFindingFamilies);
    }

    int uniqueFindingFamilies() {
        return findingFamilies.size();
    }

    /** Avoids rewarding concrete IDs or every repeated instance of one bug. */
    private static String findingFamilyOf(BehavioralFingerprint fingerprint) {
        // Keep status and finding outcome fields. Ignore step, effect, conflict, reads-from,
        // and semantic-lock features because they describe execution details that can vary
        // between occurrences of the same underlying bug.
        return fingerprint.features().stream()
                .filter(feature -> feature.startsWith("status|")
                        || feature.startsWith("inter-invariant-violation|")
                        || feature.startsWith("exception|"))
                .collect(Collectors.joining("\n"));
    }
}
