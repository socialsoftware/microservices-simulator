package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationCheckpoint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EagerFaultScenarioGenerationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventConsequence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenario;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioAction;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioActionKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeArgument;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeAssignment;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeMapEntry;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeNode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.NormalActionKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.NormalActionRef;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.PrerequisiteBaseline;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioCatalogManifest;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupAction;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupArgument;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupParticipantBinding;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupPropertyAssignment;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupValueKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupValueRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ScenarioModelAdapterResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Publishes the current executable catalog by extending the static package
 * produced by {@link StaticAnalysisArtifactWriter}.  The existing analysis,
 * schedule, and recovery models remain the source of semantic conclusions;
 * this class owns only their compact current-package projection.
 */
public final class ExecutableArtifactWriter {
    public static final String DEFAULT_SETUP_FILE = "setups.jsonl";
    public static final String DEFAULT_WORKLOAD_FILE = "workloads.jsonl";
    public static final String DEFAULT_FAULT_FILE = "fault-scenarios.jsonl";
    public static final String DEFAULT_REQUEST_FILE = "requests.jsonl";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ScenarioCatalogManifest.Current write(ScenarioModelAdapterResult model,
                                                  String targetApplication,
                                                  EagerFaultScenarioGenerationResult generation,
                                                  Path outputDirectory) throws IOException {
        Path root = Objects.requireNonNull(outputDirectory, "outputDirectory").toAbsolutePath().normalize();
        return write(model, targetApplication, generation,
                root.resolve("scenario-catalog-manifest.json"),
                root.resolve(StaticAnalysisArtifactWriter.DEFAULT_ACCOUNTING_FILE),
                root.resolve(StaticAnalysisArtifactWriter.DEFAULT_SAGA_FACT_FILE),
                root.resolve(StaticAnalysisArtifactWriter.DEFAULT_INPUT_FACT_FILE),
                root.resolve(StaticAnalysisArtifactWriter.DEFAULT_INTERACTION_FACT_FILE),
                root.resolve(DEFAULT_SETUP_FILE), root.resolve(DEFAULT_WORKLOAD_FILE),
                root.resolve(DEFAULT_FAULT_FILE), root.resolve(DEFAULT_REQUEST_FILE),
                java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString());
    }

    public ScenarioCatalogManifest.Current write(ScenarioModelAdapterResult model,
                                                  String targetApplication,
                                                  EagerFaultScenarioGenerationResult generation,
                                                  Path manifestPath,
                                                  Path accountingPath,
                                                  Path sagaFactsPath,
                                                  Path inputFactsPath,
                                                  Path interactionFactsPath,
                                                  Path setupPath,
                                                  Path workloadPath,
                                                  Path faultPath,
                                                  Path requestPath) throws IOException {
        return write(model, targetApplication, generation, manifestPath, accountingPath, sagaFactsPath,
                inputFactsPath, interactionFactsPath, setupPath, workloadPath, faultPath, requestPath,
                java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString());
    }

    public ScenarioCatalogManifest.Current write(ScenarioModelAdapterResult model,
                                                  String targetApplication,
                                                  EagerFaultScenarioGenerationResult generation,
                                                  Path manifestPath,
                                                  Path accountingPath,
                                                  Path sagaFactsPath,
                                                  Path inputFactsPath,
                                                  Path interactionFactsPath,
                                                  Path setupPath,
                                                  Path workloadPath,
                                                  Path faultPath,
                                                  Path requestPath,
                                                  String generatedAt) throws IOException {
        return writeWithPackageInputs(model, targetApplication, generation, manifestPath, accountingPath, sagaFactsPath,
                inputFactsPath, interactionFactsPath, setupPath, workloadPath, faultPath, requestPath,
                generatedAt);
    }

