package pt.ulisboa.tecnico.socialsoftware.quizzes.oracle;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator.OrchestrationReport;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator.Orchestrator;
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSimulator;

class QuizzesConsistencySweep {

    private static final Logger log = LoggerFactory.getLogger(QuizzesConsistencySweep.class);
    private static final int DEFAULT_ITERATIONS = 20;

    @Test
    void sweepsApplication() {
        int iterations = Integer.getInteger("consistency.iterations", DEFAULT_ITERATIONS);
        Orchestrator orchestrator = Orchestrator.of(QuizzesSimulator.class)
                .withIterationsPerGroup(iterations)
                .withReportsDirectory(Path.of("target", "consistency-reports"));

        String configuredSeed = System.getProperty("consistency.masterSeed");
        if (configuredSeed != null && !configuredSeed.isBlank()) {
            orchestrator.withMasterSeed(Long.parseLong(configuredSeed));
        }

        OrchestrationReport report = orchestrator.run();
        log.info("{}", report.summary());
        assertFalse(report.catalogs().isEmpty(), "provider must expose at least one catalog");
    }
}
