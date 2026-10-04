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
        assertEquals(GroupBudgetStrategy.ADAPTIVE_READS_FROM,
                GroupBudgetStrategy.parse("adaptive-reads-from"));
        assertEquals(GroupBudgetStrategy.BALANCED_REDISTRIBUTION,
                GroupBudgetStrategy.parse("balanced-redistribution"));
        assertEquals(GroupBudgetStrategy.ADAPTIVE_SIGNALS,
                GroupBudgetStrategy.parse("adaptive-signals"));
    }

    @Test
    void rejectsUnknownValue() {
        assertThrows(IllegalArgumentException.class,
                () -> GroupBudgetStrategy.parse("round-robin"));
    }
}
