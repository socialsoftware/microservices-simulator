package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorProviderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventReplayCoordinator;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.SetupPlanValidator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenario;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupAction;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupArgument;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupParticipantBinding;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupPropertyAssignment;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupValueKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupValueRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Executes the validated, closed latest-package setup language outside fault measurement. */
final class ScenarioSetupRunner {
    Result run(WorkloadPlan workload,
               FaultScenario scenario,
               String attemptId,
               ScenarioRuntimeContext runtimeContext) {
        SetupPlan plan = workload.setupPlan();
        long started = System.nanoTime();
        List<ScenarioExecutionReport.SetupActionOutcome> actionOutcomes = new ArrayList<>();
        List<ScenarioExecutionReport.SetupParticipantBindingOutcome> bindingOutcomes = new ArrayList<>();

        SetupPlanValidator.ValidationResult validation = new SetupPlanValidator().validate(
                plan, workload.acceptedInputs());
        if (!validation.valid()) {
            return failure(workload, scenario, started, "SETUP_VALIDATION_FAILED",
                    validation.diagnostics().toString(), actionOutcomes, bindingOutcomes);
        }
        if (workload.prerequisiteBaseline() != null) {
            return failure(workload, scenario, started, "MIXED_PREREQUISITE_AND_SETUP",
                    "source-derived setup cannot run with a prerequisite provider", actionOutcomes, bindingOutcomes);
        }

        Map<String, ScenarioSetupActionDispatcher.SetupMethod> methods;
        try {
            methods = closedMethods(runtimeContext);
            validateRuntimeContract(plan, methods);
            if (!EventReplayCoordinator.isActive()) {
                throw new SetupFailure("SETUP_REPLAY_CONTROL_FAILED",
                        "event replay gate was not active before source-derived setup");
            }
            if (FaultVectorProviderHolder.isActive()
                    || FaultVectorProviderHolder.currentBoundary().isPresent()) {
                throw new SetupFailure("SETUP_FAULT_BOUNDARY_NOT_EMPTY",
                        "source-derived setup must run before target fault-provider installation");
            }
            EventReplayCoordinator.assertNoOpenThreadScope();
        } catch (Throwable failure) {
            Throwable cause = unwrap(failure);
            String reason = cause instanceof SetupFailure setupFailure
                    ? setupFailure.reason : "SETUP_DISPATCH_FAILED";
            return failure(workload, scenario, started, reason, details(cause),
                    actionOutcomes, bindingOutcomes);
        }

        Map<String, RetainedResult> retained = new LinkedHashMap<>();
        for (SetupAction action : plan.actions()) {
            try {
                List<Object> arguments = new ArrayList<>();
                for (SetupArgument argument : action.arguments().stream()
                        .sorted(Comparator.comparingInt(SetupArgument::index)).toList()) {
                    Object value = materialize(argument.value(), argument.expectedTypeFqn(), retained);
                    requireType(value, argument.expectedTypeFqn(),
                            "setup action " + action.actionId() + " argument " + argument.index());
                    arguments.add(value);
                }
                ScenarioSetupActionDispatcher.SetupMethod method = methods.get(action.methodKey());
                Object result = method.invocation().invoke(List.copyOf(arguments));
                if (action.voidResult()) {
                    if (result != null) {
                        throw new SetupFailure("SETUP_RESULT_TYPE_MISMATCH",
                                action.actionId() + " is void but returned " + result.getClass().getName());
                    }
                    actionOutcomes.add(new ScenarioExecutionReport.SetupActionOutcome(
                            action.actionId(), action.orderIndex(), action.methodKey(), "SUCCEEDED",
                            action.declaredResultTypeFqn(), null, null, null));
                } else {
                    if (result == null) {
                        throw new SetupFailure("SETUP_NULL_RESULT",
                                action.actionId() + " returned null");
                    }
                    requireResultType(result, action.declaredResultTypeFqn(), action.actionId());
                    String resultId = attemptId + ":" + action.actionId();
                    RetainedResult retainedResult = new RetainedResult(result, resultId);
                    retained.put(action.actionId(), retainedResult);
                    actionOutcomes.add(new ScenarioExecutionReport.SetupActionOutcome(
                            action.actionId(), action.orderIndex(), action.methodKey(), "SUCCEEDED",
                            action.declaredResultTypeFqn(), result.getClass().getName(), resultId,
                            resultAggregateId(result)));
                }
            } catch (Throwable failure) {
                Throwable cause = unwrap(failure);
                String reason = cause instanceof SetupFailure setupFailure
                        ? setupFailure.reason : "SETUP_INVOCATION_FAILED";
                actionOutcomes.add(new ScenarioExecutionReport.SetupActionOutcome(
                        action.actionId(), action.orderIndex(), action.methodKey(), "FAILED",
                        action.declaredResultTypeFqn(), null, null, null));
                return failure(workload, scenario, started, reason, details(cause),
                        actionOutcomes, bindingOutcomes);
            }
        }

        long pendingEvents;
        try {
            EventService eventService = (EventService) runtimeContext.bean(EventService.class);
            pendingEvents = eventService.eventCountForReplay();
            eventService.clearEventsForReplay();
            if (eventService.eventCountForReplay() != 0) {
                throw new SetupFailure("SETUP_PENDING_EVENT_BASELINE_NOT_EMPTY",
                        "pending setup events remained after cleanup");
            }
            EventReplayCoordinator.assertNoOpenThreadScope();
        } catch (Throwable failure) {
            Throwable cause = unwrap(failure);
            String reason = cause instanceof SetupFailure setupFailure
                    ? setupFailure.reason : "SETUP_CLEANUP_FAILED";
            return failure(workload, scenario, started, reason, details(cause),
                    actionOutcomes, bindingOutcomes);
        }

        Map<ParticipantArgument, Object> participantArguments = new LinkedHashMap<>();
        try {
            for (SetupParticipantBinding binding : plan.participantBindings()) {
                MaterializedValue materialized = materializeWithSource(
                        binding.value(), binding.expectedTypeFqn(), retained);
                requireType(materialized.value(), binding.expectedTypeFqn(),
                        "participant binding " + binding.inputVariantId() + "#" + binding.argumentIndex());
                ParticipantArgument key = new ParticipantArgument(
                        binding.inputVariantId(), binding.argumentIndex());
                if (participantArguments.putIfAbsent(key, materialized.value()) != null) {
                    throw new SetupFailure("DUPLICATE_SETUP_PARTICIPANT_BINDING", key.toString());
                }
                bindingOutcomes.add(new ScenarioExecutionReport.SetupParticipantBindingOutcome(
                        binding.inputVariantId(), binding.argumentIndex(), materialized.actionId(),
                        materialized.propertyName(), "RESOLVED",
                        materialized.value() == null ? null : materialized.value().getClass().getName(),
                        materialized.retainedResultId(), String.valueOf(materialized.value())));
            }
        } catch (Throwable failure) {
            Throwable cause = unwrap(failure);
            String reason = cause instanceof SetupFailure setupFailure
                    ? setupFailure.reason : "SETUP_PARTICIPANT_MATERIALIZATION_FAILED";
            return failure(workload, scenario, started, reason, details(cause),
                    actionOutcomes, bindingOutcomes);
        }

        ScenarioExecutionReport.SourceSetup report = new ScenarioExecutionReport.SourceSetup(
                "SUCCEEDED", System.nanoTime() - started, pendingEvents, true,
                actionOutcomes, bindingOutcomes, null, null);
        return new Result(true, "SETUP_READY", Map.copyOf(participantArguments), report, List.of());
    }

