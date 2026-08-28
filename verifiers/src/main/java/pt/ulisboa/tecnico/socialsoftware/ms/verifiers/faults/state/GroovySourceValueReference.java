package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.util.List;

/**
 * Source identity for a value produced by one caller-visible helper/facade call.
 * The occurrence id identifies the call site; the property path identifies the
 * selected part of that call's result.
 */
public record GroovySourceValueReference(
        String occurrenceId,
        String producerMethodName,
        List<String> propertyPath) {

    public GroovySourceValueReference {
        propertyPath = propertyPath == null ? List.of() : List.copyOf(propertyPath);
    }

    public GroovySourceValueReference appendProperty(String propertyName) {
        if (propertyName == null || propertyName.isBlank()) {
            return this;
        }
        java.util.ArrayList<String> path = new java.util.ArrayList<>(propertyPath);
        path.add(propertyName);
        return new GroovySourceValueReference(occurrenceId, producerMethodName, path);
    }
}
