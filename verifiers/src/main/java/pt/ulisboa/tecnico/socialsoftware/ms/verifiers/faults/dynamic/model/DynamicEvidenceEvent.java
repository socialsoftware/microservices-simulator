package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model;

import com.fasterxml.jackson.databind.JsonNode;

import java.nio.file.Path;

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
        JsonNode payload,
        Path sourcePath,
        int lineNumber,
        JsonNode raw) {

    public static DynamicEvidenceEvent fromJson(JsonNode node, Path sourcePath, int lineNumber) {
        return new DynamicEvidenceEvent(
                text(node, "eventId"),
                text(node, "eventKind"),
                text(node, "testClassFqn"),
                text(node, "testMethodName"),
                text(node, "testDisplayName"),
                text(node, "testUniqueId"),
                text(node, "inputVariantId"),
                text(node, "functionalityName"),
                text(node, "functionalityInvocationId"),
                text(node, "stepName"),
                node == null ? null : node.get("payload"),
                sourcePath,
                lineNumber,
                node);
    }

    private static String text(JsonNode node, String field) {
        if (node == null || !node.hasNonNull(field)) {
            return null;
        }
        String value = node.get(field).asText();
        return value == null || value.isBlank() ? null : value;
    }

    public String payloadText(String field) {
        if (payload == null || !payload.hasNonNull(field)) {
            return null;
        }
        String value = payload.get(field).asText();
        return value == null || value.isBlank() ? null : value;
    }
}
