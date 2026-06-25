package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeArgument;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeAssignment;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeMapEntry;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeNode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyAssignmentRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyRuntimeCallArgument;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyRuntimeCallRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceArgument;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueMetadata;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueResolutionCategory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

final class InputRecipeMapper {

    InputRecipe map(List<GroovyTraceArgument> traceArguments, InputResolutionStatus inputStatus) {
        List<GroovyTraceArgument> arguments = traceArguments == null ? List.of() : traceArguments.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(GroovyTraceArgument::index))
                .toList();
        if (arguments.stream().noneMatch(argument -> argument.recipe() != null)) {
            return null;
        }

        List<InputRecipeArgument> recipeArguments = new ArrayList<>();
        for (GroovyTraceArgument argument : arguments) {
            InputRecipeNode node = mapNode(argument.recipe(), argument.provenance(), argument.expectedTypeFqn());
            recipeArguments.add(toRecipeArgument(argument.index(), argument.expectedTypeFqn(), argument.provenance(), node));
        }

        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        recipeArguments.forEach(argument -> blockers.addAll(argument.blockers()));
        InputResolutionStatus safeStatus = inputStatus == null ? InputResolutionStatus.UNRESOLVED : inputStatus;
        boolean statusAllowsExecution = safeStatus == InputResolutionStatus.RESOLVED
                || safeStatus == InputResolutionStatus.REPLAYABLE;
        if (!statusAllowsExecution) {
            blockers.add("INPUT_STATUS_" + safeStatus.name());
        }
        boolean executorReady = statusAllowsExecution && recipeArguments.stream().allMatch(InputRecipeArgument::executorReady);
        return new InputRecipe(InputRecipe.SCHEMA_VERSION, null, executorReady, List.copyOf(blockers), recipeArguments);
    }

    private InputRecipeArgument toRecipeArgument(int index,
                                                 String expectedTypeFqn,
                                                 String provenanceText,
                                                 InputRecipeNode node) {
        boolean ready = node != null && node.executorReady();
        List<String> blockers = node == null ? List.of("MISSING_RECIPE_NODE") : node.blockers();
        return new InputRecipeArgument(index,
                expectedTypeFqn,
                statusForNode(node),
                ready,
                blockers,
                provenanceText,
                node);
    }

    private InputResolutionStatus statusForNode(InputRecipeNode node) {
        if (node == null || !node.blockers().isEmpty()) {
            return InputResolutionStatus.UNRESOLVED;
        }
        return node.executorReady() ? InputResolutionStatus.RESOLVED : InputResolutionStatus.PARTIAL;
    }

    private InputRecipeNode mapNode(GroovyValueRecipe recipe, String provenanceText, String expectedTypeFqn) {
        if (recipe == null) {
            return InputRecipeNode.builder("unresolved")
                    .provenanceText(provenanceText)
                    .executorReady(false)
                    .blockers(List.of("MISSING_RECIPE_NODE"))
                    .build();
        }

        GroovyValueMetadata metadata = recipe.metadata() == null
                ? GroovyValueMetadata.defaultMetadata()
                : recipe.metadata();
        GroovyValueResolutionCategory category = metadata.category() == null
                ? GroovyValueResolutionCategory.RESOLVED
                : metadata.category();
        String effectiveExpectedType = firstNonBlank(expectedTypeFqn, metadata.expectedTypeFqn());

        if (recipe.kind() == GroovyValueKind.UNRESOLVED_VARIABLE
                && category == GroovyValueResolutionCategory.EVENT_PLACEHOLDER) {
            return eventPlaceholderNode(recipe, provenanceText, metadata, effectiveExpectedType);
        }

        if (recipe.kind() == GroovyValueKind.UNRESOLVED_VARIABLE
                && (category == GroovyValueResolutionCategory.INJECTABLE_PLACEHOLDER
                || category == GroovyValueResolutionCategory.SOURCE_PLACEHOLDER)) {
            return placeholderNode(recipe, provenanceText, metadata, category, effectiveExpectedType);
        }

        if (recipe.kind() == GroovyValueKind.UNRESOLVED_RUNTIME_EDGE
                && (category == GroovyValueResolutionCategory.RUNTIME_CALL || metadata.runtimeCall() != null)) {
            return callResultNode(recipe, provenanceText, metadata, effectiveExpectedType);
        }

        return switch (recipe.kind()) {
            case LITERAL -> literalNode(recipe, provenanceText, effectiveExpectedType);
            case CONSTRUCTOR -> constructorNode(recipe, provenanceText, metadata, effectiveExpectedType);
            case COLLECTION_LITERAL -> collectionNode(recipe, provenanceText, effectiveExpectedType);
            case LOCAL_TRANSFORM -> transformNode(recipe, provenanceText, effectiveExpectedType);
            case HELPER_CALL_RESULT -> helperResultNode(recipe, provenanceText, effectiveExpectedType);
            case PROPERTY_ACCESS -> propertyAccessNode(recipe, provenanceText, effectiveExpectedType);
            case UNRESOLVED_VARIABLE, UNRESOLVED_RUNTIME_EDGE -> unresolvedNode(recipe, provenanceText, metadata);
        };
    }

    private InputRecipeNode literalNode(GroovyValueRecipe recipe, String provenanceText, String expectedTypeFqn) {
        LiteralValue literal = parseLiteral(recipe.text(), expectedTypeFqn);
        return InputRecipeNode.builder("literal")
                .sourceText(recipe.text())
                .provenanceText(provenanceText)
                .executorReady(true)
                .literalKind(literal.kind())
                .value(literal.value())
                .expectedTypeFqn(expectedTypeFqn)
                .build();
    }

    private InputRecipeNode constructorNode(GroovyValueRecipe recipe,
                                            String provenanceText,
                                            GroovyValueMetadata metadata,
                                            String expectedTypeFqn) {
        String targetTypeFqn = firstNonBlank(metadata.expectedTypeFqn(), expectedTypeFqn, qualifiedTypeText(recipe.text()));
        List<InputRecipeArgument> positionalArguments = new ArrayList<>();
        List<GroovyValueRecipe> children = recipe.children() == null ? List.of() : recipe.children();
        for (int index = 0; index < children.size(); index++) {
            InputRecipeNode childNode = mapNode(children.get(index), null, null);
            positionalArguments.add(toRecipeArgument(index, null, null, childNode));
        }

        List<InputRecipeAssignment> assignments = mapAssignments(metadata.assignments());
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        if (targetTypeFqn == null) {
            blockers.add("MISSING_TARGET_TYPE");
        }
        positionalArguments.forEach(argument -> blockers.addAll(argument.blockers()));
        assignments.forEach(assignment -> blockers.addAll(assignment.blockers()));
        blockers.addAll(duplicateAssignmentBlockers(assignments));

        boolean ready = targetTypeFqn != null
                && positionalArguments.stream().allMatch(InputRecipeArgument::executorReady)
                && assignments.stream().allMatch(InputRecipeAssignment::executorReady)
                && blockers.isEmpty();

        return InputRecipeNode.builder("constructor")
                .sourceText(recipe.text())
                .provenanceText(provenanceText)
                .executorReady(ready)
                .blockers(List.copyOf(blockers))
                .targetTypeFqn(targetTypeFqn)
                .targetTypeText(recipe.text())
                .arguments(positionalArguments)
                .assignments(assignments)
                .build();
    }

    private List<InputRecipeAssignment> mapAssignments(List<GroovyAssignmentRecipe> assignmentRecipes) {
        if (assignmentRecipes == null || assignmentRecipes.isEmpty()) {
            return List.of();
        }

        return assignmentRecipes.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(GroovyAssignmentRecipe::orderIndex)
                        .thenComparing(GroovyAssignmentRecipe::propertyName, Comparator.nullsFirst(String::compareTo)))
                .map(assignment -> {
                    InputRecipeNode valueNode = mapNode(assignment.valueRecipe(), assignment.sourceText(), null);
                    LinkedHashSet<String> blockers = new LinkedHashSet<>(valueNode.blockers());
                    if (assignment.blocker() != null && !assignment.blocker().isBlank()) {
                        blockers.add(assignment.blocker());
                    }
                    if (assignment.propertyName() == null || assignment.propertyName().isBlank()) {
                        blockers.add("MISSING_ASSIGNMENT_PROPERTY");
                    }
                    boolean ready = valueNode.executorReady() && blockers.isEmpty();
                    return new InputRecipeAssignment(
                            assignment.assignmentKind(),
                            assignment.propertyName(),
                            assignment.sourceName(),
                            assignment.orderIndex(),
                            assignment.sourceText(),
                            ready,
                            List.copyOf(blockers),
                            valueNode);
                })
                .toList();
    }

    private List<String> duplicateAssignmentBlockers(List<InputRecipeAssignment> assignments) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (InputRecipeAssignment assignment : assignments) {
            if (assignment.propertyName() != null) {
                counts.merge(assignment.propertyName(), 1, Integer::sum);
            }
        }
        return counts.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(entry -> "AMBIGUOUS_MULTI_WRITER:" + entry.getKey())
                .toList();
    }

    private InputRecipeNode collectionNode(GroovyValueRecipe recipe, String provenanceText, String expectedTypeFqn) {
        String collectionKind = collectionKind(recipe.text());
        List<GroovyValueRecipe> children = recipe.children() == null ? List.of() : recipe.children();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();

        if ("map".equals(collectionKind)) {
            List<InputRecipeMapEntry> entries = new ArrayList<>();
            for (int childIndex = 0; childIndex < children.size(); childIndex += 2) {
                InputRecipeNode keyNode = mapNode(children.get(childIndex), null, null);
                InputRecipeNode valueNode = childIndex + 1 < children.size()
                        ? mapNode(children.get(childIndex + 1), null, null)
                        : mapNode(null, null, null);
                blockers.addAll(keyNode.blockers());
                blockers.addAll(valueNode.blockers());
                entries.add(new InputRecipeMapEntry(childIndex / 2, keyNode, valueNode));
            }
            boolean ready = entries.stream().allMatch(entry -> entry.keyRecipe().executorReady() && entry.valueRecipe().executorReady())
                    && blockers.isEmpty();
            return InputRecipeNode.builder("collection")
                    .sourceText(recipe.text())
                    .provenanceText(provenanceText)
                    .executorReady(ready)
                    .blockers(List.copyOf(blockers))
                    .collectionKind(collectionKind)
                    .entries(entries)
                    .expectedTypeFqn(expectedTypeFqn)
                    .build();
        }

        List<InputRecipeNode> elements = new ArrayList<>();
        for (GroovyValueRecipe child : children) {
            InputRecipeNode element = mapNode(child, null, null);
            blockers.addAll(element.blockers());
            elements.add(element);
        }
        if ("unknown".equals(collectionKind)) {
            blockers.add("UNKNOWN_COLLECTION_KIND");
        }
        boolean ready = elements.stream().allMatch(InputRecipeNode::executorReady) && blockers.isEmpty();
        return InputRecipeNode.builder("collection")
                .sourceText(recipe.text())
                .provenanceText(provenanceText)
                .executorReady(ready)
                .blockers(List.copyOf(blockers))
                .collectionKind(collectionKind)
                .elements(elements)
                .expectedTypeFqn(expectedTypeFqn)
                .build();
    }

    private String collectionKind(String text) {
        String normalized = text == null ? null : text.trim().toLowerCase(Locale.ROOT);
        if ("list".equals(normalized) || "set".equals(normalized) || "map".equals(normalized)) {
            return normalized;
        }
        return "unknown";
    }

    private InputRecipeNode transformNode(GroovyValueRecipe recipe, String provenanceText, String expectedTypeFqn) {
        InputRecipeNode receiver = firstChild(recipe, expectedTypeFqn);
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        if (receiver == null) {
            blockers.add("MISSING_TRANSFORM_RECEIVER");
        } else {
            blockers.addAll(receiver.blockers());
            if (!receiver.executorReady()) {
                blockers.add("TRANSFORM_RECEIVER_NOT_READY");
            }
        }
        String transformName = recipe.text();
        if (!isSupportedTransform(transformName)) {
            blockers.add("UNSUPPORTED_TRANSFORM");
        }
        boolean ready = receiver != null && receiver.executorReady() && blockers.isEmpty();
        return InputRecipeNode.builder("local_transform")
                .sourceText(recipe.text())
                .provenanceText(provenanceText)
                .executorReady(ready)
                .blockers(List.copyOf(blockers))
                .transformName(transformName)
                .targetType(targetTypeForTransform(transformName))
                .receiver(receiver)
                .expectedTypeFqn(expectedTypeFqn)
                .build();
    }

    private InputRecipeNode helperResultNode(GroovyValueRecipe recipe, String provenanceText, String expectedTypeFqn) {
        InputRecipeNode result = firstChild(recipe, expectedTypeFqn);
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        if (result == null) {
            blockers.add("MISSING_HELPER_RESULT");
        } else {
            blockers.addAll(result.blockers());
        }
        boolean ready = result != null && result.executorReady() && blockers.isEmpty();
        return InputRecipeNode.builder("helper_result")
                .sourceText(recipe.text())
                .provenanceText(provenanceText)
                .executorReady(ready)
                .blockers(List.copyOf(blockers))
                .helperName(recipe.text())
                .resultRecipe(result)
                .expectedTypeFqn(expectedTypeFqn)
                .build();
    }

    private InputRecipeNode propertyAccessNode(GroovyValueRecipe recipe, String provenanceText, String expectedTypeFqn) {
        InputRecipeNode receiver = firstChild(recipe, null);
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        if (receiver == null) {
            blockers.add("MISSING_PROPERTY_RECEIVER");
        } else {
            blockers.addAll(receiver.blockers());
            if (!receiver.executorReady()) {
                blockers.add("PROPERTY_RECEIVER_NOT_READY");
            }
        }
        boolean ready = receiver != null && receiver.executorReady() && blockers.isEmpty();
        return InputRecipeNode.builder("property_access")
                .sourceText(recipe.text())
                .provenanceText(provenanceText)
                .executorReady(ready)
                .blockers(List.copyOf(blockers))
                .propertyName(recipe.text())
                .receiver(receiver)
                .expectedTypeFqn(expectedTypeFqn)
                .build();
    }

    private InputRecipeNode callResultNode(GroovyValueRecipe recipe,
                                           String provenanceText,
                                           GroovyValueMetadata metadata,
                                           String expectedTypeFqn) {
        GroovyRuntimeCallRecipe runtimeCall = metadata.runtimeCall();
        InputRecipeNode receiver = firstChild(recipe, null);
        String receiverReference = runtimeCall == null ? null : runtimeCall.receiverText();
        List<InputRecipeArgument> callArguments = mapRuntimeArguments(runtimeCall == null ? List.of() : runtimeCall.arguments());
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        String methodName = runtimeCall == null ? null : runtimeCall.methodName();
        if (methodName == null || methodName.isBlank()) {
            blockers.add("MISSING_METHOD_NAME");
        }
        if (receiver == null && (receiverReference == null || receiverReference.isBlank())) {
            blockers.add("MISSING_CALL_RECEIVER");
        }
        if (receiver != null) {
            blockers.addAll(receiver.blockers());
            if (!receiver.executorReady()) {
                blockers.add("CALL_RECEIVER_NOT_READY");
            }
        }
        callArguments.forEach(argument -> blockers.addAll(argument.blockers()));
        boolean receiverReady = receiver == null || receiver.executorReady();
        boolean ready = methodName != null && !methodName.isBlank()
                && (receiver != null || (receiverReference != null && !receiverReference.isBlank()))
                && receiverReady
                && callArguments.stream().allMatch(InputRecipeArgument::executorReady)
                && blockers.isEmpty();
        return InputRecipeNode.builder("call_result")
                .sourceText(runtimeCall == null ? recipe.text() : firstNonBlank(runtimeCall.sourceText(), recipe.text()))
                .provenanceText(provenanceText)
                .executorReady(ready)
                .blockers(List.copyOf(blockers))
                .receiver(receiver)
                .receiverReference(receiverReference)
                .methodName(methodName)
                .callArguments(callArguments)
                .expectedReturnTypeFqn(expectedTypeFqn)
                .internalCategory(metadata.category().name())
                .build();
    }

    private List<InputRecipeArgument> mapRuntimeArguments(List<GroovyRuntimeCallArgument> runtimeArguments) {
        if (runtimeArguments == null || runtimeArguments.isEmpty()) {
            return List.of();
        }
        return runtimeArguments.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(GroovyRuntimeCallArgument::index))
                .map(argument -> toRecipeArgument(argument.index(), null, argument.provenance(),
                        mapNode(argument.recipe(), argument.provenance(), null)))
                .toList();
    }

    private InputRecipeNode placeholderNode(GroovyValueRecipe recipe,
                                            String provenanceText,
                                            GroovyValueMetadata metadata,
                                            GroovyValueResolutionCategory category,
                                            String expectedTypeFqn) {
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        if (metadata.placeholderId() == null || metadata.placeholderId().isBlank()) {
            blockers.add("MISSING_PLACEHOLDER_ID");
        }
        boolean ready = blockers.isEmpty();
        return InputRecipeNode.builder("placeholder")
                .sourceText(recipe.text())
                .provenanceText(provenanceText)
                .executorReady(ready)
                .blockers(List.copyOf(blockers))
                .placeholderId(metadata.placeholderId())
                .placeholderPurpose(category == GroovyValueResolutionCategory.INJECTABLE_PLACEHOLDER
                        ? "injectable"
                        : "source_provided")
                .expectedTypeFqn(expectedTypeFqn)
                .internalCategory(category.name())
                .build();
    }

    private InputRecipeNode eventPlaceholderNode(GroovyValueRecipe recipe,
                                                 String provenanceText,
                                                 GroovyValueMetadata metadata,
                                                 String expectedTypeFqn) {
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        blockers.add("EVENT_PAYLOAD_PLACEHOLDER");
        if (metadata.placeholderId() == null || metadata.placeholderId().isBlank()) {
            blockers.add("MISSING_PLACEHOLDER_ID");
        }
        return InputRecipeNode.builder("event_placeholder")
                .sourceText(recipe.text())
                .provenanceText(provenanceText)
                .executorReady(false)
                .blockers(List.copyOf(blockers))
                .placeholderId(metadata.placeholderId())
                .placeholderPurpose("event_payload")
                .expectedTypeFqn(expectedTypeFqn)
                .internalCategory(metadata.category().name())
                .build();
    }

    private InputRecipeNode unresolvedNode(GroovyValueRecipe recipe,
                                           String provenanceText,
                                           GroovyValueMetadata metadata) {
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        if (metadata.category() == GroovyValueResolutionCategory.UNKNOWN_UNRESOLVED) {
            blockers.add("UNKNOWN_VALUE");
        }
        if (recipe.kind() == GroovyValueKind.UNRESOLVED_VARIABLE) {
            blockers.add("UNRESOLVED_VARIABLE");
        }
        if (recipe.kind() == GroovyValueKind.UNRESOLVED_RUNTIME_EDGE) {
            blockers.add("UNRESOLVED_RUNTIME_EDGE");
        }
        return InputRecipeNode.builder("unresolved")
                .sourceText(recipe.text())
                .provenanceText(provenanceText)
                .executorReady(false)
                .blockers(List.copyOf(blockers))
                .internalCategory(metadata.category() == null ? null : metadata.category().name())
                .build();
    }

    private InputRecipeNode firstChild(GroovyValueRecipe recipe, String expectedTypeFqn) {
        if (recipe.children() == null || recipe.children().isEmpty()) {
            return null;
        }
        return mapNode(recipe.children().get(0), null, expectedTypeFqn);
    }

    private boolean isSupportedTransform(String transformName) {
        if (transformName == null) {
            return false;
        }
        String normalized = transformName.trim();
        return "toSet".equals(normalized) || normalized.startsWith("as ");
    }

    private String targetTypeForTransform(String transformName) {
        if (transformName == null) {
            return null;
        }
        String normalized = transformName.trim();
        return normalized.startsWith("as ") && normalized.length() > 3 ? normalized.substring(3).trim() : null;
    }

    private String qualifiedTypeText(String text) {
        String normalized = text == null ? null : text.trim();
        return normalized != null && normalized.contains(".") ? normalized : null;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private LiteralValue parseLiteral(String sourceText, String expectedTypeFqn) {
        String text = sourceText == null ? null : sourceText.trim();
        if (text == null || "null".equals(text)) {
            return new LiteralValue("null", null);
        }
        if ("true".equals(text) || "false".equals(text)) {
            return new LiteralValue("boolean", Boolean.valueOf(text));
        }
        if (isQuoted(text)) {
            return new LiteralValue("string", text.substring(1, text.length() - 1));
        }
        if ("java.lang.String".equals(expectedTypeFqn) || "String".equals(expectedTypeFqn)) {
            return new LiteralValue("string", text);
        }
        if (text.matches("-?\\d+")) {
            try {
                return new LiteralValue("integer", Long.valueOf(text));
            } catch (NumberFormatException ignored) {
                return new LiteralValue("decimal", new BigDecimal(text));
            }
        }
        if (text.matches("-?\\d+\\.\\d+")) {
            return new LiteralValue("decimal", new BigDecimal(text));
        }
        return new LiteralValue("string", text);
    }

    private boolean isQuoted(String text) {
        return text.length() >= 2
                && ((text.startsWith("\"") && text.endsWith("\""))
                || (text.startsWith("'") && text.endsWith("'")));
    }

    private record LiteralValue(String kind, Object value) {
    }
}
