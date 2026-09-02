package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record AggregateKey(
        String aggregateTypeName,
        String aggregateName,
        String keyText,
        FootprintConfidence confidence,
        Integer sourceConstructorArgumentIndex,
        List<String> sourcePropertyPath) {

    public AggregateKey(String aggregateTypeName,
                        String aggregateName,
                        String keyText,
                        FootprintConfidence confidence) {
        this(aggregateTypeName, aggregateName, keyText, confidence, null, List.of());
    }

    public AggregateKey {
        aggregateTypeName = normalize(aggregateTypeName);
        aggregateName = normalize(aggregateName);
        keyText = normalize(keyText);
        confidence = confidence == null ? FootprintConfidence.UNKNOWN : confidence;
        sourcePropertyPath = sourcePropertyPath == null ? List.of() : List.copyOf(sourcePropertyPath);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
