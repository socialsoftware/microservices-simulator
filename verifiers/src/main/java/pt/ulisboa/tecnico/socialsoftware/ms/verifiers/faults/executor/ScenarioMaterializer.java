package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*;

import java.lang.reflect.*;
import java.util.*;

class ScenarioMaterializer {
    MaterializedArguments materialize(InputVariant input, ScenarioRuntimeContext runtimeContext, String functionalityName) {
        InputRecipe recipe = input == null ? null : input.inputRecipe();
        if (recipe == null) {
            return MaterializedArguments.failure(List.of(blocker(input, null, "MISSING_INPUT_RECIPE", "Input variant has no inputRecipe")));
        }
        List<InputRecipeArgument> arguments = recipe.arguments().stream()
                .sorted(Comparator.comparingInt(InputRecipeArgument::index))
                .toList();
        List<Object> values = new ArrayList<>();
        List<ScenarioExecutionReport.Blocker> blockers = new ArrayList<>();
        for (InputRecipeArgument argument : arguments) {
            MaterializationResult result = materializeArgument(input, argument, runtimeContext, functionalityName);
            if (result.blocker() != null) {
                blockers.add(result.blocker());
            } else {
                values.add(result.value());
            }
        }
        return blockers.isEmpty() ? MaterializedArguments.success(values) : MaterializedArguments.failure(blockers);
    }

    private MaterializationResult materializeArgument(InputVariant input,
                                                     InputRecipeArgument argument,
                                                     ScenarioRuntimeContext runtimeContext,
                                                     String functionalityName) {
        String type = argument.expectedTypeFqn();
        if (ScenarioExecutorMaterializationPolicy.isRuntimeOwned(type)) {
            return MaterializationResult.value(runtimeOwnedValue(type, runtimeContext, functionalityName));
        }
        if (!argument.executorReady()) {
            return MaterializationResult.blocker(blocker(input, argument.index(), "UNRESOLVED_ARGUMENT", "Argument is not executor-ready"));
        }
        return materializeNode(input, argument.index(), argument.recipe(), runtimeContext, functionalityName);
    }

    private MaterializationResult materializeNode(InputVariant input,
                                                 Integer argumentIndex,
                                                 InputRecipeNode node,
                                                 ScenarioRuntimeContext runtimeContext,
                                                 String functionalityName) {
        if (node == null) {
            return MaterializationResult.blocker(blocker(input, argumentIndex, "MISSING_RECIPE", "Missing recipe node"));
        }
        String kind = node.kind();
        try {
            return switch (kind == null ? "" : kind) {
                case "literal" -> MaterializationResult.value(node.value());
                case "placeholder" -> materializePlaceholder(input, argumentIndex, node, runtimeContext);
                case "constructor" -> materializeConstructor(input, argumentIndex, node, runtimeContext, functionalityName);
                case "collection" -> materializeCollection(input, argumentIndex, node, runtimeContext, functionalityName);
                case "local_transform" -> materializeLocalTransform(input, argumentIndex, node, runtimeContext, functionalityName);
                case "helper_result" -> materializeNode(input, argumentIndex, node.resultRecipe(), runtimeContext, functionalityName);
                case "property_access" -> materializePropertyAccess(input, argumentIndex, node, runtimeContext, functionalityName);
                case "call_result" -> MaterializationResult.blocker(blocker(input, argumentIndex, "UNSUPPORTED_CALL_RESULT", "Unsupported call_result recipe"));
                case "unresolved" -> MaterializationResult.blocker(blocker(input, argumentIndex, "UNRESOLVED_VALUE", "Non-whitelisted unresolved recipe"));
                default -> MaterializationResult.blocker(blocker(input, argumentIndex, "UNSUPPORTED_RECIPE_KIND", "Unsupported recipe kind: " + kind));
            };
        } catch (ReflectiveOperationException | RuntimeException e) {
            return MaterializationResult.blocker(blocker(input, argumentIndex, "MATERIALIZATION_EXCEPTION", e.getMessage()));
        }
    }

    private MaterializationResult materializePlaceholder(InputVariant input, Integer argumentIndex, InputRecipeNode node, ScenarioRuntimeContext runtimeContext) {
        if (ScenarioExecutorMaterializationPolicy.isRuntimeOwned(node.expectedTypeFqn())) {
            return MaterializationResult.value(runtimeContext.bean(loadClass(node.expectedTypeFqn())));
        }
        return MaterializationResult.blocker(blocker(input, argumentIndex, "UNRESOLVED_PLACEHOLDER", "Source-provided placeholder has no configured value"));
    }