    private ScenarioCatalogManifest.Current writeWithPackageInputs(ScenarioModelAdapterResult model,
                                                  String targetApplication,
                                                  EagerFaultScenarioGenerationResult generation,
                                                  Path manifestPath,
                                                  Path accountingPath,
                                                  Path sagaFactsPath,
                                                  Path inputFactsPath,
                                                  Path interactionFactsPath,
                                                  Path setupPath,
                                                  Path workloadPath,
                                                  Path faultPath,
                                                  Path requestPath,
                                                  String generatedAt) throws IOException {
        ScenarioModelAdapterResult safeModel = Objects.requireNonNull(model, "model");
        EagerFaultScenarioGenerationResult safeGeneration = Objects.requireNonNull(generation, "generation");
        Path manifest = Objects.requireNonNull(manifestPath, "manifestPath").toAbsolutePath().normalize();
        Path root = manifest.getParent();
        if (root == null) throw new IllegalArgumentException("manifest path must have a package directory");
        List<Path> allPaths = List.of(accountingPath, sagaFactsPath, inputFactsPath, interactionFactsPath,
                setupPath, workloadPath, faultPath, requestPath);
        for (Path path : allPaths) {
            Path normalized = Objects.requireNonNull(path, "package artifact path").toAbsolutePath().normalize();
            if (!normalized.startsWith(root.toAbsolutePath().normalize())) {
                throw new IllegalArgumentException("executable artifact must remain under manifest directory: " + normalized);
            }
        }

        // Reuse M1's sole static projection. It also performs static model
        // normalization and current-package validation before we add executable roles.
        List<InputVariant> requiredPackageInputs =
                safeGeneration.workloadPlans().stream()
                        .filter(Objects::nonNull)
                        .flatMap(plan -> plan.acceptedInputs() == null
                                ? Stream.empty() : plan.acceptedInputs().stream())
                        .filter(Objects::nonNull)
                        .toList();
        new StaticAnalysisArtifactWriter().writeExecutable(safeModel, targetApplication,
                safeGeneration.effectiveConfig(), manifest, accountingPath, sagaFactsPath,
                inputFactsPath, interactionFactsPath,
                generatedAt, requiredPackageInputs);
        Map<String, String> interactionIds = interactionIds(interactionFactsPath);
        Map<String, List<String>> sagaStepIds = sagaStepIds(sagaFactsPath);
        Map<String, List<PersistedRoute>> sagaRoutes = sagaRoutes(sagaFactsPath);

        Map<String, String> setupIds = new LinkedHashMap<>();
        List<Map<String, Object>> setups = setupRecords(safeGeneration.workloadPlans(), setupIds);
        List<Map<String, Object>> workloads = safeGeneration.workloadPlans().stream()
                .sorted(Comparator.comparing(WorkloadPlan::deterministicId, Comparator.nullsFirst(String::compareTo)))
                .map(plan -> workloadRecord(plan, setupIds, interactionIds, sagaStepIds, sagaRoutes))
                .toList();
        List<Map<String, Object>> faults = safeGeneration.faultScenarios().stream()
                .sorted(Comparator.comparing(FaultScenario::workloadPlanId, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(FaultScenario::assignedVector, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(FaultScenario::deterministicId, Comparator.nullsFirst(String::compareTo)))
                .map(scenario -> faultRecord(scenario, safeGeneration.workloadPlans()))
                .toList();

        writeJsonLines(setupPath, setups);
        writeJsonLines(workloadPath, workloads);
        writeJsonLines(faultPath, faults);
        Files.createDirectories(requestPath.getParent());
        Files.write(requestPath, new byte[0], StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        enrichAccounting(accountingPath, safeGeneration, setups.size());

        JsonNode manifestNode = MAPPER.readTree(Files.readString(manifest));
        if (!manifestNode.isObject()) throw new IllegalArgumentException("static manifest is not an object");
        ObjectNode files = (ObjectNode) manifestNode.with("files");
        putArtifact(files, root, "accounting", accountingPath);
        putArtifact(files, root, "setups", setupPath);
        putArtifact(files, root, "workloads", workloadPath);
        putArtifact(files, root, "faultScenarios", faultPath);
        putArtifact(files, root, "requests", requestPath);
        writeCompact(manifest, manifestNode);
        return MAPPER.treeToValue(manifestNode, ScenarioCatalogManifest.Current.class);
    }

    private List<Map<String, Object>> setupRecords(List<WorkloadPlan> plans, Map<String, String> ids) throws IOException {
        LinkedHashMap<String, Map<String, Object>> records = new LinkedHashMap<>();
        for (WorkloadPlan plan : plans) {
            Object setup = plan.setupPlan() != null ? plan.setupPlan() : plan.prerequisiteBaseline();
            if (setup == null) continue;
            String canonical = MAPPER.writeValueAsString(setup);
            String id = "setup-" + sha256(canonical).substring(0, 24);
            ids.put(plan.deterministicId(), id);
            if (records.containsKey(id)) continue;
            if (setup instanceof SetupPlan source) records.put(id, sourceSetup(id, source));
            else records.put(id, providerSetup(id, (PrerequisiteBaseline) setup));
        }
        return records.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(Map.Entry::getValue).toList();
    }

    private Map<String, Object> sourceSetup(String id, SetupPlan setup) {
        LinkedHashMap<String, Object> record = new LinkedHashMap<>();
        record.put("id", id);
        record.put("kind", "sourceDerived");
        record.put("materializable", setup.blockers().isEmpty()
                && setup.actions().stream().allMatch(action -> action.blockers().isEmpty())
                && setup.actions().stream().flatMap(action -> action.arguments().stream()).allMatch(arg -> arg.blockers().isEmpty()));
        record.put("actions", setup.actions().stream().sorted(Comparator.comparingInt(SetupAction::orderIndex)).map(this::setupAction).toList());
        record.put("bindings", setup.participantBindings().stream().map(this::setupBinding).toList());
        if (!setup.blockers().isEmpty()) record.put("blockers", setup.blockers());
        return record;
    }

    private Map<String, Object> providerSetup(String id, PrerequisiteBaseline baseline) {
        LinkedHashMap<String, Object> record = new LinkedHashMap<>();
        record.put("id", id);
        record.put("kind", "providerBacked");
        record.put("provider", baseline.providerId() + "@" + baseline.providerVersion());
        record.put("bindings", baseline.requiredBindings().stream().map(binding -> {
            LinkedHashMap<String, Object> value = new LinkedHashMap<>();
            value.put("key", binding.key());
            value.put("type", binding.typeFqn());
            return value;
        }).toList());
        return record;
    }

    private Map<String, Object> setupAction(SetupAction action) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("id", action.actionId());
        value.put("call", action.methodKey());
        value.put("arguments", action.arguments().stream().sorted(Comparator.comparingInt(SetupArgument::index))
                .map(argument -> setupValue(argument.value())).toList());
        if (!action.voidResult()) value.put("result", action.actionId());
        return value;
    }

    private Map<String, Object> setupBinding(SetupParticipantBinding binding) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("input", binding.inputVariantId());
        value.put("argument", binding.argumentIndex());
        value.put("value", setupValue(binding.value()));
        return value;
    }

    private Map<String, Object> setupValue(SetupValueRecipe recipe) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        if (recipe == null) {
            value.put("kind", "unresolved");
            return value;
        }
        switch (recipe.kind()) {
            case LITERAL -> {
                value.put("kind", "literal");
                if (recipe.literalValue() != null) value.put("value", recipe.literalValue());
            }
            case ACTION_RESULT -> {
                value.put("kind", "result");
                value.put("action", recipe.actionId());
                if (!recipe.assignments().isEmpty()) {
                    LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
                    recipe.assignments().stream().sorted(Comparator.comparingInt(SetupPropertyAssignment::orderIndex))
                            .forEach(assignment -> fields.put(assignment.propertyName(), setupValue(assignment.value())));
                    value.put("fields", fields);
                }
            }
            case ACTION_RESULT_PROPERTY -> {
                value.put("kind", "resultProperty");
                value.put("action", recipe.actionId());
                value.put("property", recipe.propertyName());
            }
            case CONSTRUCTOR -> {
                value.put("kind", "constructor");
                value.put("type", recipe.targetTypeFqn());
                if (!recipe.constructorArguments().isEmpty()) value.put("arguments", recipe.constructorArguments().stream().map(this::setupValue).toList());
                if (!recipe.assignments().isEmpty()) {
                    LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
                    recipe.assignments().forEach(assignment -> fields.put(assignment.propertyName(), setupValue(assignment.value())));
                    value.put("fields", fields);
                }
            }
            case LIST, SET -> {
                value.put("kind", "collection");
                value.put("collectionKind", recipe.kind() == SetupValueKind.SET ? "set" : "list");
                value.put("elements", recipe.elements().stream().map(this::setupValue).toList());
            }
            case LOCAL_DATE_TO_STRING -> {
                value.put("kind", "transform");
                value.put("name", "DateHandler.toISOString");
                value.put("receiver", setupValue(recipe.receiver()));
            }
            case LOCAL_DATE_TIME -> {
                value.put("kind", "literal");
                value.put("value", recipe.literalValue());
            }
            default -> {
                value.put("kind", "unresolved");
                if (!recipe.blockers().isEmpty()) value.put("reason", recipe.blockers().getFirst());
            }
        }
        return value;
    }

