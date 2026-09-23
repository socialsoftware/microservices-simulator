package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import java.util.Arrays;

/** Policy deciding which unordered functionality pairs enter a campaign plan. */
public enum PlanningPolicy {
    /** Retain only pairs with observed read/write or write/write footprint conflicts. */
    FOOTPRINT_CONFLICTS("footprint-conflicts"),

    /** Retain every unordered pair, including every self-pair. */
    ALL_GROUPS("all-groups");

    private final String propertyValue;

    PlanningPolicy(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String propertyValue() {
        return propertyValue;
    }

    public static PlanningPolicy parse(String value) {
        return Arrays.stream(values())
                .filter(policy -> policy.propertyValue.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown planning policy '%s'; expected one of %s"
                                .formatted(value, Arrays.stream(values())
                                        .map(PlanningPolicy::propertyValue).toList())));
    }
}