    private Map<String, ScenarioSetupActionDispatcher.SetupMethod> closedMethods(
            ScenarioRuntimeContext runtimeContext) {
        Map<String, ScenarioSetupActionDispatcher.SetupMethod> methods = new LinkedHashMap<>();
        for (ScenarioSetupActionDispatcher dispatcher
                : runtimeContext.beans(ScenarioSetupActionDispatcher.class)) {
            if (dispatcher == null || dispatcher.setupMethods() == null) {
                throw new SetupFailure("MALFORMED_SETUP_DISPATCHER", "dispatcher returned no method map");
            }
            for (Map.Entry<String, ScenarioSetupActionDispatcher.SetupMethod> entry
                    : dispatcher.setupMethods().entrySet()) {
                ScenarioSetupActionDispatcher.SetupMethod method = entry.getValue();
                if (entry.getKey() == null || method == null
                        || !Objects.equals(entry.getKey(), method.methodKey())
                        || method.invocation() == null) {
                    throw new SetupFailure("MALFORMED_SETUP_DISPATCHER", String.valueOf(entry.getKey()));
                }
                if (methods.putIfAbsent(entry.getKey(), method) != null) {
                    throw new SetupFailure("DUPLICATE_SETUP_METHOD_DISPATCH", entry.getKey());
                }
            }
        }
        return Map.copyOf(methods);
    }

