package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record InputRecipeNode(
        String kind,
        String sourceText,
        String provenanceText,
        boolean executorReady,
        List<String> blockers,
        String literalKind,
        Object value,
        String targetTypeFqn,
        String targetTypeText,
        List<InputRecipeArgument> arguments,
        List<InputRecipeAssignment> assignments,
        String collectionKind,
        List<InputRecipeNode> elements,
        List<InputRecipeMapEntry> entries,
        String transformName,
        String targetType,
        InputRecipeNode receiver,
        String receiverReference,
        String methodName,
        List<InputRecipeArgument> callArguments,
        String expectedReturnTypeFqn,
        String propertyName,
        String placeholderId,
        String placeholderPurpose,
        String expectedTypeFqn,
        String helperName,
        InputRecipeNode resultRecipe,
        String internalCategory) {

    public InputRecipeNode {
        kind = normalize(kind);
        sourceText = normalize(sourceText);
        provenanceText = normalize(provenanceText);
        blockers = InputRecipeCollections.stableStrings(blockers);
        literalKind = normalize(literalKind);
        targetTypeFqn = normalize(targetTypeFqn);
        targetTypeText = normalize(targetTypeText);
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
        assignments = assignments == null ? List.of() : List.copyOf(assignments);
        collectionKind = normalize(collectionKind);
        elements = elements == null ? List.of() : List.copyOf(elements);
        entries = entries == null ? List.of() : List.copyOf(entries);
        transformName = normalize(transformName);
        targetType = normalize(targetType);
        receiverReference = normalize(receiverReference);
        methodName = normalize(methodName);
        callArguments = callArguments == null ? List.of() : List.copyOf(callArguments);
        expectedReturnTypeFqn = normalize(expectedReturnTypeFqn);
        propertyName = normalize(propertyName);
        placeholderId = normalize(placeholderId);
        placeholderPurpose = normalize(placeholderPurpose);
        expectedTypeFqn = normalize(expectedTypeFqn);
        helperName = normalize(helperName);
        internalCategory = normalize(internalCategory);
    }

    public static Builder builder(String kind) {
        return new Builder(kind);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static final class Builder {
        private final String kind;
        private String sourceText;
        private String provenanceText;
        private boolean executorReady;
        private List<String> blockers = List.of();
        private String literalKind;
        private Object value;
        private String targetTypeFqn;
        private String targetTypeText;
        private List<InputRecipeArgument> arguments = List.of();
        private List<InputRecipeAssignment> assignments = List.of();
        private String collectionKind;
        private List<InputRecipeNode> elements = List.of();
        private List<InputRecipeMapEntry> entries = List.of();
        private String transformName;
        private String targetType;
        private InputRecipeNode receiver;
        private String receiverReference;
        private String methodName;
        private List<InputRecipeArgument> callArguments = List.of();
        private String expectedReturnTypeFqn;
        private String propertyName;
        private String placeholderId;
        private String placeholderPurpose;
        private String expectedTypeFqn;
        private String helperName;
        private InputRecipeNode resultRecipe;
        private String internalCategory;

        private Builder(String kind) {
            this.kind = kind;
        }

        public Builder sourceText(String sourceText) {
            this.sourceText = sourceText;
            return this;
        }

        public Builder provenanceText(String provenanceText) {
            this.provenanceText = provenanceText;
            return this;
        }

        public Builder executorReady(boolean executorReady) {
            this.executorReady = executorReady;
            return this;
        }

        public Builder blockers(List<String> blockers) {
            this.blockers = blockers;
            return this;
        }

        public Builder literalKind(String literalKind) {
            this.literalKind = literalKind;
            return this;
        }

        public Builder value(Object value) {
            this.value = value;
            return this;
        }

        public Builder targetTypeFqn(String targetTypeFqn) {
            this.targetTypeFqn = targetTypeFqn;
            return this;
        }

        public Builder targetTypeText(String targetTypeText) {
            this.targetTypeText = targetTypeText;
            return this;
        }

        public Builder arguments(List<InputRecipeArgument> arguments) {
            this.arguments = arguments;
            return this;
        }

        public Builder assignments(List<InputRecipeAssignment> assignments) {
            this.assignments = assignments;
            return this;
        }

        public Builder collectionKind(String collectionKind) {
            this.collectionKind = collectionKind;
            return this;
        }

        public Builder elements(List<InputRecipeNode> elements) {
            this.elements = elements;
            return this;
        }

        public Builder entries(List<InputRecipeMapEntry> entries) {
            this.entries = entries;
            return this;
        }

        public Builder transformName(String transformName) {
            this.transformName = transformName;
            return this;
        }

        public Builder targetType(String targetType) {
            this.targetType = targetType;
            return this;
        }

        public Builder receiver(InputRecipeNode receiver) {
            this.receiver = receiver;
            return this;
        }

        public Builder receiverReference(String receiverReference) {
            this.receiverReference = receiverReference;
            return this;
        }

        public Builder methodName(String methodName) {
            this.methodName = methodName;
            return this;
        }

        public Builder callArguments(List<InputRecipeArgument> callArguments) {
            this.callArguments = callArguments;
            return this;
        }

        public Builder expectedReturnTypeFqn(String expectedReturnTypeFqn) {
            this.expectedReturnTypeFqn = expectedReturnTypeFqn;
            return this;
        }

        public Builder propertyName(String propertyName) {
            this.propertyName = propertyName;
            return this;
        }

        public Builder placeholderId(String placeholderId) {
            this.placeholderId = placeholderId;
            return this;
        }

        public Builder placeholderPurpose(String placeholderPurpose) {
            this.placeholderPurpose = placeholderPurpose;
            return this;
        }

        public Builder expectedTypeFqn(String expectedTypeFqn) {
            this.expectedTypeFqn = expectedTypeFqn;
            return this;
        }

        public Builder helperName(String helperName) {
            this.helperName = helperName;
            return this;
        }

        public Builder resultRecipe(InputRecipeNode resultRecipe) {
            this.resultRecipe = resultRecipe;
            return this;
        }

        public Builder internalCategory(String internalCategory) {
            this.internalCategory = internalCategory;
            return this;
        }

        public InputRecipeNode build() {
            return new InputRecipeNode(kind,
                    sourceText,
                    provenanceText,
                    executorReady,
                    blockers,
                    literalKind,
                    value,
                    targetTypeFqn,
                    targetTypeText,
                    arguments,
                    assignments,
                    collectionKind,
                    elements,
                    entries,
                    transformName,
                    targetType,
                    receiver,
                    receiverReference,
                    methodName,
                    callArguments,
                    expectedReturnTypeFqn,
                    propertyName,
                    placeholderId,
                    placeholderPurpose,
                    expectedTypeFqn,
                    helperName,
                    resultRecipe,
                    internalCategory);
        }
    }
}
