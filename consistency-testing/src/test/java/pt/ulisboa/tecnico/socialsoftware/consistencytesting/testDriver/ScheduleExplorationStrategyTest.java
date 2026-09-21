package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ScheduleExplorationStrategyTest {

    @Test
    void parsesStablePropertyValues() {
        assertEquals(ScheduleExplorationStrategy.RANDOM_CONSTRAINTS,
                ScheduleExplorationStrategy.parse("random-constraints"));
        assertEquals(ScheduleExplorationStrategy.UNIFORM_RANDOM,
                ScheduleExplorationStrategy.parse("uniform-random"));
        assertEquals(ScheduleExplorationStrategy.FEEDBACK_GUIDED,
                ScheduleExplorationStrategy.parse("feedback-guided"));
    }

    @Test
    void rejectsUnknownStrategy() {
        assertThrows(IllegalArgumentException.class,
                () -> ScheduleExplorationStrategy.parse("pct"));
    }
}
