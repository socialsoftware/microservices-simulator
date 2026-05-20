package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

public class DynamicEvidenceJsonlRecorder implements DynamicEvidenceRecorder {
    private static final Logger logger = LoggerFactory.getLogger(DynamicEvidenceJsonlRecorder.class);
    private static final DateTimeFormatter RUN_DIR_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss-SSS")
            .withZone(ZoneOffset.UTC);
    private static final List<String> DEFAULT_EVENT_KIND_FIELDS = List.of(
            "STEP_STARTED",
            "COMMAND_SENT",
            "AGGREGATE_ACCESSED",
            "STEP_FINISHED"
    );

    private final DynamicEvidenceProperties properties;
    private final ObjectMapper objectMapper;
    private final String runId = UUID.randomUUID().toString();
    private final String startedAt = Instant.now().toString();
    private final AtomicLong sequence = new AtomicLong();
    private final Map<String, Long> eventKindCounts = new LinkedHashMap<>();
    private final List<String> warnings = new ArrayList<>();
    private final AtomicLong writeErrors = new AtomicLong();
    private final boolean enabled;
    private Path outputDirectory;
    private Path evidencePath;
    private Path manifestPath;
    private BufferedWriter writer;
    private boolean closed;
    private boolean installedInputMap;

    public DynamicEvidenceJsonlRecorder(DynamicEvidenceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.enabled = properties.isEnabled();
        if (enabled) {
            initializeWriter();
            initializeInputMap();
        }
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    boolean hasSameConfiguration(DynamicEvidenceProperties other) {
        return other != null
                && enabled == other.isEnabled()
                && Objects.equals(properties.getOutputRoot(), other.getOutputRoot())
                && Objects.equals(properties.getOutputDir(), other.getOutputDir())
                && Objects.equals(properties.getApplicationName(), other.getApplicationName())
                && Objects.equals(properties.getEvidenceFileName(), other.getEvidenceFileName())
                && Objects.equals(properties.getManifestFileName(), other.getManifestFileName())
                && Objects.equals(properties.getInputMapPath(), other.getInputMapPath())
                && properties.isIncludeCommandFields() == other.isIncludeCommandFields()
                && properties.isTestContextEnabled() == other.isTestContextEnabled()
                && properties.getMaxFieldDepth() == other.getMaxFieldDepth()
                && properties.getMaxFieldValueLength() == other.getMaxFieldValueLength();
    }

    @Override
    public synchronized void record(DynamicEvidenceEvent event) {
        if (!enabled || closed) {
            return;
        }
        try {
            long nextSequence = sequence.get() + 1;
            writer.write(objectMapper.writeValueAsString(toJsonObject(event, nextSequence)));
            writer.newLine();
            writer.flush();
            sequence.incrementAndGet();
            eventKindCounts.merge(event.getEventKind(), 1L, Long::sum);
        } catch (IOException | RuntimeException e) {
            writeErrors.incrementAndGet();
            warnings.add("Failed to write dynamic evidence event: " + e.getMessage());
            logger.warn("Failed to write dynamic evidence event", e);
        }
    }

    @Override
    public synchronized void close() {
        if (!enabled || closed) {
            return;
        }
        closed = true;
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            warnings.add("Failed to close dynamic evidence file: " + e.getMessage());
            logger.warn("Failed to close dynamic evidence file", e);
        }
        writeManifest();
        if (installedInputMap) {
            DynamicInputAttributionHolder.clear();
        }
        logger.info("Dynamic evidence recorder closed: eventsWritten={}, writeErrors={}, warnings={}, evidencePath={}, manifestPath={}",
                sequence.get(),
                writeErrors.get(),
                warnings.size(),
                evidencePath.toAbsolutePath().normalize(),
                manifestPath.toAbsolutePath().normalize());
    }

