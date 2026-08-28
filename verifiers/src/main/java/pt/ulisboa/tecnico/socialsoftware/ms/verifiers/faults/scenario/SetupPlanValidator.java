package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The single static authority for the closed source-derived setup contract. */
public final class SetupPlanValidator {
    private static final Set<String> RESULT_PROPERTIES = Set.of("aggregateId", "courseAggregateId");
    private static final Pattern METHOD_KEY = Pattern.compile("^([^#]+)#([^#(]+)\\(([^)]*)\\):(.+)$");
    private static final Map<String, Map<String, String>> DTO_PROPERTIES = Map.of(
            "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto", Map.of(
                    "name", "java.lang.String", "type", "java.lang.String", "acronym", "java.lang.String",
                    "academicTerm", "java.lang.String", "endDate", "java.lang.String"),
            "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto", Map.of(
                    "name", "java.lang.String", "username", "java.lang.String", "role", "java.lang.String"),
            "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto", Map.of(
                    "name", "java.lang.String"),
            "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto", Map.of(
                    "title", "java.lang.String", "content", "java.lang.String",
                    "topicDto", "java.util.Set<pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto>",
                    "optionDtos", "java.util.List<pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.OptionDto>"),
            "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.OptionDto", Map.of(
                    "sequence", "java.lang.Integer", "correct", "java.lang.Boolean", "content", "java.lang.String"),
            "pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto", Map.of(
                    "startTime", "java.lang.String", "endTime", "java.lang.String",
                    "numberOfQuestions", "java.lang.Integer"));

    public ValidationResult validate(SetupPlan plan) {
        return validate(plan, List.of());
    }

    public ValidationResult validate(SetupPlan plan, List<InputVariant> acceptedInputs) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        if (plan == null) {
            return new ValidationResult(true, List.of());
        }
        if (!SetupPlan.SCHEMA_VERSION.equals(plan.schemaVersion())) {
            diagnostics.add(new Diagnostic("UNSUPPORTED_SETUP_SCHEMA", String.valueOf(plan.schemaVersion())));
        }
        plan.blockers().forEach(blocker -> diagnostics.add(new Diagnostic("SETUP_BLOCKER", blocker)));
        if (plan.actions().isEmpty()) {
            diagnostics.add(new Diagnostic("EMPTY_SETUP_PLAN", "setup plan requires actions"));
        }

        LinkedHashMap<String, SetupAction> actionsById = new LinkedHashMap<>();
        Set<String> occurrences = new HashSet<>();
        for (int index = 0; index < plan.actions().size(); index++) {
            SetupAction action = plan.actions().get(index);
            if (action == null) {
                diagnostics.add(new Diagnostic("MISSING_SETUP_ACTION", Integer.toString(index)));
                continue;
            }
            if (action.orderIndex() != index) {
                diagnostics.add(new Diagnostic("INVALID_SETUP_ACTION_ORDER", action.actionId()));
            }
            if (action.actionId() == null || action.actionId().isBlank()
                    || actionsById.putIfAbsent(action.actionId(), action) != null) {
                diagnostics.add(new Diagnostic("DUPLICATE_OR_MISSING_SETUP_ACTION_ID", String.valueOf(action.actionId())));
            }
            if (action.sourceOccurrence() == null || action.sourceOccurrence().isBlank()
                    || !occurrences.add(action.sourceOccurrence())) {
                diagnostics.add(new Diagnostic("DUPLICATE_OR_MISSING_SOURCE_OCCURRENCE", String.valueOf(action.sourceOccurrence())));
            }
            validateMethodKey(action, diagnostics);
            if (action.voidResult() != "void".equals(action.declaredResultTypeFqn())) {
                diagnostics.add(new Diagnostic("INVALID_SETUP_RESULT_DECLARATION", action.actionId()));
            }
            action.blockers().forEach(blocker -> diagnostics.add(new Diagnostic("SETUP_ACTION_BLOCKER", blocker)));
            validateArguments(action.arguments(), actionsById, action.orderIndex(), diagnostics);
        }

