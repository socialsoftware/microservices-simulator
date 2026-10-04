package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrchestratorGroupBudgetTest {

    private final Map<String, String> originalProperties = new HashMap<>();
    private static final String[] PROPERTIES = {
            "consistency.groupBudgetStrategy", "consistency.totalRunBudget",
            "consistency.minimumRunsPerGroup", "consistency.maximumRunsPerGroup",
            "consistency.allocationBatchSize"
    };

    @BeforeEach
    void clearProperties() {
        for (String property : PROPERTIES) {
            originalProperties.put(property, System.getProperty(property));
            System.clearProperty(property);
        }
    }

    @AfterEach
    void restoreProperties() {
        originalProperties.forEach((property, value) -> {
            if (value == null) {
                System.clearProperty(property);
            } else {
                System.setProperty(property, value);
            }
        });
    }

    @Test
    void sharedStrategiesAcceptAnOmittedMaximum() {
        for (GroupBudgetStrategy strategy : GroupBudgetStrategy.values()) {
            if (strategy.sharesCampaignBudget()) {
                configure(strategy);
                assertDoesNotThrow(() -> Orchestrator.of(getClass()));
            }
        }
    }

    @Test
    void explicitMaximumIsStillValidated() {
        configure(GroupBudgetStrategy.BALANCED_REDISTRIBUTION);
        System.setProperty("consistency.maximumRunsPerGroup", "1");
        assertThrows(IllegalArgumentException.class, () -> Orchestrator.of(getClass()));
    }

    @Test
    void fixedDefaultsNeedNoSharedBudgetProperties() {
        assertDoesNotThrow(() -> Orchestrator.of(getClass()));
    }

    @Test
    void sharedBudgetApiRejectsFixedAllocation() {
        assertThrows(IllegalArgumentException.class, () -> Orchestrator.of(getClass())
                .withGroupBudget(GroupBudgetStrategy.FIXED_PER_GROUP, 12, 2, 12, 2));
    }

    private static void configure(GroupBudgetStrategy strategy) {
        System.setProperty("consistency.groupBudgetStrategy", strategy.propertyValue());
        System.setProperty("consistency.totalRunBudget", "12");
        System.setProperty("consistency.minimumRunsPerGroup", "2");
        System.setProperty("consistency.allocationBatchSize", "2");
    }
}