    private Map<String, Object> setupAssignment(SetupPropertyAssignment assignment) {
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        value.put("method", assignment.propertyName());
        value.put("value", setupValue(assignment.value()));
        return value;
    }

    private Map<String, Object> workloadRecord(WorkloadPlan plan, Map<String, String> setupIds,
                                               Map<String, String> interactionIds,
                                               Map<String, List<String>> sagaStepIds,
                                               Map<String, List<PersistedRoute>> sagaRoutes) {
        LinkedHashMap<String, Object> record = new LinkedHashMap<>();
        record.put("id", plan.deterministicId());
        List<SagaInstance> participants = plan.participants();
        record.put("participants", java.util.stream.IntStream.range(0, participants.size()).mapToObj(index -> {
            SagaInstance participant = participants.get(index);
            LinkedHashMap<String, Object> value = new LinkedHashMap<>();
            value.put("id", "p" + (index + 1));
            value.put("saga", participant.sagaFqn());
            value.put("input", participant.inputVariantId());
            return value;
        }).toList());
        String setup = setupIds.get(plan.deterministicId());
        if (setup != null) record.put("setup", setup);
        record.put("interactions", plan.conflictEvidence().stream()
                .map(conflict -> interactionIds.get(interactionKey(plan, conflict)))
                .filter(Objects::nonNull).distinct().toList());

        Map<String, String> occurrenceIds = new LinkedHashMap<>();
        Map<String, String> consequenceIds = new LinkedHashMap<>();
        Map<String, String> localStepIds = new LinkedHashMap<>();
        List<Map<String, Object>> schedule = new ArrayList<>();
        int stepIndex = 0;
        for (ScheduledStep step : plan.forwardSchedule()) {
            String id = "s" + (++stepIndex);
            occurrenceIds.put(step.deterministicId(), id);
            LinkedHashMap<String, Object> value = new LinkedHashMap<>();
            value.put("id", id);
            value.put("kind", "step");
            value.put("participant", participantAlias(participants, step.sagaInstanceId()));
            String localStep = exactSagaStep(plan, step, sagaStepIds);
            localStepIds.put(step.deterministicId(), localStep);
            value.put("sagaStep", localStep);
            plan.faultSlots().stream().filter(slot -> Objects.equals(slot.scheduledStepId(), step.deterministicId())).findFirst()
                    .ifPresent(slot -> value.put("faultSlot", slot.slotIndex()));
            schedule.add(value);
        }
        int eventIndex = 0;
        for (NormalActionRef action : plan.normalSchedule()) {
            if (action.kind() != NormalActionKind.EVENT_CONSEQUENCE) continue;
            EventConsequence event = plan.eventConsequences().stream().filter(candidate -> Objects.equals(candidate.deterministicId(), action.eventConsequenceId())).findFirst().orElse(null);
            if (event == null) continue;
            String id = "e" + (++eventIndex);
            consequenceIds.put(event.deterministicId(), id);
            LinkedHashMap<String, Object> value = new LinkedHashMap<>();
            value.put("id", id);
            value.put("kind", "event");
            value.put("route", routeId(event, plan, localStepIds, sagaRoutes));
            value.put("triggeringStep", occurrenceIds.get(event.triggerScheduledStepId()));
            schedule.add(value);
        }
        // The normal schedule, rather than forwardSchedule concatenation, is
        // authoritative for interleaved event placement.
        schedule.sort(Comparator.comparingInt(entry -> normalPosition(plan, entry, occurrenceIds, consequenceIds)));
        record.put("schedule", schedule);
        return record;
    }