    private void initializeWriter() {
        String evidenceFileName = validateSimpleFileName("evidence-file-name", properties.getEvidenceFileName());
        String manifestFileName = validateSimpleFileName("manifest-file-name", properties.getManifestFileName());

        try {
            this.outputDirectory = resolveOutputDirectory();
            Files.createDirectories(outputDirectory);
            this.evidencePath = outputDirectory.resolve(evidenceFileName);
            this.manifestPath = outputDirectory.resolve(manifestFileName);
            this.writer = Files.newBufferedWriter(evidencePath, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            logger.info("Dynamic evidence recording enabled: outputDirectory={}, evidencePath={}, manifestPath={}",
                    outputDirectory.toAbsolutePath().normalize(),
                    evidencePath.toAbsolutePath().normalize(),
                    manifestPath.toAbsolutePath().normalize());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to initialize dynamic evidence recorder", e);
        }
    }

    private void initializeInputMap() {
        DynamicInputMapLoader.LoadResult loadResult = new DynamicInputMapLoader(objectMapper).load(properties.getInputMapPath());
        DynamicInputAttributionHolder.setInputMap(loadResult.inputMap(), loadResult.active());
        installedInputMap = true;
        warnings.addAll(loadResult.warnings());
    }

    private Path resolveOutputDirectory() {
        if (StringUtils.hasText(properties.getOutputDir())) {
            return Paths.get(properties.getOutputDir());
        }
        String app = sanitizePathPart(properties.getApplicationName());
        String timestamp = RUN_DIR_TIMESTAMP.format(Instant.now());
        return Paths.get(properties.getOutputRoot()).resolve("dynamic-evidence-" + app + "-" + timestamp);
    }

    private String sanitizePathPart(String value) {
        if (!StringUtils.hasText(value)) {
            return "application";
        }
        return value.replaceAll("[^A-Za-z0-9._-]", "-");
    }

    private String validateSimpleFileName(String propertyKey, String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalStateException(invalidFileNameMessage(propertyKey, fileName));
        }

        Path fileNamePath;
        try {
            fileNamePath = Paths.get(fileName);
        } catch (RuntimeException e) {
            throw new IllegalStateException(invalidFileNameMessage(propertyKey, fileName), e);
        }

        boolean invalidName = fileNamePath.isAbsolute()
                || fileName.contains("/")
                || fileName.contains("\\")
                || fileName.contains("..")
                || fileNamePath.getNameCount() != 1
                || !fileNamePath.getFileName().toString().equals(fileName);
        if (invalidName) {
            throw new IllegalStateException(invalidFileNameMessage(propertyKey, fileName));
        }

        return fileName;
    }

    private String invalidFileNameMessage(String propertyKey, String fileName) {
        return "Invalid simulator.dynamic-evidence." + propertyKey + " value '" + fileName
                + "': expected a simple file name without path segments";
    }

    private Map<String, Object> toJsonObject(DynamicEvidenceEvent event, long eventSequence) {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("schema", DynamicEvidenceEvent.SCHEMA);
        json.put("eventId", event.getEventId());
        json.put("eventKind", event.getEventKind());
        json.put("timestamp", Instant.now().toString());
        json.put("sequence", eventSequence);
        json.put("threadName", Thread.currentThread().getName());
        json.put("applicationName", properties.getApplicationName());
        addTestIdentity(json, event);
        json.put("functionalityName", event.getFunctionalityName());
        putIfPresent(json, "functionalityClassFqn", event.getFunctionalityClassFqn());
        putIfPresent(json, "functionalityClassSimpleName", event.getFunctionalityClassSimpleName());
        putIfPresent(json, "inputVariantId", event.getInputVariantId());
        json.put("functionalityInvocationId", event.getFunctionalityInvocationId());
        json.put("stepName", event.getStepName());
        json.put("unitOfWorkVersion", event.getUnitOfWorkVersion());
        json.put("payload", sanitizePayload(event.getPayload()));
        return json;
    }

