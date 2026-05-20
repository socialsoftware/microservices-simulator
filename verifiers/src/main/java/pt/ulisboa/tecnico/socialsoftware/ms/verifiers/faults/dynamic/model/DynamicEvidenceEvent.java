package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DynamicEvidenceEvent(
        String eventId,
        String eventKind,
        String testClassFqn,
        String testMethodName,
        String testDisplayName,
        String testUniqueId,
        String inputVariantId,
        String functionalityName,
        String functionalityInvocationId,
        String stepName,
        Map<String, Object> payload,
        Path sourcePath,
        int lineNumber) {

    public DynamicEvidenceEvent {
        payload = payload == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(payload));
    }

    public String payloadText(String field) {
        Object value = payloadValue(field);
        if (value == null) {
            return null;
        }
        String text = value.toString();
        return text.isBlank() ? null : text;
    }

    public Object payloadValue(String field) {
        return payload.get(field);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> payloadMap(String field) {
        Object value = payloadValue(field);
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    public static Object compactValue(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> compact = new LinkedHashMap<>();
            map.forEach((key, nested) -> {
                if (key != null) {
                    compact.put(key.toString(), compactValue(nested));
                }
            });
            return Collections.unmodifiableMap(compact);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(DynamicEvidenceEvent::compactValue).toList();
        }
        return value;
    }
}
