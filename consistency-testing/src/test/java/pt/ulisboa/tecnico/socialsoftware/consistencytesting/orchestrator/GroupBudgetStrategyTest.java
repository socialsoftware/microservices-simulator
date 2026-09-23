package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GroupBudgetStrategyTest {

    @Test
    void parsesPropertyValues() {
        assertEquals(GroupBudgetStrategy.FIXED_PER_GROUP,
                GroupBudgetStrategy.parse("fixed-per-group"));
        assertEquals(GroupBudgetStrategy.ADAPTIVE_NOVELTY,
                GroupBudgetStrategy.parse("adaptive-novelty"));
    }

    @Test
    void rejectsUnknownValue() {
        assertThrows(IllegalArgumentException.class,
                () -> GroupBudgetStrategy.parse("round-robin"));
    }
}
