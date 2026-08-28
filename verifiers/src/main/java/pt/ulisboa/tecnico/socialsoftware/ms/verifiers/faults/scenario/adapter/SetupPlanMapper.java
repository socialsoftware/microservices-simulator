package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Maps only the closed value forms supported by source-derived setup. */
final class SetupPlanMapper {
    SetupPlan map(List<GroovyFacadeSetupActionTrace> traces,
                  List<ParticipantSource> participants) {
        List<GroovyFacadeSetupActionTrace> ordered = traces == null ? List.of() : traces.stream()
                .filter(Objects::nonNull)
                .toList();
        LinkedHashMap<String, String> actionIdByOccurrence = new LinkedHashMap<>();
        List<SetupAction> actions = new ArrayList<>();
        LinkedHashSet<String> planBlockers = new LinkedHashSet<>();
        for (int order = 0; order < ordered.size(); order++) {
            GroovyFacadeSetupActionTrace trace = ordered.get(order);
            String actionId = "setup-action-" + (order + 1);
            actionIdByOccurrence.put(trace.sourceOccurrence(), actionId);
            List<SetupArgument> arguments = trace.arguments().stream()
                    .sorted(Comparator.comparingInt(GroovyTraceArgument::index))
                    .map(argument -> {
                        SetupValueRecipe value = mapArgument(argument, actionIdByOccurrence);
                        return new SetupArgument(argument.index(), argument.expectedTypeFqn(), value,
                                value == null ? List.of("MISSING_SETUP_VALUE") : value.blockers());
                    })
                    .toList();
            actions.add(new SetupAction(actionId, order, trace.sourceOccurrence(), trace.methodKey(),
                    arguments, trace.declaredResultTypeFqn(), trace.voidResult(), trace.blockers()));
            planBlockers.addAll(trace.blockers());
        }

        List<SetupParticipantBinding> bindings = new ArrayList<>();
        if (participants != null) {
            for (ParticipantSource participant : participants) {
                for (GroovyTraceArgument argument : participant.arguments().stream()
                        .sorted(Comparator.comparingInt(GroovyTraceArgument::index)).toList()) {
                    if (argument.producerReference() == null
                            || !actionIdByOccurrence.containsKey(argument.producerReference().occurrenceId())) {
                        continue;
                    }
                    SetupValueRecipe value = referenceValue(argument.producerReference(),
                            argument.expectedTypeFqn(), actionIdByOccurrence);
                    bindings.add(new SetupParticipantBinding(participant.inputVariantId(), argument.index(),
                            argument.expectedTypeFqn(), value, value.blockers()));
                }
            }
        }
        return new SetupPlan(SetupPlan.SCHEMA_VERSION, actions, bindings, List.copyOf(planBlockers));
    }

    private SetupValueRecipe mapArgument(GroovyTraceArgument argument,
                                         Map<String, String> actionIdByOccurrence) {
        if (argument == null) return blocked("MISSING_SETUP_ARGUMENT", null);
        if (argument.producerReference() != null
                && actionIdByOccurrence.containsKey(argument.producerReference().occurrenceId())) {
            return referenceValue(argument.producerReference(), argument.expectedTypeFqn(), actionIdByOccurrence);
        }
        return mapValue(argument.recipe(), argument.expectedTypeFqn(), actionIdByOccurrence);
    }

    private SetupValueRecipe mapValue(GroovyValueRecipe source,
                                      String expectedType,
                                      Map<String, String> actionIdByOccurrence) {
        if (source != null && source.sourceReference() != null
                && actionIdByOccurrence.containsKey(source.sourceReference().occurrenceId())) {
            return referenceValue(source.sourceReference(), expectedType, actionIdByOccurrence);
        }
        if (source == null || source.kind() == null) return blocked("MISSING_SETUP_VALUE", expectedType);
        return switch (source.kind()) {
            case LITERAL -> literal(source.text(), expectedType);
            case CONSTRUCTOR -> constructor(source, expectedType, actionIdByOccurrence);
            case COLLECTION_LITERAL -> collection(source, expectedType, actionIdByOccurrence);
            case LOCAL_TRANSFORM -> localTransform(source, expectedType, actionIdByOccurrence);
            case HELPER_CALL_RESULT -> mapValue(firstChild(source), expectedType, actionIdByOccurrence);
            case PROPERTY_ACCESS -> blocked("UNRESOLVED_SETUP_PROPERTY", expectedType);
            case UNRESOLVED_RUNTIME_EDGE -> localDateTime(source, expectedType);
            case UNRESOLVED_VARIABLE -> blocked("UNSUPPORTED_SETUP_VALUE:" + source.kind(), expectedType);
        };
    }

