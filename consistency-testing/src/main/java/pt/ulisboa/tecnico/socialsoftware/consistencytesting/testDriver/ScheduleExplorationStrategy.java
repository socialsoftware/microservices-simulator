package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import java.util.Arrays;

/** Schedule exploration policy used within each functionality group. */
public enum ScheduleExplorationStrategy {
    /** Random interleavings plus learned cross-functionality ordering constraints. */
    RANDOM_CONSTRAINTS("random-constraints"),

    /** Seeded uniform choices over each dynamically materialized ready set. */
    UNIFORM_RANDOM("uniform-random"),

    /** Mutates useful prior choice traces while retaining random exploration. */
    FEEDBACK_GUIDED("feedback-guided");

    private final String propertyValue;

    ScheduleExplorationStrategy(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String propertyValue() {
        return propertyValue;
    }

    public static ScheduleExplorationStrategy parse(String value) {
        return Arrays.stream(values())
                .filter(strategy -> strategy.propertyValue.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown schedule exploration strategy '%s'; expected one of %s"
                                .formatted(
                                        value,
                                        Arrays.stream(values())
                                                .map(ScheduleExplorationStrategy::propertyValue).toList())));
    }
}
