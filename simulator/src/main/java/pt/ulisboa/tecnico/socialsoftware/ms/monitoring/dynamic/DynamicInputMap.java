package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import java.util.List;
import java.util.Objects;

public record DynamicInputMap(
        String schemaVersion,
        String generatedAt,
        String testClassFqn,
        int inputCount,
        List<Entry> inputs) {

    static final String BASIS_TEST_FUNCTIONALITY_CLASS_STEP = "TEST_FUNCTIONALITY_CLASS_STEP";

    public DynamicInputMap {
        testClassFqn = normalize(testClassFqn);
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
    }

    public static DynamicInputMap empty() {
        return new DynamicInputMap(null, null, null, 0, List.of());
    }

    DynamicInputAttribution resolve(DynamicEvidenceTestContext.TestIdentity testIdentity,
                                    String functionalityClassFqn,
                                    String stepName) {
        if (testIdentity == null || isBlank(testIdentity.testClassFqn())
                || !Objects.equals(testClassFqn, testIdentity.testClassFqn())
                || isBlank(functionalityClassFqn)
                || isBlank(stepName)) {
            return DynamicInputAttribution.noMatch(BASIS_TEST_FUNCTIONALITY_CLASS_STEP);
        }

        List<Entry> candidates = inputs.stream()
                .filter(entry -> entry.matches(testIdentity, functionalityClassFqn, stepName))
                .toList();
        if (candidates.isEmpty()) {
            return DynamicInputAttribution.noMatch(BASIS_TEST_FUNCTIONALITY_CLASS_STEP);
        }

        List<String> candidateIds = candidates.stream()
                .map(Entry::inputVariantId)
                .filter(value -> !isBlank(value))
                .distinct()
                .sorted()
                .toList();
        if (candidateIds.size() == 1) {
            return DynamicInputAttribution.matched(BASIS_TEST_FUNCTIONALITY_CLASS_STEP, candidateIds.getFirst());
        }
        return DynamicInputAttribution.ambiguous(BASIS_TEST_FUNCTIONALITY_CLASS_STEP, candidateIds);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record Entry(
            String inputVariantId,
            String sagaFqn,
            String sourceClassFqn,
            String sourceMethodName,
            String sourceBindingName,
            String resolutionStatus,
            String sourceMode,
            String sourceModeConfidence,
            List<String> stepNameHints,
            List<String> literalArgumentValueHints,
            List<String> constructorArgumentSummaries,
            List<String> expectedCommands,
            List<String> expectedAggregateTypes,
            java.util.Map<String, String> logicalKeyBindings,
            List<String> scenarioPlanIds,
            String stableSourceText,
            String provenanceText,
            List<String> warnings) {

        public Entry {
            inputVariantId = normalize(inputVariantId);
            sagaFqn = normalize(sagaFqn);
            sourceClassFqn = normalize(sourceClassFqn);
            sourceMethodName = normalize(sourceMethodName);
            stepNameHints = stepNameHints == null ? List.of() : List.copyOf(stepNameHints);
        }

        private boolean matches(DynamicEvidenceTestContext.TestIdentity testIdentity,
                                String functionalityClassFqn,
                                String stepName) {
            return !isBlank(inputVariantId)
                    && Objects.equals(sourceClassFqn, testIdentity.testClassFqn())
                    && methodMatches(testIdentity)
                    && Objects.equals(sagaFqn, functionalityClassFqn)
                    && stepNameHints.contains(stepName.trim());
        }

        private boolean methodMatches(DynamicEvidenceTestContext.TestIdentity testIdentity) {
            return isBlank(sourceMethodName)
                    || Objects.equals(sourceMethodName, testIdentity.testMethodName())
                    || Objects.equals(sourceMethodName, testIdentity.testDisplayName());
        }
    }
}
