package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.NoveltyMetric;

class ScheduleExplorationStrategyTest {

    @Test
    void parsesStablePropertyValues() {
        assertEquals(ScheduleExplorationStrategy.RANDOM_CONSTRAINTS,
                ScheduleExplorationStrategy.parse("random-constraints"));
        assertEquals(ScheduleExplorationStrategy.UNIFORM_RANDOM,
                ScheduleExplorationStrategy.parse("uniform-random"));
        assertEquals(ScheduleExplorationStrategy.FEEDBACK_GUIDED,
                ScheduleExplorationStrategy.parse("feedback-guided"));
        assertEquals(ScheduleExplorationStrategy.READS_FROM_GUIDED,
                ScheduleExplorationStrategy.parse("reads-from-guided"));
        assertEquals(ScheduleExplorationStrategy.FEEDBACK_SIGNALS,
                ScheduleExplorationStrategy.parse("feedback-signals"));
    }

    @Test
    void signalModeUsesTheFeedbackCorpusWithoutChangingOtherModes() {
        for (ScheduleExplorationStrategy strategy : ScheduleExplorationStrategy.values()) {
            if (strategy == ScheduleExplorationStrategy.FEEDBACK_SIGNALS) {
                assertTrue(strategy.usesFeedbackCorpus());
                assertEquals(NoveltyMetric.BEHAVIORAL_SIGNALS, strategy.noveltyMetric());
            } else {
                // every other strategy uses the detailed fingerprint corpus
                assertEquals(NoveltyMetric.DETAILED_FINGERPRINT, strategy.noveltyMetric());
                if (strategy == ScheduleExplorationStrategy.FEEDBACK_GUIDED) {
                    assertTrue(strategy.usesFeedbackCorpus());
                } else {
                    assertFalse(strategy.usesFeedbackCorpus());
                }
            }
        }
    }

    @Test
    void rejectsUnknownStrategy() {
        assertThrows(IllegalArgumentException.class,
                () -> ScheduleExplorationStrategy.parse("pct"));
    }
}
