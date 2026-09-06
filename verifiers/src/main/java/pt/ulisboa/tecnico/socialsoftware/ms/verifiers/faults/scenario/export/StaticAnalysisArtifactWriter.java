package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.AccessPolicy;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicity;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchMultiplicityKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchPhase;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.StepDispatchFootprint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ConflictGraphBuilder;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.DateExpressionSupport;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.InputVariantNormalizer;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.InputTupleSelection;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioIdGenerator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ScenarioModelAdapterResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingCalculator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport.GroupedSagaSetRow;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationEvidenceClass;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventConsequenceDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeArgument;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeAssignment;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeMapEntry;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeNode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioCatalogManifest;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepFootprint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FixtureOrigin;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorReadinessEvaluator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceAggregateKeyInputEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Publishes the current count-only package: accounting and three authoritative
 * static fact streams. Catalog-writing reuses this static boundary and adds the
 * current executable roles through {@link ExecutableArtifactWriter}.
 */
public final class StaticAnalysisArtifactWriter {

    public static final String DEFAULT_ACCOUNTING_FILE = "accounting.json";
    public static final String DEFAULT_SAGA_FACT_FILE = "sagas.jsonl";
    public static final String DEFAULT_INPUT_FACT_FILE = "inputs.jsonl";
    public static final String DEFAULT_INTERACTION_FACT_FILE = "interactions.jsonl";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public ScenarioCatalogManifest.Current write(ScenarioModelAdapterResult model,
                                        String targetApplication,
                                        ScenarioGeneratorConfig config,
                                        Path outputDirectory,
                                        String generatedAt) throws IOException {
        Path base = Objects.requireNonNull(outputDirectory, "outputDirectory");
        return write(model, targetApplication, config,
                base.resolve("scenario-catalog-manifest.json"),
                base.resolve(DEFAULT_ACCOUNTING_FILE),
                base.resolve(DEFAULT_SAGA_FACT_FILE),
                base.resolve(DEFAULT_INPUT_FACT_FILE),
                base.resolve(DEFAULT_INTERACTION_FACT_FILE), generatedAt);
    }

    public ScenarioCatalogManifest.Current write(ScenarioModelAdapterResult model,
                                        String targetApplication,
                                        ScenarioGeneratorConfig config,
                                        Path manifestPath,
                                        Path accountingPath,
                                        Path sagaFactsPath,
                                        Path inputFactsPath,
                                        Path interactionFactsPath,
                                        String generatedAt) throws IOException {
        return writeInternal(model, targetApplication, config, manifestPath, accountingPath, sagaFactsPath,
                inputFactsPath, interactionFactsPath, generatedAt, List.of());
    }

    ScenarioCatalogManifest.Current writeExecutable(ScenarioModelAdapterResult model,
                                        String targetApplication,
                                        ScenarioGeneratorConfig config,
                                        Path manifestPath,
                                        Path accountingPath,
                                        Path sagaFactsPath,
                                        Path inputFactsPath,
                                        Path interactionFactsPath,
                                        String generatedAt,
                                        List<InputVariant> requiredPackageInputs) throws IOException {
        return writeInternal(model, targetApplication, config, manifestPath, accountingPath, sagaFactsPath,
                inputFactsPath, interactionFactsPath, generatedAt, requiredPackageInputs);
    }

