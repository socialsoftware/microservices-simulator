package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.discovery.DiscoverySelectors;
import org.junit.platform.launcher.Launcher;
import org.junit.platform.launcher.LauncherDiscoveryRequest;
import org.junit.platform.launcher.core.LauncherDiscoveryRequestBuilder;
import org.junit.platform.launcher.core.LauncherFactory;
import org.junit.platform.launcher.listeners.SummaryGeneratingListener;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class DynamicEvidenceTestExecutionListenerIntegrationTest {
    private static final AtomicReference<DynamicEvidenceEvent> CAPTURED_EVENT = new AtomicReference<>();
    private static final AtomicReference<Path> CAPTURED_OUTPUT_DIR = new AtomicReference<>();

    @Test
    void launcherAutoDetectionCapturesTestIdentityWhenPropertiesAreEnabled() {
        String previousAutodetection = System.getProperty(
                DynamicEvidenceTestExecutionListener.JUNIT_PLATFORM_LISTENER_AUTODETECTION_ENABLED_PROPERTY);
        String previousTestContext = System.getProperty(
                DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY);

        try {
            DynamicEvidenceTestContext.clear();
            CAPTURED_EVENT.set(null);
            System.setProperty(DynamicEvidenceTestExecutionListener.JUNIT_PLATFORM_LISTENER_AUTODETECTION_ENABLED_PROPERTY, "true");
            System.setProperty(DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY, "true");

            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(AutodetectedTestCase.class))
                    .build();
            Launcher launcher = LauncherFactory.create();
            SummaryGeneratingListener summaryGeneratingListener = new SummaryGeneratingListener();
            launcher.registerTestExecutionListeners(summaryGeneratingListener);
            launcher.execute(request);

            assertThat(summaryGeneratingListener.getSummary().getTestsSucceededCount()).isEqualTo(1);
            DynamicEvidenceEvent event = CAPTURED_EVENT.get();
            assertThat(event).isNotNull();
            assertThat(event.getTestClassFqn()).isEqualTo(AutodetectedTestCase.class.getName());
            assertThat(event.getTestMethodName()).isEqualTo("capturesIdentity");
            assertThat(event.getTestDisplayName()).isEqualTo("captures identity");
            assertThat(event.getTestUniqueId()).contains("AutodetectedTestCase");
            assertThat(event.getTestUniqueId()).contains("capturesIdentity");
            assertThat(DynamicEvidenceTestContext.current()).isEmpty();
        } finally {
            restoreProperty(DynamicEvidenceTestExecutionListener.JUNIT_PLATFORM_LISTENER_AUTODETECTION_ENABLED_PROPERTY,
                    previousAutodetection);
            restoreProperty(DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY, previousTestContext);
            DynamicEvidenceTestContext.clear();
            CAPTURED_EVENT.set(null);
        }
    }

    @Test
    void launcherAutoDetectionInitializesRecorderFromSystemPropertiesWithoutSpringContext() throws Exception {
        String previousAutodetection = System.getProperty(
                DynamicEvidenceTestExecutionListener.JUNIT_PLATFORM_LISTENER_AUTODETECTION_ENABLED_PROPERTY);
        String previousTestContext = System.getProperty(
                DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY);
        String previousEvidenceEnabled = System.getProperty("simulator.dynamic-evidence.enabled");
        String previousOutputDir = System.getProperty("simulator.dynamic-evidence.output-dir");
        String previousApplicationName = System.getProperty("simulator.dynamic-evidence.application-name");
        Path outputDir = Files.createTempDirectory("dynamic-evidence-listener-");

        try {
            DynamicEvidenceTestContext.clear();
            DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
            CAPTURED_OUTPUT_DIR.set(outputDir);
            System.setProperty(DynamicEvidenceTestExecutionListener.JUNIT_PLATFORM_LISTENER_AUTODETECTION_ENABLED_PROPERTY, "true");
            System.setProperty(DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY, "true");
            System.setProperty("simulator.dynamic-evidence.enabled", "true");
            System.setProperty("simulator.dynamic-evidence.output-dir", outputDir.toString());
            System.setProperty("simulator.dynamic-evidence.application-name", "listener-integration");

            LauncherDiscoveryRequest request = LauncherDiscoveryRequestBuilder.request()
                    .selectors(DiscoverySelectors.selectClass(RecorderInitializedTestCase.class))
                    .build();
            Launcher launcher = LauncherFactory.create();
            SummaryGeneratingListener summaryGeneratingListener = new SummaryGeneratingListener();
            launcher.registerTestExecutionListeners(summaryGeneratingListener);
            launcher.execute(request);

            assertThat(summaryGeneratingListener.getSummary().getTestsSucceededCount()).isEqualTo(1);
            Path evidencePath = outputDir.resolve("dynamic-evidence.jsonl");
            Path manifestPath = outputDir.resolve("dynamic-evidence-manifest.json");
            assertThat(evidencePath).exists();
            assertThat(manifestPath).exists();
            String jsonl = Files.readString(evidencePath);
            assertThat(jsonl).contains("\"applicationName\":\"listener-integration\"");
            assertThat(jsonl).contains("\"testClassFqn\":\"" + RecorderInitializedTestCase.class.getName() + "\"");
            assertThat(jsonl).contains("\"testMethodName\":\"recordsThroughHolder\"");
            assertThat(jsonl).contains("\"eventKind\":\"STEP_STARTED\"");
        } finally {
            restoreProperty(DynamicEvidenceTestExecutionListener.JUNIT_PLATFORM_LISTENER_AUTODETECTION_ENABLED_PROPERTY,
                    previousAutodetection);
            restoreProperty(DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY, previousTestContext);
            restoreProperty("simulator.dynamic-evidence.enabled", previousEvidenceEnabled);
            restoreProperty("simulator.dynamic-evidence.output-dir", previousOutputDir);
            restoreProperty("simulator.dynamic-evidence.application-name", previousApplicationName);
            DynamicEvidenceTestContext.clear();
            DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
            CAPTURED_OUTPUT_DIR.set(null);
            deleteRecursively(outputDir);
        }
    }

    private void restoreProperty(String propertyName, String value) {
        if (value == null) {
            System.clearProperty(propertyName);
            return;
        }
        System.setProperty(propertyName, value);
    }

    private void deleteRecursively(Path path) throws Exception {
        if (path == null || !Files.exists(path)) {
            return;
        }
        try (var stream = Files.walk(path)) {
            stream.sorted((left, right) -> right.compareTo(left))
                    .forEach(candidate -> {
                        try {
                            Files.deleteIfExists(candidate);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        }
    }

    public static class AutodetectedTestCase {
        @Test
        @DisplayName("captures identity")
        void capturesIdentity() {
            CAPTURED_EVENT.set(DynamicEvidenceEvent.of(
                    "STEP_STARTED",
                    "checkout",
                    "invocation-1",
                    "reserve",
                    1L,
                    Map.of("marker", "value")));
        }
    }

    public static class RecorderInitializedTestCase {
        @Test
        @DisplayName("records through holder")
        void recordsThroughHolder() {
            assertThat(CAPTURED_OUTPUT_DIR.get()).isNotNull();
            DynamicEvidenceRecorderHolder.recordStepStarted(new DynamicEvidenceContext.StepContext(
                    "checkout",
                    "invocation-2",
                    "reserve",
                    2L,
                    System.currentTimeMillis(),
                    System.nanoTime()));
        }
    }
}
