package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceEvent;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceReadResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
            return new DynamicEvidenceReadResult(List.of(), List.of(), 0);
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
            return new DynamicEvidenceReadResult(List.of(), List.of("Failed to scan dynamic evidence root " + evidenceRoot + ": " + e.getMessage()), 0);
        }
        for (Path file : files) {
            readFile(file, events, warnings);
        }
        return new DynamicEvidenceReadResult(events, warnings, files.size());
    }

    private void readFile(Path file, List<DynamicEvidenceEvent> events, List<String> warnings) {
        List<String> lines;
        try {
            lines = Files.readAllLines(file);
        } catch (IOException e) {
            warnings.add(file + ": Failed to read dynamic evidence file: " + e.getMessage());
            return;
        }
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int lineNumber = i + 1;
            if (line == null || line.isBlank()) {
                continue;
            }
            try {
                JsonNode node = objectMapper.readTree(line);
                if (node == null || !node.isObject()) {
                    warnings.add(file + ":" + lineNumber + ": Malformed dynamic evidence JSON: expected object");
                    continue;
                }
                events.add(DynamicEvidenceEvent.fromJson(node, file, lineNumber));
            } catch (Exception e) {
                warnings.add(file + ":" + lineNumber + ": Malformed dynamic evidence JSON: " + e.getMessage());
            }
        }
    }
}
