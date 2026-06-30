package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynamicInputMapWriter {
    public static final String SCHEMA_VERSION = "microservices-simulator.dynamic-input-map.v1";
    public static final String FILE_NAME = "dynamic-input-map.json";

    private static final Pattern SIMPLE_ARGUMENT_LITERAL = Pattern.compile(
            "^arg\\[\\d+]\\s*:\\s*(?:[A-Za-z_$][\\w$]*\\s*<-\\s*)?(\"[^\"]*\"|'[^']*'|-?\\d+|true|false|null)(?:\\b|$)");

    private final ObjectMapper objectMapper;

    public DynamicInputMapWriter() {
        this(new ObjectMapper());
    }

    public DynamicInputMapWriter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null")
                .copy()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public DynamicInputMap write(Path path,
                                 List<String> selectedTestClassFqns,
                                 List<ScenarioPlan> scenarioPlans,
                                 String generatedAt) throws IOException {
        Objects.requireNonNull(path, "path cannot be null");
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        DynamicInputMap map = build(selectedTestClassFqns, scenarioPlans, generatedAt);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), map);
        return map;
    }

    DynamicInputMap build(List<String> selectedTestClassFqns, List<ScenarioPlan> scenarioPlans, String generatedAt) {
        Map<String, EntryBuilder> entries = new LinkedHashMap<>();
        for (ScenarioPlan plan : sortedPlans(scenarioPlans)) {
            for (InputVariant input : sortedInputs(plan.inputs())) {
                if (input == null || isBlank(input.deterministicId())) {
                    continue;
                }

                EntryBuilder builder = entries.computeIfAbsent(input.deterministicId(), ignored -> new EntryBuilder(input));
                builder.scenarioPlanIds.add(plan.deterministicId());
                builder.stepNameHints.addAll(stepNameHints(plan, input));
                builder.expectedAggregateTypes.addAll(expectedAggregateTypes(plan, input));
            }
        }

        List<DynamicInputMapEntry> inputEntries = entries.values().stream()
                .sorted(Comparator.comparing(builder -> builder.input.deterministicId(), Comparator.nullsFirst(String::compareTo)))
                .map(EntryBuilder::toEntry)
                .toList();
        return new DynamicInputMap(SCHEMA_VERSION, generatedAt, sortedSelectedTestClassFqns(selectedTestClassFqns), inputEntries.size(), inputEntries);
    }

    private List<String> sortedSelectedTestClassFqns(List<String> selectedTestClassFqns) {
        return (selectedTestClassFqns == null ? List.<String>of() : selectedTestClassFqns).stream()
                .map(DynamicInputMapWriter::normalize)
                .filter(value -> !isBlank(value))
                .distinct()
                .sorted()
                .toList();
    }

    private List<ScenarioPlan> sortedPlans(List<ScenarioPlan> scenarioPlans) {
        return (scenarioPlans == null ? List.<ScenarioPlan>of() : scenarioPlans).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(ScenarioPlan::deterministicId, Comparator.nullsFirst(String::compareTo)))
                .toList();
    }

    private List<InputVariant> sortedInputs(List<InputVariant> inputs) {
        return (inputs == null ? List.<InputVariant>of() : inputs).stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(InputVariant::deterministicId, Comparator.nullsFirst(String::compareTo)))
                .toList();
    }

    private Set<String> stepNameHints(ScenarioPlan plan, InputVariant input) {
        Set<String> sagaInstanceIds = sagaInstanceIds(plan, input);
        LinkedHashSet<String> hints = new LinkedHashSet<>();
        plan.expandedSchedule().stream()
                .filter(step -> step != null && sagaInstanceIds.contains(step.sagaInstanceId()))
                .map(ScheduledStep::stepId)
                .map(DynamicInputMapWriter::normalizedStepName)
                .filter(value -> !isBlank(value))
                .forEach(hints::add);
        return hints;
    }

    private Set<String> expectedAggregateTypes(ScenarioPlan plan, InputVariant input) {
        Set<String> sagaInstanceIds = sagaInstanceIds(plan, input);
        Set<String> scheduledStepIds = plan.expandedSchedule().stream()
                .filter(step -> step != null && sagaInstanceIds.contains(step.sagaInstanceId()))
                .map(ScheduledStep::deterministicId)
                .filter(value -> !isBlank(value))
                .collect(LinkedHashSet::new, LinkedHashSet::add, LinkedHashSet::addAll);
        LinkedHashSet<String> aggregateTypes = new LinkedHashSet<>();
        for (ConflictEvidence evidence : plan.conflictEvidence()) {
            if (evidence == null) {
                continue;
            }
            if (scheduledStepIds.contains(evidence.leftScheduledStepId())) {
                addAggregateType(aggregateTypes, evidence.leftAggregateKey());
            }
            if (scheduledStepIds.contains(evidence.rightScheduledStepId())) {
                addAggregateType(aggregateTypes, evidence.rightAggregateKey());
            }
        }
        return aggregateTypes;
    }

    private Set<String> sagaInstanceIds(ScenarioPlan plan, InputVariant input) {
        LinkedHashSet<String> ids = new LinkedHashSet<>();
        for (SagaInstance sagaInstance : plan.sagaInstances()) {
            if (sagaInstance == null) {
                continue;
            }
            if (Objects.equals(sagaInstance.inputVariantId(), input.deterministicId())
                    && (isBlank(input.sagaFqn()) || Objects.equals(sagaInstance.sagaFqn(), input.sagaFqn()))) {
                ids.add(sagaInstance.deterministicId());
            }
        }
        return ids;
    }

    private static void addAggregateType(Set<String> aggregateTypes, AggregateKey aggregateKey) {
        if (aggregateKey != null && !isBlank(aggregateKey.aggregateTypeName())) {
            aggregateTypes.add(aggregateKey.aggregateTypeName());
        }
    }

    private static List<String> literalArgumentValueHints(InputVariant input) {
        LinkedHashSet<String> hints = new LinkedHashSet<>();
        for (String summary : input.constructorArgumentSummaries()) {
            Matcher matcher = SIMPLE_ARGUMENT_LITERAL.matcher(summary == null ? "" : summary.trim());
            if (matcher.find()) {
                hints.add(unquote(matcher.group(1)));
            }
        }
        return List.copyOf(hints);
    }

    private static String normalizedStepName(String stepId) {
        if (stepId == null) {
            return null;
        }
        int index = stepId.lastIndexOf("::");
        String name = index >= 0 ? stepId.substring(index + 2) : stepId;
        return normalize(name.trim().replaceFirst("#\\d+$", ""));
    }

    private static String unquote(String value) {
        if (value == null || value.length() < 2) {
            return value;
        }
        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public record DynamicInputMap(
            String schemaVersion,
            String generatedAt,
            List<String> selectedTestClassFqns,
            int inputCount,
            List<DynamicInputMapEntry> inputs) {
        public DynamicInputMap {
            schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion;
            selectedTestClassFqns = selectedTestClassFqns == null ? List.of() : List.copyOf(selectedTestClassFqns);
            inputs = inputs == null ? List.of() : List.copyOf(inputs);
        }
    }

    public record DynamicInputMapEntry(
            String inputVariantId,
            String sagaFqn,
            String sourceClassFqn,
            String sourceMethodName,
            String sourceBindingName,
            String callContextMethodName,
            String inputRole,
            String fixtureOrigin,
            List<InputOwnerEntry> owners,
            String resolutionStatus,
            String sourceMode,
            String sourceModeConfidence,
            List<String> stepNameHints,
            List<String> literalArgumentValueHints,
            List<String> constructorArgumentSummaries,
            List<String> expectedCommands,
            List<String> expectedAggregateTypes,
            Map<String, String> logicalKeyBindings,
            List<String> scenarioPlanIds,
            String stableSourceText,
            String provenanceText,
            List<String> warnings) {
        public DynamicInputMapEntry {
            owners = owners == null ? List.of() : List.copyOf(owners);
            stepNameHints = stepNameHints == null ? List.of() : List.copyOf(stepNameHints);
            literalArgumentValueHints = literalArgumentValueHints == null ? List.of() : List.copyOf(literalArgumentValueHints);
            constructorArgumentSummaries = constructorArgumentSummaries == null ? List.of() : List.copyOf(constructorArgumentSummaries);
            expectedCommands = expectedCommands == null ? List.of() : List.copyOf(expectedCommands);
            expectedAggregateTypes = expectedAggregateTypes == null ? List.of() : List.copyOf(expectedAggregateTypes);
            logicalKeyBindings = logicalKeyBindings == null
                    ? Map.of()
                    : Collections.unmodifiableMap(new LinkedHashMap<>(logicalKeyBindings));
            scenarioPlanIds = scenarioPlanIds == null ? List.of() : List.copyOf(scenarioPlanIds);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
        }
    }

    public record InputOwnerEntry(String testClassFqn, String testMethodName) {
    }

    private static final class EntryBuilder {
        private final InputVariant input;
        private final LinkedHashSet<String> stepNameHints = new LinkedHashSet<>();
        private final LinkedHashSet<String> expectedAggregateTypes = new LinkedHashSet<>();
        private final LinkedHashSet<String> scenarioPlanIds = new LinkedHashSet<>();

        private EntryBuilder(InputVariant input) {
            this.input = input;
        }

        private DynamicInputMapEntry toEntry() {
            return new DynamicInputMapEntry(
                    input.deterministicId(),
                    input.sagaFqn(),
                    input.sourceClassFqn(),
                    input.sourceMethodName(),
                    input.sourceBindingName(),
                    input.callContextMethodName(),
                    input.inputRole().name(),
                    input.fixtureOrigin().name(),
                    input.owners().stream()
                            .map(owner -> new InputOwnerEntry(owner.testClassFqn(), owner.testMethodName()))
                            .toList(),
                    input.resolutionStatus().name(),
                    input.sourceMode().name(),
                    input.sourceModeConfidence().name(),
                    sorted(stepNameHints),
                    literalArgumentValueHints(input),
                    input.constructorArgumentSummaries(),
                    List.of(),
                    sorted(expectedAggregateTypes),
                    input.logicalKeyBindings(),
                    sorted(scenarioPlanIds),
                    input.stableSourceText(),
                    input.provenanceText(),
                    input.warnings());
        }

        private static List<String> sorted(Set<String> values) {
            return values.stream().filter(value -> !isBlank(value)).sorted().toList();
        }
    }
}
