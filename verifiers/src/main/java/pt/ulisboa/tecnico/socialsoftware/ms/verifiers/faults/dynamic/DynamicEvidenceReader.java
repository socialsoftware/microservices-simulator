package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceReadResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class DynamicEvidenceReader {
    private static final String EVIDENCE_FILE_NAME = "dynamic-evidence.jsonl";

    private final ObjectMapper objectMapper;

    public DynamicEvidenceReader() {
        this(new ObjectMapper());
    }

    public DynamicEvidenceReader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public DynamicEvidenceReadResult read(Path evidenceRoot) {
        if (evidenceRoot == null || !Files.exists(evidenceRoot)) {
            return readResult(List.of(), List.of(), 0, 0);
        }
        List<DynamicEvidenceEvent> events = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        List<Path> files;
        try (Stream<Path> stream = Files.walk(evidenceRoot)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> EVIDENCE_FILE_NAME.equals(path.getFileName().toString()))
                    .sorted(Comparator.comparing(Path::toString))
                    .toList();
        } catch (IOException e) {
            return readResult(List.of(), List.of("Failed to scan dynamic evidence root " + evidenceRoot + ": " + e.getMessage()), 0, 0);
        }
        long evidenceBytesRead = 0L;
        for (Path file : files) {
            try {
                evidenceBytesRead += Files.size(file);
            } catch (IOException e) {
                warnings.add(file + ": Failed to inspect dynamic evidence file size: " + e.getMessage());
            }
            readFile(file, events, warnings);
        }
        return readResult(events, warnings, files.size(), evidenceBytesRead);
    }

    private void readFile(Path file, List<DynamicEvidenceEvent> events, List<String> warnings) {
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.isBlank()) {
                    continue;
                }
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> node = objectMapper.readValue(line, Map.class);
                    if (node == null) {
                        warnings.add(file + ":" + lineNumber + ": Malformed dynamic evidence JSON: expected object");
                        continue;
                    }
                    events.add(fromMap(node, file, lineNumber));
                } catch (Exception e) {
                    warnings.add(file + ":" + lineNumber + ": Malformed dynamic evidence JSON: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            warnings.add(file + ": Failed to read dynamic evidence file: " + e.getMessage());
        }
    }

    private DynamicEvidenceEvent fromMap(Map<String, Object> node, Path sourcePath, int lineNumber) {
        return new DynamicEvidenceEvent(
                text(node, "eventId"),
                text(node, "eventKind"),
                text(node, "testClassFqn"),
                text(node, "testMethodName"),
                text(node, "testDisplayName"),
                text(node, "testUniqueId"),
                text(node, "inputVariantId"),
                text(node, "functionalityName"),
                text(node, "functionalityClassFqn"),
                text(node, "functionalityClassSimpleName"),
                text(node, "functionalityInvocationId"),
                text(node, "stepName"),
                payload(node.get("payload")),
                sourcePath,
                lineNumber);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> payload(Object value) {
        if (!(value instanceof Map<?, ?> map)) {
            return Map.of();
        }
        Map<String, Object> compact = new LinkedHashMap<>();
        map.forEach((key, nested) -> {
            if (key != null) {
                compact.put(key.toString(), DynamicEvidenceEvent.compactValue(nested));
            }
        });
        return compact;
    }

    private DynamicEvidenceReadResult readResult(List<DynamicEvidenceEvent> events, List<String> warnings, int evidenceFilesRead, long evidenceBytesRead) {
        int missingContext = (int) events.stream().filter(event -> event.testClassFqn() == null || event.testClassFqn().isBlank()).count();
        return new DynamicEvidenceReadResult(events, warnings, evidenceFilesRead, events.size(), missingContext, evidenceBytesRead);
    }

    private String text(Map<String, Object> node, String field) {
        Object value = node.get(field);
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }
}