    private void validateRuntimeContract(
            SetupPlan plan,
            Map<String, ScenarioSetupActionDispatcher.SetupMethod> methods) {
        Map<String, SetupAction> actionsById = new LinkedHashMap<>();
        for (SetupAction action : plan.actions()) {
            ScenarioSetupActionDispatcher.SetupMethod method = methods.get(action.methodKey());
            if (method == null) {
                throw new SetupFailure("SETUP_METHOD_NOT_AUTHORIZED", action.methodKey());
            }
            if (!Objects.equals(method.declaredResultTypeFqn(), action.declaredResultTypeFqn())
                    || method.voidResult() != action.voidResult()) {
                throw new SetupFailure("SETUP_DISPATCH_SIGNATURE_MISMATCH", action.methodKey());
            }
            actionsById.put(action.actionId(), action);
            for (SetupArgument argument : action.arguments()) {
                validateRuntimeValue(argument.value(), argument.expectedTypeFqn(), actionsById);
            }
        }
        for (SetupParticipantBinding binding : plan.participantBindings()) {
            validateRuntimeValue(binding.value(), binding.expectedTypeFqn(), actionsById);
        }
    }

    private void validateRuntimeValue(SetupValueRecipe recipe,
                                      String expectedType,
                                      Map<String, SetupAction> actionsById) {
        validateRuntimeValue(recipe, runtimeType(expectedType), actionsById);
    }

