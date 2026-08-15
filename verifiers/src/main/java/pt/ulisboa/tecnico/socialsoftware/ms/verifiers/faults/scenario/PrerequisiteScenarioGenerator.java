package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class PrerequisiteScenarioGenerator {
    public static final String SCHEMA_VERSION = "microservices-simulator.prerequisite-scenario-descriptor.v1";
    public static final String DEFAULT_RELATIVE_PATH = "src/test/resources/verifier-prerequisite-scenarios.json";

    private final ObjectMapper mapper;

    public PrerequisiteScenarioGenerator() {
        this(new ObjectMapper());
    }

    PrerequisiteScenarioGenerator(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper);
    }

    public Result generate(Path applicationPath,
                           List<SagaDefinition> sagaDefinitions,
                           List<InputVariant> inputVariants,
                           List<EventConsequenceDefinition> eventDefinitions,
                           ScenarioGeneratorConfig baseConfig) throws IOException {
        Path descriptorPath = applicationPath.resolve(DEFAULT_RELATIVE_PATH).normalize();
        if (!Files.isRegularFile(descriptorPath)) {
            return new Result(List.of(), List.of());
        }
        DescriptorFile file = mapper.readValue(descriptorPath.toFile(), DescriptorFile.class);
        if (!SCHEMA_VERSION.equals(file.schemaVersion())) {
            throw new IllegalArgumentException("Unsupported prerequisite scenario descriptor schema "
                    + file.schemaVersion());
        }
        List<WorkloadPlan> workloads = new ArrayList<>();
        List<String> diagnostics = new ArrayList<>();
        for (ScenarioDescriptor descriptor : file.scenarios()) {
            workloads.addAll(generateDescriptor(descriptor, sagaDefinitions, inputVariants,
                    eventDefinitions, baseConfig, diagnostics));
        }
        Map<String, WorkloadPlan> distinct = new TreeMap<>();
        workloads.forEach(workload -> distinct.put(workload.deterministicId(), workload));
        return new Result(List.copyOf(distinct.values()), List.copyOf(diagnostics));
    }

    private List<WorkloadPlan> generateDescriptor(ScenarioDescriptor descriptor,
                                                  List<SagaDefinition> sagaDefinitions,
                                                  List<InputVariant> inputVariants,
                                                  List<EventConsequenceDefinition> eventDefinitions,
                                                  ScenarioGeneratorConfig baseConfig,
                                                  List<String> diagnostics) {
        validateDescriptor(descriptor);
        Set<String> targetSagaFqns = descriptor.participants().stream()
                .map(ParticipantBinding::sagaFqn)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        List<SagaDefinition> targetDefinitions = sagaDefinitions.stream()
                .filter(definition -> targetSagaFqns.contains(definition.sagaFqn()))
                .sorted(Comparator.comparing(SagaDefinition::sagaFqn))
                .toList();
        if (targetDefinitions.size() != targetSagaFqns.size()) {
            throw new IllegalArgumentException("Prerequisite scenario " + descriptor.id()
                    + " references missing Saga definitions");
        }

        List<InputVariant> boundInputs = descriptor.participants().stream()
                .sorted(Comparator.comparing(ParticipantBinding::sagaFqn))
                .map(binding -> bindInput(descriptor, binding, inputVariants))
                .toList();
        ScenarioGeneratorConfig targetedConfig = new ScenarioGeneratorConfig(
                true,
                ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE,
                ScenarioGeneratorConfig.CatalogWriteMode.WRITE_WORKLOADS,
                false,
                targetSagaFqns.size(),
                1000,
                1,
                Math.max(20, baseConfig.maxSchedulesPerInputTuple()),
                false,
                ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING,
                baseConfig.deterministicSeed(),
                baseConfig.maxGroupedSagaSetRows());
        WorkloadGenerationResult generated = ScenarioGenerator.generate(
                targetDefinitions, boundInputs, eventDefinitions, targetedConfig);

        PrerequisiteBaseline baseline = new PrerequisiteBaseline(
                descriptor.providerId(), descriptor.providerVersion(), requiredBindings(descriptor));
        List<WorkloadPlan> selected = generated.workloadPlans().stream()
                .filter(workload -> exactParticipants(workload, targetSagaFqns))
                .filter(workload -> exactForwardOrder(workload, descriptor.forwardRuntimeOrder()))
                .filter(workload -> selectedEventRoute(workload, descriptor))
                .map(workload -> withBaseline(workload, baseline))
                .sorted(Comparator.comparing(WorkloadPlan::deterministicId))
                .toList();
        if (selected.size() != descriptor.expectedWorkloadCount()) {
            throw new IllegalArgumentException("Prerequisite scenario " + descriptor.id() + " expected "
                    + descriptor.expectedWorkloadCount() + " workloads but selected " + selected.size()
                    + " from " + generated.workloadPlans().size() + "; counts=" + generated.counts()
                    + "; warnings=" + generated.warnings());
        }
        diagnostics.add("prerequisite scenario " + descriptor.id() + " generated " + selected.size()
                + " workload plans from " + descriptorPathLabel(descriptor));
        return selected;
    }

    private InputVariant bindInput(ScenarioDescriptor descriptor,
                                   ParticipantBinding binding,
                                   List<InputVariant> inputVariants) {
        InputVariant template = inputVariants.stream()
                .filter(input -> Objects.equals(input.sagaFqn(), binding.sagaFqn()))
                .filter(input -> input.inputRecipe() != null)
                .sorted(Comparator.comparing(InputVariant::deterministicId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Prerequisite scenario " + descriptor.id()
                        + " has no input template for " + binding.sagaFqn()));
        Map<Integer, BindingReference> replacements = new TreeMap<>();
        binding.bindings().forEach(reference -> {
            if (replacements.putIfAbsent(reference.argumentIndex(), reference) != null) {
                throw new IllegalArgumentException("Duplicate prerequisite argument index "
                        + reference.argumentIndex() + " for " + binding.sagaFqn());
            }
        });
        List<InputRecipeArgument> arguments = template.inputRecipe().arguments().stream()
                .sorted(Comparator.comparingInt(InputRecipeArgument::index))
                .map(argument -> bindArgument(argument, replacements.get(argument.index())))
                .toList();
        if (!replacements.keySet().stream().allMatch(index -> arguments.stream()
                .anyMatch(argument -> argument.index() == index))) {
            throw new IllegalArgumentException("Prerequisite scenario " + descriptor.id()
                    + " references an unavailable constructor argument for " + binding.sagaFqn());
        }
        InputRecipe recipe = new InputRecipe(InputRecipe.SCHEMA_VERSION, null, true, List.of(), arguments);
        InputVariant withoutId = new InputVariant(
                null, template.sagaFqn(), template.sourceClassFqn(), template.sourceMethodName(),
                descriptor.id() + ":" + binding.sagaFqn(), template.callContextMethodName(),
                template.inputRole(), template.fixtureOrigin(), InputResolutionStatus.REPLAYABLE,
                template.sourceMode(), template.sourceModeConfidence(), template.sourceModeEvidence(),
                template.stableSourceText(), "prerequisite scenario " + descriptor.id(), template.owners(),
                template.constructorArgumentSummaries(), template.logicalKeyBindings(),
                List.of("runtime values supplied by prerequisite provider " + descriptor.providerId()), recipe);
        String id = ScenarioIdGenerator.inputVariantId(
                withoutId.sagaFqn(), withoutId.sourceClassFqn(), withoutId.sourceMethodName(),
                withoutId.sourceBindingName(), withoutId.resolutionStatus(), withoutId.stableSourceText(),
                withoutId.provenanceText(), withoutId.constructorArgumentSummaries(),
                withoutId.logicalKeyBindings(), recipe.recipeFingerprint());
        return new InputVariant(
                id, withoutId.sagaFqn(), withoutId.sourceClassFqn(), withoutId.sourceMethodName(),
                withoutId.sourceBindingName(), withoutId.callContextMethodName(), withoutId.inputRole(),
                withoutId.fixtureOrigin(), withoutId.resolutionStatus(), withoutId.sourceMode(),
                withoutId.sourceModeConfidence(), withoutId.sourceModeEvidence(), withoutId.stableSourceText(),
                withoutId.provenanceText(), withoutId.owners(), withoutId.constructorArgumentSummaries(),
                withoutId.logicalKeyBindings(), withoutId.warnings(), recipe);
    }

    private InputRecipeArgument bindArgument(InputRecipeArgument argument, BindingReference reference) {
        if (reference == null) return argument;
        if (!Objects.equals(argument.expectedTypeFqn(), reference.typeFqn())) {
            throw new IllegalArgumentException("Prerequisite binding " + reference.key() + " type "
                    + reference.typeFqn() + " does not match constructor argument type "
                    + argument.expectedTypeFqn());
        }
        InputRecipeNode node = InputRecipeNode.builder("baseline_binding")
                .sourceText(reference.key())
                .provenanceText("prerequisite provider binding")
                .executorReady(true)
                .bindingKey(reference.key())
                .bindingTypeFqn(reference.typeFqn())
                .build();
        return new InputRecipeArgument(argument.index(), argument.expectedTypeFqn(),
                InputResolutionStatus.RESOLVED, true, List.of(),
                "prerequisite binding " + reference.key(), node);
    }

    private List<BaselineBindingRequirement> requiredBindings(ScenarioDescriptor descriptor) {
        Map<String, String> byKey = new TreeMap<>();
        descriptor.participants().forEach(participant -> participant.bindings().forEach(binding -> {
            String previous = byKey.putIfAbsent(binding.key(), binding.typeFqn());
            if (previous != null && !Objects.equals(previous, binding.typeFqn())) {
                throw new IllegalArgumentException("Prerequisite binding key " + binding.key()
                        + " has conflicting types");
            }
        }));
        return byKey.entrySet().stream()
                .map(entry -> new BaselineBindingRequirement(entry.getKey(), entry.getValue()))
                .toList();
    }

    private boolean exactParticipants(WorkloadPlan workload, Set<String> targetSagaFqns) {
        return workload.participants().stream().map(SagaInstance::sagaFqn)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new)).equals(targetSagaFqns);
    }

    private boolean exactForwardOrder(WorkloadPlan workload, List<String> expected) {
        return workload.forwardSchedule().stream().map(ScheduledStep::runtimeStepName).toList().equals(expected);
    }

    private boolean selectedEventRoute(WorkloadPlan workload, ScenarioDescriptor descriptor) {
        return workload.eventConsequences().size() == 1
                && Objects.equals(workload.eventConsequences().get(0).eventTypeFqn(), descriptor.eventTypeFqn())
                && Objects.equals(workload.eventConsequences().get(0).eventHandlingClassFqn(),
                descriptor.eventHandlingClassFqn())
                && Objects.equals(workload.eventConsequences().get(0).eventHandlingMethodName(),
                descriptor.eventHandlingMethodName());
    }

    private WorkloadPlan withBaseline(WorkloadPlan workload, PrerequisiteBaseline baseline) {
        WorkloadPlan withoutId = new WorkloadPlan(
                workload.schemaVersion(), null, workload.kind(), workload.executionShape(), workload.participants(),
                workload.acceptedInputs(), workload.forwardSchedule(), workload.eventConsequences(),
                workload.normalSchedule(), baseline, workload.conflictEvidence(), workload.faultSlots(),
                workload.compensationCheckpoints(), workload.warnings());
        return new WorkloadPlan(
                withoutId.schemaVersion(), ScenarioIdGenerator.workloadPlanId(withoutId), withoutId.kind(),
                withoutId.executionShape(), withoutId.participants(), withoutId.acceptedInputs(),
                withoutId.forwardSchedule(), withoutId.eventConsequences(), withoutId.normalSchedule(),
                withoutId.prerequisiteBaseline(), withoutId.conflictEvidence(), withoutId.faultSlots(),
                withoutId.compensationCheckpoints(), withoutId.warnings());
    }

    private void validateDescriptor(ScenarioDescriptor descriptor) {
        if (descriptor == null || blank(descriptor.id()) || blank(descriptor.providerId())
                || blank(descriptor.providerVersion()) || descriptor.participants().size() < 2
                || descriptor.forwardRuntimeOrder().isEmpty() || blank(descriptor.eventTypeFqn())
                || blank(descriptor.eventHandlingClassFqn()) || blank(descriptor.eventHandlingMethodName())
                || descriptor.expectedWorkloadCount() < 1) {
            throw new IllegalArgumentException("Malformed prerequisite scenario descriptor");
        }
    }

    private boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String descriptorPathLabel(ScenarioDescriptor descriptor) {
        return descriptor.providerId() + "@" + descriptor.providerVersion();
    }

    public record Result(List<WorkloadPlan> workloads, List<String> diagnostics) {
        public Result {
            workloads = workloads == null ? List.of() : List.copyOf(workloads);
            diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        }
    }

    public record DescriptorFile(String schemaVersion, List<ScenarioDescriptor> scenarios) {
        public DescriptorFile {
            scenarios = scenarios == null ? List.of() : List.copyOf(scenarios);
        }
    }

    public record ScenarioDescriptor(
            String id,
            String providerId,
            String providerVersion,
            List<ParticipantBinding> participants,
            List<String> forwardRuntimeOrder,
            String eventTypeFqn,
            String eventHandlingClassFqn,
            String eventHandlingMethodName,
            int expectedWorkloadCount) {
        public ScenarioDescriptor {
            participants = participants == null ? List.of() : List.copyOf(participants);
            forwardRuntimeOrder = forwardRuntimeOrder == null ? List.of() : List.copyOf(forwardRuntimeOrder);
        }
    }

    public record ParticipantBinding(String sagaFqn, List<BindingReference> bindings) {
        public ParticipantBinding {
            bindings = bindings == null ? List.of() : List.copyOf(bindings);
        }
    }

    public record BindingReference(int argumentIndex, String key, String typeFqn) {
    }
}