    private MaterializationResult materializeConstructor(InputVariant input,
                                                        Integer argumentIndex,
                                                        InputRecipeNode node,
                                                        ScenarioRuntimeContext runtimeContext,
                                                        String functionalityName) throws ReflectiveOperationException {
        if (node.targetTypeFqn() == null) {
            return MaterializationResult.blocker(blocker(input, argumentIndex, "MISSING_TARGET_TYPE", "Constructor recipe has no target type FQN"));
        }
        List<Object> args = new ArrayList<>();
        for (InputRecipeArgument child : node.arguments().stream().sorted(Comparator.comparingInt(InputRecipeArgument::index)).toList()) {
            MaterializationResult result = child.executorReady()
                    ? materializeNode(input, argumentIndex, child.recipe(), runtimeContext, functionalityName)
                    : MaterializationResult.blocker(blocker(input, argumentIndex, "UNRESOLVED_CONSTRUCTOR_ARGUMENT", "Constructor argument is not executor-ready"));
            if (result.blocker() != null) {
                return result;
            }
            args.add(result.value());
        }
        Class<?> type = Class.forName(node.targetTypeFqn());
        Object instance = instantiate(type, args);
        for (InputRecipeAssignment assignment : node.assignments().stream().sorted(Comparator.comparingInt(InputRecipeAssignment::orderIndex)).toList()) {
            MaterializationResult value = materializeNode(input, argumentIndex, assignment.valueRecipe(), runtimeContext, functionalityName);
            if (value.blocker() != null) {
                return MaterializationResult.blocker(new ScenarioExecutionReport.Blocker(input == null ? null : null,
                        input == null ? null : input.deterministicId(), argumentIndex, null, "UNMATERIALIZABLE_ASSIGNMENT",
                        "Assignment " + assignment.propertyName() + " cannot be materialized: " + value.blocker().reason()));
            }
            applyAssignment(instance, assignment, value.value());
        }
        return MaterializationResult.value(instance);
    }

    private MaterializationResult materializeCollection(InputVariant input,
                                                       Integer argumentIndex,
                                                       InputRecipeNode node,
                                                       ScenarioRuntimeContext runtimeContext,
                                                       String functionalityName) {
        String collectionKind = node.collectionKind() == null ? "list" : node.collectionKind().toLowerCase(Locale.ROOT);
        if ("map".equals(collectionKind)) {
            Map<Object, Object> map = new LinkedHashMap<>();
            for (InputRecipeMapEntry entry : node.entries().stream().sorted(Comparator.comparingInt(InputRecipeMapEntry::index)).toList()) {
                MaterializationResult key = materializeNode(input, argumentIndex, entry.keyRecipe(), runtimeContext, functionalityName);
                MaterializationResult value = materializeNode(input, argumentIndex, entry.valueRecipe(), runtimeContext, functionalityName);
                if (key.blocker() != null) return key;
                if (value.blocker() != null) return value;
                map.put(key.value(), value.value());
            }
            return MaterializationResult.value(map);
        }
        List<Object> values = new ArrayList<>();
        for (InputRecipeNode element : node.elements()) {
            MaterializationResult result = materializeNode(input, argumentIndex, element, runtimeContext, functionalityName);
            if (result.blocker() != null) return result;
            values.add(result.value());
        }
        if ("set".equals(collectionKind)) {
            return MaterializationResult.value(new LinkedHashSet<>(values));
        }
        return MaterializationResult.value(values);
    }

    private MaterializationResult materializeLocalTransform(InputVariant input,
                                                           Integer argumentIndex,
                                                           InputRecipeNode node,
                                                           ScenarioRuntimeContext runtimeContext,
                                                           String functionalityName) {
        if (!"toSet".equals(node.transformName())) {
            return MaterializationResult.blocker(blocker(input, argumentIndex, "UNSUPPORTED_TRANSFORM", "Unsupported local_transform: " + node.transformName()));
        }
        MaterializationResult receiver = materializeNode(input, argumentIndex, node.receiver(), runtimeContext, functionalityName);
        if (receiver.blocker() != null) return receiver;
        Object value = receiver.value();
        if (value instanceof Collection<?> collection) {
            return MaterializationResult.value(new LinkedHashSet<>(collection));
        }
        return MaterializationResult.blocker(blocker(input, argumentIndex, "UNSUPPORTED_TRANSFORM_RECEIVER", "toSet receiver is not a collection"));
    }