    private void validateRuntimeValue(SetupValueRecipe recipe,
                                      RuntimeType expected,
                                      Map<String, SetupAction> actionsById) {
        Class<?> expectedRaw = expected == null ? null : expected.raw();
        switch (recipe.kind()) {
            case LITERAL -> requireDeclaredCompatibility(literalClass(recipe), expectedRaw, recipe.declaredTypeFqn());
            case ACTION_RESULT -> {
                SetupAction source = sourceAction(recipe, actionsById);
                requireDeclaredCompatibility(loadType(source.declaredResultTypeFqn()), expectedRaw,
                        recipe.declaredTypeFqn());
            }
            case ACTION_RESULT_PROPERTY -> {
                SetupAction source = sourceAction(recipe, actionsById);
                Method getter = exactGetter(loadType(source.declaredResultTypeFqn()), recipe.propertyName());
                requireDeclaredCompatibility(box(getter.getReturnType()), expectedRaw, recipe.declaredTypeFqn());
            }
            case CONSTRUCTOR -> {
                Class<?> target = loadType(recipe.targetTypeFqn());
                requireDeclaredCompatibility(target, expectedRaw, recipe.declaredTypeFqn());
                exactNoArgumentConstructor(target);
                for (SetupPropertyAssignment assignment : recipe.assignments()) {
                    Method setter = exactSetter(target, assignment.propertyName());
                    validateRuntimeValue(assignment.value(), runtimeType(setter.getGenericParameterTypes()[0]), actionsById);
                }
            }
            case LIST -> {
                requireCollectionCompatibility(ArrayList.class, expectedRaw, recipe.declaredTypeFqn());
                RuntimeType elementType = expected == null ? null : expected.elementType();
                recipe.elements().forEach(element -> validateRuntimeValue(element, elementType, actionsById));
            }
            case SET -> {
                Class<?> materializedType = expectedRaw != null && HashSet.class.equals(expectedRaw)
                        ? HashSet.class : LinkedHashSet.class;
                requireCollectionCompatibility(materializedType, expectedRaw, recipe.declaredTypeFqn());
                RuntimeType elementType = expected == null ? null : expected.elementType();
                recipe.elements().forEach(element -> validateRuntimeValue(element, elementType, actionsById));
            }
            case LOCAL_DATE_TIME -> requireDeclaredCompatibility(
                    LocalDateTime.class, expectedRaw, recipe.declaredTypeFqn());
            case LOCAL_DATE_TO_STRING -> {
                requireDeclaredCompatibility(String.class, expectedRaw, recipe.declaredTypeFqn());
                validateRuntimeValue(recipe.receiver(), runtimeType(LocalDateTime.class), actionsById);
            }
        }
    }

    private Object materialize(SetupValueRecipe recipe,
                               String expectedType,
                               Map<String, RetainedResult> retained) throws ReflectiveOperationException {
        return materialize(recipe, runtimeType(expectedType), retained);
    }

    private Object materialize(SetupValueRecipe recipe,
                               RuntimeType expectedType,
                               Map<String, RetainedResult> retained) throws ReflectiveOperationException {
        return materializeWithSource(recipe, expectedType, retained).value();
    }

    private MaterializedValue materializeWithSource(SetupValueRecipe recipe,
                                                    String expectedType,
                                                    Map<String, RetainedResult> retained)
            throws ReflectiveOperationException {
        return materializeWithSource(recipe, runtimeType(expectedType), retained);
    }

    private MaterializedValue materializeWithSource(SetupValueRecipe recipe,
                                                    RuntimeType expectedType,
                                                    Map<String, RetainedResult> retained)
            throws ReflectiveOperationException {
        return switch (recipe.kind()) {
            case LITERAL -> new MaterializedValue(materializeLiteral(recipe, expectedType), null, null, null);
            case ACTION_RESULT -> {
                RetainedResult result = retainedResult(recipe.actionId(), retained);
                yield new MaterializedValue(result.value(), recipe.actionId(), null, result.resultId());
            }
            case ACTION_RESULT_PROPERTY -> {
                RetainedResult result = retainedResult(recipe.actionId(), retained);
                Object value = exactGetter(result.value().getClass(), recipe.propertyName())
                        .invoke(result.value());
                yield new MaterializedValue(value, recipe.actionId(), recipe.propertyName(), result.resultId());
            }
            case CONSTRUCTOR -> {
                Class<?> type = loadType(recipe.targetTypeFqn());
                Constructor<?> constructor = exactNoArgumentConstructor(type);
                Object instance = constructor.newInstance();
                for (SetupPropertyAssignment assignment : recipe.assignments().stream()
                        .sorted(Comparator.comparingInt(SetupPropertyAssignment::orderIndex)).toList()) {
                    Method setter = exactSetter(type, assignment.propertyName());
                    RuntimeType expected = runtimeType(setter.getGenericParameterTypes()[0]);
                    Object value = materialize(assignment.value(), expected, retained);
                    requireCompleteType(value, expected,
                            type.getName() + "." + assignment.propertyName());
                    setter.invoke(instance, value);
                }
                yield new MaterializedValue(instance, null, null, null);
            }
            case LIST -> new MaterializedValue(materializeElements(
                    recipe.elements(), retained, expectedType, false), null, null, null);
            case SET -> new MaterializedValue(materializeElements(
                    recipe.elements(), retained, expectedType, true), null, null, null);
            case LOCAL_DATE_TIME -> new MaterializedValue(materializeLocalDateTime(recipe), null, null, null);
            case LOCAL_DATE_TO_STRING -> {
                Object receiver = materialize(recipe.receiver(), LocalDateTime.class.getName(), retained);
                if (!(receiver instanceof LocalDateTime dateTime)) {
                    throw new SetupFailure("SETUP_VALUE_TYPE_MISMATCH",
                            "local date conversion receiver was not LocalDateTime");
                }
                yield new MaterializedValue(DateHandler.toISOString(dateTime), null, null, null);
            }
        };
    }