    private SetupValueRecipe literal(String text, String expectedType) {
        String normalized = text == null ? null : text.trim();
        if ("true".equals(normalized) || "false".equals(normalized)) {
            return value(SetupValueKind.LITERAL, expectedType, "boolean", Boolean.valueOf(normalized));
        }
        if (normalized != null && normalized.matches("-?\\d+")) {
            try {
                return value(SetupValueKind.LITERAL, expectedType, "integer", Long.valueOf(normalized));
            } catch (NumberFormatException ignored) {
                return blocked("UNSUPPORTED_SETUP_INTEGER:" + normalized, expectedType);
            }
        }
        if (isQuoted(normalized)) {
            normalized = normalized.substring(1, normalized.length() - 1);
        }
        if (normalized != null) {
            return value(SetupValueKind.LITERAL, expectedType, "string", normalized);
        }
        return blocked("UNSUPPORTED_SETUP_LITERAL:" + text, expectedType);
    }

    private SetupValueRecipe constructor(GroovyValueRecipe source,
                                         String expectedType,
                                         Map<String, String> actionIdByOccurrence) {
        String targetType = source.metadata() == null ? null : source.metadata().expectedTypeFqn();
        if ("java.util.HashSet".equals(targetType)) {
            SetupValueRecipe child = mapValue(firstChild(source), "java.util.Collection", actionIdByOccurrence);
            List<SetupValueRecipe> elements = child.kind() == SetupValueKind.LIST || child.kind() == SetupValueKind.SET
                    ? child.elements() : List.of(child);
            return recipe(SetupValueKind.SET, "java.util.Set", null, null, null,
                    List.of(), List.of(), elements, null, null, null, child.blockers());
        }
        List<SetupValueRecipe> constructorArguments = source.children().stream()
                .map(child -> mapValue(child, null, actionIdByOccurrence)).toList();
        List<GroovyAssignmentRecipe> sourceAssignments = source.metadata() == null
                ? List.of() : source.metadata().assignments().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(GroovyAssignmentRecipe::orderIndex))
                .toList();
        List<SetupPropertyAssignment> assignments = new ArrayList<>();
        for (GroovyAssignmentRecipe assignment : sourceAssignments) {
            SetupValueRecipe mapped = mapValue(assignment.valueRecipe(), null, actionIdByOccurrence);
            assignments.add(new SetupPropertyAssignment(assignment.orderIndex(), assignment.propertyName(), mapped,
                    assignment.blocker() == null ? List.of() : List.of(assignment.blocker())));
        }
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        constructorArguments.forEach(value -> blockers.addAll(value.blockers()));
        assignments.forEach(value -> blockers.addAll(value.blockers()));
        return recipe(SetupValueKind.CONSTRUCTOR, expectedType, null, null, targetType,
                constructorArguments, assignments, List.of(), null, null, null, List.copyOf(blockers));
    }

    private SetupValueRecipe collection(GroovyValueRecipe source,
                                        String expectedType,
                                        Map<String, String> actionIdByOccurrence) {
        String collectionKind = source.text() == null ? "" : source.text().trim().toLowerCase();
        if (!"list".equals(collectionKind) && !"set".equals(collectionKind)) {
            return blocked("UNSUPPORTED_SETUP_COLLECTION:" + source.text(), expectedType);
        }
        List<SetupValueRecipe> elements = source.children().stream()
                .map(child -> mapValue(child, null, actionIdByOccurrence)).toList();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        elements.forEach(value -> blockers.addAll(value.blockers()));
        return recipe("set".equals(collectionKind) ? SetupValueKind.SET : SetupValueKind.LIST,
                expectedType, null, null, null, List.of(), List.of(), elements,
                null, null, null, List.copyOf(blockers));
    }

    private SetupValueRecipe localTransform(GroovyValueRecipe source,
                                            String expectedType,
                                            Map<String, String> actionIdByOccurrence) {
        SetupValueRecipe receiver = mapValue(firstChild(source), null, actionIdByOccurrence);
        if ("toSet".equals(source.text()) || "as Set".equals(source.text())) {
            List<SetupValueRecipe> elements = receiver.kind() == SetupValueKind.LIST
                    ? receiver.elements() : List.of(receiver);
            return recipe(SetupValueKind.SET, expectedType, null, null, null,
                    List.of(), List.of(), elements, null, null, null, receiver.blockers());
        }
        if ("DateHandler.toISOString".equals(source.text())) {
            return recipe(SetupValueKind.LOCAL_DATE_TO_STRING, "java.lang.String", null, null, null,
                    List.of(), List.of(), List.of(), receiver, null, null, receiver.blockers());
        }
        return blocked("UNSUPPORTED_SETUP_TRANSFORM:" + source.text(), expectedType);
    }

    private SetupValueRecipe localDateTime(GroovyValueRecipe source, String expectedType) {
        String text = source == null ? null : source.text();
        if (text != null && text.matches("DateHandler\\.now\\(\\)(\\.plusHours\\(1\\))?\\.plusMinutes\\((5|25)\\)")) {
            return value(SetupValueKind.LOCAL_DATE_TIME, "java.time.LocalDateTime",
                    "local_date_time_expression", text);
        }
        return blocked("UNSUPPORTED_LOCAL_DATE_EXPRESSION:" + text, expectedType);
    }

    private SetupValueRecipe referenceValue(GroovySourceValueReference reference,
                                             String expectedType,
                                             Map<String, String> actionIdByOccurrence) {
        String actionId = actionIdByOccurrence.get(reference.occurrenceId());
        if (reference.propertyPath().isEmpty()) {
            return SetupValueRecipe.actionResult(actionId, expectedType);
        }
        if (reference.propertyPath().size() == 1 && reference.propertyPath().get(0) != null) {
            return SetupValueRecipe.actionProperty(actionId, reference.propertyPath().get(0), expectedType);
        }
        return blocked("UNSUPPORTED_SETUP_PROPERTY_PATH:" + reference.propertyPath(), expectedType);
    }

    private GroovyValueRecipe firstChild(GroovyValueRecipe recipe) {
        return recipe == null || recipe.children().isEmpty() ? null : recipe.children().get(0);
    }

    private boolean isQuoted(String text) {
        return text != null && text.length() >= 2
                && ((text.startsWith("\"") && text.endsWith("\""))
                || (text.startsWith("'") && text.endsWith("'")));
    }

    private SetupValueRecipe value(SetupValueKind kind, String type, String literalKind, Object literalValue) {
        return recipe(kind, type, literalKind, literalValue, null,
                List.of(), List.of(), List.of(), null, null, null, List.of());
    }

    private SetupValueRecipe blocked(String blocker, String type) {
        return recipe(SetupValueKind.LITERAL, type, "blocked", null, null,
                List.of(), List.of(), List.of(), null, null, null, List.of(blocker));
    }

    private SetupValueRecipe recipe(SetupValueKind kind, String declaredType, String literalKind,
                                    Object literalValue, String targetType,
                                    List<SetupValueRecipe> constructorArguments,
                                    List<SetupPropertyAssignment> assignments,
                                    List<SetupValueRecipe> elements,
                                    SetupValueRecipe receiver, String actionId, String propertyName,
                                    List<String> blockers) {
        return new SetupValueRecipe(kind, declaredType, literalKind, literalValue, targetType,
                constructorArguments, assignments, elements, receiver, actionId, propertyName, blockers);
    }

    record ParticipantSource(String inputVariantId, List<GroovyTraceArgument> arguments) {
        ParticipantSource {
            arguments = arguments == null ? List.of() : List.copyOf(arguments);
        }
    }
}
