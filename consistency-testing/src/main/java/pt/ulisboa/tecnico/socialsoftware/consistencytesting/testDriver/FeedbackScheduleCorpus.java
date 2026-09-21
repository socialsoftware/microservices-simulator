package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.BehavioralFingerprint;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ScheduleDecision;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ScheduleTrace;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;

/** Group-local corpus for feedback-guided schedule choice mutation. */
final class FeedbackScheduleCorpus {

    /**
     * Controls exploration vs exploitation by preventing convergence on corpus
     * descendants only.
     */
    static final double RANDOM_SCHEDULE_PROBABILITY = 0.20;
    /** Small bounded mutation keeps one child near its parent and limits run-plan noise. */
    private static final int MAX_MUTATIONS_PER_RUN = 3;
    /** Cap feature contribution so feature-rich traces do not monopolize selection. */
    private static final int MAX_FEATURE_ENERGY = 8;
    /** Minimum selection weight for every admitted trace. */
    private static final int BASE_ENERGY = 1;
    /** Extra selection weight awarded when a trace produces a new fingerprint. */
    private static final int NEW_BEHAVIOR_BONUS = 2;

    private final Set<String> observedFingerprintHashes = new HashSet<>();
    private final Set<String> observedFeatures = new HashSet<>();
    private final List<Entry> entries = new ArrayList<>();

    Plan nextPlan(Random random) {
        List<Entry> mutableEntries = entries.stream().filter(entry -> hasMutableChoice(entry.trace)).toList();
        if (mutableEntries.isEmpty() || random.nextDouble() < RANDOM_SCHEDULE_PROBABILITY) {
            return Plan.randomSchedule();
        }

        Entry parent = weightedChoice(mutableEntries, random);
        parent.selections++; // Count parent reuse so its selection weight decays.

        List<Integer> prefix = new ArrayList<>(parent.trace.choicePrefix());
        List<Integer> mutablePositions = mutablePositions(parent.trace);
        // nextInt(max) yields 0..max-1, so adding 1 yields exactly 1..max.
        int mutationCount = 1 + random.nextInt(Math.min(MAX_MUTATIONS_PER_RUN, mutablePositions.size()));

        for (int mutation = 0; mutation < mutationCount; mutation++) {
            int positionIndex = random.nextInt(mutablePositions.size());
            int decisionPosition = mutablePositions.remove(positionIndex);
            ScheduleDecision decision = parent.trace.decisions().get(decisionPosition);

            // Choose a replacement from all ready-set indexes except the current one.
            // For indexes [0, 1, 2, 3] with current index 1, nextInt(N - 1)
            // draws compact values [0, 1, 2]. We map them to [0, 2, 3] by
            // leaving values below 1 unchanged and shifting values at/above 1 by one.
            int replacement = random.nextInt(decision.readySetSize() - 1);
            if (replacement >= decision.selectedIndex()) {
                replacement++; // Skip the original index while preserving the range (shift by one).
            }

            prefix.set(decisionPosition, replacement);
        }

        return new Plan(prefix, parent.fingerprintHash, mutationCount);
    }

    Observation observe(TestResult result) {
        if (result.statuses().contains(TestStatus.INTERDEPENDENCY_RESOLUTION_FAILED)) {
            return new Observation(false, false, 0, false, entries.size());
        }

        BehavioralFingerprint fingerprint = BehavioralFingerprint.from(result);
        boolean newBehavior = observedFingerprintHashes.add(fingerprint.hash());
        int previousFeatureCount = observedFeatures.size();
        observedFeatures.addAll(fingerprint.features());
        int newFeatures = observedFeatures.size() - previousFeatureCount;

        boolean admitted = (newBehavior || newFeatures > 0) && hasMutableChoice(result.scheduleTrace());
        if (admitted) {
            // Every trace starts with BASE_ENERGY; new features contribute up to the
            // MAX_FEATURE_ENERGY cap, and a new behavior receives NEW_BEHAVIOR_BONUS.
            int initialEnergy = BASE_ENERGY + Math.min(newFeatures, MAX_FEATURE_ENERGY)
                    + (newBehavior ? NEW_BEHAVIOR_BONUS : 0);
            entries.add(new Entry(fingerprint.hash(), result.scheduleTrace(), initialEnergy));
        }
        return new Observation(true, newBehavior, newFeatures, admitted, entries.size());
    }

    private static boolean hasMutableChoice(ScheduleTrace trace) {
        return trace.decisions().stream().anyMatch(decision -> decision.readySetSize() > 1);
    }

    private static List<Integer> mutablePositions(ScheduleTrace trace) {
        List<Integer> positions = new ArrayList<>();
        for (int position = 0; position < trace.decisions().size(); position++) {
            if (trace.decisions().get(position).readySetSize() > 1) {
                positions.add(position);
            }
        }
        return positions;
    }

    /**
     * Chooses one entry proportionally to its current weight.
     *
     * For weights [5, 10, 5], the entries occupy [0, 5), [5, 15), and [15, 20),
     * so a random pick in [0, 20) selects them with probabilities 25%, 50%,
     * and 25% respectively.
     */
    private static Entry weightedChoice(List<Entry> entries, Random random) {
        double totalWeight = entries.stream().mapToDouble(Entry::selectionWeight).sum();
        double pick = random.nextDouble() * totalWeight;

        double intervalEnd = 0.0;
        for (Entry entry : entries) {
            intervalEnd += entry.selectionWeight();
            if (pick < intervalEnd) {
                return entry; // pick falls in this entry's interval, so select it
            }
        }

        // In exact arithmetic the pick is always below totalWeight, so a matching interval
        // always exists. If floating-point additions leave a tiny unmatched remainder,
        // the last entry is the only correct interval to use as a safety fallback.
        return entries.get(entries.size() - 1);
    }

    record Plan(List<Integer> choicePrefix, @Nullable String parentFingerprintHash, int mutatedChoices) {
        Plan {
            choicePrefix = List.copyOf(choicePrefix);
            if (mutatedChoices < 0) {
                throw new IllegalArgumentException("mutatedChoices cannot be negative");
            }
        }

        static Plan randomSchedule() {
            return new Plan(List.of(), null, 0);
        }
    }

    /** Feedback data produced after a run; ineligible runs cannot guide mutation reward. */
    record Observation(
            boolean rewardEligible,
            boolean newBehavior,
            int newFeatures,
            boolean admittedToCorpus,
            int corpusSize) {
    }

    private static final class Entry {
        private final String fingerprintHash;
        private final ScheduleTrace trace;
        /** Initial weight: base value plus bounded feature and behavior rewards. */
        private final int initialEnergy;
        /** Number of times this entry has been selected as a mutation parent. */
        private int selections;

        private Entry(String fingerprintHash, ScheduleTrace trace, int initialEnergy) {
            this.fingerprintHash = fingerprintHash;
            this.trace = trace;
            this.initialEnergy = initialEnergy;
        }

        /**
         * Decays the initial reward as the entry is reused. Square-root decay rewards
         * novelty while gradually restoring probability to less-used entries.
         */
        private double selectionWeight() {
            return initialEnergy / Math.sqrt(1.0 + selections);
        }
    }
}
