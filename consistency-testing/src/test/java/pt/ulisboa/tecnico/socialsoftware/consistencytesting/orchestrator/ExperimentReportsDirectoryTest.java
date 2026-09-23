package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.SemanticLockId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.ScheduleExplorationStrategy;

class ExperimentReportsDirectoryTest {

    @Test
    void createsReadableUtcDirectoryNameWithShortUniqueSuffix() {
        Path directory = ExperimentReportsDirectory.pathForExperiment(
                Instant.parse("2026-08-30T21:45:22Z"),
                UUID.fromString("7f3a91c2-0000-0000-0000-000000000000"));

        assertEquals(
                Path.of("target", "consistency-experiments", "2026-08-30_21-45-22-7f3a91c2"),
                directory);
    }

    @Test
    void semanticLockFaultUsesAutomaticExperimentDirectoryAtDefaultPath() {
        Instant startedAt = Instant.parse("2026-08-30T21:45:22Z");
        UUID runId = UUID.fromString("7f3a91c2-0000-0000-0000-000000000000");
        Set<SemanticLockId> ignoredLocks = Set.of(new SemanticLockId("example.State", "LOCKED"));

        assertEquals(
                Path.of("target", "consistency-experiments", "2026-08-30_21-45-22-7f3a91c2"),
                Orchestrator.resolveReportsDirectory(
                        Path.of("target", "consistency-reports"), ignoredLocks, Set.of(),
                        ScheduleExplorationStrategy.RANDOM_CONSTRAINTS, GroupBudgetStrategy.FIXED_PER_GROUP,
                        null, startedAt, runId));
        assertEquals(
                Path.of("target", "chosen-directory"),
                Orchestrator.resolveReportsDirectory(
                        Path.of("target", "consistency-reports"), ignoredLocks, Set.of(),
                        ScheduleExplorationStrategy.RANDOM_CONSTRAINTS,
                        GroupBudgetStrategy.FIXED_PER_GROUP,
                        "target/chosen-directory", startedAt, runId));
    }

    @Test
    void selectedGroupsUseAutomaticExperimentDirectoryAtDefaultPath() {
        assertEquals(
                Path.of("target", "consistency-experiments", "2026-08-30_21-45-22-7f3a91c2"),
                Orchestrator.resolveReportsDirectory(
                        Path.of("target", "consistency-reports"),
                        Set.of(),
                        Set.of(GroupSelector.parse("catalog/first__second")),
                        ScheduleExplorationStrategy.RANDOM_CONSTRAINTS,
                        GroupBudgetStrategy.FIXED_PER_GROUP,
                        null,
                        Instant.parse("2026-08-30T21:45:22Z"),
                        UUID.fromString("7f3a91c2-0000-0000-0000-000000000000")));
    }

    @Test
    void nonDefaultScheduleExplorationUsesAnAutomaticExperimentDirectory() {
        assertEquals(
                Path.of("target", "consistency-experiments", "2026-08-30_21-45-22-7f3a91c2"),
                Orchestrator.resolveReportsDirectory(
                        Path.of("target", "consistency-reports"),
                        Set.of(),
                        Set.of(),
                        ScheduleExplorationStrategy.FEEDBACK_GUIDED,
                        GroupBudgetStrategy.FIXED_PER_GROUP,
                        null,
                        Instant.parse("2026-08-30T21:45:22Z"),
                        UUID.fromString("7f3a91c2-0000-0000-0000-000000000000")));
    }

    @Test
    void adaptiveGroupBudgetUsesAnAutomaticExperimentDirectory() {
        assertEquals(
                Path.of("target", "consistency-experiments", "2026-08-30_21-45-22-7f3a91c2"),
                Orchestrator.resolveReportsDirectory(
                        Path.of("target", "consistency-reports"),
                        Set.of(),
                        Set.of(),
                        ScheduleExplorationStrategy.RANDOM_CONSTRAINTS,
                        GroupBudgetStrategy.ADAPTIVE_NOVELTY,
                        null,
                        Instant.parse("2026-08-30T21:45:22Z"),
                        UUID.fromString("7f3a91c2-0000-0000-0000-000000000000")));
    }
}
