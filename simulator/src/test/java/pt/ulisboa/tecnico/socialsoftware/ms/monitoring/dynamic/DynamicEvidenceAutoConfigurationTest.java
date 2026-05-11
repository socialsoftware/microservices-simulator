package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.engine.support.descriptor.MethodSource;
import org.junit.platform.launcher.TestIdentifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.context.annotation.ImportCandidates;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DynamicEvidenceAutoConfigurationTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @AfterEach
    void resetHolder() {
        DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
        DynamicEvidenceTestContext.clear();
    }

    @Test
    void recorderBeanIsAvailableViaAutoConfigurationWhenDisabled() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(loadDynamicEvidenceAutoConfigurationClass()))
                .withPropertyValues("simulator.dynamic-evidence.enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(DynamicEvidenceRecorder.class);
                    assertThat(context.getBean(DynamicEvidenceRecorder.class)).isInstanceOf(DynamicEvidenceNoopRecorder.class);
                });
    }

    @Test
    void recorderBeanIsAvailableViaAutoConfigurationWhenEnabled() {
        Path outputDir = tempDir.resolve("enabled");

        contextRunner
                .withConfiguration(AutoConfigurations.of(loadDynamicEvidenceAutoConfigurationClass()))
                .withPropertyValues(
                        "simulator.dynamic-evidence.enabled=true",
                        "simulator.dynamic-evidence.output-dir=" + outputDir,
                        "simulator.dynamic-evidence.application-name=orders")
                .run(context -> {
                    assertThat(context).hasSingleBean(DynamicEvidenceRecorder.class);
                    assertThat(context.getBean(DynamicEvidenceRecorder.class)).isInstanceOf(DynamicEvidenceJsonlRecorder.class);
                });

        assertThat(outputDir.resolve("dynamic-evidence.jsonl")).exists();
        assertThat(outputDir.resolve("dynamic-evidence-manifest.json")).exists();
    }

    @Test
    void nestedTestContextPropertyBindsToRecorderProperties() {
        contextRunner
                .withConfiguration(AutoConfigurations.of(loadDynamicEvidenceAutoConfigurationClass()))
                .withPropertyValues(
                        "simulator.dynamic-evidence.enabled=true",
                        "simulator.dynamic-evidence.output-dir=" + tempDir.resolve("bound-properties"),
                        "simulator.dynamic-evidence.test-context.enabled=true")
                .run(context -> assertThat(context.getBean(DynamicEvidenceProperties.class).isTestContextEnabled()).isTrue());
    }

    @Test
    void recorderEmitsTestIdentityFieldsWhenNestedTestContextPropertyIsEnabled() throws Exception {
        Path outputDir = tempDir.resolve("nested-enabled");

        contextRunner
                .withConfiguration(AutoConfigurations.of(loadDynamicEvidenceAutoConfigurationClass()))
                .withPropertyValues(
                        "simulator.dynamic-evidence.enabled=true",
                        "simulator.dynamic-evidence.output-dir=" + outputDir,
                        "simulator.dynamic-evidence.application-name=orders",
                        "simulator.dynamic-evidence.test-context.enabled=true")
                .run(context -> {
                    assertThat(context.getBean(DynamicEvidenceProperties.class).isTestContextEnabled()).isTrue();
                    DynamicEvidenceTestContext.set(new DynamicEvidenceTestContext.TestIdentity(
                            "example.MySpec", "featureMethod", "feature display name", "unique-id"));
                    try {
                        DynamicEvidenceRecorder recorder = context.getBean(DynamicEvidenceRecorder.class);
                        recorder.record(DynamicEvidenceEvent.of("STEP_STARTED", "checkout", "invocation-1", "reserve", 7L,
                                Map.of("aggregate", "cart")));
                        recorder.close();
                    } finally {
                        DynamicEvidenceTestContext.clear();
                    }
                });

        JsonNode event = objectMapper.readTree(Files.readString(outputDir.resolve("dynamic-evidence.jsonl")));
        assertThat(event.get("testClassFqn").asText()).isEqualTo("example.MySpec");
        assertThat(event.get("testMethodName").asText()).isEqualTo("featureMethod");
        assertThat(event.get("testDisplayName").asText()).isEqualTo("feature display name");
        assertThat(event.get("testUniqueId").asText()).isEqualTo("unique-id");

        JsonNode manifest = objectMapper.readTree(Files.readString(outputDir.resolve("dynamic-evidence-manifest.json")));
        assertThat(manifest.get("effectiveConfig").get("testContextEnabled").asBoolean()).isTrue();
    }

    @Test
    void recorderHolderResetsToNoopWhenContextCloses() {
        RecordingRecorder recorder = new RecordingRecorder();

        contextRunner
                .withConfiguration(AutoConfigurations.of(loadDynamicEvidenceAutoConfigurationClass()))
                .withBean(DynamicEvidenceRecorder.class, () -> recorder)
                .run(context -> assertThat(DynamicEvidenceRecorderHolder.getRecorder()).isSameAs(recorder));

        assertThat(DynamicEvidenceRecorderHolder.getRecorder())
                .isInstanceOf(DynamicEvidenceNoopRecorder.class)
                .isNotSameAs(recorder);
    }

    @Test
    void registrationCloseDoesNotResetHolderWhenAnotherRecorderIsRegisteredLater() {
        RecordingRecorder initialRecorder = new RecordingRecorder();
        DynamicEvidenceRecorderRegistration registration = new DynamicEvidenceRecorderRegistration(initialRecorder);
        RecordingRecorder replacementRecorder = new RecordingRecorder();
        DynamicEvidenceRecorderHolder.setRecorder(replacementRecorder);

        registration.close();

        assertThat(DynamicEvidenceRecorderHolder.getRecorder()).isSameAs(replacementRecorder);
    }

    @Test
    void springAutoConfigurationDoesNotTruncateListenerFallbackRecorderForSharedOutputDir() throws Exception {
        Path outputDir = tempDir.resolve("shared-output-dir");
        String previousEnabled = System.getProperty("simulator.dynamic-evidence.enabled");
        String previousOutputDir = System.getProperty("simulator.dynamic-evidence.output-dir");
        String previousApplicationName = System.getProperty("simulator.dynamic-evidence.application-name");
        String previousTestContext = System.getProperty(DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY);
        String previousAutodetection = System.getProperty(DynamicEvidenceTestExecutionListener.JUNIT_PLATFORM_LISTENER_AUTODETECTION_ENABLED_PROPERTY);
        TestIdentifier testIdentifier = testIdentifier();
        DynamicEvidenceTestExecutionListener listener = new DynamicEvidenceTestExecutionListener();

        try {
            DynamicEvidenceTestContext.clear();
            DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
            System.setProperty("simulator.dynamic-evidence.enabled", "true");
            System.setProperty("simulator.dynamic-evidence.output-dir", outputDir.toString());
            System.setProperty("simulator.dynamic-evidence.application-name", "orders");
            System.setProperty(DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY, "true");
            System.setProperty(DynamicEvidenceTestExecutionListener.JUNIT_PLATFORM_LISTENER_AUTODETECTION_ENABLED_PROPERTY, "true");

            listener.testPlanExecutionStarted(null);
            listener.executionStarted(testIdentifier);

            DynamicEvidenceRecorderHolder.recordStepStarted(new DynamicEvidenceContext.StepContext(
                    "checkout",
                    "listener-invocation",
                    "listener-step",
                    11L,
                    System.currentTimeMillis(),
                    System.nanoTime()));

            contextRunner
                    .withConfiguration(AutoConfigurations.of(loadDynamicEvidenceAutoConfigurationClass()))
                    .withPropertyValues(
                            "simulator.dynamic-evidence.enabled=true",
                            "simulator.dynamic-evidence.output-dir=" + outputDir,
                            "simulator.dynamic-evidence.application-name=orders",
                            "simulator.dynamic-evidence.test-context.enabled=true")
                    .run(context -> {
                        assertThat(context).hasSingleBean(DynamicEvidenceRecorder.class);
                        DynamicEvidenceRecorderHolder.recordStepStarted(new DynamicEvidenceContext.StepContext(
                                "checkout",
                                "spring-invocation",
                                "spring-step",
                                12L,
                                System.currentTimeMillis(),
                                System.nanoTime()));
                    });

            listener.executionFinished(testIdentifier, TestExecutionResult.successful());
            listener.testPlanExecutionFinished(null);

            Path evidencePath = outputDir.resolve("dynamic-evidence.jsonl");
            Path manifestPath = outputDir.resolve("dynamic-evidence-manifest.json");
            assertThat(evidencePath).exists();
            assertThat(manifestPath).exists();

            List<JsonNode> events = Files.readAllLines(evidencePath).stream().map(this::parseJsonLine).toList();
            assertThat(events).hasSize(2);
            assertThat(events.get(0).get("functionalityInvocationId").asText()).isEqualTo("listener-invocation");
            assertThat(events.get(1).get("functionalityInvocationId").asText()).isEqualTo("spring-invocation");
            assertThat(events).allSatisfy(event -> {
                assertThat(event.get("testClassFqn").asText()).isEqualTo("example.MySpec");
                assertThat(event.get("testMethodName").asText()).isEqualTo("featureMethod");
                assertThat(event.get("testDisplayName").asText()).isEqualTo("feature display name");
                assertThat(event.get("testUniqueId").asText()).isEqualTo("[engine:spock]/[spec:example.MySpec]/[feature:featureMethod]");
            });

            JsonNode manifest = objectMapper.readTree(Files.readString(manifestPath));
            assertThat(manifest.get("counts").get("eventsWritten").asInt()).isEqualTo(2);
            assertThat(manifest.get("counts").get("STEP_STARTED").asInt()).isEqualTo(2);
        } finally {
            restoreProperty("simulator.dynamic-evidence.enabled", previousEnabled);
            restoreProperty("simulator.dynamic-evidence.output-dir", previousOutputDir);
            restoreProperty("simulator.dynamic-evidence.application-name", previousApplicationName);
            restoreProperty(DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY, previousTestContext);
            restoreProperty(DynamicEvidenceTestExecutionListener.JUNIT_PLATFORM_LISTENER_AUTODETECTION_ENABLED_PROPERTY, previousAutodetection);
            DynamicEvidenceTestContext.clear();
            DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
        }
    }

    @Test
    void springAutoConfigurationPreservesListenerOwnedRecorderAcrossSequentialContexts() throws Exception {
        Path outputDir = tempDir.resolve("sequential-shared-output-dir");
        String previousEnabled = System.getProperty("simulator.dynamic-evidence.enabled");
        String previousOutputDir = System.getProperty("simulator.dynamic-evidence.output-dir");
        String previousApplicationName = System.getProperty("simulator.dynamic-evidence.application-name");
        String previousTestContext = System.getProperty(DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY);
        String previousAutodetection = System.getProperty(DynamicEvidenceTestExecutionListener.JUNIT_PLATFORM_LISTENER_AUTODETECTION_ENABLED_PROPERTY);
        TestIdentifier testIdentifier = testIdentifier();
        DynamicEvidenceTestExecutionListener listener = new DynamicEvidenceTestExecutionListener();

        try {
            DynamicEvidenceTestContext.clear();
            DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
            System.setProperty("simulator.dynamic-evidence.enabled", "true");
            System.setProperty("simulator.dynamic-evidence.output-dir", outputDir.toString());
            System.setProperty("simulator.dynamic-evidence.application-name", "orders");
            System.setProperty(DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY, "true");
            System.setProperty(DynamicEvidenceTestExecutionListener.JUNIT_PLATFORM_LISTENER_AUTODETECTION_ENABLED_PROPERTY, "true");

            listener.testPlanExecutionStarted(null);
            listener.executionStarted(testIdentifier);

            DynamicEvidenceRecorderHolder.recordStepStarted(new DynamicEvidenceContext.StepContext(
                    "checkout",
                    "listener-before-context",
                    "listener-step-before",
                    11L,
                    System.currentTimeMillis(),
                    System.nanoTime()));

            contextRunner
                    .withConfiguration(AutoConfigurations.of(loadDynamicEvidenceAutoConfigurationClass()))
                    .withPropertyValues(
                            "simulator.dynamic-evidence.enabled=true",
                            "simulator.dynamic-evidence.output-dir=" + outputDir,
                            "simulator.dynamic-evidence.application-name=orders",
                            "simulator.dynamic-evidence.test-context.enabled=true")
                    .run(context -> {
                        assertThat(context).hasSingleBean(DynamicEvidenceRecorder.class);
                        DynamicEvidenceRecorderHolder.recordStepStarted(new DynamicEvidenceContext.StepContext(
                                "checkout",
                                "spring-context-1",
                                "spring-step-1",
                                12L,
                                System.currentTimeMillis(),
                                System.nanoTime()));
                    });

            contextRunner
                    .withConfiguration(AutoConfigurations.of(loadDynamicEvidenceAutoConfigurationClass()))
                    .withPropertyValues(
                            "simulator.dynamic-evidence.enabled=true",
                            "simulator.dynamic-evidence.output-dir=" + outputDir,
                            "simulator.dynamic-evidence.application-name=orders",
                            "simulator.dynamic-evidence.test-context.enabled=true")
                    .run(context -> {
                        assertThat(context).hasSingleBean(DynamicEvidenceRecorder.class);
                        DynamicEvidenceRecorderHolder.recordStepStarted(new DynamicEvidenceContext.StepContext(
                                "checkout",
                                "spring-context-2",
                                "spring-step-2",
                                13L,
                                System.currentTimeMillis(),
                                System.nanoTime()));
                    });

            DynamicEvidenceRecorderHolder.recordStepStarted(new DynamicEvidenceContext.StepContext(
                    "checkout",
                    "listener-after-context",
                    "listener-step-after",
                    14L,
                    System.currentTimeMillis(),
                    System.nanoTime()));

            listener.executionFinished(testIdentifier, TestExecutionResult.successful());
            listener.testPlanExecutionFinished(null);

            Path evidencePath = outputDir.resolve("dynamic-evidence.jsonl");
            Path manifestPath = outputDir.resolve("dynamic-evidence-manifest.json");
            assertThat(evidencePath).exists();
            assertThat(manifestPath).exists();

            List<JsonNode> events = Files.readAllLines(evidencePath).stream().map(this::parseJsonLine).toList();
            assertThat(events).hasSize(4);
            assertThat(events.stream().map(event -> event.get("functionalityInvocationId").asText()).toList())
                    .containsExactly(
                            "listener-before-context",
                            "spring-context-1",
                            "spring-context-2",
                            "listener-after-context");
            assertThat(events.stream().map(event -> event.get("sequence").asLong()).toList())
                    .containsExactly(1L, 2L, 3L, 4L);
            assertThat(events).allSatisfy(event -> {
                assertThat(event.get("testClassFqn").asText()).isEqualTo("example.MySpec");
                assertThat(event.get("testMethodName").asText()).isEqualTo("featureMethod");
                assertThat(event.get("testDisplayName").asText()).isEqualTo("feature display name");
                assertThat(event.get("testUniqueId").asText()).isEqualTo("[engine:spock]/[spec:example.MySpec]/[feature:featureMethod]");
            });

            JsonNode manifest = objectMapper.readTree(Files.readString(manifestPath));
            assertThat(manifest.get("counts").get("eventsWritten").asInt()).isEqualTo(4);
            assertThat(manifest.get("counts").get("STEP_STARTED").asInt()).isEqualTo(4);
            assertThat(manifest.get("counts").get("warnings").asInt()).isZero();
            assertThat(manifest.get("counts").get("writeErrors").asInt()).isZero();
        } finally {
            restoreProperty("simulator.dynamic-evidence.enabled", previousEnabled);
            restoreProperty("simulator.dynamic-evidence.output-dir", previousOutputDir);
            restoreProperty("simulator.dynamic-evidence.application-name", previousApplicationName);
            restoreProperty(DynamicEvidenceTestExecutionListener.TEST_CONTEXT_ENABLED_PROPERTY, previousTestContext);
            restoreProperty(DynamicEvidenceTestExecutionListener.JUNIT_PLATFORM_LISTENER_AUTODETECTION_ENABLED_PROPERTY, previousAutodetection);
            DynamicEvidenceTestContext.clear();
            DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
        }
    }

    private void restoreProperty(String propertyName, String value) {
        if (value == null) {
            System.clearProperty(propertyName);
            return;
        }
        System.setProperty(propertyName, value);
    }

    private JsonNode parseJsonLine(String line) {
        try {
            return objectMapper.readTree(line);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JSON line: " + line, e);
        }
    }

    private TestIdentifier testIdentifier() {
        TestIdentifier testIdentifier = mock(TestIdentifier.class);
        when(testIdentifier.isTest()).thenReturn(true);
        when(testIdentifier.getDisplayName()).thenReturn("feature display name");
        when(testIdentifier.getUniqueId()).thenReturn("[engine:spock]/[spec:example.MySpec]/[feature:featureMethod]");
        when(testIdentifier.getSource()).thenReturn(Optional.of(MethodSource.from("example.MySpec", "featureMethod")));
        return testIdentifier;
    }

    private Class<?> loadDynamicEvidenceAutoConfigurationClass() {
        List<String> matches = new java.util.ArrayList<>();
        for (String candidate : ImportCandidates.load(AutoConfiguration.class, getClass().getClassLoader())) {
            if (DynamicEvidenceConfiguration.class.getName().equals(candidate)) {
                matches.add(candidate);
            }
        }

        assertThat(matches)
                .as("dynamic evidence auto-configuration registration")
                .hasSize(1);

        try {
            return Class.forName(matches.getFirst());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to load dynamic evidence auto-configuration class", e);
        }
    }

    private static final class RecordingRecorder implements DynamicEvidenceRecorder {
        @Override
        public void record(DynamicEvidenceEvent event) {
        }

        @Override
        public void close() {
        }
    }
}