    private Object materializeLiteral(SetupValueRecipe recipe, RuntimeType expectedType) {
        Object value = recipe.literalValue();
        if ("integer".equals(recipe.literalKind())) {
            Class<?> expected = expectedType == null ? null : expectedType.raw();
            Class<?> target = expected == null ? Integer.class : box(expected);
            return exactIntegral(value, target);
        }
        return value;
    }

    private Object exactIntegral(Object value, Class<?> target) {
        if (!(value instanceof Number number)) {
            throw new SetupFailure("SETUP_VALUE_TYPE_MISMATCH", "integer literal was not numeric");
        }
        BigInteger integral;
        try {
            if (number instanceof BigInteger bigInteger) integral = bigInteger;
            else if (number instanceof BigDecimal decimal) integral = decimal.toBigIntegerExact();
            else integral = BigInteger.valueOf(number.longValue());
            if (target == Integer.class) return integral.intValueExact();
            if (target == Long.class) return integral.longValueExact();
        } catch (ArithmeticException failure) {
            throw new SetupFailure("SETUP_INTEGER_OUT_OF_RANGE", String.valueOf(value));
        }
        throw new SetupFailure("SETUP_VALUE_TYPE_MISMATCH",
                "integer literal cannot target " + target.getName());
    }

    private Collection<Object> materializeElements(List<SetupValueRecipe> elements,
                                                   Map<String, RetainedResult> retained,
                                                   RuntimeType expected,
                                                   boolean set) throws ReflectiveOperationException {
        Collection<Object> values = set && expected != null && HashSet.class.equals(expected.raw())
                ? new HashSet<>() : set ? new LinkedHashSet<>() : new ArrayList<>();
        RuntimeType elementType = expected == null ? null : expected.elementType();
        for (SetupValueRecipe element : elements) {
            Object value = materialize(element, elementType, retained);
            requireCompleteType(value, elementType, "setup collection element");
            values.add(value);
        }
        return values;
    }

    private LocalDateTime materializeLocalDateTime(SetupValueRecipe recipe) {
        String expression = String.valueOf(recipe.literalValue());
        LocalDateTime value = DateHandler.now();
        if (expression.contains(".plusHours(1)")) value = value.plusHours(1);
        if (expression.endsWith(".plusMinutes(5)")) return value.plusMinutes(5);
        if (expression.endsWith(".plusMinutes(25)")) return value.plusMinutes(25);
        throw new SetupFailure("UNSUPPORTED_LOCAL_DATE_EXPRESSION", expression);
    }

    private SetupAction sourceAction(SetupValueRecipe recipe, Map<String, SetupAction> actionsById) {
        SetupAction source = actionsById.get(recipe.actionId());
        if (source == null) {
            throw new SetupFailure("INVALID_SETUP_RESULT_REFERENCE", String.valueOf(recipe.actionId()));
        }
        return source;
    }

    private RetainedResult retainedResult(String actionId, Map<String, RetainedResult> retained) {
        RetainedResult result = retained.get(actionId);
        if (result == null) {
            throw new SetupFailure("MISSING_SETUP_RESULT", String.valueOf(actionId));
        }
        return result;
    }

