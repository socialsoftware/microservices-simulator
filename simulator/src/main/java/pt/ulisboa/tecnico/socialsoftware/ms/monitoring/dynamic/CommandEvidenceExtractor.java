package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class CommandEvidenceExtractor {
    private static final Set<String> TOP_LEVEL_COMMAND_FIELDS = Set.of(
            "rootAggregateId",
            "serviceName",
            "unitOfWork"
    );
    private static final List<String> SENSITIVE_NAME_MARKERS = List.of(
            "password",
            "secret",
            "token",
            "credential",
            "authorization"
    );

    private final boolean includeCommandFields;
    private final int maxFieldDepth;
    private final int maxFieldValueLength;

    public CommandEvidenceExtractor(DynamicEvidenceProperties properties) {
        DynamicEvidenceProperties effective = properties == null ? new DynamicEvidenceProperties() : properties;
        this.includeCommandFields = effective.isIncludeCommandFields();
        this.maxFieldDepth = Math.max(0, effective.getMaxFieldDepth());
        this.maxFieldValueLength = effective.getMaxFieldValueLength();
    }

    public DynamicEvidenceEvent buildCommandSentEvent(Command command, DynamicEvidenceContext.StepContext context) {
        Objects.requireNonNull(command, "command");

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("commandType", command.getClass().getSimpleName());
        payload.put("commandFqn", command.getClass().getName());
        payload.put("serviceName", command.getServiceName());
        payload.put("rootAggregateId", stringifyIdentifier(command.getRootAggregateId(), "rootAggregateId"));

        List<String> warnings = new ArrayList<>();
        payload.put("fields", includeCommandFields
                ? extractFields(command, warnings)
                : Collections.emptyMap());
        if (!warnings.isEmpty()) {
            payload.put("warnings", warnings);
        }

        String functionalityName = context != null ? context.functionalityName() : resolveFunctionalityName(command);
        Long unitOfWorkVersion = context != null ? context.unitOfWorkVersion() : resolveUnitOfWorkVersion(command);
        String invocationId = context != null
                ? context.functionalityInvocationId()
                : buildInvocationId(functionalityName, unitOfWorkVersion);
        String stepName = context != null ? context.stepName() : null;
        String functionalityClassFqn = context != null ? context.functionalityClassFqn() : null;
        String functionalityClassSimpleName = context != null ? context.functionalityClassSimpleName() : null;

        return DynamicEvidenceEvent.of(
                "COMMAND_SENT",
                functionalityName,
                functionalityClassFqn,
                functionalityClassSimpleName,
                context == null ? null : context.inputVariantId(),
                invocationId,
                stepName,
                unitOfWorkVersion,
                payload);
    }

    public Map<String, Object> extractFields(Command command) {
        return extractFields(command, new ArrayList<>());
    }

    private Map<String, Object> extractFields(Command command, List<String> warnings) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (command == null) {
            return fields;
        }

        IdentityHashMap<Object, Boolean> visited = new IdentityHashMap<>();
        visited.put(command, Boolean.TRUE);

        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(command.getClass(), Object.class);
            for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
                String propertyName = descriptor.getName();
                if (shouldSkipTopLevelProperty(propertyName) || shouldSkipProperty(propertyName)) {
                    continue;
                }

                Method readMethod = descriptor.getReadMethod();
                if (readMethod == null || readMethod.getParameterCount() != 0 || Modifier.isStatic(readMethod.getModifiers())) {
                    continue;
                }

                try {
                    if (!readMethod.canAccess(command)) {
                        readMethod.setAccessible(true);
                    }
                    Object value = readMethod.invoke(command);
                    Object sanitized = sanitizeValue(propertyName, value, 0, visited, warnings);
                    if (sanitized != SkipValue.INSTANCE) {
                        fields.put(propertyName, sanitized);
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    warnings.add("Failed to extract command field '" + propertyName + "': " + e.getMessage());
                }
            }
        } catch (IntrospectionException e) {
            warnings.add("Failed to inspect command fields for '" + command.getClass().getName() + "': " + e.getMessage());
        }

        return fields;
    }

    private Object sanitizeValue(String propertyName, Object value, int depth, IdentityHashMap<Object, Boolean> visited,
                                 List<String> warnings) {
        if (value == null) {
            return null;
        }
        if (shouldSkipProperty(propertyName)) {
            return SkipValue.INSTANCE;
        }
        if (value instanceof String stringValue) {
            return truncate(stringValue);
        }
        if (value instanceof CharSequence charSequence) {
            return truncate(charSequence.toString());
        }
        if (value instanceof Character character) {
            return truncate(String.valueOf(character));
        }
        if (isIdentifierPropertyName(propertyName)) {
            return truncate(String.valueOf(value));
        }
        if (value instanceof Number || value instanceof Boolean) {
            return value;
        }
        if (value instanceof Enum<?> enumeration) {
            return truncate(enumeration.name());
        }
        if (value instanceof Command) {
            return SkipValue.INSTANCE;
        }
        if (value instanceof UnitOfWork) {
            return SkipValue.INSTANCE;
        }
        if (value.getClass().isArray()) {
            return sanitizeArray(value, depth, visited, warnings);
        }
        if (value instanceof Collection<?> collection) {
            return sanitizeCollection(collection, depth, visited, warnings);
        }
        if (value instanceof Map<?, ?> map) {
            return sanitizeMap(map, depth, visited, warnings);
        }

        if (visited.containsKey(value)) {
            return truncate(value.toString());
        }

        if (depth >= maxFieldDepth) {
            return truncate(value.toString());
        }

        visited.put(value, Boolean.TRUE);
        try {
            Map<String, Object> nested = new LinkedHashMap<>();
            BeanInfo beanInfo = Introspector.getBeanInfo(value.getClass(), Object.class);
            for (PropertyDescriptor descriptor : beanInfo.getPropertyDescriptors()) {
                String nestedProperty = descriptor.getName();
                if (shouldSkipProperty(nestedProperty)) {
                    continue;
                }
                Method readMethod = descriptor.getReadMethod();
                if (readMethod == null || readMethod.getParameterCount() != 0 || Modifier.isStatic(readMethod.getModifiers())) {
                    continue;
                }
                try {
                    if (!readMethod.canAccess(value)) {
                        readMethod.setAccessible(true);
                    }
                    Object nestedValue = readMethod.invoke(value);
                    Object sanitized = sanitizeValue(nestedProperty, nestedValue, depth + 1, visited, warnings);
                    if (sanitized != SkipValue.INSTANCE) {
                        nested.put(nestedProperty, sanitized);
                    }
                } catch (ReflectiveOperationException | RuntimeException e) {
                    warnings.add("Failed to extract nested field '" + nestedProperty + "' from '"
                            + value.getClass().getName() + "': " + e.getMessage());
                }
            }
            if (!nested.isEmpty()) {
                return nested;
            }
        } catch (IntrospectionException e) {
            warnings.add("Failed to inspect nested value of type '" + value.getClass().getName() + "': " + e.getMessage());
        } finally {
            visited.remove(value);
        }

        return truncate(value.toString());
    }

    private Object sanitizeCollection(Collection<?> collection, int depth, IdentityHashMap<Object, Boolean> visited,
                                      List<String> warnings) {
        if (depth >= maxFieldDepth) {
            return truncate(collection.toString());
        }
        List<Object> sanitized = new ArrayList<>(collection.size());
        for (Object element : collection) {
            Object normalized = sanitizeValue(null, element, depth + 1, visited, warnings);
            if (normalized != SkipValue.INSTANCE) {
                sanitized.add(normalized);
            }
        }
        return sanitized;
    }

    private Object sanitizeArray(Object array, int depth, IdentityHashMap<Object, Boolean> visited,
                                 List<String> warnings) {
        int length = Array.getLength(array);
        if (depth >= maxFieldDepth) {
            return truncate(array.toString());
        }
        List<Object> sanitized = new ArrayList<>(length);
        for (int index = 0; index < length; index++) {
            Object element = Array.get(array, index);
            Object normalized = sanitizeValue(null, element, depth + 1, visited, warnings);
            if (normalized != SkipValue.INSTANCE) {
                sanitized.add(normalized);
            }
        }
        return sanitized;
    }

    private Object sanitizeMap(Map<?, ?> map, int depth, IdentityHashMap<Object, Boolean> visited,
                               List<String> warnings) {
        if (depth >= maxFieldDepth) {
            return truncate(map.toString());
        }
        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = String.valueOf(entry.getKey());
            Object normalized = sanitizeValue(key, entry.getValue(), depth + 1, visited, warnings);
            if (normalized != SkipValue.INSTANCE) {
                sanitized.put(truncate(key), normalized);
            }
        }
        return sanitized;
    }

    private boolean shouldSkipTopLevelProperty(String propertyName) {
        return propertyName != null && TOP_LEVEL_COMMAND_FIELDS.contains(propertyName);
    }

    private boolean shouldSkipProperty(String propertyName) {
        if (propertyName == null) {
            return false;
        }
        if ("unitOfWork".equals(propertyName)) {
            return true;
        }
        String lower = propertyName.toLowerCase(Locale.ROOT);
        for (String marker : SENSITIVE_NAME_MARKERS) {
            if (lower.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private boolean isIdentifierPropertyName(String propertyName) {
        if (propertyName == null) {
            return false;
        }
        String lower = propertyName.toLowerCase(Locale.ROOT);
        return lower.equals("aggregateid") || lower.endsWith("aggregateid");
    }

    private Object stringifyIdentifier(Object value, String propertyName) {
        if (value == null) {
            return null;
        }
        return truncate(String.valueOf(value));
    }

    private String resolveFunctionalityName(Command command) {
        UnitOfWork unitOfWork = command.getUnitOfWork();
        return unitOfWork != null ? unitOfWork.getFunctionalityName() : null;
    }

    private Long resolveUnitOfWorkVersion(Command command) {
        UnitOfWork unitOfWork = command.getUnitOfWork();
        return unitOfWork != null ? unitOfWork.getVersion() : null;
    }

    private String buildInvocationId(String functionalityName, Long unitOfWorkVersion) {
        if (functionalityName == null || unitOfWorkVersion == null) {
            return null;
        }
        return functionalityName + "-" + unitOfWorkVersion;
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        if (maxFieldValueLength >= 0 && value.length() > maxFieldValueLength) {
            return value.substring(0, maxFieldValueLength);
        }
        return value;
    }

    private enum SkipValue {
        INSTANCE
    }
}
