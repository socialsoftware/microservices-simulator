package pt.ulisboa.tecnico.socialsoftware.quizzes.oracle;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator.OrchestrationReport;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator.Orchestrator;
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;

/**
 * The whole consistency-testing tool applied to this application: profile every
 * functionality the {@link QuizzesFunctionalityCatalogsProvider} exposes, plan
 * the pairs worth running concurrently, and explore all of them.
 * <p>
 * Not named {@code *Test}, so it does not run in the ordinary build — a sweep
 * boots the application and runs hundreds of schedules. Run it explicitly:
 *
 * <pre>{@code mvn test -Pconsistency-sweep}</pre>
 */
class QuizzesConsistencySweep {

    private static final Logger log = LoggerFactory.getLogger(QuizzesConsistencySweep.class);

    private static final int ITERATIONS_PER_GROUP = 20;

    // The planted anomalies, by the group label the planner gives each pair.
    private static final String QUOTA_WRITE_SKEW_GROUP = "joinTournamentA__joinTournamentB";
    private static final String IMPOSSIBLE_COMPENSATION_GROUP = "moveMemberToStartedTournament__removeTournamentA";

    @Test
    void sweepsTheWholeApplication() {
        OrchestrationReport report = Orchestrator.of(QuizzesSimulator.class)
                .withIterationsPerGroup(ITERATIONS_PER_GROUP)
                .withReportsDirectory(Path.of("target", "consistency-reports"))
                .run();

        log.info("{}", report.summary());

        assertFalse(report.catalogs().isEmpty(), "the provider should expose at least one catalog");

        OrchestrationReport.CatalogSummary tournaments = report.catalogs().get(0);
        assertTrue(tournaments.groupsPlanned() > 0, "the planner should find pairs worth exploring");
        assertTrue(tournaments.groupsPlanned() < tournaments.possiblePairs(),
                "the planner should prune some of the %d possible pairs, planned %d"
                        .formatted(tournaments.possiblePairs(), tournaments.groupsPlanned()));
        assertTrue(report.totalRuns() > 0, "the campaign should have executed runs");

        assertTrue(hasFindingIn(report, QUOTA_WRITE_SKEW_GROUP),
                "the sweep should surface the per-user tournament quota violation, findings: "
                        + report.findings());
        assertTrue(hasFindingIn(report, IMPOSSIBLE_COMPENSATION_GROUP),
                "the sweep should strand the move's compensation, findings: " + report.findings());

        // Everything beyond the planted anomalies: not asserted, but the reason to
        // read the reports. A clean application would assert this list is empty.
        List<OrchestrationReport.Finding> unexpected = report.findingsExcluding(
                finding -> finding.group().equals(QUOTA_WRITE_SKEW_GROUP)
                        || finding.group().equals(IMPOSSIBLE_COMPENSATION_GROUP));
        log.info("{} finding(s) outside the planted anomalies: {}", unexpected.size(), unexpected);
    }

    private static boolean hasFindingIn(OrchestrationReport report, String groupLabel) {
        return report.findings().stream().anyMatch(finding -> finding.group().equals(groupLabel));
    }
}