        Map<String, InputVariant> inputsById = new LinkedHashMap<>();
        if (acceptedInputs != null) {
            acceptedInputs.stream().filter(Objects::nonNull)
                    .forEach(input -> inputsById.put(input.deterministicId(), input));
        }
        Set<String> bindingKeys = new HashSet<>();
        for (SetupParticipantBinding binding : plan.participantBindings()) {
            if (binding == null || binding.inputVariantId() == null || binding.argumentIndex() < 0
                    || !bindingKeys.add(binding.inputVariantId() + "#" + binding.argumentIndex())) {
                diagnostics.add(new Diagnostic("MALFORMED_SETUP_PARTICIPANT_BINDING", String.valueOf(binding)));
                continue;
            }
            binding.blockers().forEach(blocker -> diagnostics.add(new Diagnostic("SETUP_BINDING_BLOCKER", blocker)));
            validateValue(binding.value(), binding.expectedTypeFqn(), actionsById,
                    plan.actions().size(), diagnostics);
            if (!inputsById.isEmpty()) {
                validateParticipantBinding(binding, inputsById, diagnostics);
            }
        }
        return new ValidationResult(diagnostics.isEmpty(), diagnostics);
    }

    private void validateMethodKey(SetupAction action, List<Diagnostic> diagnostics) {
        Matcher matcher = action.methodKey() == null ? null : METHOD_KEY.matcher(action.methodKey());
        if (matcher == null || !matcher.matches()) {
            diagnostics.add(new Diagnostic("MALFORMED_SETUP_METHOD_KEY", String.valueOf(action.methodKey())));
            return;
        }
        List<String> parameterTypes = matcher.group(3).isBlank()
                ? List.of() : List.of(matcher.group(3).split(",", -1));
        List<String> argumentTypes = action.arguments().stream()
                .map(SetupArgument::expectedTypeFqn).toList();
        if (!parameterTypes.equals(argumentTypes)
                || !Objects.equals(matcher.group(4), action.declaredResultTypeFqn())) {
            diagnostics.add(new Diagnostic("SETUP_METHOD_KEY_SIGNATURE_MISMATCH", action.actionId()));
        }
    }

    private void validateParticipantBinding(SetupParticipantBinding binding,
                                            Map<String, InputVariant> inputsById,
                                            List<Diagnostic> diagnostics) {
        InputVariant input = inputsById.get(binding.inputVariantId());
        if (input == null) {
            diagnostics.add(new Diagnostic("SETUP_BINDING_UNKNOWN_PARTICIPANT_INPUT", binding.inputVariantId()));
            return;
        }
        InputRecipe recipe = input.inputRecipe();
        InputRecipeArgument argument = recipe == null ? null : recipe.arguments().stream()
                .filter(candidate -> candidate.index() == binding.argumentIndex())
                .findFirst().orElse(null);
        if (argument == null || !Objects.equals(argument.expectedTypeFqn(), binding.expectedTypeFqn())) {
            diagnostics.add(new Diagnostic("SETUP_BINDING_INPUT_TYPE_MISMATCH",
                    binding.inputVariantId() + "#" + binding.argumentIndex()));
        }
    }

    private void validateArguments(List<SetupArgument> arguments,
                                   Map<String, SetupAction> earlierActions,
                                   int actionOrder,
                                   List<Diagnostic> diagnostics) {
        for (int index = 0; index < arguments.size(); index++) {
            SetupArgument argument = arguments.get(index);
            if (argument == null || argument.index() != index) {
                diagnostics.add(new Diagnostic("INVALID_SETUP_ARGUMENT_ORDER", Integer.toString(index)));
                continue;
            }
            argument.blockers().forEach(blocker -> diagnostics.add(new Diagnostic("SETUP_ARGUMENT_BLOCKER", blocker)));
            validateValue(argument.value(), argument.expectedTypeFqn(), earlierActions, actionOrder, diagnostics);
        }
    }

    private void validateValue(SetupValueRecipe value,
                               String expectedType,
                               Map<String, SetupAction> actionsById,
                               int referenceBoundary,
                               List<Diagnostic> diagnostics) {
        if (value == null || value.kind() == null) {
            diagnostics.add(new Diagnostic("MISSING_SETUP_VALUE", String.valueOf(expectedType)));
            return;
        }
        value.blockers().forEach(blocker -> diagnostics.add(new Diagnostic("SETUP_VALUE_BLOCKER", blocker)));
        validateValueShape(value, diagnostics);
        switch (value.kind()) {
            case LITERAL -> {
                String actualType = literalType(value.literalKind(), value.literalValue());
                if (actualType == null || !compatible(actualType, value.declaredTypeFqn())
                        || !compatible(actualType, expectedType)) {
                    diagnostics.add(new Diagnostic("INCOMPATIBLE_SETUP_LITERAL", String.valueOf(expectedType)));
                }
            }
            case ACTION_RESULT, ACTION_RESULT_PROPERTY -> {
                SetupAction source = actionsById.get(value.actionId());
                if (source == null || source.orderIndex() >= referenceBoundary || source.voidResult()) {
                    diagnostics.add(new Diagnostic("INVALID_SETUP_RESULT_REFERENCE", String.valueOf(value.actionId())));
                    return;
                }
                String actualType = source.declaredResultTypeFqn();
                if (value.kind() == SetupValueKind.ACTION_RESULT_PROPERTY) {
                    if (!RESULT_PROPERTIES.contains(value.propertyName()) || !actualType.endsWith("Dto")) {
                        diagnostics.add(new Diagnostic("UNSUPPORTED_SETUP_RESULT_PROPERTY", String.valueOf(value.propertyName())));
                        return;
                    }
                    actualType = "java.lang.Integer";
                }
                if (!compatible(actualType, value.declaredTypeFqn())
                        || !compatible(actualType, expectedType)) {
                    diagnostics.add(new Diagnostic("INCOMPATIBLE_SETUP_RESULT_REFERENCE", value.actionId()));
                }
            }
            case CONSTRUCTOR -> {
                Map<String, String> properties = value.targetTypeFqn() == null
                        ? null : DTO_PROPERTIES.get(value.targetTypeFqn());
                if (properties == null || !compatible(value.targetTypeFqn(), expectedType)
                        || !value.constructorArguments().isEmpty()) {
                    diagnostics.add(new Diagnostic("UNSUPPORTED_SETUP_CONSTRUCTOR", String.valueOf(value.targetTypeFqn())));
                    return;
                }
                Set<String> assigned = new HashSet<>();
                for (int index = 0; index < value.assignments().size(); index++) {
                    SetupPropertyAssignment assignment = value.assignments().get(index);
                    String propertyType = assignment == null ? null : properties.get(assignment.propertyName());
                    if (assignment == null || assignment.orderIndex() != index || propertyType == null
                            || !assigned.add(assignment.propertyName())) {
                        diagnostics.add(new Diagnostic("UNSUPPORTED_SETUP_ASSIGNMENT", String.valueOf(assignment)));
                        continue;
                    }
                    assignment.blockers().forEach(blocker -> diagnostics.add(new Diagnostic("SETUP_ASSIGNMENT_BLOCKER", blocker)));
                    validateValue(assignment.value(), propertyType, actionsById, referenceBoundary, diagnostics);
                }
            }
            case LIST, SET -> {
                boolean list = value.kind() == SetupValueKind.LIST;
                if (list && expectedType != null && !isListType(expectedType)
                        || !list && expectedType != null && !isSetType(expectedType)) {
                    diagnostics.add(new Diagnostic("INCOMPATIBLE_SETUP_COLLECTION", String.valueOf(expectedType)));
                }
                String elementType = collectionElementType(expectedType);
                value.elements().forEach(element -> validateValue(
                        element, elementType, actionsById, referenceBoundary, diagnostics));
            }
            case LOCAL_DATE_TIME -> {
                String expression = value.literalValue() instanceof String text ? text : null;
                if (!compatible("java.time.LocalDateTime", value.declaredTypeFqn())
                        || expression == null || !expression.matches(
                        "DateHandler\\.now\\(\\)(\\.plusHours\\(1\\))?\\.plusMinutes\\((5|25)\\)")) {
                    diagnostics.add(new Diagnostic("UNSUPPORTED_LOCAL_DATE_EXPRESSION", String.valueOf(expression)));
                }
            }
            case LOCAL_DATE_TO_STRING -> {
                if (!compatible("java.lang.String", expectedType) || value.receiver() == null) {
                    diagnostics.add(new Diagnostic("INVALID_LOCAL_DATE_TO_STRING", String.valueOf(expectedType)));
                } else {
                    validateValue(value.receiver(), null, actionsById, referenceBoundary, diagnostics);
                }
            }
        }
    }

    private void validateValueShape(SetupValueRecipe value, List<Diagnostic> diagnostics) {
        boolean invalid = switch (value.kind()) {
            case LITERAL -> value.targetTypeFqn() != null || !value.constructorArguments().isEmpty()
                    || !value.assignments().isEmpty() || !value.elements().isEmpty()
                    || value.receiver() != null || value.actionId() != null || value.propertyName() != null;
            case CONSTRUCTOR -> value.literalKind() != null || value.literalValue() != null
                    || value.targetTypeFqn() == null || !value.elements().isEmpty()
                    || value.receiver() != null || value.actionId() != null || value.propertyName() != null;
            case LIST, SET -> value.literalKind() != null || value.literalValue() != null
                    || value.targetTypeFqn() != null || !value.constructorArguments().isEmpty()
                    || !value.assignments().isEmpty() || value.receiver() != null
                    || value.actionId() != null || value.propertyName() != null;
            case LOCAL_DATE_TIME -> !"local_date_time_expression".equals(value.literalKind())
                    || !(value.literalValue() instanceof String) || value.targetTypeFqn() != null
                    || !value.constructorArguments().isEmpty() || !value.assignments().isEmpty()
                    || !value.elements().isEmpty() || value.receiver() != null
                    || value.actionId() != null || value.propertyName() != null;
            case LOCAL_DATE_TO_STRING -> value.literalKind() != null || value.literalValue() != null
                    || value.targetTypeFqn() != null || !value.constructorArguments().isEmpty()
                    || !value.assignments().isEmpty() || !value.elements().isEmpty()
                    || value.receiver() == null || value.actionId() != null || value.propertyName() != null;
            case ACTION_RESULT -> value.literalKind() != null || value.literalValue() != null
                    || value.targetTypeFqn() != null || !value.constructorArguments().isEmpty()
                    || !value.assignments().isEmpty() || !value.elements().isEmpty()
                    || value.receiver() != null || value.actionId() == null || value.propertyName() != null;
            case ACTION_RESULT_PROPERTY -> value.literalKind() != null || value.literalValue() != null
                    || value.targetTypeFqn() != null || !value.constructorArguments().isEmpty()
                    || !value.assignments().isEmpty() || !value.elements().isEmpty()
                    || value.receiver() != null || value.actionId() == null || value.propertyName() == null;
        };
        if (invalid) {
            diagnostics.add(new Diagnostic("MALFORMED_SETUP_VALUE_SHAPE", value.kind().name()));
        }
    }

    private String literalType(String literalKind, Object value) {
        if ("string".equals(literalKind) && value instanceof String) return "java.lang.String";
        if ("integer".equals(literalKind) && value instanceof Number) return "java.lang.Integer";
        if ("boolean".equals(literalKind) && value instanceof Boolean) return "java.lang.Boolean";
        return null;
    }

    private boolean compatible(String actual, String expected) {
        if (expected == null || expected.isBlank() || actual == null || actual.isBlank()) return true;
        return Objects.equals(eraseGenerics(box(actual)), eraseGenerics(box(expected)));
    }

    private String box(String type) {
        return switch (type) {
            case "int" -> "java.lang.Integer";
            case "long" -> "java.lang.Long";
            case "boolean" -> "java.lang.Boolean";
            default -> type;
        };
    }

    private String eraseGenerics(String type) {
        int generic = type.indexOf('<');
        return generic < 0 ? type : type.substring(0, generic);
    }

    private String collectionElementType(String type) {
        if (type == null) return null;
        int start = type.indexOf('<');
        int end = type.lastIndexOf('>');
        if (start < 0 || end <= start + 1) return null;
        String elementType = type.substring(start + 1, end).trim();
        return elementType.isEmpty() ? null : elementType;
    }

    private boolean isListType(String type) {
        if (type == null) return false;
        String erased = eraseGenerics(type);
        return "java.util.List".equals(erased) || "java.util.ArrayList".equals(erased)
                || "java.util.Collection".equals(erased);
    }

    private boolean isSetType(String type) {
        if (type == null) return false;
        String erased = eraseGenerics(type);
        return "java.util.Set".equals(erased) || "java.util.HashSet".equals(erased)
                || "java.util.LinkedHashSet".equals(erased) || "java.util.Collection".equals(erased);
    }

    public record ValidationResult(boolean valid, List<Diagnostic> diagnostics) {
        public ValidationResult {
            diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        }
    }

    public record Diagnostic(String code, String message) { }
}
