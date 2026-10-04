package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.List;

/** Cross-run observations used for novelty rewards, independent of scheduling mechanics. */
public enum NoveltyMetric {
    DETAILED_FINGERPRINT(BehavioralFingerprint.SCHEMA),
    BEHAVIORAL_SIGNALS(BehavioralSignals.SCHEMA);

    private final String schema;

    NoveltyMetric(String schema) {
        this.schema = schema;
    }

    public String schema() {
        return schema;
    }

    public Observation measure(TestResult result) {
        return switch (this) {
            case DETAILED_FINGERPRINT -> {
                BehavioralFingerprint fingerprint = BehavioralFingerprint.from(result);
                yield new Observation(fingerprint.hash(), fingerprint.features());
            }
            case BEHAVIORAL_SIGNALS -> {
                BehavioralSignals signals = BehavioralSignals.from(result);
                yield new Observation(signals.hash(), signals.signals());
            }
        };
    }

    public record Observation(String hash, List<String> features) {
        public Observation {
            features = List.copyOf(features);
        }
    }
}
