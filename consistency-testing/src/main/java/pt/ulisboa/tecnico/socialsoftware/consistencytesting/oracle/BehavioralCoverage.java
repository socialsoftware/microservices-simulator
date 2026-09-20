package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Group-local normalized behavioral diversity over an ordered sequence of runs.
 * <p>
 * A behavior is one complete {@link BehavioralFingerprint}; a feature is one
 * normalized observation contributing to that fingerprint. Therefore behavior
 * counts answer "how many distinct run outcomes?", while feature counts answer
 * "how many distinct observations contributed across those outcomes?".
 */
public record BehavioralCoverage(
        /** Schema version used to interpret the fingerprint and its features. */
        String fingerprintSchema,
        int runs,
        /** Number of distinct behavioral fingerprints observed. */
        int uniqueBehaviors,
        /** Number of runs whose fingerprint duplicated an earlier run. */
        int duplicateRuns,
        /** Unique behaviors divided by total runs, in the range {@code 0..1}. */
        double discoveryRate,
        /** Number of distinct normalized features observed across all runs. */
        int uniqueFeatures,
        /** Number of runs that introduced at least one new feature. */
        int runsAddingFeatures,
        /** Runs adding features divided by total runs, in the range {@code 0..1}. */
        double featureDiscoveryRate,
        /** Unique behavior count after each run, in execution order. */
        List<Integer> cumulativeUniqueBehaviors,
        /** Unique feature count after each run, in execution order. */
        List<Integer> cumulativeUniqueFeatures,
        /** Number of features introduced by each run, in execution order. */
        List<Integer> newFeaturesPerRun) {

    public BehavioralCoverage {
        Objects.requireNonNull(fingerprintSchema);
        cumulativeUniqueBehaviors = List.copyOf(cumulativeUniqueBehaviors);
        if (runs < 0 || uniqueBehaviors < 0 || uniqueBehaviors > runs || duplicateRuns != runs - uniqueBehaviors) {
            throw new IllegalArgumentException("Invalid behavioral coverage counts");
        }
        double expectedRate = runs == 0 ? 0.0 : (double) uniqueBehaviors / runs;
        if (Double.compare(discoveryRate, expectedRate) != 0) {
            throw new IllegalArgumentException("Discovery rate does not match behavioral coverage counts");
        }
        if (uniqueFeatures < 0 || runsAddingFeatures < 0 || runsAddingFeatures > runs) {
            throw new IllegalArgumentException("Invalid behavioral feature coverage counts");
        }
        double expectedFeatureRate = runs == 0 ? 0.0 : (double) runsAddingFeatures / runs;
        if (Double.compare(featureDiscoveryRate, expectedFeatureRate) != 0) {
            throw new IllegalArgumentException("Feature discovery rate does not match coverage counts");
        }
        if (cumulativeUniqueBehaviors.size() != runs) {
            throw new IllegalArgumentException("Behavior coverage curve must contain one point per run");
        }
        int previous = 0;
        for (int uniqueAtRun : cumulativeUniqueBehaviors) {
            if (uniqueAtRun < previous || uniqueAtRun > previous + 1) {
                throw new IllegalArgumentException("Coverage curve must be monotonic and grow by at most one per run");
            }
            previous = uniqueAtRun;
        }
        if (previous != uniqueBehaviors) {
            throw new IllegalArgumentException("Behavior coverage curve endpoint does not match unique count");
        }
        if (cumulativeUniqueFeatures.size() != runs || newFeaturesPerRun.size() != runs) {
            throw new IllegalArgumentException("Feature coverage curves must contain one point per run");
        }
        int previousFeatures = 0;
        int observedRunsAddingFeatures = 0;
        for (int run = 0; run < runs; run++) {
            int added = newFeaturesPerRun.get(run);
            int cumulative = cumulativeUniqueFeatures.get(run);
            if (added < 0 || cumulative != previousFeatures + added) {
                throw new IllegalArgumentException("Feature coverage curve must equal cumulative per-run additions");
            }
            if (added > 0) {
                observedRunsAddingFeatures++;
            }
            previousFeatures = cumulative;
        }
        if (previousFeatures != uniqueFeatures || observedRunsAddingFeatures != runsAddingFeatures) {
            throw new IllegalArgumentException("Feature coverage curves do not match summary counts");
        }
    }

    public static BehavioralCoverage from(List<TestResult> results) {
        Set<String> observedBehaviors = new HashSet<>();
        Set<String> observedFeatures = new HashSet<>();
        // Cumulative unique behavior count after each run, in run order.
        List<Integer> cumulativeUniqueBehaviors = new ArrayList<>();
        // Cumulative unique feature count after each run, in run order.
        List<Integer> cumulativeUniqueFeatures = new ArrayList<>();
        List<Integer> featureAdditions = new ArrayList<>();
        int runsAddingFeatures = 0;

        for (TestResult result : results) {
            BehavioralFingerprint fingerprint = BehavioralFingerprint.from(result);
            observedBehaviors.add(fingerprint.hash());
            cumulativeUniqueBehaviors.add(observedBehaviors.size());

            int previousFeatureCount = observedFeatures.size();
            observedFeatures.addAll(fingerprint.features());
            int addedFeatures = observedFeatures.size() - previousFeatureCount;
            if (addedFeatures > 0) {
                runsAddingFeatures++;
            }
            featureAdditions.add(addedFeatures);
            cumulativeUniqueFeatures.add(observedFeatures.size());
        }

        int runs = results.size();
        int unique = observedBehaviors.size();
        return new BehavioralCoverage(
                BehavioralFingerprint.SCHEMA,
                runs,
                unique,
                runs - unique,
                runs == 0 ? 0.0 : (double) unique / runs,
                observedFeatures.size(),
                runsAddingFeatures,
                runs == 0 ? 0.0 : (double) runsAddingFeatures / runs,
                cumulativeUniqueBehaviors,
                cumulativeUniqueFeatures,
                featureAdditions);
    }
}