    private Constructor<?> exactNoArgumentConstructor(Class<?> type) {
        try {
            return type.getConstructor();
        } catch (NoSuchMethodException failure) {
            throw new SetupFailure("UNSUPPORTED_SETUP_CONSTRUCTOR",
                    type.getName() + " has no public no-argument constructor");
        }
    }

    private Method exactGetter(Class<?> type, String property) {
        String getterName = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        try {
            return type.getMethod(getterName);
        } catch (NoSuchMethodException failure) {
            throw new SetupFailure("UNSUPPORTED_SETUP_RESULT_PROPERTY",
                    type.getName() + "#" + getterName + "()");
        }
    }

    private Method exactSetter(Class<?> type, String property) {
        String setterName = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        List<Method> matches = java.util.Arrays.stream(type.getMethods())
                .filter(method -> method.getName().equals(setterName) && method.getParameterCount() == 1)
                .toList();
        if (matches.size() != 1) {
            throw new SetupFailure("UNSUPPORTED_SETUP_ASSIGNMENT",
                    type.getName() + "#" + setterName + " expected one exact public setter but found " + matches.size());
        }
        return matches.get(0);
    }

    private Class<?> literalClass(SetupValueRecipe recipe) {
        return switch (recipe.literalKind()) {
            case "string" -> String.class;
            case "integer" -> Integer.class;
            case "boolean" -> Boolean.class;
            default -> throw new SetupFailure("UNSUPPORTED_SETUP_LITERAL", String.valueOf(recipe.literalKind()));
        };
    }

    private void requireDeclaredCompatibility(Class<?> actual,
                                              Class<?> expected,
                                              String declaredType) {
        Class<?> declared = loadType(declaredType);
        if (expected != null && !box(expected).isAssignableFrom(box(actual))) {
            throw new SetupFailure("SETUP_VALUE_TYPE_MISMATCH",
                    actual.getName() + " cannot target " + expected.getName());
        }
        if (declared != null && !box(declared).isAssignableFrom(box(actual))) {
            throw new SetupFailure("SETUP_DECLARED_TYPE_MISMATCH",
                    actual.getName() + " does not match " + declaredType);
        }
    }

    private void requireCollectionCompatibility(Class<?> actual,
                                                 Class<?> expected,
                                                 String declaredType) {
        requireDeclaredCompatibility(actual, expected, declaredType);
    }

    private void requireResultType(Object value, String expectedType, String actionId) {
        Class<?> expected = box(loadType(expectedType));
        if (expected == null || value == null || !expected.isInstance(value)) {
            throw new SetupFailure("SETUP_RESULT_TYPE_MISMATCH",
                    "setup action " + actionId + " expected " + expectedType + " but got "
                            + (value == null ? "null" : value.getClass().getName()));
        }
    }

    private void requireType(Object value, String expectedType, String label) {
        requireType(value, loadType(expectedType), label);
    }

    private void requireType(Object value, Class<?> expectedType, String label) {
        if (expectedType == null) return;
        Class<?> expected = box(expectedType);
        if (value == null || !expected.isInstance(value)) {
            throw new SetupFailure("SETUP_VALUE_TYPE_MISMATCH",
                    label + " expected " + expected.getName() + " but got "
                            + (value == null ? "null" : value.getClass().getName()));
        }
    }

    private void requireCompleteType(Object value, RuntimeType expectedType, String label) {
        if (expectedType == null) return;
        requireType(value, expectedType.raw(), label);
        if (expectedType.elementType() != null && value instanceof Collection<?> collection) {
            for (Object element : collection) {
                requireCompleteType(element, expectedType.elementType(), label + " element");
            }
        }
    }

    private RuntimeType runtimeType(String typeFqn) {
        if (typeFqn == null || typeFqn.isBlank() || "void".equals(typeFqn)) return null;
        int start = typeFqn.indexOf('<');
        int end = typeFqn.lastIndexOf('>');
        String rawType = start < 0 ? typeFqn : typeFqn.substring(0, start);
        RuntimeType elementType = start < 0 || end <= start + 1
                ? null : runtimeType(typeFqn.substring(start + 1, end).trim());
        return new RuntimeType(loadType(rawType), elementType);
    }