    private Map<String, List<String>> sagaStepIds(Path sagaFactsPath) throws IOException {
        Map<String, List<String>> result = new LinkedHashMap<>();
        for (String line : Files.readAllLines(sagaFactsPath, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            JsonNode saga = MAPPER.readTree(line);
            List<String> steps = new ArrayList<>();
            saga.path("steps").forEach(step -> steps.add(step.path("id").asText()));
            result.put(saga.path("fqn").asText(), List.copyOf(steps));
        }
        return Map.copyOf(result);
    }

    private String exactSagaStep(WorkloadPlan plan, ScheduledStep scheduled,
                                 Map<String, List<String>> sagaStepIds) {
        String saga = plan.participants().stream()
                .filter(participant -> Objects.equals(participant.deterministicId(), scheduled.sagaInstanceId()))
                .map(SagaInstance::sagaFqn).findFirst().orElseThrow();
        List<String> facts = sagaStepIds.getOrDefault(saga, List.of());
        String supplied = localStepId(scheduled.stepId());
        if (facts.contains(supplied)) return supplied;
        List<String> matchingRuntimeName = facts.stream()
                .filter(candidate -> candidate.replaceFirst("#\\d+$", "").equals(scheduled.runtimeStepName()))
                .toList();
        if (matchingRuntimeName.size() == 1) return matchingRuntimeName.getFirst();
        throw new IllegalArgumentException("scheduled step " + scheduled.deterministicId()
                + " has no unique exact Saga fact in " + saga + ": " + supplied);
    }

    private Map<String, String> interactionIds(Path interactionPath) throws IOException {
        LinkedHashMap<String, String> result = new LinkedHashMap<>();
        for (String line : Files.readAllLines(interactionPath, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            JsonNode interaction = MAPPER.readTree(line);
            List<String> accesses = new ArrayList<>();
            interaction.path("accesses").forEach(access -> accesses.add(access.path("saga").asText() + "|" + access.path("step").asText()));
            if (accesses.size() == 2) { accesses.sort(String::compareTo); result.putIfAbsent(String.join("\u0000", accesses), interaction.path("id").asText()); }
        }
        return result;
    }

    private String interactionKey(WorkloadPlan plan, pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictEvidence conflict) {
        String leftSaga = plan.participants().stream().filter(participant -> Objects.equals(participant.deterministicId(),
                scheduledParticipant(plan, conflict.leftScheduledStepId()))).map(SagaInstance::sagaFqn).findFirst().orElse("");
        String rightSaga = plan.participants().stream().filter(participant -> Objects.equals(participant.deterministicId(),
                scheduledParticipant(plan, conflict.rightScheduledStepId()))).map(SagaInstance::sagaFqn).findFirst().orElse("");
        String left = leftSaga + "|" + runtimeStep(plan, conflict.leftScheduledStepId());
        String right = rightSaga + "|" + runtimeStep(plan, conflict.rightScheduledStepId());
        return left.compareTo(right) <= 0 ? left + "\u0000" + right : right + "\u0000" + left;
    }

    private String scheduledParticipant(WorkloadPlan plan, String stepId) {
        return plan.forwardSchedule().stream().filter(step -> Objects.equals(step.deterministicId(), stepId))
                .map(ScheduledStep::sagaInstanceId).findFirst().orElse(null);
    }

    private String runtimeStep(WorkloadPlan plan, String stepId) {
        return plan.forwardSchedule().stream().filter(step -> Objects.equals(step.deterministicId(), stepId))
                .map(ScheduledStep::stepId).map(value -> {
                    int marker = value.lastIndexOf("::");
                    return marker < 0 ? value : value.substring(marker + 2);
                }).findFirst().orElse("");
    }

    private int normalPosition(WorkloadPlan plan, Map<String, Object> entry,
                               Map<String, String> occurrenceIds, Map<String, String> consequenceIds) {
        String id = String.valueOf(entry.get("id"));
        for (int index = 0; index < plan.normalSchedule().size(); index++) {
            NormalActionRef action = plan.normalSchedule().get(index);
            if (action.kind() == NormalActionKind.FORWARD && Objects.equals(occurrenceIds.get(action.scheduledStepId()), id)) return index;
            if (action.kind() == NormalActionKind.EVENT_CONSEQUENCE && Objects.equals(consequenceIds.get(action.eventConsequenceId()), id)) return index;
        }
        return Integer.MAX_VALUE;
    }

    private Map<String, Object> faultRecord(FaultScenario scenario, List<WorkloadPlan> plans) {
        WorkloadPlan plan = plans.stream().filter(candidate -> Objects.equals(candidate.deterministicId(), scenario.workloadPlanId())).findFirst().orElse(null);
        Map<String, String> stepAliases = new HashMap<>();
        Map<String, String> eventAliases = new HashMap<>();
        Map<String, String> checkpointAliases = new HashMap<>();
        if (plan != null) {
            for (int index = 0; index < plan.forwardSchedule().size(); index++) stepAliases.put(plan.forwardSchedule().get(index).deterministicId(), "s" + (index + 1));
            int eventIndex = 0;
            for (NormalActionRef ref : plan.normalSchedule()) if (ref.kind() == NormalActionKind.EVENT_CONSEQUENCE) eventAliases.put(ref.eventConsequenceId(), "e" + (++eventIndex));
            for (CompensationCheckpoint checkpoint : plan.compensationCheckpoints()) checkpointAliases.put(checkpoint.deterministicId(), stepAliases.get(checkpoint.sourceScheduledStepId()));
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("id", scenario.deterministicId());
        result.put("workload", scenario.workloadPlanId());
        result.put("faultVector", scenario.assignedVector());
        List<Map<String, Object>> actions = new ArrayList<>();
        for (FaultScenarioAction action : scenario.actions()) {
            LinkedHashMap<String, Object> value = new LinkedHashMap<>();
            if (action.kind() == FaultScenarioActionKind.FORWARD) value.put("step", stepAliases.get(action.occurrenceId()));
            else if (action.kind() == FaultScenarioActionKind.EVENT_CONSEQUENCE) value.put("event", eventAliases.get(action.sourceEventConsequenceId()));
            else if (action.kind() == FaultScenarioActionKind.COMPENSATION) value.put("compensate", checkpointAliases.get(action.sourceCompensationCheckpointId()));
            actions.add(value);
        }
        result.put("actions", actions);
        return result;
    }

    /** Package-local projection seam used by the atomic on-demand publisher. */
    Map<String, Object> currentFaultRecord(FaultScenario scenario, List<WorkloadPlan> plans) {
        return faultRecord(scenario, plans);
    }

    private String participantAlias(List<SagaInstance> participants, String id) {
        for (int index = 0; index < participants.size(); index++) if (Objects.equals(participants.get(index).deterministicId(), id)) return "p" + (index + 1);
        return id;
    }

    private Map<String, List<PersistedRoute>> sagaRoutes(Path sagaFactsPath) throws IOException {
        Map<String, List<PersistedRoute>> result = new LinkedHashMap<>();
        for (String line : Files.readAllLines(sagaFactsPath, StandardCharsets.UTF_8)) {
            if (line.isBlank()) continue;
            JsonNode saga = MAPPER.readTree(line);
            List<PersistedRoute> routes = new ArrayList<>();
            saga.path("steps").forEach(step -> step.path("eventRoutes").forEach(route ->
                    routes.add(new PersistedRoute(step.path("id").asText(), route))));
            result.put(saga.path("fqn").asText(), List.copyOf(routes));
        }
        return Map.copyOf(result);
    }

    private String routeId(EventConsequence event, WorkloadPlan plan,
                           Map<String, String> localStepIds,
                           Map<String, List<PersistedRoute>> sagaRoutes) {
        ScheduledStep trigger = plan.forwardSchedule().stream()
                .filter(step -> Objects.equals(step.deterministicId(), event.triggerScheduledStepId()))
                .findFirst().orElseThrow(() -> new IllegalArgumentException("event has no exact trigger step"));
        String saga = plan.participants().stream()
                .filter(participant -> Objects.equals(participant.deterministicId(), trigger.sagaInstanceId()))
                .map(SagaInstance::sagaFqn).findFirst().orElseThrow();
        String step = localStepIds.get(event.triggerScheduledStepId());
        if (event.emissionSite() == null || step == null) {
            throw new IllegalArgumentException("event has no exact emission/step identity: " + event.deterministicId());
        }
        String origin = step + "/event#" + event.emissionSite().emissionOrdinal();
        String eventName = event.eventTypeFqn() == null ? null
                : event.eventTypeFqn().substring(event.eventTypeFqn().lastIndexOf('.') + 1);
        // The static package owns route numbering. A workload often selects only one
        // of several routes, so its own consequence index cannot identify that route.
        List<String> matches = sagaRoutes.getOrDefault(saga, List.of()).stream()
                .filter(route -> Objects.equals(route.stepId(), step))
                .map(PersistedRoute::value)
                .filter(route -> route.path("id").asText().equals(origin)
                        || route.path("id").asText().startsWith(origin + "-route#"))
                .filter(route -> Objects.equals(route.path("event").asText(null), eventName))
                .filter(route -> Objects.equals(route.path("eventHandlingClass").asText(null), event.eventHandlingClassFqn()))
                .filter(route -> Objects.equals(route.path("handler").asText(null), event.eventHandlerClassFqn()))
                .filter(route -> Objects.equals(route.path("processingMethod").asText(null), event.eventHandlingMethodName()))
                .filter(route -> Objects.equals(route.path("functionalityMethod").asText(null), event.facadeMethodName()))
                .filter(route -> Objects.equals(route.path("downstreamSaga").asText(null), event.downstreamSagaFqn()))
                .map(route -> route.path("id").asText())
                .toList();
        if (matches.size() != 1) {
            throw new IllegalArgumentException("event " + event.deterministicId()
                    + " has no unique exact Saga route in " + saga + "::" + origin
                    + " (matches=" + matches.size() + ")");
        }
        return matches.getFirst();
    }

    private record PersistedRoute(String stepId, JsonNode value) {}

    private String runtimeName(String stepId) {
        if (stepId == null) return "step";
        int marker = stepId.lastIndexOf("::");
        String value = marker < 0 ? stepId : stepId.substring(marker + 2);
        return value.replaceFirst("#\\d+$", "");
    }

    private String localStepId(String stepId) {
        if (stepId == null) return "step#0";
        int marker = stepId.lastIndexOf("::");
        return marker < 0 ? stepId : stepId.substring(marker + 2);
    }

    private void enrichAccounting(Path path, EagerFaultScenarioGenerationResult generation, int setupCount) throws IOException {
        ObjectNode account = (ObjectNode) MAPPER.readTree(Files.readString(path));
        ObjectNode workloads = account.with("workloads");
        workloads.with("written").put("total", generation.workloadPlans().size());
        ObjectNode setups = account.with("setups");
        long sourceMaterializable = generation.workloadPlans().stream().filter(plan -> plan.setupPlan() != null)
                .filter(plan -> plan.setupPlan().blockers().isEmpty()).count();
        long sourceBlocked = generation.workloadPlans().stream().filter(plan -> plan.setupPlan() != null).count() - sourceMaterializable;
        ObjectNode source = setups.with("sourceDerived");
        source.put("materializable", sourceMaterializable);
        source.put("blocked", sourceBlocked);
        ObjectNode provider = setups.with("providerBacked");
        provider.put("setups", generation.workloadPlans().stream().filter(plan -> plan.prerequisiteBaseline() != null).map(WorkloadPlan::prerequisiteBaseline).distinct().count());
        provider.put("workloads", generation.workloadPlans().stream().filter(plan -> plan.prerequisiteBaseline() != null).count());
        ObjectNode faults = account.with("faultScenarios");
        BigIntegerSum sums = new BigIntegerSum(generation);
        ObjectNode initial = faults.with("initial");
        initial.put("vectorsComputed", generation.computedVectors().size());
        initial.put("possibleForComputedVectors", sums.uncapped);
        initial.put("written", generation.faultScenarios().size());
        faults.set("current", initial.deepCopy());
        account.with("configuration").put("maxRecoverySchedulesPerVector", generation.recoveryScheduleCap());
        writeCompact(path, account);
    }

    private void writeJsonLines(Path path, List<?> records) throws IOException {
        Files.createDirectories(path.getParent());
        StringBuilder content = new StringBuilder();
        for (Object record : records) content.append(MAPPER.writeValueAsString(record)).append('\n');
        Files.writeString(path, content.toString(), StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private void putArtifact(ObjectNode files, Path root, String role, Path path) throws IOException {
        ObjectNode metadata = MAPPER.createObjectNode();
        metadata.put("path", root.relativize(path.toAbsolutePath().normalize()).toString());
        metadata.put("sha256", sha256(path));
        files.set(role, metadata);
    }

    private void writeCompact(Path path, Object value) throws IOException {
        Files.writeString(path, MAPPER.writeValueAsString(value), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private String sha256(Path path) throws IOException { return sha256(Files.readAllBytes(path)); }

    private String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(byte[] bytes) {
        try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch (NoSuchAlgorithmException exception) { throw new IllegalStateException("SHA-256 is unavailable", exception); }
    }

    private static final class BigIntegerSum {
        private final java.math.BigInteger uncapped;
        private BigIntegerSum(EagerFaultScenarioGenerationResult generation) {
            uncapped = generation.computedVectors().stream().map(vector -> vector.uncappedScheduleCount()).reduce(java.math.BigInteger.ZERO, java.math.BigInteger::add);
        }
    }
}
