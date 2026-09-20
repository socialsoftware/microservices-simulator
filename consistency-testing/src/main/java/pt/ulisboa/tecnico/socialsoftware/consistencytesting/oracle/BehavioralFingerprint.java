package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Normalized, cross-run description of behavior observed in one schedule run.
 * <p>
 * Features deliberately exclude database-generated IDs. Concrete IDs remain
 * available in the ordinary run report for diagnosis, but cannot manufacture
 * behavioral novelty.
 *
 * @param schema   feature schema; changes whenever feature meaning changes
 * @param hash     SHA-256 of the schema and canonical sorted feature set
 * @param features canonical sorted feature set
 */
public record BehavioralFingerprint(String schema, String hash, List<String> features) {

    public static final String SCHEMA = "normalized-behavior-v2";

    public BehavioralFingerprint {
        Objects.requireNonNull(schema);
        Objects.requireNonNull(hash);
        features = List.copyOf(features);
    }

    public static BehavioralFingerprint from(TestResult result) {
        Objects.requireNonNull(result);
        TreeSet<String> features = new TreeSet<>(); // Deduplicates and sorts features for deterministic hashing.

        addStepPathFeatures(features, result.schedule());
        addEffectFeatures(features, result.effectSequence());

        result.readsFromRelations().forEach(relation -> features.add(feature(
                "reads-from",
                normalized(relation.writer()),
                normalized(relation.reader()),
                relation.aggregateType())));

        addCountedFeatures(features, "semantic-lock", result.semanticLockTrace().stream()
                .map(activity -> feature(
                        normalized(activity.stepId()),
                        activity.semanticLock().toSelector(),
                        activity.outcome().name()))
                .toList());

        result.statuses().forEach(status -> features.add(feature("status", status.name())));
        result.anomalies().forEach(anomaly -> features.add(anomalyFeature(anomaly)));
        result.interInvariantViolations().keySet().forEach(
                invariant -> features.add(feature("inter-invariant-violation", invariant)));
        result.exceptions().forEach((step, exception) -> features.add(feature(
                "exception", normalized(step), exception.getClass().getName())));

        List<String> canonicalFeatures = List.copyOf(features);
        return new BehavioralFingerprint(SCHEMA, sha256(canonicalFeatures), canonicalFeatures);
    }

    /** Adds counted schedule-step features, preserving step role but not incidental IDs. */
    private static void addStepPathFeatures(TreeSet<String> features, List<StepId> schedule) {
        addCountedFeatures(features, "step", schedule.stream()
                .map(step -> feature(step.stepKind().name(), normalized(step)))
                .toList());
    }

    /** Adds counted effects and pairwise same-aggregate conflict features. */
    private static void addEffectFeatures(TreeSet<String> features, List<StepEffect> effects) {
        addCountedFeatures(features, "effect", effects.stream()
                .map(effect -> feature(
                        normalized(effect.stepId()),
                        effect.stepKind().name(),
                        effect.effectKind().name(),
                        effect.aggregateType()))
                .toList());

        // Record ordered pairs of effects that touch the same aggregate and include a write.
        List<String> conflicts = new ArrayList<>();
        for (int firstIndex = 0; firstIndex < effects.size(); firstIndex++) {
            StepEffect first = effects.get(firstIndex);
            for (int secondIndex = firstIndex + 1; secondIndex < effects.size(); secondIndex++) {
                StepEffect second = effects.get(secondIndex);
                if (first.stepId().equals(second.stepId())
                        || !Objects.equals(first.aggregateId(), second.aggregateId())
                        || !first.aggregateType().equals(second.aggregateType())
                        || (!first.isWrite() && !second.isWrite())) {
                    continue;
                }
                conflicts.add(feature(
                        normalized(first.stepId()), first.effectKind().name(),
                        "before", normalized(second.stepId()), second.effectKind().name(),
                        first.aggregateType()));
            }
        }
        addCountedFeatures(features, "conflict", conflicts);
    }

    /** Adds observations with multiplicity so repeated effects remain behaviorally visible. */
    private static void addCountedFeatures(TreeSet<String> features, String category, List<String> observations) {
        // TreeMap sorts observations so counted feature output is deterministic.
        Map<String, Integer> counts = new TreeMap<>();
        observations.forEach(observation -> counts.merge(observation, 1, Integer::sum));
        counts.forEach((observation, count) -> features.add(category + "|" + observation + "|count=" + count));
    }

    private static String normalized(StepId stepId) {
        return stepId.behavioralIdentity();
    }

    private static String anomalyFeature(Anomaly anomaly) {
        return switch (anomaly) {
            case Anomaly.DirtyRead dirtyRead -> feature(
                    "anomaly", anomaly.type().name(), normalized(dirtyRead.doomedWriter()),
                    normalized(dirtyRead.reader()), dirtyRead.aggregateType());
            case Anomaly.NonRepeatableRead nonRepeatableRead -> feature(
                    "anomaly", anomaly.type().name(),
                    normalized(nonRepeatableRead.firstWriter()), normalized(nonRepeatableRead.firstRead()),
                    normalized(nonRepeatableRead.secondWriter()), normalized(nonRepeatableRead.secondRead()),
                    nonRepeatableRead.aggregateType());
            case Anomaly.WriteSkew writeSkew -> feature(
                    "anomaly", anomaly.type().name(),
                    writeSkew.functionalityA().behavioralIdentity(),
                    writeSkew.functionalityB().behavioralIdentity(),
                    writeSkew.aggregateTypeOverwrittenByB(),
                    writeSkew.aggregateTypeOverwrittenByA());
        };
    }

    private static String feature(String... parts) {
        List<String> escaped = new ArrayList<>(parts.length);
        for (String part : parts) {
            escaped.add(escape(part));
        }
        return String.join("|", escaped);
    }

    /** Escapes delimiters so feature components cannot change their field boundaries. */
    private static String escape(String value) {
        return value.replace("%", "%25")
                .replace("|", "%7C")
                .replace("\r", "%0D")
                .replace("\n", "%0A");
    }

    private static String sha256(List<String> features) {
        String canonical = SCHEMA + "\n" + String.join("\n", features);
        try {
            return java.util.HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is required to fingerprint oracle behavior", e);
        }
    }
}
