package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.util.Arrays;

/** How a campaign distributes oracle runs across planned groups. */
public enum GroupBudgetStrategy {
    /** Gives every group the configured fixed number of oracle runs. */
    FIXED_PER_GROUP("fixed-per-group"),
    /** Shares campaign budget, prioritizing groups by novelty and exploration. */
    ADAPTIVE_NOVELTY("adaptive-novelty"),
    /** Shares campaign budget among least-sampled active groups. */
    BALANCED_REDISTRIBUTION("balanced-redistribution");

    private final String propertyValue;

    GroupBudgetStrategy(String propertyValue) {
        this.propertyValue = propertyValue;
    }

    public String propertyValue() {
        return propertyValue;
    }

    /** Returns whether strategy allocates one campaign-wide run budget. */
    public boolean sharesCampaignBudget() {
        return this != FIXED_PER_GROUP;
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
