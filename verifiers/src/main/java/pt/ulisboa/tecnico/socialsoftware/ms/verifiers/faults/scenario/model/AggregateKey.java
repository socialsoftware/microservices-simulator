package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

public record AggregateKey(
        String aggregateTypeName,
        String aggregateName,
        String keyText,
        FootprintConfidence confidence) {

    public AggregateKey {
        aggregateTypeName = normalize(aggregateTypeName);
        aggregateName = normalize(aggregateName);
        keyText = normalize(keyText);
        confidence = confidence == null ? FootprintConfidence.UNKNOWN : confidence;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