    private void addTestIdentity(Map<String, Object> json, DynamicEvidenceEvent event) {
        if (!properties.isTestContextEnabled()) {
            return;
        }

        DynamicEvidenceTestContext.TestIdentity currentIdentity = DynamicEvidenceTestContext.current().orElse(null);
        putIfPresent(json, "testClassFqn", firstNonNull(event.getTestClassFqn(), currentIdentity == null ? null : currentIdentity.testClassFqn()));
        putIfPresent(json, "testMethodName", firstNonNull(event.getTestMethodName(), currentIdentity == null ? null : currentIdentity.testMethodName()));
        putIfPresent(json, "testDisplayName", firstNonNull(event.getTestDisplayName(), currentIdentity == null ? null : currentIdentity.testDisplayName()));
        putIfPresent(json, "testUniqueId", firstNonNull(event.getTestUniqueId(), currentIdentity == null ? null : currentIdentity.testUniqueId()));
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }

    private void putIfPresent(Map<String, Object> json, String key, Object value) {
        if (value != null) {
            json.put(key, value);
        }
    }

    private Map<String, Object> sanitizePayload(Map<String, Object> payload) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        if (payload == null) {
            return sanitized;
        }
        payload.forEach((key, value) -> sanitized.put(key, sanitizeValue(value, 0)));
        return sanitized;
    }

    private Object sanitizeValue(Object value, int depth) {
        if (value == null) {
            return null;
        }
        if (value instanceof Map<?, ?> map && depth < properties.getMaxFieldDepth()) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> sanitized.put(String.valueOf(key), sanitizeValue(nestedValue, depth + 1)));
            return sanitized;
        }
        if (value instanceof Iterable<?> iterable && depth < properties.getMaxFieldDepth()) {
            List<Object> sanitized = new ArrayList<>();
            for (Object nestedValue : iterable) {
                sanitized.add(sanitizeValue(nestedValue, depth + 1));
            }
            return sanitized;
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        String text = String.valueOf(value);
        int maxLength = properties.getMaxFieldValueLength();
        if (maxLength >= 0 && text.length() > maxLength) {
            return text.substring(0, maxLength);
        }
        return text;
    }

    private void writeManifest() {
        try {
            DynamicEvidenceManifest manifest = new DynamicEvidenceManifest();
            manifest.setRunId(runId);
            manifest.setGeneratedAt(Instant.now().toString());
            manifest.setStartedAt(startedAt);
            manifest.setFinishedAt(Instant.now().toString());
            manifest.setApplicationName(properties.getApplicationName());
            manifest.setEnabled(true);
            manifest.setEvidencePath(evidencePath.toAbsolutePath().normalize().toString());
            manifest.setManifestPath(manifestPath.toAbsolutePath().normalize().toString());
            manifest.setEffectiveConfig(effectiveConfig());
            manifest.setCounts(counts());
            manifest.setWarnings(warnings);
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), manifest);
            logger.info("Dynamic evidence manifest written to {}", manifestPath);
        } catch (IOException | RuntimeException e) {
            logger.warn("Failed to write dynamic evidence manifest", e);
        }
    }

    private Map<String, Object> counts() {
        Map<String, Object> counts = new LinkedHashMap<>();
        counts.put("eventsWritten", sequence.get());
        DEFAULT_EVENT_KIND_FIELDS.forEach(eventKind -> counts.put(eventKind, 0L));
        counts.putAll(eventKindCounts);
        counts.put("warnings", warnings.size());
        counts.put("writeErrors", writeErrors.get());
        return counts;
    }

    private Map<String, Object> effectiveConfig() {
        Map<String, Object> config = new LinkedHashMap<>();
        config.put("enabled", properties.isEnabled());
        config.put("outputRoot", properties.getOutputRoot());
        config.put("outputDir", properties.getOutputDir());
        config.put("applicationName", properties.getApplicationName());
        config.put("evidenceFileName", properties.getEvidenceFileName());
        config.put("manifestFileName", properties.getManifestFileName());
        config.put("inputMapPath", properties.getInputMapPath());
        config.put("includeCommandFields", properties.isIncludeCommandFields());
        config.put("testContextEnabled", properties.isTestContextEnabled());
        config.put("maxFieldDepth", properties.getMaxFieldDepth());
        config.put("maxFieldValueLength", properties.getMaxFieldValueLength());
        return config;
    }
}
