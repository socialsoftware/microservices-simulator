package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DynamicEvidenceRecorderTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @AfterEach
    void clearDynamicEvidenceTestContext() {
        DynamicEvidenceTestContext.clear();
        DynamicEvidenceContext.clear();
        DynamicInputAttributionHolder.clear();
        DynamicEvidenceRecorderHolder.setRecorder(new DynamicEvidenceNoopRecorder());
    }

    @Test
    void disabledRecorderCreatesNoArtifactsAndAcceptsRecordCalls() throws Exception {
        DynamicEvidenceProperties properties = new DynamicEvidenceProperties();
        properties.setEnabled(false);
        properties.setOutputRoot(tempDir.toString());
        properties.setApplicationName("orders");

        DynamicEvidenceRecorder recorder = new DynamicEvidenceJsonlRecorder(properties, objectMapper);
        recorder.record(DynamicEvidenceEvent.of("STEP_STARTED", "checkout", "invocation-1", "reserve", 7L,
                Map.of("key", "value")));
        recorder.close();

        try (var stream = Files.list(tempDir)) {
            assertThat(stream).isEmpty();
        }
    }

    @Test
    void enabledRecorderWritesValidJsonlAndManifestWithMatchingCounts() throws Exception {
        DynamicEvidenceProperties properties = enabledProperties();
        properties.setOutputDir(tempDir.resolve("evidence-run").toString());

        DynamicEvidenceRecorder recorder = new DynamicEvidenceJsonlRecorder(properties, objectMapper);
        recorder.record(DynamicEvidenceEvent.of("STEP_STARTED", "checkout", "invocation-1", "reserve", 7L,
                Map.of("aggregate", "cart")));
        recorder.record(DynamicEvidenceEvent.of("STEP_FINISHED", "checkout", "invocation-1", "reserve", 7L,
                Map.of("outcome", "SUCCESS")));
        recorder.close();

        Path evidencePath = tempDir.resolve("evidence-run/dynamic-evidence.jsonl");
        Path manifestPath = tempDir.resolve("evidence-run/dynamic-evidence-manifest.json");
        assertThat(evidencePath).exists();
        assertThat(manifestPath).exists();

        List<String> lines = Files.readAllLines(evidencePath);
        assertThat(lines).hasSize(2);
        List<JsonNode> events = lines.stream().map(this::parseJsonLine).toList();
        JsonNode firstEvent = events.getFirst();
        JsonNode secondEvent = events.get(1);
        assertThat(firstEvent.get("schema").asText()).isEqualTo("microservices-simulator.dynamic-evidence.v1");
        assertThat(firstEvent.get("eventKind").asText()).isEqualTo("STEP_STARTED");
        assertThat(firstEvent.get("sequence").asLong()).isEqualTo(1L);
        assertThat(firstEvent.get("applicationName").asText()).isEqualTo("orders");
        assertThat(firstEvent.get("payload").get("aggregate").asText()).isEqualTo("cart");
        assertThat(secondEvent.get("eventKind").asText()).isEqualTo("STEP_FINISHED");
        assertThat(secondEvent.get("sequence").asLong()).isEqualTo(2L);

        JsonNode manifest = objectMapper.readTree(manifestPath.toFile());
        JsonNode counts = manifest.get("counts");
        JsonNode effectiveConfig = manifest.get("effectiveConfig");
        assertThat(manifest.get("schema").asText()).isEqualTo("microservices-simulator.dynamic-evidence-manifest.v1");
        assertThat(manifest.get("enabled").asBoolean()).isTrue();
        assertThat(effectiveConfig.hasNonNull("testContextEnabled")).isTrue();
        assertThat(effectiveConfig.get("testContextEnabled").asBoolean()).isFalse();
        assertThat(counts.hasNonNull("eventsWritten")).isTrue();
        assertThat(counts.hasNonNull("STEP_STARTED")).isTrue();
        assertThat(counts.hasNonNull("COMMAND_SENT")).isTrue();
        assertThat(counts.hasNonNull("AGGREGATE_ACCESSED")).isTrue();
        assertThat(counts.hasNonNull("STEP_FINISHED")).isTrue();
        assertThat(counts.hasNonNull("warnings")).isTrue();
        assertThat(counts.hasNonNull("writeErrors")).isTrue();
        assertThat(counts.get("eventsWritten").asInt()).isEqualTo(lines.size());
        assertThat(counts.get("STEP_STARTED").asInt()).isEqualTo(1);
        assertThat(counts.get("COMMAND_SENT").asInt()).isZero();
        assertThat(counts.get("AGGREGATE_ACCESSED").asInt()).isZero();
        assertThat(counts.get("STEP_FINISHED").asInt()).isEqualTo(1);
        assertThat(counts.get("warnings").asInt()).isZero();
        assertThat(counts.get("writeErrors").asInt()).isZero();
    }

    @Test
    void enabledRecorderWritesFunctionalityClassIdentityWhenPresent() throws Exception {
        DynamicEvidenceProperties properties = enabledProperties();
        properties.setOutputDir(tempDir.resolve("with-functionality-class").toString());

        DynamicEvidenceRecorder recorder = new DynamicEvidenceJsonlRecorder(properties, objectMapper);
        recorder.record(DynamicEvidenceEvent.of(
                "STEP_STARTED",
                "checkout",
                "example.CheckoutSaga",
                "CheckoutSaga",
                "invocation-1",
                "reserve",
                7L,
                Map.of()));
        recorder.close();

        Path evidencePath = tempDir.resolve("with-functionality-class/dynamic-evidence.jsonl");
        JsonNode event = objectMapper.readTree(Files.readString(evidencePath));
        assertThat(event.get("functionalityName").asText()).isEqualTo("checkout");
        assertThat(event.get("functionalityClassFqn").asText()).isEqualTo("example.CheckoutSaga");
        assertThat(event.get("functionalityClassSimpleName").asText()).isEqualTo("CheckoutSaga");
    }

    @Test
    void enabledRecorderLoadsInputMapAndWritesResolvedInputVariantId() throws Exception {
        Path inputMapPath = tempDir.resolve("dynamic-input-map.json");
        Files.writeString(inputMapPath, """
                {
                  "schemaVersion": "microservices-simulator.dynamic-input-map.v1",
                  "generatedAt": "2026-05-12T00:00:00Z",
                  "testClassFqn": "example.OrderSpec",
                  "inputCount": 1,
                  "inputs": [
                    {
                      "inputVariantId": "input-1",
                      "sagaFqn": "example.OrderSaga",
                      "sourceClassFqn": "example.OrderSpec",
                      "sourceMethodName": "createsOrder",
                      "sourceBindingName": "order",
                      "resolutionStatus": "RESOLVED",
                      "sourceMode": "CONSTRUCTOR",
                      "sourceModeConfidence": "HIGH",
                      "stepNameHints": ["reserve"],
                      "literalArgumentValueHints": [],
                      "constructorArgumentSummaries": [],
                      "expectedCommands": [],
                      "expectedAggregateTypes": [],
                      "logicalKeyBindings": {},
                      "scenarioPlanIds": ["scenario-1"],
                      "stableSourceText": "new OrderSaga(order)",
                      "provenanceText": "test constructor",
                      "warnings": []
                    }
                  ]
                }
                """);
        DynamicEvidenceProperties properties = enabledProperties();
        properties.setOutputDir(tempDir.resolve("with-input-map").toString());
        properties.setTestContextEnabled(true);
        properties.setInputMapPath(inputMapPath.toString());
        DynamicEvidenceTestContext.set(new DynamicEvidenceTestContext.TestIdentity(
                "example.OrderSpec", "createsOrder", "createsOrder", "unique-id"));

        DynamicEvidenceRecorder recorder = new DynamicEvidenceJsonlRecorder(properties, objectMapper);
        DynamicEvidenceRecorderHolder.setRecorder(recorder);
        try (DynamicEvidenceContext.Scope scope = DynamicEvidenceContext.enterStep(
                "createOrder", "example.OrderSaga", "OrderSaga", "reserve", 7L)) {
            DynamicEvidenceRecorderHolder.recordStepStarted(scope.context());
        }
        recorder.close();

        Path evidencePath = tempDir.resolve("with-input-map/dynamic-evidence.jsonl");
        JsonNode event = objectMapper.readTree(Files.readString(evidencePath));
        JsonNode payload = event.get("payload");
        assertThat(event.get("inputVariantId").asText()).isEqualTo("input-1");
        assertThat(payload.get("inputVariantAttributionStatus").asText()).isEqualTo("MATCHED");
        assertThat(payload.get("inputVariantAttributionBasis").asText()).isEqualTo("TEST_FUNCTIONALITY_CLASS_STEP");
        assertThat(payload.get("candidateInputVariantIds")).extracting(JsonNode::asText).containsExactly("input-1");

        JsonNode manifest = objectMapper.readTree(tempDir.resolve("with-input-map/dynamic-evidence-manifest.json").toFile());
        assertThat(manifest.get("effectiveConfig").get("inputMapPath").asText()).isEqualTo(inputMapPath.toString());
        assertThat(manifest.get("counts").get("warnings").asInt()).isZero();
    }

    @Test
    void enabledRecorderWritesCurrentTestContextFieldsWhenPresent() throws Exception {
        DynamicEvidenceProperties properties = enabledProperties();
        properties.setOutputDir(tempDir.resolve("with-test-context").toString());
        properties.setTestContextEnabled(true);
        DynamicEvidenceTestContext.set(new DynamicEvidenceTestContext.TestIdentity(
                "example.MySpec", "featureMethod", "feature display name", "unique-id"));

        DynamicEvidenceRecorder recorder = new DynamicEvidenceJsonlRecorder(properties, objectMapper);
        recorder.record(DynamicEvidenceEvent.of("STEP_STARTED", "checkout", "invocation-1", "reserve", 7L,
                Map.of("aggregate", "cart")));
        recorder.close();

        Path evidencePath = tempDir.resolve("with-test-context/dynamic-evidence.jsonl");
        JsonNode event = objectMapper.readTree(Files.readString(evidencePath));
        assertThat(event.get("testClassFqn").asText()).isEqualTo("example.MySpec");
        assertThat(event.get("testMethodName").asText()).isEqualTo("featureMethod");
        assertThat(event.get("testDisplayName").asText()).isEqualTo("feature display name");
        assertThat(event.get("testUniqueId").asText()).isEqualTo("unique-id");
    }

    @Test
    void enabledRecorderOmitsTestContextFieldsWhenNoContextIsActive() throws Exception {
        DynamicEvidenceProperties properties = enabledProperties();
        properties.setOutputDir(tempDir.resolve("without-test-context").toString());

        DynamicEvidenceRecorder recorder = new DynamicEvidenceJsonlRecorder(properties, objectMapper);
        recorder.record(DynamicEvidenceEvent.of("STEP_STARTED", "checkout", "invocation-1", "reserve", 7L,
                Map.of("aggregate", "cart")));
        recorder.close();

        Path evidencePath = tempDir.resolve("without-test-context/dynamic-evidence.jsonl");
        JsonNode event = objectMapper.readTree(Files.readString(evidencePath));
        assertThat(event.has("testClassFqn")).isFalse();
        assertThat(event.has("testMethodName")).isFalse();
        assertThat(event.has("testDisplayName")).isFalse();
        assertThat(event.has("testUniqueId")).isFalse();
    }

    @Test
    void enabledRecorderOmitsTestContextFieldsWhenPropertyIsDisabledEvenIfContextIsSet() throws Exception {
        DynamicEvidenceProperties properties = enabledProperties();
        properties.setOutputDir(tempDir.resolve("disabled-test-context").toString());
        DynamicEvidenceTestContext.set(new DynamicEvidenceTestContext.TestIdentity(
                "example.MySpec", "featureMethod", "feature display name", "unique-id"));

        DynamicEvidenceRecorder recorder = new DynamicEvidenceJsonlRecorder(properties, objectMapper);
        recorder.record(DynamicEvidenceEvent.of("STEP_STARTED", "checkout", "invocation-1", "reserve", 7L,
                Map.of("aggregate", "cart")));
        recorder.close();

        Path evidencePath = tempDir.resolve("disabled-test-context/dynamic-evidence.jsonl");
        JsonNode event = objectMapper.readTree(Files.readString(evidencePath));
        assertThat(event.has("testClassFqn")).isFalse();
        assertThat(event.has("testMethodName")).isFalse();
        assertThat(event.has("testDisplayName")).isFalse();
        assertThat(event.has("testUniqueId")).isFalse();
    }

    @Test
    void defaultOutputRootCreatesApplicationTimestampRunDirectory() throws Exception {
        DynamicEvidenceProperties properties = enabledProperties();
        properties.setOutputRoot(tempDir.toString());
        properties.setOutputDir("");
        properties.setApplicationName("quiz-app");

        DynamicEvidenceRecorder recorder = new DynamicEvidenceJsonlRecorder(properties, objectMapper);
        recorder.record(DynamicEvidenceEvent.of("STEP_STARTED", "takeQuiz", "invocation-1", "answer", null, Map.of()));
        recorder.close();

        List<Path> runDirs;
        try (var stream = Files.list(tempDir)) {
            runDirs = stream.toList();
        }
        assertThat(runDirs).hasSize(1);
        String runDirName = runDirs.getFirst().getFileName().toString();
        assertThat(runDirName).matches("dynamic-evidence-quiz-app-\\d{8}-\\d{6}-\\d{3}");
        assertThat(runDirs.getFirst().resolve("dynamic-evidence.jsonl")).exists();
        assertThat(runDirs.getFirst().resolve("dynamic-evidence-manifest.json")).exists();
    }

    @Test
    void explicitOutputDirWritesDirectlyThere() throws Exception {
        Path explicitDir = tempDir.resolve("explicit");
        DynamicEvidenceProperties properties = enabledProperties();
        properties.setOutputRoot(tempDir.resolve("unused-root").toString());
        properties.setOutputDir(explicitDir.toString());

        DynamicEvidenceRecorder recorder = new DynamicEvidenceJsonlRecorder(properties, objectMapper);
        recorder.record(DynamicEvidenceEvent.of("COMMAND_SENT", "checkout", "invocation-1", "reserve", 8L,
                Map.of("commandType", "ReserveStock")));
        recorder.close();

        assertThat(explicitDir.resolve("dynamic-evidence.jsonl")).exists();
        assertThat(explicitDir.resolve("dynamic-evidence-manifest.json")).exists();
        assertThat(tempDir.resolve("unused-root")).doesNotExist();
    }

    @Test
    void enabledRecorderRejectsParentTraversalInEvidenceFileName() {
        DynamicEvidenceProperties properties = enabledProperties();
        properties.setOutputDir(tempDir.resolve("invalid-name").toString());
        properties.setEvidenceFileName("../outside.jsonl");

        assertThatThrownBy(() -> new DynamicEvidenceJsonlRecorder(properties, objectMapper))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("evidence-file-name")
                .hasMessageContaining("../outside.jsonl");
    }

    @Test
    void enabledRecorderRejectsAbsoluteManifestFileName() {
        DynamicEvidenceProperties properties = enabledProperties();
        properties.setOutputDir(tempDir.resolve("invalid-name").toString());
        String absoluteManifestPath = tempDir.resolve("outside-manifest.json").toAbsolutePath().toString();
        properties.setManifestFileName(absoluteManifestPath);

        assertThatThrownBy(() -> new DynamicEvidenceJsonlRecorder(properties, objectMapper))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("manifest-file-name")
                .hasMessageContaining(absoluteManifestPath);
    }

    @Test
    void manifestTracksWriteErrorsAndOnlyCountsSuccessfullyWrittenEvents() throws Exception {
        DynamicEvidenceProperties properties = enabledProperties();
        properties.setOutputDir(tempDir.resolve("write-errors").toString());

        DynamicEvidenceRecorder recorder = new DynamicEvidenceJsonlRecorder(properties, new FailFirstEventWriteObjectMapper());
        recorder.record(DynamicEvidenceEvent.of("STEP_STARTED", "checkout", "invocation-1", "reserve", 7L,
                Map.of("aggregate", "cart")));
        recorder.record(DynamicEvidenceEvent.of("STEP_FINISHED", "checkout", "invocation-1", "reserve", 7L,
                Map.of("outcome", "SUCCESS")));
        recorder.close();

        Path evidencePath = tempDir.resolve("write-errors/dynamic-evidence.jsonl");
        Path manifestPath = tempDir.resolve("write-errors/dynamic-evidence-manifest.json");
        List<String> lines = Files.readAllLines(evidencePath);
        assertThat(lines).hasSize(1);

        JsonNode onlyEvent = objectMapper.readTree(lines.getFirst());
        assertThat(onlyEvent.get("eventKind").asText()).isEqualTo("STEP_FINISHED");

        JsonNode manifest = objectMapper.readTree(manifestPath.toFile());
        JsonNode counts = manifest.get("counts");
        assertThat(counts.hasNonNull("eventsWritten")).isTrue();
        assertThat(counts.hasNonNull("STEP_STARTED")).isTrue();
        assertThat(counts.hasNonNull("COMMAND_SENT")).isTrue();
        assertThat(counts.hasNonNull("AGGREGATE_ACCESSED")).isTrue();
        assertThat(counts.hasNonNull("STEP_FINISHED")).isTrue();
        assertThat(counts.hasNonNull("warnings")).isTrue();
        assertThat(counts.hasNonNull("writeErrors")).isTrue();
        assertThat(counts.get("eventsWritten").asInt()).isEqualTo(1);
        assertThat(counts.get("STEP_STARTED").asInt()).isZero();
        assertThat(counts.get("COMMAND_SENT").asInt()).isZero();
        assertThat(counts.get("AGGREGATE_ACCESSED").asInt()).isZero();
        assertThat(counts.get("STEP_FINISHED").asInt()).isEqualTo(1);
        assertThat(counts.get("warnings").asInt()).isEqualTo(1);
        assertThat(counts.get("writeErrors").asInt()).isEqualTo(1);
    }

    private JsonNode parseJsonLine(String line) {
        try {
            return objectMapper.readTree(line);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Invalid JSONL line: " + line, e);
        }
    }

    private DynamicEvidenceProperties enabledProperties() {
        DynamicEvidenceProperties properties = new DynamicEvidenceProperties();
        properties.setEnabled(true);
        properties.setApplicationName("orders");
        return properties;
    }

    private static class FailFirstEventWriteObjectMapper extends ObjectMapper {
        private boolean failNextEventWrite = true;

        @Override
        public String writeValueAsString(Object value) throws JsonProcessingException {
            if (failNextEventWrite && value instanceof Map<?, ?> map
                    && DynamicEvidenceEvent.SCHEMA.equals(map.get("schema"))) {
                failNextEventWrite = false;
                throw new JsonProcessingException("simulated event write failure") {
                };
            }
            return super.writeValueAsString(value);
        }
    }
}