    private ScenarioCatalogManifest.Current writeInternal(ScenarioModelAdapterResult model,
                                        String targetApplication,
                                        ScenarioGeneratorConfig config,
                                        Path manifestPath,
                                        Path accountingPath,
                                        Path sagaFactsPath,
                                        Path inputFactsPath,
                                        Path interactionFactsPath,
                                        String generatedAt,
                                        List<InputVariant> requiredPackageInputs) throws IOException {
        ScenarioModelAdapterResult safeModel = Objects.requireNonNull(model, "model");
        ScenarioGeneratorConfig safeConfig = config == null ? new ScenarioGeneratorConfig() : config;
        Path manifest = Objects.requireNonNull(manifestPath, "manifestPath").toAbsolutePath().normalize();
        Path accounting = Objects.requireNonNull(accountingPath, "accountingPath").toAbsolutePath().normalize();
        Path sagas = Objects.requireNonNull(sagaFactsPath, "sagaFactsPath").toAbsolutePath().normalize();
        Path inputs = Objects.requireNonNull(inputFactsPath, "inputFactsPath").toAbsolutePath().normalize();
        Path interactions = Objects.requireNonNull(interactionFactsPath, "interactionFactsPath").toAbsolutePath().normalize();
        Path root = manifest.getParent();
        if (root == null) {
            throw new IllegalArgumentException("manifest path must have a package directory");
        }
        for (Path path : List.of(accounting, sagas, inputs, interactions)) {
            if (!path.startsWith(root)) {
                throw new IllegalArgumentException("static artifact must remain under manifest directory: " + path);
            }
        }
        createParents(manifest, accounting, sagas, inputs, interactions);

        List<SagaDefinition> sagaDefinitions = uniqueSagas(safeModel.sagaDefinitions());
        List<InputProjection> accountingInputProjections = inputProjections(safeModel.inputVariants(), safeConfig,
                sagaDefinitions);
        List<InputVariant> packageInputs = mergeInputVariants(safeModel.inputVariants(), requiredPackageInputs);
        Set<String> requiredInputIds = (requiredPackageInputs == null ? List.<InputVariant>of() : requiredPackageInputs)
                .stream()
                .filter(Objects::nonNull)
                .map(InputVariantNormalizer::normalizeForArtifact)
                .filter(Objects::nonNull)
                .map(InputVariant::deterministicId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<InputProjection> inputProjections = inputProjections(packageInputs, safeConfig,
                sagaDefinitions, requiredInputIds);
        List<Map<String, Object>> sagaFacts = sagaFacts(sagaDefinitions, safeModel);
        List<Map<String, Object>> inputFacts = inputProjections.stream()
                .map(projection -> inputFact(projection, safeModel.aggregateKeyInputEvidence()))
                .toList();
        List<Map<String, Object>> interactionFacts = interactionFacts(sagaDefinitions, safeConfig);
        // ScenarioSpaceAccountingCalculator owns input-bound workload totals.
        // It shares InputTupleSelection with catalog enumeration and preserves
        // the configured SEGMENT_COMPRESSED schedule count. The compact
        // artifact is only a projection of that report.
        ScenarioSpaceAccountingReport accountingReport = new ScenarioSpaceAccountingCalculator().calculate(
                targetApplication, sagaDefinitions, safeModel.inputVariants(),
                safeModel.sourceSetupPlanBindings(), safeModel.aggregateKeyInputEvidence(), safeConfig, 0);
        Map<String, Object> account = accounting(targetApplication, safeConfig, sagaDefinitions,
                accountingInputProjections, interactionFacts, safeModel.eventConsequenceDefinitions(),
                safeModel.aggregateKeyInputEvidence(), accountingReport);

        return publishProjection(manifest, accounting, sagas, inputs, interactions,
                account, sagaFacts, inputFacts, interactionFacts);
    }

    /* Package-private seam used by the exact fixture contract test. */
    static ScenarioCatalogManifest.Current writeProjection(Path outputDirectory,
                                                            Object accounting,
                                                            List<?> sagas,
                                                            List<?> inputs,
                                                            List<?> interactions) throws IOException {
        Path root = Objects.requireNonNull(outputDirectory, "outputDirectory").toAbsolutePath().normalize();
        return publishProjection(root.resolve("scenario-catalog-manifest.json"),
                root.resolve(DEFAULT_ACCOUNTING_FILE), root.resolve(DEFAULT_SAGA_FACT_FILE),
                root.resolve(DEFAULT_INPUT_FACT_FILE), root.resolve(DEFAULT_INTERACTION_FACT_FILE),
                accounting, sagas, inputs, interactions);
    }

    private static ScenarioCatalogManifest.Current publishProjection(Path manifest,
                                                                      Path accountingPath,
                                                                      Path sagaPath,
                                                                      Path inputPath,
                                                                      Path interactionPath,
                                                                      Object accounting,
                                                                      List<?> sagas,
                                                                      List<?> inputs,
                                                                      List<?> interactions) throws IOException {
        createParents(manifest, accountingPath, sagaPath, inputPath, interactionPath);
        writeJsonLines(sagaPath, sagas);
        writeJsonLines(inputPath, inputs);
        writeJsonLines(interactionPath, interactions);
        writeCompactJson(accountingPath, accounting);

        Path root = manifest.getParent();
        LinkedHashMap<String, ScenarioCatalogManifest.Current.ArtifactFile> files = new LinkedHashMap<>();
        files.put("accounting", file(accountingPath, root));
        files.put("sagas", file(sagaPath, root));
        files.put("inputs", file(inputPath, root));
        files.put("interactions", file(interactionPath, root));
        ScenarioCatalogManifest.Current result = new ScenarioCatalogManifest.Current(
                ScenarioCatalogManifest.Current.FORMAT_VERSION, files);
        writeCompactJson(manifest, result);
        return result;
    }

    private List<SagaDefinition> uniqueSagas(List<SagaDefinition> source) {
        LinkedHashMap<String, SagaDefinition> unique = new LinkedHashMap<>();
        (source == null ? List.<SagaDefinition>of() : source).stream()
                .filter(Objects::nonNull)
                .filter(saga -> saga.sagaFqn() != null && !saga.sagaFqn().isBlank())
                .sorted(Comparator.comparing(SagaDefinition::sagaFqn))
                .forEach(saga -> unique.putIfAbsent(saga.sagaFqn(), saga));
        return List.copyOf(unique.values());
    }

    private List<InputProjection> inputProjections(List<InputVariant> raw,
                                                    ScenarioGeneratorConfig config,
                                                    List<SagaDefinition> sagas) {
        return inputProjections(raw, config, sagas, Set.of());
    }

    private List<InputProjection> inputProjections(List<InputVariant> raw,
                                                    ScenarioGeneratorConfig config,
                                                    List<SagaDefinition> sagas,
                                                    Set<String> requiredInputIds) {
        Set<String> knownSagas = (sagas == null ? List.<SagaDefinition>of() : sagas).stream()
                .map(SagaDefinition::sagaFqn)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        LinkedHashMap<String, InputVariant> unique = new LinkedHashMap<>();
        (raw == null ? List.<InputVariant>of() : raw).stream()
                .filter(Objects::nonNull)
                .map(InputVariantNormalizer::normalizeForArtifact)
                .filter(input -> input != null && input.deterministicId() != null
                        && input.sagaFqn() != null && knownSagas.contains(input.sagaFqn()))
                .sorted(Comparator.comparing(InputVariant::deterministicId))
                .forEach(input -> unique.putIfAbsent(input.deterministicId(), input));
        Map<String, List<InputVariant>> eligibleBySaga = new LinkedHashMap<>();
        List<InputProjection> projections = new ArrayList<>();
        for (InputVariant input : unique.values()) {
            String reason = null;
            if (input.sourceMode() == SourceMode.TCC || input.sourceMode() == SourceMode.MIXED) {
                reason = "sourceModeRejected";
            } else if (!InputVariantNormalizer.allowedByPolicy(input.resolutionStatus(), config.inputPolicy())) {
                reason = "inputPolicyRejected";
            } else {
                eligibleBySaga.computeIfAbsent(input.sagaFqn(), ignored -> new ArrayList<>()).add(input);
            }
            projections.add(new InputProjection(input, reason, false, readiness(input)));
        }
        int max = Math.max(0, config.maxInputVariantsPerSaga());
        Set<String> retained = new HashSet<>();
        Set<String> eligibleIds = eligibleBySaga.values().stream()
                .flatMap(List::stream)
                .map(InputVariant::deterministicId)
                .collect(Collectors.toSet());
        retained.addAll(requiredInputIds == null ? Set.of() : requiredInputIds.stream()
                .filter(eligibleIds::contains)
                .collect(Collectors.toSet()));
        eligibleBySaga.forEach((saga, values) -> values.stream()
                .sorted(Comparator.comparing(InputVariant::deterministicId))
                .limit(max)
                .forEach(input -> retained.add(input.deterministicId())));
        List<InputProjection> result = new ArrayList<>();
        for (InputProjection projection : projections) {
            InputVariant input = projection.input();
            String reason = projection.rejectionReason();
            if (reason == null && !retained.contains(input.deterministicId())) {
                reason = "maxInputsPerSaga";
            }
            result.add(new InputProjection(input, reason, reason == null, projection.readiness()));
        }
        result.sort(Comparator.comparing(projection -> projection.input().deterministicId()));
        return List.copyOf(result);
    }

    private List<InputVariant> mergeInputVariants(List<InputVariant> original,
                                                   List<InputVariant> additional) {
        LinkedHashMap<String, InputVariant> byId = new LinkedHashMap<>();
        Stream.concat(original == null ? Stream.empty() : original.stream(),
                        additional == null ? Stream.empty() : additional.stream())
                .filter(Objects::nonNull)
                .map(InputVariantNormalizer::normalizeForArtifact)
                .filter(input -> input != null && input.deterministicId() != null)
                .sorted(Comparator.comparing(InputVariant::deterministicId))
                .forEach(input -> byId.putIfAbsent(input.deterministicId(), input));
        return List.copyOf(byId.values());
    }

    private ScenarioExecutorReadinessEvaluator.Readiness readiness(InputVariant input) {
        return new ScenarioExecutorReadinessEvaluator().evaluate(input);
    }

    private List<Map<String, Object>> sagaFacts(List<SagaDefinition> sagas, ScenarioModelAdapterResult model) {
        Map<String, List<StepDispatchFootprint>> dispatches = model.dispatchesBySaga();
        Map<String, List<EventConsequenceDefinition>> eventsByStep = model.eventConsequenceDefinitions().stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(EventConsequenceDefinition::triggerSagaFqn,
                        LinkedHashMap::new, Collectors.toList()));
        List<Map<String, Object>> facts = new ArrayList<>();
        for (SagaDefinition saga : sagas) {
            LinkedHashMap<String, Object> fact = new LinkedHashMap<>();
            fact.put("fqn", saga.sagaFqn());
            List<Map<String, Object>> steps = new ArrayList<>();
            Map<String, Integer> ordinalByName = new HashMap<>();
            for (StepDefinition step : orderedSteps(saga)) {
                String localId = localStepId(step, ordinalByName);
                LinkedHashMap<String, Object> stepFact = new LinkedHashMap<>();
                stepFact.put("id", localId);
                stepFact.put("commandAccesses", commandAccesses(saga, step, dispatches.getOrDefault(saga.sagaFqn(), List.of())));
                // StepDefinition records whether registration was observed and
                // carries its analyzed compensation footprint, but it does not
                // carry a named target-step mapping. Do not turn the registering
                // forward step into a fabricated target; a future analyzer
                // extension can pass an actual Saga-local target here.
                stepFact.put("compensation", compensation(step, null));
                stepFact.put("eventRoutes", eventRoutes(saga.sagaFqn(), localId, step, eventsByStep.getOrDefault(saga.sagaFqn(), List.of())));
                stepFact.put("analysisLimitations", step.analysisDiagnostics().stream().distinct().sorted().toList());
                steps.add(stepFact);
            }
            fact.put("dependencies", dependencies(eventsByStep.getOrDefault(saga.sagaFqn(), List.of())));
            fact.put("steps", steps);
            facts.add(fact);
        }
        return List.copyOf(facts);
    }

