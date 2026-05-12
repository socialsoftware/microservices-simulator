package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class DynamicInputMapLoader {
    private final ObjectMapper objectMapper;

    public DynamicInputMapLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public LoadResult load(String inputMapPath) {
        if (!StringUtils.hasText(inputMapPath)) {
            return LoadResult.disabled();
        }

        Path path;
        try {
            path = Path.of(inputMapPath).toAbsolutePath().normalize();
        } catch (RuntimeException e) {
            return LoadResult.failed("Invalid simulator.dynamic-evidence.input-map-path '" + inputMapPath + "': " + e.getMessage());
        }

        if (!Files.isRegularFile(path)) {
            return LoadResult.failed("Dynamic input map does not exist or is not a regular file: " + path);
        }

        try {
            DynamicInputMap inputMap = objectMapper.readValue(path.toFile(), DynamicInputMap.class);
            return new LoadResult(inputMap, true, List.of());
        } catch (Exception e) {
            return LoadResult.failed("Failed to load dynamic input map " + path + ": " + e.getMessage());
        }
    }

    public record LoadResult(DynamicInputMap inputMap, boolean active, List<String> warnings) {
        public LoadResult {
            inputMap = inputMap == null ? DynamicInputMap.empty() : inputMap;
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }

        private static LoadResult disabled() {
            return new LoadResult(DynamicInputMap.empty(), false, List.of());
        }

        private static LoadResult failed(String warning) {
            return new LoadResult(DynamicInputMap.empty(), false, List.of(warning));
        }
    }
}