    private MaterializationResult materializePropertyAccess(InputVariant input,
                                                           Integer argumentIndex,
                                                           InputRecipeNode node,
                                                           ScenarioRuntimeContext runtimeContext,
                                                           String functionalityName) throws ReflectiveOperationException {
        MaterializationResult receiver = materializeNode(input, argumentIndex, node.receiver(), runtimeContext, functionalityName);
        if (receiver.blocker() != null) {
            return MaterializationResult.blocker(blocker(input, argumentIndex, "UNMATERIALIZABLE_RECEIVER", receiver.blocker().reason()));
        }
        Object target = receiver.value();
        String property = node.propertyName();
        if (target instanceof Map<?, ?> map) {
            return MaterializationResult.value(map.get(property));
        }
        String getter = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        try {
            return MaterializationResult.value(target.getClass().getMethod(getter).invoke(target));
        } catch (NoSuchMethodException ignored) {
            Field field = target.getClass().getDeclaredField(property);
            field.setAccessible(true);
            return MaterializationResult.value(field.get(target));
        }
    }

    private Object runtimeOwnedValue(String type, ScenarioRuntimeContext runtimeContext, String functionalityName) {
        if ("pt.ulisboa.tecnico.socialsoftware.ms.transactional.sagas.unitOfWork.SagaUnitOfWork".equals(type)) {
            return runtimeContext.createSagaUnitOfWork(functionalityName);
        }
        return runtimeContext.bean(loadClass(type));
    }

    private static Class<?> loadClass(String type) {
        try {
            return Class.forName(type);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Missing runtime type " + type, e);
        }
    }

    private Object instantiate(Class<?> type, List<Object> args) throws ReflectiveOperationException {
        for (Constructor<?> constructor : type.getConstructors()) {
            if (constructor.getParameterCount() == args.size() && parametersAccept(constructor.getParameterTypes(), args)) {
                return constructor.newInstance(args.toArray());
            }
        }
        Constructor<?> declared = type.getDeclaredConstructors()[0];
        declared.setAccessible(true);
        return declared.newInstance(args.toArray());
    }

    private boolean parametersAccept(Class<?>[] parameterTypes, List<Object> args) {
        for (int i = 0; i < parameterTypes.length; i++) {
            Object arg = args.get(i);
            if (arg != null && !wrap(parameterTypes[i]).isInstance(arg)) return false;
        }
        return true;
    }

    private Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) return type;
        if (type == int.class) return Integer.class;
        if (type == long.class) return Long.class;
        if (type == boolean.class) return Boolean.class;
        if (type == double.class) return Double.class;
        if (type == float.class) return Float.class;
        if (type == short.class) return Short.class;
        if (type == byte.class) return Byte.class;
        if (type == char.class) return Character.class;
        return type;
    }

    private void applyAssignment(Object instance, InputRecipeAssignment assignment, Object value) throws ReflectiveOperationException {
        String property = assignment.propertyName() != null ? assignment.propertyName() : assignment.sourceName();
        String setter = "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
        for (Method method : instance.getClass().getMethods()) {
            if (method.getName().equals(setter) && method.getParameterCount() == 1) {
                method.invoke(instance, value);
                return;
            }
        }
        Field field = instance.getClass().getDeclaredField(property);
        field.setAccessible(true);
        field.set(instance, value);
    }

    private ScenarioExecutionReport.Blocker blocker(InputVariant input, Integer argumentIndex, String reason, String message) {
        return new ScenarioExecutionReport.Blocker(null, input == null ? null : input.deterministicId(), argumentIndex, null, reason, message);
    }

    record MaterializedArguments(boolean success, List<Object> values, List<ScenarioExecutionReport.Blocker> blockers) {
        static MaterializedArguments success(List<Object> values) { return new MaterializedArguments(true, values, List.of()); }
        static MaterializedArguments failure(List<ScenarioExecutionReport.Blocker> blockers) { return new MaterializedArguments(false, List.of(), blockers); }
    }

    private record MaterializationResult(Object value, ScenarioExecutionReport.Blocker blocker) {
        static MaterializationResult value(Object value) { return new MaterializationResult(value, null); }
        static MaterializationResult blocker(ScenarioExecutionReport.Blocker blocker) { return new MaterializationResult(null, blocker); }
    }
}