    private RuntimeType runtimeType(Type type) {
        if (type instanceof Class<?> raw) return new RuntimeType(raw, null);
        if (type instanceof ParameterizedType parameterized
                && parameterized.getRawType() instanceof Class<?> raw) {
            Type[] arguments = parameterized.getActualTypeArguments();
            RuntimeType elementType = arguments.length == 1 ? runtimeType(arguments[0]) : null;
            return new RuntimeType(raw, elementType);
        }
        throw new SetupFailure("UNSUPPORTED_SETUP_GENERIC_TYPE", type.getTypeName());
    }

    private Class<?> loadType(String typeFqn) {
        if (typeFqn == null || typeFqn.isBlank() || "void".equals(typeFqn)) return null;
        String erased = typeFqn.contains("<") ? typeFqn.substring(0, typeFqn.indexOf('<')) : typeFqn;
        try {
            return switch (erased) {
                case "int" -> int.class;
                case "long" -> long.class;
                case "boolean" -> boolean.class;
                default -> Class.forName(erased);
            };
        } catch (ClassNotFoundException failure) {
            throw new SetupFailure("MISSING_SETUP_RUNTIME_TYPE", typeFqn);
        }
    }

    private Class<?> box(Class<?> type) {
        if (type == null || !type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        return type;
    }

    private String resultAggregateId(Object value) {
        try {
            Object aggregateId = exactGetter(value.getClass(), "aggregateId").invoke(value);
            return aggregateId == null ? null : aggregateId.toString();
        } catch (SetupFailure ignored) {
            return null;
        } catch (IllegalAccessException | InvocationTargetException failure) {
            throw new SetupFailure("SETUP_RESULT_EVIDENCE_FAILED", details(unwrap(failure)));
        }
    }

    private Result failure(WorkloadPlan workload,
                           FaultScenario scenario,
                           long started,
                           String reason,
                           String message,
                           List<ScenarioExecutionReport.SetupActionOutcome> actionOutcomes,
                           List<ScenarioExecutionReport.SetupParticipantBindingOutcome> bindingOutcomes) {
        ScenarioExecutionReport.SourceSetup report = new ScenarioExecutionReport.SourceSetup(
                "FAILED", System.nanoTime() - started, 0L, false,
                actionOutcomes, bindingOutcomes, reason, message);
        ScenarioExecutionReport.Blocker blocker = new ScenarioExecutionReport.Blocker(
                workload.deterministicId(), scenario == null ? null : scenario.deterministicId(),
                null, null, null, null, reason, message);
        return new Result(false, reason, Map.of(), report, List.of(blocker));
    }

    private Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while (current != null && current.getCause() != null
                && current instanceof InvocationTargetException) {
            current = current.getCause();
        }
        return current == null ? failure : current;
    }

    private String details(Throwable failure) {
        return failure.getClass().getName()
                + (failure.getMessage() == null ? "" : ": " + failure.getMessage());
    }

    record ParticipantArgument(String inputVariantId, int argumentIndex) {
    }

    record Result(
            boolean success,
            String status,
            Map<ParticipantArgument, Object> participantArguments,
            ScenarioExecutionReport.SourceSetup report,
            List<ScenarioExecutionReport.Blocker> blockers) {
    }

    private record RetainedResult(Object value, String resultId) {
    }

    private record MaterializedValue(
            Object value,
            String actionId,
            String propertyName,
            String retainedResultId) {
    }

    private record RuntimeType(Class<?> raw, RuntimeType elementType) {
    }

    private static final class SetupFailure extends RuntimeException {
        private final String reason;

        private SetupFailure(String reason, String message) {
            super(message);
            this.reason = reason;
        }
    }
}
