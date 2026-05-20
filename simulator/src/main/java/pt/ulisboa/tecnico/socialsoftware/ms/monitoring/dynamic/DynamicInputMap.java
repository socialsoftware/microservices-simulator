package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import java.util.List;
import java.util.Objects;

public record DynamicInputMap(
        String schemaVersion,
        String generatedAt,
        List<String> selectedTestClassFqns,
        int inputCount,
        List<Entry> inputs) {

    static final String BASIS_TEST_FUNCTIONALITY_CLASS_STEP = "TEST_FUNCTIONALITY_CLASS_STEP";

    public DynamicInputMap {
        selectedTestClassFqns = selectedTestClassFqns == null ? List.of() : List.copyOf(selectedTestClassFqns);
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
    }

    public static DynamicInputMap empty() {
        return new DynamicInputMap(null, null, List.of(), 0, List.of());
    }

    DynamicInputAttribution resolve(DynamicEvidenceTestContext.TestIdentity testIdentity,
                                    String functionalityClassFqn,
                                    String stepName) {
        if (testIdentity == null || isBlank(testIdentity.testClassFqn())
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
            List<InputOwner> owners,
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
            owners = owners == null ? List.of() : List.copyOf(owners);
            stepNameHints = stepNameHints == null ? List.of() : List.copyOf(stepNameHints);
        }

        private boolean matches(DynamicEvidenceTestContext.TestIdentity testIdentity,
                                String functionalityClassFqn,
                                String stepName) {
            return !isBlank(inputVariantId)
                    && ownerMatches(testIdentity)
                    && Objects.equals(sagaFqn, functionalityClassFqn)
                    && stepNameHints.contains(stepName.trim());
        }

        private boolean ownerMatches(DynamicEvidenceTestContext.TestIdentity testIdentity) {
            if (!owners.isEmpty()) {
                return owners.stream().anyMatch(owner -> owner.matches(testIdentity));
            }
            return Objects.equals(sourceClassFqn, testIdentity.testClassFqn()) && methodMatches(testIdentity);
        }

        private boolean methodMatches(DynamicEvidenceTestContext.TestIdentity testIdentity) {
            return isBlank(sourceMethodName)
                    || Objects.equals(sourceMethodName, testIdentity.testMethodName())
                    || Objects.equals(sourceMethodName, testIdentity.testDisplayName());
        }
    }

    public record InputOwner(String testClassFqn, String testMethodName) {
        public InputOwner {
            testClassFqn = normalize(testClassFqn);
            testMethodName = normalize(testMethodName);
        }

        private boolean matches(DynamicEvidenceTestContext.TestIdentity testIdentity) {
            return Objects.equals(testClassFqn, testIdentity.testClassFqn())
                    && (Objects.equals(testMethodName, testIdentity.testMethodName())
                    || Objects.equals(testMethodName, testIdentity.testDisplayName()));
        }
    }
}