    private List<StepDefinition> orderedSteps(SagaDefinition saga) {
        return saga.steps().stream().filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(StepDefinition::orderIndex)
                        .thenComparing(StepDefinition::deterministicId, Comparator.nullsFirst(String::compareTo)))
                .toList();
    }

    private String localStepId(StepDefinition step, Map<String, Integer> ordinalByName) {
        String name = step.name();
        if (name == null || name.isBlank()) {
            name = step.stepKey() == null ? "step" : step.stepKey().substring(step.stepKey().lastIndexOf("::") + 2);
        }
        int ordinal = ordinalByName.merge(name, 1, Integer::sum) - 1;
        return name + "#" + ordinal;
    }

    private List<Map<String, Object>> commandAccesses(SagaDefinition saga,
                                                       StepDefinition step,
                                                       List<StepDispatchFootprint> dispatches) {
        List<StepDispatchFootprint> matching = dispatches.stream()
                .filter(dispatch -> dispatch.phase() != DispatchPhase.COMPENSATION)
                .filter(dispatch -> Objects.equals(dispatch.stepKey(), step.stepKey()))
                .toList();
        List<Map<String, Object>> result = new ArrayList<>();
        if (!matching.isEmpty()) {
            for (StepDispatchFootprint dispatch : matching) {
                StepFootprint footprint = step.footprints().stream()
                        .filter(candidate -> Objects.equals(candidate.aggregateKey() == null ? null : candidate.aggregateKey().aggregateName(), dispatch.aggregateName()))
                        .findFirst().orElse(null);
                result.add(commandAccess(dispatch.commandTypeFqn(), dispatch.aggregateName(),
                        dispatch.accessPolicy(), dispatch.aggregateKeyText(), dispatch.aggregateKeyConfidence(),
                        footprint == null ? null : footprint.aggregateKey(), dispatch.multiplicity()));
            }
        } else {
            for (StepFootprint footprint : step.footprints()) {
                result.add(commandAccess(null,
                        footprint.aggregateKey() == null ? null : footprint.aggregateKey().aggregateName(),
                        footprint.accessMode() == AccessMode.READ ? AccessPolicy.READ : AccessPolicy.WRITE,
                        footprint.aggregateKey() == null ? null : footprint.aggregateKey().keyText(), null,
                        footprint.aggregateKey(), null));
            }
        }
        return List.copyOf(result);
    }

    private Map<String, Object> commandAccess(String command,
                                               String aggregateName,
                                               AccessPolicy policy,
                                               String dispatchKey,
                                               StepDispatchFootprint.AggregateKeyConfidence dispatchConfidence,
                                               AggregateKey footprintKey,
                                               DispatchMultiplicity multiplicity) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        if (command != null && !command.isBlank()) result.put("command", simpleName(command));
        LinkedHashMap<String, Object> aggregate = new LinkedHashMap<>();
        if (footprintKey != null && footprintKey.aggregateTypeName() != null) aggregate.put("type", footprintKey.aggregateTypeName());
        if (aggregateName != null && !aggregateName.isBlank()) aggregate.put("name", aggregateName);
        aggregate.put("mode", policy == AccessPolicy.READ ? "read" : "write");
        Map<String, Object> keyEvidence = keyEvidence(
                footprintKey == null ? dispatchKey : footprintKey.keyText(),
                footprintKey == null ? dispatchConfidence : toDispatchConfidence(footprintKey.confidence()));
        if (!keyEvidence.isEmpty()) aggregate.put("keyEvidence", keyEvidence);
        result.put("aggregate", aggregate);
        if (multiplicity != null && multiplicity.kind() != null && multiplicity.kind() != DispatchMultiplicityKind.SINGLE) {
            LinkedHashMap<String, Object> repetition = new LinkedHashMap<>();
            repetition.put("kind", multiplicity.kind() == DispatchMultiplicityKind.STATIC_REPEAT ? "staticRepeat" : "inputDependent");
            if (multiplicity.staticCount() != null) repetition.put("max", multiplicity.staticCount());
            result.put("repetition", repetition);
        }
        return result;
    }

    private StepDispatchFootprint.AggregateKeyConfidence toDispatchConfidence(FootprintConfidence confidence) {
        return confidence == FootprintConfidence.EXACT
                ? StepDispatchFootprint.AggregateKeyConfidence.EXACT
                : StepDispatchFootprint.AggregateKeyConfidence.SYMBOLIC;
    }

    private Map<String, Object> compensation(StepDefinition step, String targetStep) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        CompensationEvidenceClass evidence = step.compensationEvidence();
        if (evidence == null) {
            result.put("kind", "none");
        } else {
            result.put("kind", switch (evidence) {
                case EXPLICIT_COMPENSATION -> "explicit";
                case IMPLICIT_SAGA_ROLLBACK -> "implicitSagaRollback";
                case CONSERVATIVE_UNKNOWN -> "conservativeUnknown";
            });
            // A target is serialized only when the analyzer supplies an actual
            // Saga-local target. The current StepDefinition has no such mapping,
            // so explicit registration remains explicit without a false target.
            if (evidence == CompensationEvidenceClass.EXPLICIT_COMPENSATION
                    && targetStep != null && !targetStep.isBlank()) {
                result.put("step", targetStep);
            }
        }
        return result;
    }

    private List<Map<String, Object>> eventRoutes(String sagaFqn,
                                                   String localStep,
                                                   StepDefinition step,
                                                   List<EventConsequenceDefinition> definitions) {
        return definitions.stream()
                .filter(definition -> Objects.equals(definition.triggerSagaFqn(), sagaFqn))
                .filter(definition -> Objects.equals(definition.triggerStepKey(), step.stepKey()))
                .filter(definition -> definition.emissionSite() != null)
                .sorted(Comparator.comparing((EventConsequenceDefinition definition) -> definition.emissionSite().emissionOrdinal())
                        .thenComparing(definition -> definition.emissionSite().eventTypeFqn(), Comparator.nullsFirst(String::compareTo))
                        .thenComparing(EventConsequenceDefinition::eventHandlerClassFqn, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(EventConsequenceDefinition::eventHandlingMethodName, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(EventConsequenceDefinition::facadeMethodName, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(EventConsequenceDefinition::downstreamSagaFqn, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(EventConsequenceDefinition::triggerStepKey, Comparator.nullsFirst(String::compareTo)))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(this::routeIdentity, definition -> definition,
                                (left, right) -> left, LinkedHashMap::new),
                        routes -> new ArrayList<>(routes.values())))
                .stream()
                .collect(Collectors.groupingBy(definition -> definition.emissionSite().emissionOrdinal(), LinkedHashMap::new, Collectors.toList()))
                .entrySet().stream()
                .flatMap(entry -> {
                    List<EventConsequenceDefinition> routes = entry.getValue();
                    return java.util.stream.IntStream.range(0, routes.size()).mapToObj(index -> Map.entry(routes.get(index), index));
                })
                .map(entry -> {
                    EventConsequenceDefinition definition = entry.getKey();
                    int routeIndex = entry.getValue();
                    LinkedHashMap<String, Object> route = new LinkedHashMap<>();
                    String routeId = localStep + "/event#" + definition.emissionSite().emissionOrdinal();
                    if (routeIndex > 0) routeId += "-route#" + routeIndex;
                    route.put("id", routeId);
                    route.put("event", simpleName(definition.emissionSite().eventTypeFqn()));
                    if (definition.eventHandlingClassFqn() != null) route.put("eventHandlingClass", definition.eventHandlingClassFqn());
                    if (definition.eventHandlerClassFqn() != null) route.put("handler", definition.eventHandlerClassFqn());
                    if (definition.eventHandlingMethodName() != null) route.put("processingMethod", definition.eventHandlingMethodName());
                    if (definition.facadeMethodName() != null) route.put("functionalityMethod", definition.facadeMethodName());
                    if (definition.downstreamSagaFqn() != null) route.put("downstreamSaga", definition.downstreamSagaFqn());
                    return (Map<String, Object>) route;
                }).toList();
    }

    private String routeIdentity(EventConsequenceDefinition definition) {
        return String.join("\u0000",
                String.valueOf(definition.triggerStepKey()),
                String.valueOf(definition.emissionSite().sourceServiceClassFqn()),
                String.valueOf(definition.emissionSite().sourceServiceMethodSignature()),
                String.valueOf(definition.emissionSite().emissionOrdinal()),
                String.valueOf(definition.emissionSite().eventTypeFqn()),
                String.valueOf(definition.eventHandlingClassFqn()),
                String.valueOf(definition.eventHandlingMethodName()),
                String.valueOf(definition.eventHandlerClassFqn()),
                String.valueOf(definition.eventProcessingClassFqn()),
                String.valueOf(definition.eventProcessingMethodName()),
                String.valueOf(definition.facadeClassFqn()),
                String.valueOf(definition.facadeMethodName()),
                String.valueOf(definition.downstreamSagaFqn()),
                String.valueOf(definition.deliveryPolicy()));
    }

    private List<String> dependencies(List<EventConsequenceDefinition> definitions) {
        return definitions.stream().map(EventConsequenceDefinition::downstreamSagaFqn)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }

    private Map<String, Object> inputFact(InputProjection projection,
                                          List<SourceAggregateKeyInputEvidence> sourceEvidence) {
        InputVariant input = projection.input();
        LinkedHashMap<String, Object> fact = new LinkedHashMap<>();
        fact.put("id", input.deterministicId());
        fact.put("saga", input.sagaFqn());
        LinkedHashMap<String, Object> source = new LinkedHashMap<>();
        source.put("testClass", nonBlankOrUnknown(input.sourceClassFqn()));
        source.put("method", nonBlankOrUnknown(input.sourceMethodName()));
        if (input.callContextMethodName() != null && !Objects.equals(input.callContextMethodName(), input.sourceMethodName())) {
            source.put("calledFrom", input.callContextMethodName());
        }
        source.put("testRole", testRole(input));
        String call = input.stableSourceText();
        if (call == null || call.isBlank()) call = input.provenanceText();
        if (call == null || call.isBlank()) call = input.sourceBindingName();
        if (call == null || call.isBlank()) call = "unresolved";
        source.put("call", call);
        fact.put("source", source);
        fact.put("transactionModel", transactionModel(input.sourceMode()));
        fact.put("resolution", resolution(input.resolutionStatus()));
        Map<String, Object> aggregateKey = aggregateKeyEvidence(input, sourceEvidence);
        if (!aggregateKey.isEmpty()) fact.put("aggregateKeyEvidence", aggregateKey);
        fact.put("arguments", arguments(input.inputRecipe()));
        fact.put("accepted", projection.accepted());
        if (!projection.accepted()) fact.put("notAcceptedReason", projection.rejectionReason());
        fact.put("materializable", projection.readiness().materializable());
        List<Map<String, Object>> blockers = blockers(input, projection.readiness());
        if (!blockers.isEmpty()) fact.put("blockers", blockers);
        if (!input.warnings().isEmpty()) fact.put("warnings", input.warnings().stream().distinct().sorted().toList());
        List<String> usedBy = input.owners().stream().map(owner -> owner.testMethodName())
                .filter(Objects::nonNull).filter(method -> !Objects.equals(method, input.sourceMethodName())).distinct().sorted().toList();
        if (!usedBy.isEmpty()) fact.put("usedBy", usedBy);
        return fact;
    }

    private String testRole(InputVariant input) {
        return switch (input.fixtureOrigin()) {
            case SETUP, SETUP_SPEC, INHERITED_SETUP -> "setup";
            case SETUP_HELPER, INHERITED_HELPER, FIELD, INHERITED_FIELD -> "setupHelper";
            default -> "featureUnderTest";
        };
    }

    private String transactionModel(SourceMode mode) {
        return mode == SourceMode.SAGAS ? "saga" : mode == SourceMode.TCC ? "tcc" : "unknown";
    }

    private String resolution(InputResolutionStatus status) {
        return switch (status == null ? InputResolutionStatus.UNRESOLVED : status) {
            case RESOLVED -> "fullyResolved";
            case REPLAYABLE -> "runtimeDependent";
            case PARTIAL -> "partial";
            case UNRESOLVED -> "unresolved";
        };
    }

    private Map<String, Object> aggregateKeyEvidence(InputVariant input,
                                                      List<SourceAggregateKeyInputEvidence> sourceEvidence) {
        SourceAggregateKeyInputEvidence source = sourceEvidence == null ? null : sourceEvidence.stream()
                .filter(evidence -> Objects.equals(evidence.inputVariantId(), input.deterministicId()))
                .findFirst().orElse(null);
        if (source != null && source.producerReference() != null) {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            result.put("kind", "sameSource");
            result.put("aggregate", source.aggregateName());
            String origin = nonBlankOrUnknown(input.sourceClassFqn()) + "." + nonBlankOrUnknown(input.sourceMethodName());
            result.put("origin", origin);
            String expression = input.sourceBindingName();
            if (expression == null || expression.isBlank()) expression = source.producerReference().producerMethodName();
            if (source.producerReference().propertyPath() != null && !source.producerReference().propertyPath().isEmpty()) {
                expression = (expression == null ? "value" : expression) + "." + String.join(".", source.producerReference().propertyPath());
            }
            if (expression != null) result.put("expression", expression);
            return result;
        }
        // logicalKeyBindings are compatibility evidence for tuple joining, not
        // analyzer-produced aggregate-key evidence. Never pair them with an
        // unrelated Saga footprint and claim an exact aggregate key here.
        return Map.of();
    }

    private List<Map<String, Object>> arguments(InputRecipe recipe) {
        if (recipe == null || recipe.arguments() == null) return List.of();
        return recipe.arguments().stream().sorted(Comparator.comparingInt(InputRecipeArgument::index)).map(argument -> {
            LinkedHashMap<String, Object> result = new LinkedHashMap<>();
            result.put("index", argument.index());
            if (argument.expectedTypeFqn() != null) result.put("expectedType", argument.expectedTypeFqn());
            if (argument.provenanceText() != null) result.put("sourceExpression", argument.provenanceText());
            result.put("value", recipeNode(argument.recipe()));
            return result;
        }).map(value -> (Map<String, Object>) value).toList();
    }

    private Map<String, Object> recipeNode(InputRecipeNode node) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        if (node == null) {
            result.put("kind", "unresolved");
            result.put("reason", "missingRecipe");
            return result;
        }
        String kind = node.kind() == null ? "unresolved" : node.kind();
        switch (kind) {
            case "literal" -> {
                result.put("kind", "literal");
                if (node.value() != null) result.put("value", node.value());
            }
            case "constructor" -> {
                result.put("kind", "constructor");
                if (node.targetTypeFqn() != null) result.put("targetType", node.targetTypeFqn());
                LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
                node.assignments().stream().sorted(Comparator.comparingInt(InputRecipeAssignment::orderIndex))
                        .forEach(assignment -> fields.put(assignment.propertyName(), recipeNode(assignment.valueRecipe())));
                if (!fields.isEmpty()) result.put("fields", fields);
                if (!node.arguments().isEmpty()) result.put("arguments", node.arguments().stream()
                        .sorted(Comparator.comparingInt(InputRecipeArgument::index)).map(argument -> recipeNode(argument.recipe())).toList());
            }
            case "collection" -> {
                result.put("kind", "collection");
                if (node.collectionKind() != null) result.put("collectionKind", node.collectionKind());
                if (!node.elements().isEmpty()) result.put("elements", node.elements().stream().map(this::recipeNode).toList());
                if (!node.entries().isEmpty()) {
                    result.put("entries", node.entries().stream().sorted(Comparator.comparingInt(InputRecipeMapEntry::index)).map(entry -> {
                        LinkedHashMap<String, Object> item = new LinkedHashMap<>();
                        item.put("key", recipeNode(entry.keyRecipe()));
                        item.put("value", recipeNode(entry.valueRecipe()));
                        return item;
                    }).toList());
                }
            }
            case "relative_date_time" -> {
                result.put("kind", "relativeDateTime");
                result.put("anchor", node.anchor());
                result.put("offset", node.offset());
            }
            case "runtime", "call_result" -> {
                result.put("kind", "runtime");
                String type = "call_result".equals(kind) ? node.expectedReturnTypeFqn() : node.expectedTypeFqn();
                if (type != null) result.put("type", type);
                if (node.helperName() != null) result.put("provider", node.helperName());
                String scope = runtimeScope(type);
                if (scope != null) result.put("scope", scope);
            }
            case "property_access" -> {
                result.put("kind", "property");
                if (node.propertyName() != null) result.put("property", node.propertyName());
                if (node.receiver() != null) result.put("receiver", recipeNode(node.receiver()));
            }
            case "local_transform" -> {
                result.put("kind", "transform");
                if (node.transformName() != null) result.put("name", node.transformName());
                if (node.receiver() != null) {
                    var offset = "DateHandler.toISOString".equals(node.transformName())
                            ? DateExpressionSupport.normalize(node.receiver()) : Optional.<String>empty();
                    result.put("receiver", offset.isPresent()
                            ? recipeNode(DateExpressionSupport.compactNode(node.receiver(), offset.get()))
                            : recipeNode(node.receiver()));
                }
            }
            case "placeholder" -> {
                result.put("kind", "placeholder");
                if (node.placeholderId() != null) result.put("id", node.placeholderId());
            }
            case "baseline_binding" -> {
                result.put("kind", "baseline_binding");
                if (node.bindingKey() != null) result.put("key", node.bindingKey());
                if (node.bindingTypeFqn() != null) result.put("type", node.bindingTypeFqn());
            }
            default -> {
                result.put("kind", "unresolved");
                String reason = node.blockers().isEmpty() ? "unresolved" : node.blockers().get(0);
                result.put("reason", lowerCamel(reason));
            }
        }
        return result;
    }

    private List<Map<String, Object>> blockers(InputVariant input,
                                               ScenarioExecutorReadinessEvaluator.Readiness readiness) {
        LinkedHashMap<String, Map<String, Object>> unique = new LinkedHashMap<>();
        Set<String> ownedReasons = new HashSet<>();
        InputRecipe recipe = input.inputRecipe();
        if (recipe != null) {
            ScenarioExecutorReadinessEvaluator evaluator = new ScenarioExecutorReadinessEvaluator();
            for (InputRecipeArgument argument : recipe.arguments().stream()
                    .sorted(Comparator.comparingInt(InputRecipeArgument::index)).toList()) {
                for (String blocker : evaluator.evaluate(argument).blockers()) {
                    addBlocker(unique, blocker, argument);
                    ownedReasons.add(blocker);
                }
            }
        }
        if (readiness != null && readiness.blockers() != null) {
            readiness.blockers().stream().filter(blocker -> !ownedReasons.contains(blocker))
                    .forEach(blocker -> addBlocker(unique, blocker, null));
        }
        return List.copyOf(unique.values());
    }

    private void addBlocker(Map<String, Map<String, Object>> unique,
                            String blocker,
                            InputRecipeArgument argument) {
        String reason = lowerCamel(blocker);
        LinkedHashMap<String, Object> value = new LinkedHashMap<>();
        if (argument != null) value.put("argument", argument.index());
        value.put("reason", reason);
        if (argument != null && argument.provenanceText() != null && !argument.provenanceText().isBlank()) {
            value.put("sourceExpression", argument.provenanceText());
        }
        unique.putIfAbsent(reason + "\u0000" + (argument == null ? "" : argument.index()), value);
    }

    private String runtimeScope(String type) {
        if (type == null || type.isBlank()) return null;
        if (type.endsWith("SagaUnitOfWorkService") || type.endsWith("CommandGateway")) return "execution";
        if (type.endsWith("SagaUnitOfWork")) return "participant";
        return null;
    }

    private List<Map<String, Object>> interactionFacts(List<SagaDefinition> sagas,
                                                        ScenarioGeneratorConfig config) {
        ConflictGraphBuilder.Result strict = ConflictGraphBuilder.build(sagas, graphConfig(config, false));
        ConflictGraphBuilder.Result broad = ConflictGraphBuilder.build(sagas, graphConfig(config, true));
        Map<String, Map<String, String>> localStepIds = localStepIds(sagas);
        LinkedHashMap<String, CandidatePair> pairs = new LinkedHashMap<>();
        strict.conflictCandidates().forEach(candidate -> pairs.computeIfAbsent(interactionKey(candidate), ignored -> new CandidatePair(candidate)).strict = candidate);
        broad.conflictCandidates().forEach(candidate -> pairs.computeIfAbsent(interactionKey(candidate), ignored -> new CandidatePair(candidate)).broad = candidate);
        return pairs.entrySet().stream().sorted(Map.Entry.comparingByKey()).map(entry -> interactionFact(entry.getValue(), localStepIds)).toList();
    }

    private Map<String, Map<String, String>> localStepIds(List<SagaDefinition> sagas) {
        LinkedHashMap<String, Map<String, String>> result = new LinkedHashMap<>();
        for (SagaDefinition saga : sagas) {
            LinkedHashMap<String, String> byDefinitionId = new LinkedHashMap<>();
            Map<String, Integer> ordinals = new HashMap<>();
            for (StepDefinition step : orderedSteps(saga)) {
                byDefinitionId.put(ScenarioIdGenerator.stepDefinitionId(saga.sagaFqn(), step), localStepId(step, ordinals));
            }
            result.put(saga.sagaFqn(), byDefinitionId);
        }
        return result;
    }

    private Map<String, Object> interactionFact(CandidatePair pair,
                                                 Map<String, Map<String, String>> localStepIds) {
        ConflictGraphBuilder.ConflictCandidate representative = pair.strict != null ? pair.strict : pair.broad;
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("id", interactionId(interactionKey(representative)));
        List<Map<String, Object>> accesses = new ArrayList<>();
        accesses.add(access(representative.leftSagaFqn(), localStepIds.getOrDefault(representative.leftSagaFqn(), Map.of()).getOrDefault(representative.leftStepId(), localStep(representative.leftStepId())), representative.leftFootprint()));
        accesses.add(access(representative.rightSagaFqn(), localStepIds.getOrDefault(representative.rightSagaFqn(), Map.of()).getOrDefault(representative.rightStepId(), localStep(representative.rightStepId())), representative.rightFootprint()));
        result.put("accesses", accesses);
        result.put("evidence", evidence(pair.strict != null ? pair.strict : pair.broad));
        return result;
    }

    private Map<String, Object> access(String saga, String step, StepFootprint footprint) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("saga", saga);
        result.put("step", step);
        if (footprint.aggregateKey() != null) {
            result.put("aggregate", footprint.aggregateKey().aggregateName());
            result.put("mode", footprint.accessMode().name().toLowerCase());
            Map<String, Object> keyEvidence = keyEvidence(footprint.aggregateKey().keyText(), toDispatchConfidence(footprint.aggregateKey().confidence()));
            if (!keyEvidence.isEmpty()) result.put("keyEvidence", keyEvidence);
        }
        return result;
    }

    private String evidence(ConflictGraphBuilder.ConflictCandidate candidate) {
        if (!candidate.fallbackUsed()) return "exact";
        return switch (candidate.kind()) {
            case SYMBOLIC -> "symbolic";
            case TYPE_ONLY -> "typeOnly";
            case UNKNOWN -> "unknown";
            default -> "symbolic";
        };
    }

    private String interactionKey(ConflictGraphBuilder.ConflictCandidate candidate) {
        return String.join("\u0000", String.valueOf(candidate.leftStepId()), String.valueOf(candidate.rightStepId()),
                keyIdentity(candidate.leftFootprint().aggregateKey()), keyIdentity(candidate.rightFootprint().aggregateKey()),
                String.valueOf(candidate.leftFootprint().accessMode()), String.valueOf(candidate.rightFootprint().accessMode()));
    }

    private String keyIdentity(AggregateKey key) {
        return key == null ? "" : String.join("|", String.valueOf(key.aggregateTypeName()),
                String.valueOf(key.aggregateName()), String.valueOf(key.keyText()), key.confidence().name());
    }

    private String interactionId(String key) {
        return hash("direct-interaction\u0000" + key);
    }

    private Map<String, Object> keyEvidence(String value, StepDispatchFootprint.AggregateKeyConfidence confidence) {
        if (value == null || value.isBlank()) return Map.of();
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("kind", confidence == StepDispatchFootprint.AggregateKeyConfidence.EXACT ? "exact" : "symbolic");
        result.put("value", value);
        return result;
    }

    private String localStep(String full) {
        if (full == null) return "";
        int index = full.lastIndexOf("::");
        return index < 0 ? full : full.substring(index + 2);
    }

    private ScenarioGeneratorConfig graphConfig(ScenarioGeneratorConfig config, boolean fallback) {
        return new ScenarioGeneratorConfig(config.exportEnabled(), config.generationStrategy(), config.catalogWriteMode(),
                config.includeSingles(), config.maxSagaSetSize(), config.maxCatalogScenarios(), config.maxInputVariantsPerSaga(),
                config.maxSchedulesPerInputTuple(), fallback, config.inputPolicy(), config.scheduleStrategy(),
                config.deterministicSeed(), config.maxGroupedSagaSetRows(), config.maxEventConsequencesPerWorkload());
    }

    private Map<String, Object> accounting(String targetApplication,
                                           ScenarioGeneratorConfig config,
                                           List<SagaDefinition> sagas,
                                           List<InputProjection> inputs,
                                           List<Map<String, Object>> interactionFacts,
                                           List<EventConsequenceDefinition> events,
                                           List<SourceAggregateKeyInputEvidence> aggregateKeyInputEvidence,
                                           ScenarioSpaceAccountingReport accountingReport) {
        LinkedHashMap<String, Object> root = new LinkedHashMap<>();
        root.put("configuration", configuration(targetApplication, config));
        root.put("sagas", sagaMetrics(sagas, inputs, interactionFacts));
        root.put("inputs", inputMetrics(inputs));
        root.put("interactions", interactionMetrics(interactionFacts, accountingReport, sagas, inputs,
                aggregateKeyInputEvidence, config));
        root.put("events", eventMetrics(events));
        root.put("workloads", workloadMetrics(accountingReport));
        return root;
    }

    private Map<String, Object> configuration(String target, ScenarioGeneratorConfig config) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("targetApplication", target == null || target.isBlank() ? "unknown" : target);
        result.put("catalogWriteMode", config.catalogWriteMode() == ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY ? "count-only" : "catalog-writing");
        List<Integer> sizes = new ArrayList<>();
        int start = config.includeSingles() ? 1 : 2;
        for (int size = start; size <= config.maxSagaSetSize(); size++) sizes.add(size);
        result.put("sagaSetSizes", sizes);
        result.put("sagaSetSelection", config.generationStrategy() == ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE
                ? "all" : config.allowTypeOnlyFallback() ? "withTypeOnlyFallback" : "strict");
        result.put("acceptedInputStatuses", acceptedStatuses(config.inputPolicy()));
        if (config.maxEventConsequencesPerWorkload() > 1) {
            result.put("maxEventConsequencesPerWorkload", config.maxEventConsequencesPerWorkload());
        }
        if (config.maxInputVariantsPerSaga() > 0) result.put("maxInputsPerSaga", config.maxInputVariantsPerSaga());
        if (config.scheduleStrategy() != ScenarioGeneratorConfig.ScheduleStrategy.SERIAL && config.maxSchedulesPerInputTuple() > 0) {
            result.put("maxStepSchedulesPerInputCombination", config.maxSchedulesPerInputTuple());
        }
        if (config.catalogWriteMode() != ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY && config.maxCatalogScenarios() > 0) {
            result.put("maxWorkloadsWritten", config.maxCatalogScenarios());
            result.put("maxRecoverySchedulesPerVector", config.maxCatalogScenarios());
        }
        return result;
    }

    private List<String> acceptedStatuses(ScenarioGeneratorConfig.InputPolicy policy) {
        return switch (policy) {
            case RESOLVED_ONLY -> List.of("fullyResolved");
            case RESOLVED_OR_REPLAYABLE -> List.of("fullyResolved", "runtimeDependent");
            case ALLOW_PARTIAL -> List.of("fullyResolved", "runtimeDependent", "partial");
            case ALLOW_UNRESOLVED -> List.of("fullyResolved", "runtimeDependent", "partial", "unresolved");
        };
    }

    private Map<String, Object> sagaMetrics(List<SagaDefinition> sagas,
                                            List<InputProjection> inputs,
                                            List<Map<String, Object>> interactionFacts) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("found", sagas.size());
        Set<String> withAccepted = inputs.stream().filter(InputProjection::accepted).map(value -> value.input().sagaFqn()).collect(Collectors.toSet());
        result.put("withAcceptedInputs", withAccepted.size());
        result.put("withoutAcceptedInputs", sagas.size() - withAccepted.size());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (SagaDefinition saga : sagas) {
            List<InputProjection> belonging = inputs.stream().filter(input -> Objects.equals(input.input().sagaFqn(), saga.sagaFqn())).toList();
            long accepted = belonging.stream().filter(InputProjection::accepted).count();
            long materializable = belonging.stream().filter(InputProjection::accepted).filter(input -> input.readiness().materializable()).count();
            long fallback = interactionFacts.stream()
                    .filter(fact -> ((List<?>) fact.get("accesses")).stream()
                            .anyMatch(access -> Objects.equals(((Map<?, ?>) access).get("saga"), saga.sagaFqn())))
                    .count();
            long strict = interactionFacts.stream()
                    .filter(fact -> Set.of("exact", "symbolic").contains(fact.get("evidence")))
                    .filter(fact -> ((List<?>) fact.get("accesses")).stream()
                            .anyMatch(access -> Objects.equals(((Map<?, ?>) access).get("saga"), saga.sagaFqn())))
                    .count();
            LinkedHashMap<String, Object> row = new LinkedHashMap<>();
            row.put("fqn", saga.sagaFqn()); row.put("stepCount", saga.steps().size());
            row.put("acceptedInputs", accepted); row.put("notAcceptedInputs", belonging.size() - accepted);
            row.put("materializableInputs", materializable); row.put("blockedInputs", accepted - materializable);
            row.put("strictDirectInteractions", strict); row.put("fallbackDirectInteractions", fallback);
            rows.add(row);
        }
        result.put("bySaga", rows);
        return result;
    }

    private Map<String, Object> inputMetrics(List<InputProjection> inputs) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("found", inputs.size());
        long accepted = inputs.stream().filter(InputProjection::accepted).count();
        result.put("accepted", accepted); result.put("notAccepted", inputs.size() - accepted);
        result.put("acceptedByStatus", countBy(inputs.stream().filter(InputProjection::accepted).map(value -> resolution(value.input().resolutionStatus())).toList()));
        result.put("notAcceptedByReason", countBy(inputs.stream().filter(value -> !value.accepted()).map(InputProjection::rejectionReason).toList()));
        long materializable = inputs.stream().filter(InputProjection::accepted).filter(value -> value.readiness().materializable()).count();
        LinkedHashMap<String, Object> materializability = new LinkedHashMap<>();
        materializability.put("materializable", materializable);
        materializability.put("blocked", accepted - materializable);
        materializability.put("affectedInputsByReason", blockerCounts(inputs));
        result.put("materializability", materializability);
        return result;
    }

    private Map<String, Object> interactionMetrics(List<Map<String, Object>> interactionFacts,
                                                   ScenarioSpaceAccountingReport accountingReport,
                                                   List<SagaDefinition> sagas,
                                                   List<InputProjection> inputs,
                                                   List<SourceAggregateKeyInputEvidence> aggregateKeyInputEvidence,
                                                   ScenarioGeneratorConfig config) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("direct", directMetrics(interactionFacts));
        Map<String, List<InputVariant>> acceptedInputsBySaga = inputs.stream()
                .filter(InputProjection::accepted)
                .map(InputProjection::input)
                .collect(Collectors.groupingBy(InputVariant::sagaFqn, LinkedHashMap::new, Collectors.toList()));
        ConflictGraphBuilder.Result strictGraph = ConflictGraphBuilder.build(sagas, graphConfig(config, false));
        ConflictGraphBuilder.Result broadGraph = ConflictGraphBuilder.build(sagas, graphConfig(config, true));
        LinkedHashMap<String, Object> sagaSets = new LinkedHashMap<>();
        sagaSets.put("strict", sagaSetMetrics(accountingReport.typeLevelCoverage().strict(),
                accountingReport.groupedSagaSets(), acceptedInputsBySaga, strictGraph.conflictCandidates(),
                aggregateKeyInputEvidence, InputTupleSelection.Mode.STRICT));
        sagaSets.put("withTypeOnlyFallback", sagaSetMetrics(accountingReport.typeLevelCoverage().broad(),
                accountingReport.groupedSagaSets(), acceptedInputsBySaga, broadGraph.conflictCandidates(),
                aggregateKeyInputEvidence, InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK));
        result.put("sagaSets", sagaSets);
        return result;
    }

    private Map<String, Object> directMetrics(List<Map<String, Object>> interactionFacts) {
        LinkedHashMap<String, Object> evidence = new LinkedHashMap<>();
        for (String kind : List.of("exact", "symbolic", "typeOnly", "unknown")) evidence.put(kind, 0);
        for (Map<String, Object> interaction : interactionFacts) {
            String kind = String.valueOf(interaction.getOrDefault("evidence", "unknown"));
            if (!evidence.containsKey(kind)) kind = "unknown";
            evidence.put(kind, ((Integer) evidence.get(kind)) + 1);
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("total", interactionFacts.size());
        result.put("byEvidence", evidence);
        return result;
    }

    private Map<String, Object> sagaSetMetrics(ScenarioSpaceAccountingReport.InteractionCoverage coverage,
                                               List<GroupedSagaSetRow> groupedRows,
                                               Map<String, List<InputVariant>> inputsBySaga,
                                               List<ConflictGraphBuilder.ConflictCandidate> candidates,
                                               List<SourceAggregateKeyInputEvidence> aggregateKeyInputEvidence,
                                               InputTupleSelection.Mode mode) {
        LinkedHashMap<String, BigInteger> covered = new LinkedHashMap<>();
        groupedRows.stream()
                .filter(row -> row.sagaSetSize() >= 2)
                .filter(row -> (mode == InputTupleSelection.Mode.STRICT
                        ? row.strictInteractionSummary() : row.broadInteractionSummary()).connected())
                .filter(row -> InputTupleSelection.count(row.sagaFqns(), inputsBySaga, candidates,
                        aggregateKeyInputEvidence, mode).signum() > 0)
                .forEach(row -> covered.merge(Integer.toString(row.sagaSetSize()), BigInteger.ONE, BigInteger::add));
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("connectedBySize", decimalMap(coverage.connectedSetCountsBySize()));
        result.put("withAcceptedInputsBySize", stringify(covered));
        return result;
    }

    private Map<String, Object> eventMetrics(List<EventConsequenceDefinition> events) {
        Set<String> emissions = new LinkedHashSet<>(); Set<String> routes = new LinkedHashSet<>();
        for (EventConsequenceDefinition event : events == null ? List.<EventConsequenceDefinition>of() : events) {
            if (event == null || event.emissionSite() == null) continue;
            emissions.add(event.emissionSite().sourceServiceClassFqn() + "\u0000" + event.emissionSite().sourceServiceMethodSignature() + "\u0000" + event.emissionSite().emissionOrdinal() + "\u0000" + event.emissionSite().eventTypeFqn());
            // A route is the resolved event-to-handler-to-Saga chain, not one
            // placement of that chain. Include every field that survives into
            // the Saga-local route fact so repeated consequences share one
            // deterministic route count while genuinely distinct routes stay
            // distinct.
            routes.add(String.join("\u0000", String.valueOf(event.emissionSite().eventTypeFqn()),
                    String.valueOf(event.eventHandlingClassFqn()), String.valueOf(event.eventHandlingMethodName()),
                    String.valueOf(event.eventHandlerClassFqn()), String.valueOf(event.eventProcessingClassFqn()),
                    String.valueOf(event.eventProcessingMethodName()), String.valueOf(event.facadeClassFqn()),
                    String.valueOf(event.facadeMethodName()), String.valueOf(event.downstreamSagaFqn()),
                    String.valueOf(event.deliveryPolicy())));
        }
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("emissionSites", emissions.size());
        result.put("resolvedEventRoutes", routes.size());
        return result;
    }

    private Map<String, Object> workloadMetrics(ScenarioSpaceAccountingReport accountingReport) {
        ScenarioSpaceAccountingReport.InputBoundScenarioSpace space = accountingReport.inputBoundScenarioSpace();
        Map<String, Object> allTotals = workloadTotals(space.allInputBound(), accountingReport.groupedSagaSets(), false);
        Map<String, Object> selectedTotals = workloadTotals(space.selectedByGenerator(), accountingReport.groupedSagaSets(), true);
        LinkedHashMap<String, Object> written = new LinkedHashMap<>();
        written.put("total", 0);
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("all", allTotals);
        result.put("selected", selectedTotals);
        result.put("written", written);
        ScenarioSpaceAccountingReport.SetupCoverage setup = accountingReport.inputBoundScenarioSpace().setupCoverage();
        if (setup != null) {
            LinkedHashMap<String, Object> setupMetrics = new LinkedHashMap<>();
            setupMetrics.put("withSourceSetup", setupTotals(setup.withSourceSetup()));
            setupMetrics.put("withoutSetup", setupTotals(setup.withoutSetup()));
            setupMetrics.put("blocked", setupTotals(setup.blocked()));
            result.put("setup", setupMetrics);
        }
        return result;
    }

    private Map<String, Object> setupTotals(ScenarioSpaceAccountingReport.ScenarioSpaceTotals totals) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("total", new BigInteger(totals.total()));
        result.put("bySagaSetSize", decimalMap(totals.bySagaSetSize()));
        return result;
    }

    private Map<String, Object> workloadTotals(ScenarioSpaceAccountingReport.ScenarioSpaceTotals totals,
                                               List<GroupedSagaSetRow> groupedRows,
                                               boolean selected) {
        LinkedHashMap<String, BigInteger> rowsBySize = new LinkedHashMap<>();
        groupedRows.stream()
                .filter(row -> !selected || row.selectedByConfiguredGenerator())
                .forEach(row -> rowsBySize.merge(Integer.toString(row.sagaSetSize()), BigInteger.ONE, BigInteger::add));
        LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        result.put("total", rowsBySize.values().stream().reduce(BigInteger.ZERO, BigInteger::add));
        result.put("bySagaSetSize", stringify(rowsBySize));
        result.put("inputBoundTotal", new BigInteger(totals.total()));
        return result;
    }

    private Map<String, BigInteger> decimalMap(Map<String, String> values) {
        LinkedHashMap<String, BigInteger> result = new LinkedHashMap<>();
        (values == null ? Map.<String, String>of() : values).forEach((key, value) -> result.put(key, new BigInteger(value)));
        return result;
    }

    private Map<String, Object> blockerCounts(List<InputProjection> inputs) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        inputs.stream().filter(InputProjection::accepted).filter(value -> !value.readiness().materializable())
                .forEach(value -> value.readiness().blockers().stream().map(this::lowerCamel).distinct().sorted()
                        .forEach(reason -> counts.merge(reason, 1, Integer::sum)));
        return new LinkedHashMap<>(counts);
    }

    private Map<String, Object> countBy(List<String> values) {
        LinkedHashMap<String, Object> result = new LinkedHashMap<>(); values.stream().filter(Objects::nonNull).sorted().forEach(value -> result.merge(value, 1, (left, right) -> ((Integer) left) + ((Integer) right))); return result;
    }

    private Map<String, BigInteger> stringify(Map<String, BigInteger> values) {
        LinkedHashMap<String, BigInteger> result = new LinkedHashMap<>();
        values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> result.put(entry.getKey(), entry.getValue()));
        return result;
    }

    private String lowerCamel(String value) {
        if (value == null || value.isBlank()) return "unknown";
        String normalized = value.trim(); String[] pieces = normalized.split("[_:]"); StringBuilder result = new StringBuilder(pieces[0].toLowerCase());
        for (int i = 1; i < pieces.length; i++) if (!pieces[i].isBlank()) result.append(Character.toUpperCase(pieces[i].charAt(0))).append(pieces[i].substring(1).toLowerCase());
        return result.toString();
    }

    private String nonBlankOrUnknown(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }

    private String simpleName(String value) { if (value == null) return null; int index = Math.max(value.lastIndexOf('.'), value.lastIndexOf('$')); return index < 0 ? value : value.substring(index + 1); }

    private static void writeJsonLines(Path path, List<?> records) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
            for (Object record : records) { writer.write(MAPPER.writeValueAsString(record)); writer.write('\n'); }
        }
    }

    private static void writeCompactJson(Path path, Object value) throws IOException {
        Files.writeString(path, MAPPER.writeValueAsString(value), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
    }

    private static void createParents(Path... paths) throws IOException { for (Path path : paths) { if (path.getParent() != null) Files.createDirectories(path.getParent()); } }

    private static ScenarioCatalogManifest.Current.ArtifactFile file(Path path, Path root) throws IOException { return new ScenarioCatalogManifest.Current.ArtifactFile(root.relativize(path).toString(), sha256(path)); }

    private static String sha256(Path path) throws IOException { try { return HexFormatHolder.hex(MessageDigest.getInstance("SHA-256"), path); } catch (NoSuchAlgorithmException exception) { throw new IllegalStateException(exception); } }

    private String hash(String value) {
        try {
            return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static final class HexFormatHolder { static String hex(MessageDigest digest, Path path) throws IOException { try (var stream = Files.newInputStream(path)) { byte[] buffer = new byte[65536]; int read; while ((read = stream.read(buffer)) >= 0) if (read > 0) digest.update(buffer, 0, read); } return java.util.HexFormat.of().formatHex(digest.digest()); } }

    private record InputProjection(InputVariant input, String rejectionReason, boolean accepted, ScenarioExecutorReadinessEvaluator.Readiness readiness) { }
    private static final class CandidatePair {
        private ConflictGraphBuilder.ConflictCandidate strict;
        private ConflictGraphBuilder.ConflictCandidate broad;

        private CandidatePair(ConflictGraphBuilder.ConflictCandidate ignored) { }
    }
}
