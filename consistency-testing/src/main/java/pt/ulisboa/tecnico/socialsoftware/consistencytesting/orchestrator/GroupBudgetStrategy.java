package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.util.Arrays;

/** How a campaign distributes oracle runs across planned groups. */
public enum GroupBudgetStrategy {
    FIXED_PER_GROUP("fixed-per-group"),
    ADAPTIVE_NOVELTY("adaptive-novelty");

    private final String propertyValue;

    GroupBudgetStrategy(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String propertyValue() {
        return propertyValue;
    }

    public static GroupBudgetStrategy parse(String value) {
        return Arrays.stream(values())
                .filter(strategy -> strategy.propertyValue.equals(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown group budget strategy '%s'; expected one of %s"
                                .formatted(value, Arrays.stream(values())
                                        .map(GroupBudgetStrategy::propertyValue).toList())));
    }
}
