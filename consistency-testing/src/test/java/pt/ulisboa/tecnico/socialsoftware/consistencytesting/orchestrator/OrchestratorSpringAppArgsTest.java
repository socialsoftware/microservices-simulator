package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

class OrchestratorSpringAppArgsTest {

    @Test
    void defaultsToQuietApplicationLogsWhileKeepingOrchestratorProgressVisible() {
        assertEquals(
                List.of(
                        "--logging.level.root=WARN",
                        "--logging.level.pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator=INFO",
                        "--logging.level.org.springframework.scheduling.support=OFF",
                        "--spring.main.banner-mode=off"),
                Orchestrator.defaultedSpringAppArgs(List.of()));
    }

    @Test
    void letsCallerOverrideTheDefaultLoggingLevel() {
        assertEquals(
                List.of(
                        "--logging.level.root=INFO",
                        "--logging.level.pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator=INFO",
                        "--logging.level.org.springframework.scheduling.support=OFF",
                        "--spring.main.banner-mode=off"),
                Orchestrator.defaultedSpringAppArgs(List.of(
                        "--logging.level.root=INFO",
                        "--spring.main.banner-mode=off")));
    }
}
