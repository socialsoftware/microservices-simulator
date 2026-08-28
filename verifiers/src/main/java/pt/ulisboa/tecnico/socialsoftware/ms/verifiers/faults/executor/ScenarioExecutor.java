package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFinalizationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowRecoveryCheckpoint;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowStepExecutionResult;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowStepRecoveryException;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowStepRecoveryResult;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.DomainFailure;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorBoundaryContext;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorFault;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorProviderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.faults.InMemoryFaultVectorProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorderHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventReplayCoordinator;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventReplayException;
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.export.EnrichedScenarioCatalogWriter;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.WorkloadDynamicEvidenceRecord;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogPackageReader;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.BaselineBindingRequirement;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationCheckpoint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventConsequence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenario;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioAction;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioActionKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ForwardFaultSlot;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.PrerequisiteBaseline;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadMaterializability;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.CompletionException;

public final class ScenarioExecutor {
    private final ScenarioCatalogReader reader;
    private final ScenarioMaterializer materializer;
    private final ScenarioSetupRunner setupRunner;
    private final ObjectMapper mapper;

    public ScenarioExecutor() {
        this(new ScenarioCatalogReader(), new ScenarioMaterializer(), new ScenarioSetupRunner(), new ObjectMapper());
    }

    ScenarioExecutor(ScenarioCatalogReader reader,
                     ScenarioMaterializer materializer,
                     ScenarioSetupRunner setupRunner,
                     ObjectMapper mapper) {
        this.reader = Objects.requireNonNull(reader);
        this.materializer = Objects.requireNonNull(materializer);
        this.setupRunner = Objects.requireNonNull(setupRunner);
        this.mapper = Objects.requireNonNull(mapper);
    }

    public ScenarioExecutionReport execute(ScenarioExecutorOptions options, ScenarioRuntimeContext runtimeContext) {
        Objects.requireNonNull(options, "executor options are required");
        Objects.requireNonNull(runtimeContext, "scenario runtime context is required");
        ScenarioCatalogPackageReader.SelectedPackageContents selectedPackage =
                reader.readSelected(options, null, options.faultScenarioId());
        rejectPackageOutputAlias(options.packagePath(), options.outputPath(), selectedPackage,
                "Scenario execution report");
        rejectPackageOutputAlias(options.packagePath(), options.impactOutputPath(), selectedPackage,
                "Scenario impact report");
        rejectExecutionOutputAlias(options.outputPath(), options.impactOutputPath());
        String attemptId = UUID.randomUUID().toString();
        FaultScenario scenario = selectedPackage == null ? null : selectedPackage.faultScenario();
        WorkloadPlan workload = selectedPackage == null ? null : selectedPackage.workloadPlan();
        ImpactV1Collector impactCollector = options.impactOutputPath() == null
                ? null
                : new ImpactV1Collector(DynamicEvidenceRecorderHolder.getRecorder(), attemptId,
                workload == null ? null : workload.deterministicId());
        ScenarioExecutionReport report;
        if (scenario == null) {
            report = selectionFailureReport(options, attemptId, null, options.faultScenarioId(),
                    "MISSING_FAULT_SCENARIO_ID");
        } else if (workload == null) {
            report = selectionFailureReport(options, attemptId, scenario.workloadPlanId(),
                    scenario.deterministicId(), "MISSING_WORKLOAD_PLAN_ID");
        } else {
            report = executeSelected(options, runtimeContext, attemptId, workload, scenario, impactCollector);
        }
        try {
            writeReport(options, report);
        } catch (RuntimeException failure) {
            ScenarioExecutionReport failedReport = reportWriteFailure(report, failure);
            try {
                writeImpactReport(options, failedReport, findings(impactCollector));
            } catch (RuntimeException impactWriteFailure) {
                failure.addSuppressed(impactWriteFailure);
            }
            throw new ScenarioReportWriteException(failedReport, failure);
        }
        writeImpactReport(options, report, findings(impactCollector));
        return report;
    }

    private ScenarioExecutionReport selectionFailureReport(ScenarioExecutorOptions options,
                                                           String attemptId,
                                                           String workloadPlanId,
                                                           String faultScenarioId,
                                                           String reason) {
        ScenarioExecutionReport.Blocker blocker = new ScenarioExecutionReport.Blocker(
                workloadPlanId, faultScenarioId, null, null, null, null, reason,
                "MISSING_WORKLOAD_PLAN_ID".equals(reason) ? workloadPlanId : faultScenarioId);
        return report(options, attemptId, "SELECTION_FAILED", null, null, "NONE",
                TraceMetadata.hardStop(reason), List.of(), List.of(), List.of(), List.of(), List.of(),
                List.of(blocker));
    }

    public ScenarioSetupPreflightReport preflight(ScenarioSetupPreflightOptions options,
                                                   ScenarioRuntimeContext runtimeContext) {
        PreflightPlan plan = preflightPlan(options);
        if (!plan.sourceSetupWorkloadIds().isEmpty()) {
            throw new IllegalStateException("Source-derived setup preflight requires one fresh process per workload; "
                    + "use ScenarioExecutorCli or the supported wrapper");
        }
        return preflightSelected(options, runtimeContext, plan.candidateWorkloadIds(), false);
    }

    PreflightPlan preflightPlan(ScenarioSetupPreflightOptions options) {
        Objects.requireNonNull(options, "setup preflight options are required");
        ScenarioCatalogPackageReader.PackageContents packageContents = reader.read(
                new ScenarioExecutorOptions(options.packagePath(), null, null, false));
        rejectPackageOutputAlias(options.packagePath(), options.outputPath(), packageContents,
                "Scenario setup preflight report");
        Set<String> candidateIds = validateMaterializabilityTable(packageContents);
        Set<String> sourceSetupIds = packageContents.workloadPlans().stream()
                .filter(workload -> candidateIds.contains(workload.deterministicId()))
                .filter(workload -> workload.setupPlan() != null)
                .map(WorkloadPlan::deterministicId)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        return new PreflightPlan(Set.copyOf(candidateIds), Set.copyOf(sourceSetupIds));
    }

    ScenarioSetupPreflightReport preflightIsolatedAttempt(ScenarioSetupPreflightOptions options,
                                                           ScenarioRuntimeContext runtimeContext,
                                                           Set<String> workloadIds) {
        return preflightSelected(options, runtimeContext, workloadIds, true);
    }

    private ScenarioSetupPreflightReport preflightSelected(ScenarioSetupPreflightOptions options,
                                                            ScenarioRuntimeContext runtimeContext,
                                                            Set<String> selectedWorkloadIds,
                                                            boolean freshProcessAttempt) {
        Objects.requireNonNull(options, "setup preflight options are required");
        Objects.requireNonNull(runtimeContext, "scenario runtime context is required");
        Objects.requireNonNull(selectedWorkloadIds, "selected preflight workloads are required");
        ScenarioCatalogPackageReader.PackageContents packageContents = reader.read(
                new ScenarioExecutorOptions(options.packagePath(), null, null, false));
        rejectPackageOutputAlias(options.packagePath(), options.outputPath(), packageContents,
                "Scenario setup preflight report");
        Set<String> candidateIds = validateMaterializabilityTable(packageContents);
        if (!candidateIds.containsAll(selectedWorkloadIds)) {
            throw new IllegalArgumentException("Preflight selection contains a non-candidate WorkloadPlan");
        }
        List<WorkloadPlan> candidates = packageContents.workloadPlans().stream()
                .filter(workload -> selectedWorkloadIds.contains(workload.deterministicId()))
                .sorted(Comparator.comparing(WorkloadPlan::deterministicId))
                .toList();
        long sourceSetupCount = candidates.stream().filter(workload -> workload.setupPlan() != null).count();
        if (sourceSetupCount > 0 && (!freshProcessAttempt || candidates.size() != 1)) {
            throw new IllegalStateException("A source-derived setup preflight attempt must contain exactly one "
                    + "WorkloadPlan in an independently launched fresh process");
        }

        long batchStarted = System.nanoTime();
        String preflightAttemptId = UUID.randomUUID().toString();
        List<ScenarioSetupPreflightReport.WorkloadResult> results = new ArrayList<>();
        int participantCount = 0;
        for (WorkloadPlan workload : candidates) {
            long workloadStarted = System.nanoTime();
            SetupResult setup = setup(workload, null,
                    preflightAttemptId + ":" + workload.deterministicId(), runtimeContext);
            long workloadDuration = System.nanoTime() - workloadStarted;
            participantCount += setup.participants().size();
            List<ScenarioSetupPreflightReport.ParticipantResult> participants = setup.participants().stream()
                    .map(participant -> preflightParticipant(workload, setup.status(), participant))
                    .toList();
            results.add(new ScenarioSetupPreflightReport.WorkloadResult(
                    workload.deterministicId(), setup.status(), workloadDuration,
                    setup.sourceSetup(), participants, setup.blockers()));
        }
        long batchDuration = System.nanoTime() - batchStarted;
        String packagePath = manifestPath(options.packagePath()).toString();
        ScenarioExecutionReport.RuntimeMetadata runtimeMetadata = new ScenarioExecutionReport.RuntimeMetadata(
                options.applicationBase(), options.applicationId(), options.springApplicationClass(),
                options.springProfiles(), options.mavenProfile(), packagePath, null,
                "STATIC_MATERIALIZABILITY_CANDIDATE_PREFLIGHT", false);
        String terminalStatus = results.stream().allMatch(result -> "SETUP_READY".equals(result.status()))
                ? "SUCCESS"
                : "SETUP_FAILED";
        ScenarioSetupPreflightReport report = new ScenarioSetupPreflightReport(
                ScenarioSetupPreflightReport.SCHEMA_VERSION, preflightAttemptId, terminalStatus,
                packagePath, ScenarioSetupPreflightReport.CANDIDATE_SELECTION, results.size(), participantCount,
                batchDuration, runtimeMetadata, results);
        writePreflightReport(options, report);
        return report;
    }

    record PreflightPlan(Set<String> candidateWorkloadIds, Set<String> sourceSetupWorkloadIds) {
        PreflightPlan {
            candidateWorkloadIds = Set.copyOf(candidateWorkloadIds);
            sourceSetupWorkloadIds = Set.copyOf(sourceSetupWorkloadIds);
        }
    }

    private Set<String> validateMaterializabilityTable(
            ScenarioCatalogPackageReader.PackageContents packageContents) {
        Set<String> workloadIds = packageContents.workloadPlans().stream()
                .map(WorkloadPlan::deterministicId)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        Map<String, WorkloadMaterializability> declarations = new LinkedHashMap<>();
        for (WorkloadMaterializability declaration
                : packageContents.manifest().workloadMaterializability()) {
            if (declaration.workloadPlanId() == null) {
                throw new IllegalArgumentException("Manifest materializability row has no WorkloadPlan id");
            }
            if (declarations.putIfAbsent(declaration.workloadPlanId(), declaration) != null) {
                throw new IllegalArgumentException("Duplicate manifest materializability row for WorkloadPlan "
                        + declaration.workloadPlanId());
            }
        }
        if (!declarations.keySet().equals(workloadIds)) {
            Set<String> missing = new TreeSet<>(workloadIds);
            missing.removeAll(declarations.keySet());
            Set<String> extra = new TreeSet<>(declarations.keySet());
            extra.removeAll(workloadIds);
            throw new IllegalArgumentException("Manifest materializability rows do not match WorkloadPlans; missing="
                    + missing + ", extra=" + extra);
        }
        Set<String> candidateIds = declarations.values().stream()
                .filter(WorkloadMaterializability::materializable)
                .map(WorkloadMaterializability::workloadPlanId)
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
        String declaredCandidateCount = packageContents.manifest().counts().get("materializableWorkloadPlans");
        if (declaredCandidateCount != null
                && !exactNonNegativeCount(declaredCandidateCount, "materializableWorkloadPlans")
                .equals(BigInteger.valueOf(candidateIds.size()))) {
            throw new IllegalArgumentException("Manifest count mismatch for materializableWorkloadPlans");
        }
        return candidateIds;
    }

    private BigInteger exactNonNegativeCount(String value, String label) {
        try {
            BigInteger parsed = new BigInteger(value);
            if (parsed.signum() < 0) throw new NumberFormatException("negative");
            return parsed;
        } catch (RuntimeException failure) {
            throw new IllegalArgumentException("Manifest count " + label
                    + " must be a non-negative exact decimal", failure);
        }
    }

    private ScenarioExecutionReport executeSelected(ScenarioExecutorOptions options,
                                                    ScenarioRuntimeContext runtimeContext,
                                                    String attemptId,
                                                    WorkloadPlan workload,
                                                    FaultScenario scenario,
                                                    ImpactV1Collector impactCollector) {
        ResolvedContract contract = resolveContract(workload, scenario);
        if (options.dryRun()) {
            return report(options, attemptId, "DRY_RUN", workload, scenario, "NONE", TraceMetadata.none(),
                    contract.faultSlots(), contract.plannedActions(), List.of(), List.of(), participantStates(workload), List.of());
        }

        SetupResult setup = setup(workload, scenario, attemptId, runtimeContext);
        if (!"SETUP_READY".equals(setup.status())) {
            return report(options, attemptId, setup.status(), workload, scenario, "NONE",
                    TraceMetadata.hardStop(setup.status()),
                    contract.faultSlots(), contract.plannedActions(), List.of(), List.of(),
                    setup.participants(), setup.blockers());
        }
        return replay(options, attemptId, workload, scenario, contract, setup.participants(), impactCollector,
                runtimeContext);
    }

    private SetupResult setup(WorkloadPlan workload,
                              FaultScenario scenario,
                              String attemptId,
                              ScenarioRuntimeContext runtimeContext) {
        List<ParticipantState> participants = participantStates(workload);
        PrerequisiteResult prerequisite = preparePrerequisite(workload, scenario, runtimeContext);
        participants.forEach(participant -> participant.prerequisiteSetup = prerequisite.report());
        if (!prerequisite.success()) {
            return new SetupResult("PREREQUISITE_BASELINE_FAILED", participants,
                    null, prerequisite.blockers());
        }

        Map<ScenarioSetupRunner.ParticipantArgument, Object> setupArguments = Map.of();
        ScenarioExecutionReport.SourceSetup sourceSetup = null;
        if (workload.setupPlan() != null) {
            ScenarioSetupRunner.Result setup = setupRunner.run(
                    workload, scenario, attemptId, runtimeContext);
            sourceSetup = setup.report();
            ScenarioExecutionReport.SourceSetup evidence = sourceSetup;
            participants.forEach(participant -> participant.sourceSetup = evidence);
            if (!setup.success()) {
                return new SetupResult(setup.status(), participants, sourceSetup, setup.blockers());
            }
            setupArguments = setup.participantArguments();
        }

        List<ScenarioExecutionReport.Blocker> blockers = materializeAll(
                workload, scenario, participants, runtimeContext, prerequisite.bindings(), setupArguments);
        if (!blockers.isEmpty()) {
            return new SetupResult("MATERIALIZATION_FAILED", participants, sourceSetup, blockers);
        }
        blockers = startAll(workload, scenario, participants);
        if (!blockers.isEmpty()) {
            return new SetupResult("STARTUP_FAILED", participants, sourceSetup, blockers);
        }
        return new SetupResult("SETUP_READY", participants, sourceSetup, List.of());
    }

    private PrerequisiteResult preparePrerequisite(WorkloadPlan workload,
                                                    FaultScenario scenario,
                                                    ScenarioRuntimeContext runtimeContext) {
        PrerequisiteBaseline baseline = workload.prerequisiteBaseline();
        if (baseline == null) {
            return PrerequisiteResult.success(Map.of(), new ScenarioExecutionReport.PrerequisiteSetup(
                    null, null, "NOT_REQUIRED", 0L, 0L, true, List.of(), Map.of(), null, null));
        }
        long started = System.nanoTime();
        List<ScenarioExecutionReport.BaselineBinding> bindingEvidence = new ArrayList<>();
        try {
            if (!EventReplayCoordinator.isActive()) {
                throw new BaselineFailure("EVENT_REPLAY_CONTROL_FAILED",
                        "event replay gate was not active before prerequisite setup");
            }
            List<ScenarioPrerequisiteProvider> providers = runtimeContext.beans(ScenarioPrerequisiteProvider.class).stream()
                    .filter(provider -> Objects.equals(provider.providerId(), baseline.providerId()))
                    .filter(provider -> Objects.equals(provider.providerVersion(), baseline.providerVersion()))
                    .toList();
            if (providers.size() != 1) {
                throw new BaselineFailure("PREREQUISITE_PROVIDER_NOT_FOUND",
                        "expected exactly one provider " + baseline.providerId() + "@" + baseline.providerVersion()
                                + " but found " + providers.size());
            }
            ScenarioPrerequisiteResult result = providers.get(0).prepare(runtimeContext, baseline.requiredBindings());
            if (result == null) {
                throw new BaselineFailure("PREREQUISITE_PROVIDER_FAILED", "provider returned no result");
            }
            Map<String, Object> bindings = new LinkedHashMap<>();
            for (BaselineBindingRequirement requirement : baseline.requiredBindings()) {
                Object value = result.bindings().get(requirement.key());
                if (!result.bindings().containsKey(requirement.key())) {
                    bindingEvidence.add(new ScenarioExecutionReport.BaselineBinding(
                            requirement.key(), requirement.typeFqn(), null, "MISSING"));
                    throw new BaselineFailure("MISSING_BASELINE_BINDING",
                            "provider did not return required binding " + requirement.key());
                }
                Class<?> expected = Class.forName(requirement.typeFqn());
                if (value == null || !expected.isInstance(value)) {
                    bindingEvidence.add(new ScenarioExecutionReport.BaselineBinding(
                            requirement.key(), requirement.typeFqn(), value == null ? null : value.getClass().getName(),
                            "TYPE_MISMATCH"));
                    throw new BaselineFailure("BASELINE_BINDING_TYPE_MISMATCH",
                            "provider binding " + requirement.key() + " is not " + requirement.typeFqn());
                }
                bindings.put(requirement.key(), value);
                bindingEvidence.add(new ScenarioExecutionReport.BaselineBinding(
                        requirement.key(), requirement.typeFqn(), value.getClass().getName(), "RESOLVED"));
            }
            EventService eventService = (EventService) runtimeContext.bean(EventService.class);
            long pending = eventService.eventCountForReplay();
            eventService.clearEventsForReplay();
            boolean empty = eventService.eventCountForReplay() == 0;
            if (!empty) {
                throw new BaselineFailure("PENDING_EVENT_BASELINE_NOT_EMPTY",
                        "pending events remained after prerequisite cleanup");
            }
            EventReplayCoordinator.assertNoOpenThreadScope();
            ScenarioExecutionReport.PrerequisiteSetup report = new ScenarioExecutionReport.PrerequisiteSetup(
                    baseline.providerId(), baseline.providerVersion(), "SUCCEEDED", System.nanoTime() - started,
                    pending, true, bindingEvidence, new TreeMap<>(result.evidence()), null, null);
            return PrerequisiteResult.success(Map.copyOf(bindings), report);
        } catch (Throwable failure) {
            Throwable cause = unwrap(failure);
            String failureReason = cause instanceof BaselineFailure baselineFailure
                    ? baselineFailure.reason : "PREREQUISITE_PROVIDER_FAILED";
            ScenarioExecutionReport.PrerequisiteSetup report = new ScenarioExecutionReport.PrerequisiteSetup(
                    baseline.providerId(), baseline.providerVersion(), "FAILED", System.nanoTime() - started,
                    0L, false, bindingEvidence, Map.of(), failureReason, failureDetails(cause));
            ScenarioExecutionReport.Blocker blocker = new ScenarioExecutionReport.Blocker(
                    workload.deterministicId(), scenario == null ? null : scenario.deterministicId(),
                    null, null, null, null, "PREREQUISITE_BASELINE_FAILED", failureReason + ": " + failureDetails(cause));
            return PrerequisiteResult.failure(report, List.of(blocker));
        }
    }

    private List<ScenarioExecutionReport.Blocker> materializeAll(WorkloadPlan workload,
                                                                 FaultScenario scenario,
                                                                 List<ParticipantState> participants,
                                                                 ScenarioRuntimeContext runtimeContext,
                                                                 Map<String, Object> baselineBindings,
                                                                 Map<ScenarioSetupRunner.ParticipantArgument, Object> setupArguments) {
        List<ScenarioExecutionReport.Blocker> blockers = new ArrayList<>();
        for (ParticipantState participant : participants) {
            try {
                participant.unitOfWork = (UnitOfWork) runtimeContext.createSagaUnitOfWork(participant.saga.deterministicId());
                Map<Integer, Object> participantSetupArguments = new LinkedHashMap<>();
                if (participant.input != null) {
                    setupArguments.forEach((key, value) -> {
                        if (Objects.equals(key.inputVariantId(), participant.input.deterministicId())) {
                            participantSetupArguments.put(key.argumentIndex(), value);
                        }
                    });
                }
                ScenarioMaterializer.MaterializedArguments result = materializer.materialize(
                        participant.input, runtimeContext, participant.saga.sagaFqn(), participant.unitOfWork,
                        baselineBindings, participantSetupArguments);
                if (result.success()) {
                    participant.materializedArguments = result.values();
                    participant.materializationState = "MATERIALIZED";
                } else {
                    participant.materializationState = "MATERIALIZATION_FAILED";
                    List<ScenarioExecutionReport.Blocker> owned = result.blockers().stream()
                            .map(blocker -> new ScenarioExecutionReport.Blocker(
                                    workload.deterministicId(), scenario == null ? null : scenario.deterministicId(), blocker.inputVariantId(),
                                    blocker.argumentIndex(), null, blocker.sourceScheduledStepId(),
                                    blocker.reason(), blocker.message()))
                            .toList();
                    participant.blockers.addAll(owned);
                    blockers.addAll(owned);
                }
            } catch (RuntimeException failure) {
                participant.materializationState = "MATERIALIZATION_FAILED";
                ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, null, null,
                        "MATERIALIZATION_FAILED", failure);
                participant.blockers.add(blocker);
                blockers.add(blocker);
            }
        }
        return blockers;
    }

    private List<ScenarioExecutionReport.Blocker> startAll(WorkloadPlan workload,
                                                           FaultScenario scenario,
                                                           List<ParticipantState> participants) {
        List<ScenarioExecutionReport.Blocker> blockers = new ArrayList<>();
        for (ParticipantState participant : participants) {
            try {
                Object instance = instantiate(Class.forName(participant.saga.sagaFqn()), participant.materializedArguments);
                if (!(instance instanceof WorkflowFunctionality functionality)) {
                    throw new IllegalArgumentException(participant.saga.sagaFqn()
                            + " is not a WorkflowFunctionality and cannot use persisted Saga controls");
                }
                participant.functionality = functionality;
                participant.startupState = "STARTUP_READY";
            } catch (ReflectiveOperationException | RuntimeException failure) {
                participant.startupState = "STARTUP_FAILED";
                ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, null, null,
                        "STARTUP_FAILED", unwrap(failure));
                participant.blockers.add(blocker);
                blockers.add(blocker);
            }
        }
        return blockers;
    }

    private ScenarioExecutionReport replay(ScenarioExecutorOptions options,
                                           String attemptId,
                                           WorkloadPlan workload,
                                           FaultScenario scenario,
                                           ResolvedContract contract,
                                           List<ParticipantState> participants,
                                           ImpactV1Collector impactCollector,
                                           ScenarioRuntimeContext runtimeContext) {
        Map<String, ParticipantState> participantsById = new LinkedHashMap<>();
        participants.forEach(participant -> participantsById.put(participant.saga.deterministicId(), participant));
        Map<String, MutableFaultSlot> faultSlots = mutableFaultSlots(contract.faultSlots());
        List<ScenarioExecutionReport.ActionOutcome> actualActions = new ArrayList<>();
        List<ScenarioExecutionReport.LifecycleEvent> lifecycleEvents = new ArrayList<>();
        List<ScenarioExecutionReport.Blocker> blockers = new ArrayList<>();
        Map<String, String> finalFaultSlotByParticipant = new HashMap<>();
        workload.faultSlots().forEach(slot -> finalFaultSlotByParticipant.put(slot.sagaInstanceId(), slot.deterministicId()));
        Map<String, Integer> plannedCompensations = new HashMap<>();
        Map<String, String> triggerStates = new HashMap<>();
        Map<String, List<EventReplayCoordinator.CapturedEvent>> capturedEventsByTrigger = new HashMap<>();
        Set<String> eventTriggerIds = workload.eventConsequences().stream()
                .map(EventConsequence::triggerScheduledStepId)
                .collect(java.util.stream.Collectors.toSet());
        scenario.actions().stream()
                .filter(action -> action.kind() == FaultScenarioActionKind.COMPENSATION)
                .forEach(action -> plannedCompensations.merge(action.sagaInstanceId(), 1, Integer::sum));

        String deviationActionId = null;
        Integer deviationPlannedPosition = null;
        FaultScenarioAction activeAction = null;
        int activePlannedPosition = -1;
        if (!workload.eventConsequences().isEmpty() && !EventReplayCoordinator.isActive()) {
            ScenarioExecutionReport.Blocker blocker = new ScenarioExecutionReport.Blocker(
                    workload.deterministicId(), scenario.deterministicId(), null, null, null, null,
                    "EVENT_REPLAY_CONTROL_FAILED", "event replay gate was not active before application startup");
            blockers.add(blocker);
            return report(options, attemptId, "UNEXPECTED_EXECUTION_FAILURE", workload, scenario,
                    "IN_MEMORY_FAULT_VECTOR", incompleteTrace(actualActions, null, null, null,
                            "EVENT_REPLAY_CONTROL_FAILED"), snapshot(faultSlots), contract.plannedActions(),
                    actualActions, lifecycleEvents, participants, blockers);
        }
        InMemoryFaultVectorProvider provider = provider(attemptId, workload, scenario, participantsById);
        try (FaultVectorProviderHolder.Scope ignored = FaultVectorProviderHolder.install(provider)) {
            try (DynamicEvidenceRecorderHolder.Scope measured = impactCollector == null
                    ? null : DynamicEvidenceRecorderHolder.install(impactCollector)) {
                for (int plannedPosition = 0; plannedPosition < scenario.actions().size(); plannedPosition++) {
                FaultScenarioAction action = scenario.actions().get(plannedPosition);
                activeAction = action;
                activePlannedPosition = plannedPosition;
                ParticipantState participant = participantsById.get(action.sagaInstanceId());
                ResolvedAction resolved = contract.actionsById().get(action.deterministicId());
                if (action.kind() == FaultScenarioActionKind.EVENT_CONSEQUENCE) {
                    EventActionResult eventResult = executeEventConsequence(
                            resolved, plannedPosition, actualActions.size(), triggerStates,
                            capturedEventsByTrigger, runtimeContext);
                    actualActions.add(eventResult.outcome());
                    if (!eventResult.completed()) {
                        ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, action,
                                resolved.sourceScheduledStepId(), eventResult.reason(), eventResult.message());
                        blockers.add(blocker);
                        if (participant != null) participant.blockers.add(blocker);
                        markHardStopSkips(workload, scenario, plannedPosition, participantsById);
                        return report(options, attemptId, "UNEXPECTED_EXECUTION_FAILURE", workload, scenario,
                                "IN_MEMORY_FAULT_VECTOR", incompleteTrace(actualActions, deviationActionId,
                                        deviationPlannedPosition, action.deterministicId(), eventResult.reason()),
                                snapshot(faultSlots), contract.plannedActions(), actualActions, lifecycleEvents,
                                participants, blockers);
                    }
                    continue;
                }
                if (participant != null && participant.runtimeDeviation) {
                    continue;
                }
                if (participant == null || terminal(participant.finalState)) {
                    ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, action,
                            resolved == null ? null : resolved.sourceScheduledStepId(),
                            "TERMINAL_PARTICIPANT_ACTION", "action targets a terminal or missing participant");
                    blockers.add(blocker);
                    if (participant != null) participant.blockers.add(blocker);
                    markHardStopSkips(workload, scenario, plannedPosition - 1, participantsById);
                    return report(options, attemptId, "UNEXPECTED_EXECUTION_FAILURE", workload, scenario,
                            "IN_MEMORY_FAULT_VECTOR", incompleteTrace(actualActions, deviationActionId,
                                    deviationPlannedPosition, action.deterministicId(), "TERMINAL_PARTICIPANT_ACTION"),
                            snapshot(faultSlots), contract.plannedActions(), actualActions, lifecycleEvents, participants, blockers);
                }
                if ("NOT_STARTED".equals(participant.finalState)) {
                    participant.finalState = "ACTIVE";
                }
                if (action.kind() == FaultScenarioActionKind.FORWARD) {
                    ForwardFaultSlot slot = resolved.faultSlot();
                    int assignedBit = scenario.assignedVector().charAt(slot.slotIndex()) - '0';
                    FaultVectorBoundaryContext boundaryContext = boundaryContext(
                            attemptId, workload, participant, slot, assignedBit);
                    try (FaultVectorProviderHolder.BoundaryScope boundary = FaultVectorProviderHolder.enterBoundary(boundaryContext)) {
                        if (assignedBit == 1) {
                            FaultVectorFault fault = FaultVectorProviderHolder.faultForCurrentBoundary().orElse(null);
                            if (!matches(fault, boundaryContext)) {
                                ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, action,
                                        slot.scheduledStepId(), "FAULT_PROVIDER_MISMATCH",
                                        "persisted assigned fault was not returned for its exact boundary");
                                participant.blockers.add(blocker);
                                blockers.add(blocker);
                                actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                        "FAULT_PROVIDER_MISMATCH", "NOT_RUN", "NOT_RUN", "ASSIGNED", List.of(),
                                        null, blocker.message()));
                                markHardStopSkips(workload, scenario, plannedPosition, participantsById);
                                return report(options, attemptId, "FAULT_PROVIDER_MISMATCH", workload, scenario,
                                        "IN_MEMORY_FAULT_VECTOR", incompleteTrace(actualActions, deviationActionId,
                                                deviationPlannedPosition, action.deterministicId(), "FAULT_PROVIDER_MISMATCH"),
                                        snapshot(faultSlots), contract.plannedActions(), actualActions, lifecycleEvents, participants, blockers);
                            }
                            triggerStates.put(slot.scheduledStepId(), "ASSIGNED_FAULT");
                            participant.functionality.abortBeforeStepForExecutor(slot.runtimeStepName(), participant.unitOfWork);
                            markRealizedAndMasked(faultSlots, slot);
                            participant.finalState = "ABORTED";
                            participant.skippedForwardActions.addAll(skippedForFailure(workload, scenario, slot, "ASSIGNED"));
                            actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                    "ASSIGNED_FAULT", "NOT_RUN", "NOT_RUN", "ASSIGNED", List.of(), null, null));
                            lifecycleEvents.add(event(lifecycleEvents, participant, "ABORTED", action.deterministicId(), "ASSIGNED_FAULT", null));
                            if (plannedCompensations.getOrDefault(participant.saga.deterministicId(), 0) == 0) {
                                participant.finalState = "COMPENSATED";
                                lifecycleEvents.add(event(lifecycleEvents, participant, "NO_COMPENSATION_WORK", action.deterministicId(), "SUCCEEDED", null));
                                lifecycleEvents.add(event(lifecycleEvents, participant, "COMPENSATED", action.deterministicId(), "SUCCEEDED", null));
                            }
                        } else {
                            WorkflowStepExecutionResult execution;
                            if (eventTriggerIds.contains(slot.scheduledStepId())) {
                                EventReplayCoordinator.TriggerCaptureScope capture =
                                        EventReplayCoordinator.beginTriggerCapture(slot.scheduledStepId());
                                try (capture) {
                                    execution = participant.functionality.executeStepForExecutorControlled(
                                            slot.runtimeStepName(), participant.unitOfWork);
                                }
                                capturedEventsByTrigger.put(slot.scheduledStepId(), capture.capturedEvents());
                            } else {
                                execution = participant.functionality.executeStepForExecutorControlled(
                                        slot.runtimeStepName(), participant.unitOfWork);
                            }
                            if (!execution.completed()) {
                                Throwable cause = unwrap(execution.failure());
                                triggerStates.put(slot.scheduledStepId(), "FAILED");
                                if (capturedMatchingEventAfterFailure(
                                        workload, slot.scheduledStepId(), capturedEventsByTrigger)) {
                                    triggerStates.put(slot.scheduledStepId(), "FAILED_AFTER_EVENT_EMISSION");
                                    participant.finalState = "HARD_STOPPED";
                                    actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                            "TRIGGER_FAILED_AFTER_EVENT_EMISSION", "FAILED", "NOT_RUN", null,
                                            List.of(), cause, null));
                                    ScenarioExecutionReport.Blocker blocker = blocker(
                                            workload, scenario, participant, action, slot.scheduledStepId(),
                                            "TRIGGER_FAILED_AFTER_EVENT_EMISSION", cause);
                                    participant.blockers.add(blocker);
                                    blockers.add(blocker);
                                    markHardStopSkips(workload, scenario, plannedPosition, participantsById);
                                    return report(options, attemptId, "UNEXPECTED_EXECUTION_FAILURE", workload, scenario,
                                            "IN_MEMORY_FAULT_VECTOR", incompleteTrace(actualActions, deviationActionId,
                                                    deviationPlannedPosition, action.deterministicId(),
                                                    "TRIGGER_FAILED_AFTER_EVENT_EMISSION"),
                                            snapshot(faultSlots), contract.plannedActions(), actualActions,
                                            lifecycleEvents, participants, blockers);
                                }
                                if (!isDomainFailure(cause)) {
                                    participant.finalState = "HARD_STOPPED";
                                    actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                            "INFRASTRUCTURE_FAILED", "FAILED", "NOT_RUN", null, List.of(), cause, null));
                                    ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, action,
                                            slot.scheduledStepId(), "FORWARD_INFRASTRUCTURE_FAILURE", cause);
                                    participant.blockers.add(blocker);
                                    blockers.add(blocker);
                                    markHardStopSkips(workload, scenario, plannedPosition, participantsById);
                                    return report(options, attemptId, "UNEXPECTED_EXECUTION_FAILURE", workload, scenario,
                                            "IN_MEMORY_FAULT_VECTOR", incompleteTrace(actualActions, deviationActionId,
                                                    deviationPlannedPosition, action.deterministicId(), "FORWARD_INFRASTRUCTURE_FAILURE"),
                                            snapshot(faultSlots), contract.plannedActions(), actualActions, lifecycleEvents, participants, blockers);
                                }
                                participant.finalState = "ABORTED";
                                participant.runtimeDeviation = true;
                                participant.skippedForwardActions.addAll(skippedForFailure(workload, scenario, slot, "UNASSIGNED_RUNTIME"));
                                markRuntimeFailureMasks(faultSlots, slot);
                                actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                        "FAILED", "FAILED", "NOT_RUN", "UNASSIGNED_RUNTIME", List.of(), cause, null));
                                lifecycleEvents.add(event(lifecycleEvents, participant, "ABORTED", action.deterministicId(), "FORWARD_FAILED", cause));
                                ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, action,
                                        slot.scheduledStepId(), "UNASSIGNED_RUNTIME_FORWARD_FAILURE", cause);
                                participant.blockers.add(blocker);
                                blockers.add(blocker);
                                if (deviationActionId == null) {
                                    deviationActionId = action.deterministicId();
                                    deviationPlannedPosition = plannedPosition;
                                }
                                FallbackResult fallback = recoverAfterRuntimeFailure(
                                        workload, scenario, participant, resolved, action.deterministicId(),
                                        actualActions, lifecycleEvents, blockers);
                                if (!fallback.completed()) {
                                    markHardStopSkips(workload, scenario, plannedPosition, participantsById);
                                    return report(options, attemptId, "COMPENSATION_FAILED", workload, scenario,
                                            "IN_MEMORY_FAULT_VECTOR", incompleteTrace(actualActions, deviationActionId,
                                                    deviationPlannedPosition, fallback.hardStopActionId(), fallback.hardStopReason()),
                                            snapshot(faultSlots), contract.plannedActions(), actualActions, lifecycleEvents, participants, blockers);
                                }
                                continue;
                            }
                            triggerStates.put(slot.scheduledStepId(), "SUCCEEDED");
                            if (Objects.equals(finalFaultSlotByParticipant.get(slot.sagaInstanceId()), slot.deterministicId())) {
                                WorkflowFinalizationResult finalization = participant.functionality.finalizeForExecutor(participant.unitOfWork);
                                if (finalization.committed()) {
                                    participant.finalState = "COMMITTED";
                                    actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                            "COMPLETED", "SUCCEEDED", "SUCCEEDED", null, List.of(), null, null));
                                    lifecycleEvents.add(event(lifecycleEvents, participant, "AUTOMATIC_COMMIT", action.deterministicId(), "SUCCEEDED", null));
                                } else {
                                    Throwable cause = unwrap(finalization.failure());
                                    triggerStates.put(slot.scheduledStepId(), "FAILED");
                                    if (capturedMatchingEventAfterFailure(
                                            workload, slot.scheduledStepId(), capturedEventsByTrigger)) {
                                        triggerStates.put(slot.scheduledStepId(), "FAILED_AFTER_EVENT_EMISSION");
                                        participant.finalState = "HARD_STOPPED";
                                        actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                                "TRIGGER_FAILED_AFTER_EVENT_EMISSION", "SUCCEEDED", "FAILED", null,
                                                List.of(), cause, null));
                                        ScenarioExecutionReport.Blocker blocker = blocker(
                                                workload, scenario, participant, action, slot.scheduledStepId(),
                                                "TRIGGER_FAILED_AFTER_EVENT_EMISSION", cause);
                                        participant.blockers.add(blocker);
                                        blockers.add(blocker);
                                        markHardStopSkips(workload, scenario, plannedPosition, participantsById);
                                        return report(options, attemptId, "UNEXPECTED_EXECUTION_FAILURE",
                                                workload, scenario, "IN_MEMORY_FAULT_VECTOR",
                                                incompleteTrace(actualActions, deviationActionId,
                                                        deviationPlannedPosition, action.deterministicId(),
                                                        "TRIGGER_FAILED_AFTER_EVENT_EMISSION"),
                                                snapshot(faultSlots), contract.plannedActions(), actualActions,
                                                lifecycleEvents, participants, blockers);
                                    }
                                    if (!isDomainFailure(cause)) {
                                        participant.finalState = "HARD_STOPPED";
                                        actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                                "COMMIT_INFRASTRUCTURE_FAILED", "SUCCEEDED", "FAILED", null, List.of(), cause, null));
                                        ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, action,
                                                slot.scheduledStepId(), "COMMIT_INFRASTRUCTURE_FAILURE", cause);
                                        participant.blockers.add(blocker);
                                        blockers.add(blocker);
                                        markHardStopSkips(workload, scenario, plannedPosition, participantsById);
                                        return report(options, attemptId, "UNEXPECTED_EXECUTION_FAILURE", workload, scenario,
                                                "IN_MEMORY_FAULT_VECTOR", incompleteTrace(actualActions, deviationActionId,
                                                        deviationPlannedPosition, action.deterministicId(), "COMMIT_INFRASTRUCTURE_FAILURE"),
                                                snapshot(faultSlots), contract.plannedActions(), actualActions, lifecycleEvents, participants, blockers);
                                    }
                                    participant.finalState = "ABORTED";
                                    participant.runtimeDeviation = true;
                                    actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                            "COMMIT_FAILED", "SUCCEEDED", "FAILED", "UNASSIGNED_RUNTIME", List.of(), cause, null));
                                    lifecycleEvents.add(event(lifecycleEvents, participant, "ABORTED", action.deterministicId(), "COMMIT_FAILED", cause));
                                    ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, action,
                                            slot.scheduledStepId(), "UNASSIGNED_RUNTIME_COMMIT_FAILURE", cause);
                                    participant.blockers.add(blocker);
                                    blockers.add(blocker);
                                    if (deviationActionId == null) {
                                        deviationActionId = action.deterministicId();
                                        deviationPlannedPosition = plannedPosition;
                                    }
                                    FallbackResult fallback = recoverAfterRuntimeFailure(
                                            workload, scenario, participant, resolved, action.deterministicId(),
                                            actualActions, lifecycleEvents, blockers);
                                    if (!fallback.completed()) {
                                        markHardStopSkips(workload, scenario, plannedPosition, participantsById);
                                        return report(options, attemptId, "COMPENSATION_FAILED", workload, scenario,
                                                "IN_MEMORY_FAULT_VECTOR", incompleteTrace(actualActions, deviationActionId,
                                                        deviationPlannedPosition, fallback.hardStopActionId(), fallback.hardStopReason()),
                                                snapshot(faultSlots), contract.plannedActions(), actualActions, lifecycleEvents, participants, blockers);
                                    }
                                }
                            } else {
                                actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                        "COMPLETED", "SUCCEEDED", "NOT_RUN", null, List.of(), null, null));
                            }
                        }
                    }
                } else {
                    try {
                        WorkflowStepRecoveryResult recovery = participant.functionality.recoverStepForExecutor(
                                resolved.runtimeStepName(), participant.unitOfWork);
                        List<ScenarioExecutionReport.RecoverySubOutcome> subOutcomes = recoverySubOutcomes(recovery);
                        actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                "COMPENSATED", "NOT_APPLICABLE", "NOT_APPLICABLE", null, subOutcomes, null, null));
                        participant.completedCompensations++;
                        if (participant.completedCompensations
                                == plannedCompensations.getOrDefault(participant.saga.deterministicId(), 0)) {
                            participant.finalState = "COMPENSATED";
                            lifecycleEvents.add(event(lifecycleEvents, participant, "COMPENSATED", action.deterministicId(), "SUCCEEDED", null));
                        }
                    } catch (Throwable failure) {
                        Throwable cause = recoveryFailureCause(failure);
                        String failedKind = recoveryFailureKind(failure, resolved.compensationEvidenceClass());
                        participant.finalState = "COMPENSATION_FAILED";
                        actualActions.add(outcome(resolved, plannedPosition, actualActions.size(),
                                "COMPENSATION_FAILED", "NOT_APPLICABLE", "NOT_APPLICABLE", null,
                                recoveryFailureSubOutcomes(failure, failedKind), cause, null));
                        ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, action,
                                resolved.sourceScheduledStepId(), "COMPENSATION_FAILED", cause);
                        participant.blockers.add(blocker);
                        blockers.add(blocker);
                        lifecycleEvents.add(event(lifecycleEvents, participant, "COMPENSATION_FAILED",
                                action.deterministicId(), "FAILED", cause));
                        markHardStopSkips(workload, scenario, plannedPosition, participantsById);
                        return report(options, attemptId, "COMPENSATION_FAILED", workload, scenario,
                                "IN_MEMORY_FAULT_VECTOR", incompleteTrace(actualActions, deviationActionId,
                                        deviationPlannedPosition, action.deterministicId(), failedKind + "_FAILED"),
                                snapshot(faultSlots), contract.plannedActions(), actualActions, lifecycleEvents, participants, blockers);
                    }
                }
            }
        }
        } catch (RuntimeException failure) {
            ScenarioExecutionReport.Blocker blocker = new ScenarioExecutionReport.Blocker(
                    workload.deterministicId(), scenario.deterministicId(), null, null,
                    activeAction == null ? null : activeAction.deterministicId(), null,
                    "EXECUTOR_INFRASTRUCTURE_FAILURE", failureDetails(unwrap(failure)));
            blockers.add(blocker);
            if (!actualActions.isEmpty()) {
                String activeActionId = activeAction == null ? null : activeAction.deterministicId();
                boolean activeActionRecorded = activeActionId != null && actualActions.stream()
                        .anyMatch(outcome -> Objects.equals(outcome.actionId(), activeActionId));
                markHardStopSkips(workload, scenario,
                        activeActionRecorded ? activePlannedPosition : activePlannedPosition - 1, participantsById);
            }
            return report(options, attemptId, "UNEXPECTED_EXECUTION_FAILURE", workload, scenario,
                    "IN_MEMORY_FAULT_VECTOR", incompleteTrace(actualActions, deviationActionId,
                            deviationPlannedPosition, activeAction == null ? null : activeAction.deterministicId(),
                            "EXECUTOR_INFRASTRUCTURE_FAILURE"), snapshot(faultSlots),
                    contract.plannedActions(), actualActions, lifecycleEvents, participants, blockers);
        }

        return report(options, attemptId, aggregateStatus(participants), workload, scenario,
                "IN_MEMORY_FAULT_VECTOR", completedTrace(deviationActionId, deviationPlannedPosition),
                snapshot(faultSlots), contract.plannedActions(), actualActions, lifecycleEvents, participants, blockers);
    }

    private EventActionResult executeEventConsequence(
            ResolvedAction action,
            int plannedPosition,
            int actualPosition,
            Map<String, String> triggerStates,
            Map<String, List<EventReplayCoordinator.CapturedEvent>> capturedEventsByTrigger,
            ScenarioRuntimeContext runtimeContext) {
        EventConsequence consequence = action.eventConsequence();
        if (consequence == null) {
            ScenarioExecutionReport.ActionOutcome outcome = eventOutcome(action, plannedPosition, actualPosition,
                    "EVENT_REPLAY_CONTROL_FAILED", null,
                    new IllegalStateException("missing persisted event consequence"));
            return EventActionResult.failed(outcome, "EVENT_REPLAY_CONTROL_FAILED",
                    "missing persisted event consequence");
        }
        String triggerState = triggerStates.get(consequence.triggerScheduledStepId());
        if ("ASSIGNED_FAULT".equals(triggerState)) {
            return EventActionResult.completed(eventOutcome(action, plannedPosition, actualPosition,
                    "MASKED_BY_TRIGGER_FAULT", null, null));
        }
        if ("FAILED".equals(triggerState)) {
            return EventActionResult.completed(eventOutcome(action, plannedPosition, actualPosition,
                    "MASKED_BY_TRIGGER_FAILURE", null, null));
        }
        if (!"SUCCEEDED".equals(triggerState)) {
            return EventActionResult.completed(eventOutcome(action, plannedPosition, actualPosition,
                    "MASKED_BY_TRIGGER_NOT_REACHED", null, null));
        }

        List<EventReplayCoordinator.CapturedEvent> matching = capturedEventsByTrigger
                .getOrDefault(consequence.triggerScheduledStepId(), List.of()).stream()
                .filter(event -> Objects.equals(event.eventTypeFqn(), consequence.eventTypeFqn()))
                .toList();
        if (matching.isEmpty()) {
            ScenarioExecutionReport.ActionOutcome outcome = eventOutcome(action, plannedPosition, actualPosition,
                    "EXPECTED_EVENT_NOT_EMITTED", null, null);
            return EventActionResult.failed(outcome, "EXPECTED_EVENT_NOT_EMITTED",
                    "successful trigger emitted no selected event type " + consequence.eventTypeFqn());
        }
        if (matching.size() != 1) {
            ScenarioExecutionReport.ActionOutcome outcome = eventOutcome(action, plannedPosition, actualPosition,
                    "MULTIPLE_MATCHING_EVENTS_UNSUPPORTED", null, null);
            return EventActionResult.failed(outcome, "MULTIPLE_MATCHING_EVENTS_UNSUPPORTED",
                    "successful trigger emitted " + matching.size() + " matching events");
        }

        EventReplayCoordinator.CapturedEvent captured = matching.get(0);
        EventReplayCoordinator.SelectedEventScope selection = null;
        boolean handlerInvoked = false;
        try {
            Class<?> handlingType = Class.forName(consequence.eventHandlingClassFqn());
            Object handlingBean = runtimeContext.bean(handlingType);
            Method method = handlingType.getMethod(consequence.eventHandlingMethodName());
            if (method.getParameterCount() != 0) {
                throw new EventReplayException("EVENT_REPLAY_CONTROL_FAILED",
                        "selected EventHandling method must have no arguments");
            }
            selection = EventReplayCoordinator.beginSelectedEvent(
                    captured, consequence.eventTypeFqn(), consequence.eventHandlerClassFqn());
            handlerInvoked = true;
            method.invoke(handlingBean);
            selection.verifyCompleted();
            ScenarioExecutionReport.EventRuntimeEvidence evidence = eventEvidence(
                    consequence, captured, selection.subscriberAggregateId());
            return EventActionResult.completed(eventOutcome(action, plannedPosition, actualPosition,
                    "COMPLETED", evidence, null));
        } catch (Throwable failure) {
            Throwable cause = unwrap(failure);
            String reason = cause instanceof EventReplayException replayFailure
                    ? replayFailure.reason()
                    : handlerInvoked ? "EVENT_CONSEQUENCE_FAILED" : "EVENT_REPLAY_CONTROL_FAILED";
            ScenarioExecutionReport.EventRuntimeEvidence evidence = eventEvidence(
                    consequence, captured, selection == null ? null : selection.subscriberAggregateId());
            ScenarioExecutionReport.ActionOutcome outcome = eventOutcome(action, plannedPosition, actualPosition,
                    reason, evidence, cause);
            return EventActionResult.failed(outcome, reason, failureDetails(cause));
        } finally {
            if (selection != null) selection.close();
        }
    }

    private boolean capturedMatchingEventAfterFailure(
            WorkloadPlan workload,
            String triggerScheduledStepId,
            Map<String, List<EventReplayCoordinator.CapturedEvent>> capturedEventsByTrigger) {
        Set<String> selectedEventTypes = workload.eventConsequences().stream()
                .filter(consequence -> Objects.equals(
                        consequence.triggerScheduledStepId(), triggerScheduledStepId))
                .map(EventConsequence::eventTypeFqn)
                .collect(java.util.stream.Collectors.toSet());
        if (selectedEventTypes.isEmpty()) {
            return false;
        }
        return capturedEventsByTrigger.getOrDefault(triggerScheduledStepId, List.of()).stream()
                .anyMatch(captured -> selectedEventTypes.contains(captured.eventTypeFqn()));
    }

    private ScenarioExecutionReport.EventRuntimeEvidence eventEvidence(
            EventConsequence consequence,
            EventReplayCoordinator.CapturedEvent captured,
            Integer subscriberAggregateId) {
        return new ScenarioExecutionReport.EventRuntimeEvidence(
                captured.eventId(), captured.eventTypeFqn(), captured.publisherAggregateId(),
                captured.publisherAggregateVersion(), captured.published(), subscriberAggregateId,
                consequence.eventHandlingClassFqn(), consequence.eventHandlingMethodName(),
                consequence.eventHandlerClassFqn());
    }

    private ScenarioExecutionReport.ActionOutcome eventOutcome(
            ResolvedAction action,
            Integer plannedPosition,
            int actualPosition,
            String status,
            ScenarioExecutionReport.EventRuntimeEvidence evidence,
            Throwable failure) {
        return new ScenarioExecutionReport.ActionOutcome(
                action.action().deterministicId(), action.action().kind().name(), action.action().sagaInstanceId(),
                null, null, action.action().sourceEventConsequenceId(), action.sourceScheduledStepId(),
                action.sourceStepId(), action.runtimeStepName(), null, action.action().occurrenceId(),
                plannedPosition, actualPosition, status,
                "COMPLETED".equals(status) ? "SUCCEEDED" : status.startsWith("MASKED_") ? "NOT_RUN" : "FAILED",
                "NOT_APPLICABLE", null, evidence, List.of(),
                failure == null ? null : failure.getClass().getName(),
                failure == null ? null : failure.getMessage());
    }

    private FallbackResult recoverAfterRuntimeFailure(WorkloadPlan workload,
                                                       FaultScenario scenario,
                                                       ParticipantState participant,
                                                       ResolvedAction failedAction,
                                                       String deviationActionId,
                                                       List<ScenarioExecutionReport.ActionOutcome> actualActions,
                                                       List<ScenarioExecutionReport.LifecycleEvent> lifecycleEvents,
                                                       List<ScenarioExecutionReport.Blocker> blockers) {
        List<WorkflowRecoveryCheckpoint> recoveryCheckpoints;
        try {
            recoveryCheckpoints = participant.functionality.recoveryCheckpointsForExecutor(participant.unitOfWork);
        } catch (Throwable failure) {
            Throwable cause = recoveryFailureCause(failure);
            String actionId = runtimeRecoveryActionId(participant, failedAction.runtimeStepName(),
                    failedAction.sourceScheduledStepId());
            actualActions.add(runtimeRecoveryOutcome(
                    actionId, participant, null, failedAction.sourceScheduledStepId(), failedAction.sourceStepId(),
                    failedAction.runtimeStepName(), failedAction.sourceScheduledStepId(), null, actualActions.size(),
                    "COMPENSATION_FAILED", List.of(new ScenarioExecutionReport.RecoverySubOutcome(
                            "RECOVERY_CHECKPOINT_DISCOVERY", "FAILED", cause.getClass().getName(), cause.getMessage())), cause));
            participant.finalState = "COMPENSATION_FAILED";
            ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, null,
                    failedAction.sourceScheduledStepId(), "COMPENSATION_FAILED", cause);
            participant.blockers.add(blocker);
            blockers.add(blocker);
            lifecycleEvents.add(event(lifecycleEvents, participant, "COMPENSATION_FAILED", actionId, "FAILED", cause));
            return new FallbackResult(false, actionId, "RECOVERY_CHECKPOINT_DISCOVERY_FAILED");
        }

        if (recoveryCheckpoints.isEmpty()) {
            participant.finalState = "COMPENSATED";
            lifecycleEvents.add(event(lifecycleEvents, participant, "NO_COMPENSATION_WORK", deviationActionId, "SUCCEEDED", null));
            lifecycleEvents.add(event(lifecycleEvents, participant, "COMPENSATED", deviationActionId, "SUCCEEDED", null));
            return FallbackResult.success();
        }

        Set<String> usedScheduledSteps = new HashSet<>();
        String finalRecoveryActionId = deviationActionId;
        for (WorkflowRecoveryCheckpoint checkpoint : recoveryCheckpoints) {
            RuntimeRecoveryReference reference = runtimeRecoveryReference(
                    workload, participant, failedAction, checkpoint.sourceStepName(), usedScheduledSteps);
            String actionId = runtimeRecoveryActionId(participant, checkpoint.sourceStepName(), reference.runtimeOccurrenceId());
            finalRecoveryActionId = actionId;
            try {
                WorkflowStepRecoveryResult recovery = participant.functionality.recoverStepForExecutor(
                        checkpoint.sourceStepName(), participant.unitOfWork);
                actualActions.add(runtimeRecoveryOutcome(
                        actionId, participant, reference.checkpointId(), reference.sourceScheduledStepId(),
                        reference.sourceStepId(), checkpoint.sourceStepName(), reference.runtimeOccurrenceId(),
                        reference.evidenceClass(), actualActions.size(), "COMPENSATED",
                        recoverySubOutcomes(recovery), null));
            } catch (Throwable failure) {
                Throwable cause = recoveryFailureCause(failure);
                String failedKind = recoveryFailureKind(failure,
                        checkpoint.explicitCompensationPending()
                                ? "EXPLICIT_COMPENSATION"
                                : checkpoint.implicitRollbackPending() ? "IMPLICIT_SAGA_ROLLBACK" : null);
                actualActions.add(runtimeRecoveryOutcome(
                        actionId, participant, reference.checkpointId(), reference.sourceScheduledStepId(),
                        reference.sourceStepId(), checkpoint.sourceStepName(), reference.runtimeOccurrenceId(),
                        reference.evidenceClass(), actualActions.size(), "COMPENSATION_FAILED",
                        recoveryFailureSubOutcomes(failure, failedKind), cause));
                participant.finalState = "COMPENSATION_FAILED";
                ScenarioExecutionReport.Blocker blocker = blocker(workload, scenario, participant, null,
                        reference.sourceScheduledStepId(), "COMPENSATION_FAILED", cause);
                participant.blockers.add(blocker);
                blockers.add(blocker);
                lifecycleEvents.add(event(lifecycleEvents, participant, "COMPENSATION_FAILED", actionId, "FAILED", cause));
                return new FallbackResult(false, actionId, failedKind + "_FAILED");
            }
        }
        participant.finalState = "COMPENSATED";
        lifecycleEvents.add(event(lifecycleEvents, participant, "COMPENSATED", finalRecoveryActionId, "SUCCEEDED", null));
        return FallbackResult.success();
    }

    private RuntimeRecoveryReference runtimeRecoveryReference(WorkloadPlan workload,
                                                               ParticipantState participant,
                                                               ResolvedAction failedAction,
                                                               String runtimeStepName,
                                                               Set<String> usedScheduledSteps) {
        ScheduledStep source = workload.forwardSchedule().stream()
                .filter(step -> Objects.equals(step.sagaInstanceId(), participant.saga.deterministicId()))
                .filter(step -> Objects.equals(step.runtimeStepName(), runtimeStepName))
                .filter(step -> step.scheduleOrder() <= failedAction.source().scheduleOrder())
                .filter(step -> !usedScheduledSteps.contains(step.deterministicId()))
                .max(java.util.Comparator.comparingInt(ScheduledStep::scheduleOrder))
                .orElse(null);
        if (source != null) {
            usedScheduledSteps.add(source.deterministicId());
        }
        CompensationCheckpoint checkpoint = source == null ? null : workload.compensationCheckpoints().stream()
                .filter(candidate -> Objects.equals(candidate.sagaInstanceId(), participant.saga.deterministicId()))
                .filter(candidate -> Objects.equals(candidate.sourceScheduledStepId(), source.deterministicId()))
                .findFirst()
                .orElse(null);
        String occurrenceId = checkpoint != null
                ? checkpoint.occurrenceId()
                : source != null ? source.deterministicId() : participant.saga.deterministicId() + ":" + runtimeStepName;
        return new RuntimeRecoveryReference(
                checkpoint == null ? null : checkpoint.deterministicId(),
                source == null ? null : source.deterministicId(),
                source == null ? null : source.stepId(),
                occurrenceId,
                checkpoint == null ? null : checkpoint.evidenceClass().name());
    }

    private ScenarioExecutionReport.ActionOutcome runtimeRecoveryOutcome(
            String actionId,
            ParticipantState participant,
            String checkpointId,
            String sourceScheduledStepId,
            String sourceStepId,
            String runtimeStepName,
            String runtimeOccurrenceId,
            String evidenceClass,
            int actualPosition,
            String status,
            List<ScenarioExecutionReport.RecoverySubOutcome> subOutcomes,
            Throwable failure) {
        return new ScenarioExecutionReport.ActionOutcome(
                actionId, "COMPENSATION", participant.saga.deterministicId(), null, checkpointId, null,
                sourceScheduledStepId, sourceStepId, runtimeStepName, evidenceClass, runtimeOccurrenceId,
                null, actualPosition, status, "NOT_APPLICABLE", "NOT_APPLICABLE", "UNASSIGNED_RUNTIME", null,
                subOutcomes, failure == null ? null : failure.getClass().getName(),
                failure == null ? null : failure.getMessage());
    }

    private String runtimeRecoveryActionId(ParticipantState participant, String runtimeStepName, String occurrenceId) {
        return "runtime-recovery:" + participant.saga.deterministicId() + ":" + runtimeStepName + ":" + occurrenceId;
    }

    private boolean isDomainFailure(Throwable failure) {
        return failure instanceof DomainFailure;
    }

    private Throwable recoveryFailureCause(Throwable failure) {
        if (failure instanceof WorkflowStepRecoveryException recoveryFailure && recoveryFailure.getCause() != null) {
            return unwrap(recoveryFailure.getCause());
        }
        return unwrap(failure);
    }

    private String recoveryFailureKind(Throwable failure, String fallbackKind) {
        if (failure instanceof WorkflowStepRecoveryException recoveryFailure) {
            return recoveryFailure.failedRecoveryKind();
        }
        return fallbackKind == null ? "COMPENSATION" : fallbackKind;
    }

    private List<ScenarioExecutionReport.RecoverySubOutcome> recoveryFailureSubOutcomes(Throwable failure,
                                                                                         String failedKind) {
        List<ScenarioExecutionReport.RecoverySubOutcome> outcomes = new ArrayList<>();
        if (failure instanceof WorkflowStepRecoveryException recoveryFailure) {
            outcomes.addAll(recoverySubOutcomes(recoveryFailure.completedRecovery()));
        }
        Throwable cause = recoveryFailureCause(failure);
        outcomes.add(new ScenarioExecutionReport.RecoverySubOutcome(
                failedKind, "FAILED", cause.getClass().getName(), cause.getMessage()));
        return List.copyOf(outcomes);
    }

    private TraceMetadata completedTrace(String deviationActionId, Integer deviationPlannedPosition) {
        if (deviationActionId == null) {
            return new TraceMetadata("EXACT", null, null, null, null, null);
        }
        return new TraceMetadata("DEVIATED", deviationActionId, deviationPlannedPosition,
                "IMMEDIATE_CHECKPOINT_RECOVERY_AND_CONTINUE", null, null);
    }

    private TraceMetadata incompleteTrace(List<ScenarioExecutionReport.ActionOutcome> actualActions,
                                          String deviationActionId,
                                          Integer deviationPlannedPosition,
                                          String hardStopActionId,
                                          String hardStopReason) {
        return new TraceMetadata(
                actualActions.isEmpty() ? null : "INCOMPLETE",
                deviationActionId,
                deviationPlannedPosition,
                deviationActionId == null ? null : "IMMEDIATE_CHECKPOINT_RECOVERY_AND_CONTINUE",
                hardStopActionId,
                hardStopReason);
    }

    private ResolvedContract resolveContract(WorkloadPlan workload, FaultScenario scenario) {
        Map<String, ScheduledStep> stepsById = new HashMap<>();
        workload.forwardSchedule().forEach(step -> stepsById.put(step.deterministicId(), step));
        Map<String, ForwardFaultSlot> slotsById = new HashMap<>();
        workload.faultSlots().forEach(slot -> slotsById.put(slot.deterministicId(), slot));
        Map<String, CompensationCheckpoint> checkpointsById = new HashMap<>();
        workload.compensationCheckpoints().forEach(checkpoint -> checkpointsById.put(checkpoint.deterministicId(), checkpoint));
        Map<String, EventConsequence> consequencesById = new HashMap<>();
        workload.eventConsequences().forEach(consequence -> consequencesById.put(consequence.deterministicId(), consequence));
        List<ScenarioExecutionReport.FaultSlot> faultSlots = workload.faultSlots().stream()
                .map(slot -> new ScenarioExecutionReport.FaultSlot(
                        slot.slotIndex(), slot.deterministicId(), slot.scheduledStepId(), slot.stepId(),
                        stepsById.get(slot.scheduledStepId()).scheduleOrder(), slot.sagaInstanceId(),
                        slot.runtimeStepName(), scenario.assignedVector().charAt(slot.slotIndex()) - '0',
                        scenario.assignedVector().charAt(slot.slotIndex()) == '1' ? "NOT_REACHED" : "NOT_ASSIGNED", null))
                .toList();
        List<ScenarioExecutionReport.PlannedAction> plannedActions = new ArrayList<>();
        Map<String, ResolvedAction> actionsById = new LinkedHashMap<>();
        for (int index = 0; index < scenario.actions().size(); index++) {
            FaultScenarioAction action = scenario.actions().get(index);
            ForwardFaultSlot slot = slotsById.get(action.sourceFaultSlotId());
            CompensationCheckpoint checkpoint = checkpointsById.get(action.sourceCompensationCheckpointId());
            EventConsequence consequence = consequencesById.get(action.sourceEventConsequenceId());
            String sourceId = slot != null ? slot.scheduledStepId()
                    : checkpoint != null ? checkpoint.sourceScheduledStepId()
                    : consequence == null ? null : consequence.triggerScheduledStepId();
            ScheduledStep source = stepsById.get(sourceId);
            if (source == null) {
                throw new IllegalArgumentException("FaultScenario action has no valid source occurrence "
                        + action.deterministicId());
            }
            ResolvedAction resolved = new ResolvedAction(
                    action, slot, checkpoint, consequence, source, source.deterministicId(), source.stepId(),
                    source.runtimeStepName(), checkpoint == null ? null : checkpoint.evidenceClass().name());
            actionsById.put(action.deterministicId(), resolved);
            plannedActions.add(new ScenarioExecutionReport.PlannedAction(
                    action.deterministicId(), action.kind().name(), action.sagaInstanceId(),
                    action.sourceFaultSlotId(), action.sourceCompensationCheckpointId(), action.sourceEventConsequenceId(),
                    source.deterministicId(), source.stepId(), source.runtimeStepName(),
                    resolved.compensationEvidenceClass(), consequence == null ? null : consequence.eventTypeFqn(),
                    consequence == null ? null : consequence.eventHandlingClassFqn(),
                    consequence == null ? null : consequence.eventHandlingMethodName(),
                    consequence == null ? null : consequence.eventHandlerClassFqn(),
                    consequence == null ? null : consequence.deliveryPolicy(), index));
        }
        return new ResolvedContract(faultSlots, List.copyOf(plannedActions), Map.copyOf(actionsById));
    }

    private List<ParticipantState> participantStates(WorkloadPlan workload) {
        Map<String, InputVariant> inputsById = new HashMap<>();
        workload.acceptedInputs().forEach(input -> inputsById.put(input.deterministicId(), input));
        return workload.participants().stream()
                .map(saga -> new ParticipantState(saga, inputsById.get(saga.inputVariantId())))
                .toList();
    }

    private InMemoryFaultVectorProvider provider(String attemptId,
                                                  WorkloadPlan workload,
                                                  FaultScenario scenario,
                                                  Map<String, ParticipantState> participants) {
        Map<Integer, FaultVectorFault> assignments = new LinkedHashMap<>();
        for (ForwardFaultSlot slot : workload.faultSlots()) {
            int assignedBit = scenario.assignedVector().charAt(slot.slotIndex()) - '0';
            if (assignedBit == 1) {
                ParticipantState participant = participants.get(slot.sagaInstanceId());
                assignments.put(slot.slotIndex(), FaultVectorFault.from(
                        boundaryContext(attemptId, workload, participant, slot, assignedBit)));
            }
        }
        return new InMemoryFaultVectorProvider(assignments);
    }

    private FaultVectorBoundaryContext boundaryContext(String attemptId,
                                                       WorkloadPlan workload,
                                                       ParticipantState participant,
                                                       ForwardFaultSlot slot,
                                                       int assignedBit) {
        return new FaultVectorBoundaryContext(
                attemptId,
                workload.deterministicId(),
                participant.saga.deterministicId(),
                slot.scheduledStepId(),
                slot.slotIndex(),
                participant.functionality.getClass().getName(),
                participant.functionality.getClass().getSimpleName(),
                slot.runtimeStepName(),
                assignedBit);
    }

    private boolean matches(FaultVectorFault fault, FaultVectorBoundaryContext context) {
        return fault != null
                && Objects.equals(fault.scenarioExecutionId(), context.scenarioExecutionId())
                && Objects.equals(fault.scenarioPlanId(), context.scenarioPlanId())
                && Objects.equals(fault.sagaInstanceId(), context.sagaInstanceId())
                && Objects.equals(fault.scheduledStepId(), context.scheduledStepId())
                && fault.slotIndex() == context.slotIndex()
                && Objects.equals(fault.runtimeStepName(), context.runtimeStepName())
                && fault.assignedBit() == 1;
    }

    private Map<String, MutableFaultSlot> mutableFaultSlots(List<ScenarioExecutionReport.FaultSlot> slots) {
        Map<String, MutableFaultSlot> mutable = new LinkedHashMap<>();
        slots.forEach(slot -> mutable.put(slot.faultSlotId(), new MutableFaultSlot(slot)));
        return mutable;
    }

    private void markRealizedAndMasked(Map<String, MutableFaultSlot> slots, ForwardFaultSlot realized) {
        MutableFaultSlot realizedState = slots.get(realized.deterministicId());
        realizedState.state = "REALIZED";
        for (MutableFaultSlot slot : slots.values()) {
            if (slot.slot.assignedBit() == 1
                    && slot.slot.slotIndex() > realized.slotIndex()
                    && Objects.equals(slot.slot.sagaInstanceId(), realized.sagaInstanceId())) {
                slot.state = "MASKED";
                slot.reason = "masked by earlier realized slot " + realized.slotIndex();
            }
        }
    }

    private void markRuntimeFailureMasks(Map<String, MutableFaultSlot> slots, ForwardFaultSlot failed) {
        for (MutableFaultSlot slot : slots.values()) {
            if (slot.slot.assignedBit() == 1
                    && slot.slot.slotIndex() > failed.slotIndex()
                    && Objects.equals(slot.slot.sagaInstanceId(), failed.sagaInstanceId())) {
                slot.state = "MASKED";
                slot.reason = "masked by unassigned runtime failure at slot " + failed.slotIndex();
            }
        }
    }

    private List<ScenarioExecutionReport.FaultSlot> snapshot(Map<String, MutableFaultSlot> slots) {
        return slots.values().stream().map(MutableFaultSlot::snapshot).toList();
    }

    private void markHardStopSkips(WorkloadPlan workload,
                                   FaultScenario scenario,
                                   int hardStopPlannedPosition,
                                   Map<String, ParticipantState> participantsById) {
        Map<String, ForwardFaultSlot> slotsById = new HashMap<>();
        workload.faultSlots().forEach(slot -> slotsById.put(slot.deterministicId(), slot));
        Map<String, ScheduledStep> stepsById = new HashMap<>();
        workload.forwardSchedule().forEach(step -> stepsById.put(step.deterministicId(), step));
        for (int index = hardStopPlannedPosition + 1; index < scenario.actions().size(); index++) {
            FaultScenarioAction action = scenario.actions().get(index);
            if (action.kind() != FaultScenarioActionKind.FORWARD) continue;
            ForwardFaultSlot slot = slotsById.get(action.sourceFaultSlotId());
            ParticipantState participant = slot == null ? null : participantsById.get(slot.sagaInstanceId());
            if (participant == null || participant.skippedForwardActions.stream()
                    .anyMatch(skipped -> Objects.equals(skipped.faultSlotId(), slot.deterministicId()))) {
                continue;
            }
            int bit = scenario.assignedVector().charAt(slot.slotIndex()) - '0';
            ScheduledStep step = stepsById.get(slot.scheduledStepId());
            participant.skippedForwardActions.add(new ScenarioExecutionReport.SkippedForwardAction(
                    slot.deterministicId(), slot.scheduledStepId(), slot.stepId(), step.scheduleOrder(),
                    slot.runtimeStepName(), bit, bit == 1 ? "NOT_REACHED" : "NOT_EXECUTED_HARD_STOP",
                    "scenario hard-stopped before this planned forward action"));
        }
    }

    private List<ScenarioExecutionReport.SkippedForwardAction> skippedForFailure(WorkloadPlan workload,
                                                                                 FaultScenario scenario,
                                                                                 ForwardFaultSlot failedSlot,
                                                                                 String failureOrigin) {
        Map<String, ScheduledStep> stepsById = new HashMap<>();
        workload.forwardSchedule().forEach(step -> stepsById.put(step.deterministicId(), step));
        List<ScenarioExecutionReport.SkippedForwardAction> skipped = new ArrayList<>();
        for (ForwardFaultSlot slot : workload.faultSlots()) {
            if (slot.slotIndex() > failedSlot.slotIndex()
                    && Objects.equals(slot.sagaInstanceId(), failedSlot.sagaInstanceId())) {
                int bit = scenario.assignedVector().charAt(slot.slotIndex()) - '0';
                skipped.add(new ScenarioExecutionReport.SkippedForwardAction(
                        slot.deterministicId(), slot.scheduledStepId(), slot.stepId(),
                        stepsById.get(slot.scheduledStepId()).scheduleOrder(), slot.runtimeStepName(), bit,
                        bit == 1 ? "MASKED" : "SKIPPED",
                        "participant aborted after " + failureOrigin + " failure"));
            }
        }
        return List.copyOf(skipped);
    }

    private ScenarioExecutionReport.ActionOutcome outcome(ResolvedAction action,
                                                          int plannedPosition,
                                                          int actualPosition,
                                                          String status,
                                                          String bodyOutcome,
                                                          String commitOutcome,
                                                          String faultOrigin,
                                                          List<ScenarioExecutionReport.RecoverySubOutcome> recoverySubOutcomes,
                                                          Throwable failure,
                                                          String explicitMessage) {
        return new ScenarioExecutionReport.ActionOutcome(
                action.action().deterministicId(), action.action().kind().name(), action.action().sagaInstanceId(),
                action.action().sourceFaultSlotId(), action.action().sourceCompensationCheckpointId(),
                action.action().sourceEventConsequenceId(), action.sourceScheduledStepId(), action.sourceStepId(),
                action.runtimeStepName(), action.compensationEvidenceClass(), action.action().occurrenceId(),
                plannedPosition, actualPosition, status, bodyOutcome, commitOutcome,
                faultOrigin, null, recoverySubOutcomes, failure == null ? null : failure.getClass().getName(),
                explicitMessage != null ? explicitMessage : failure == null ? null : failure.getMessage());
    }

    private List<ScenarioExecutionReport.RecoverySubOutcome> recoverySubOutcomes(WorkflowStepRecoveryResult recovery) {
        List<ScenarioExecutionReport.RecoverySubOutcome> outcomes = new ArrayList<>();
        if (recovery.explicitCompensationExecuted()) {
            outcomes.add(new ScenarioExecutionReport.RecoverySubOutcome("EXPLICIT_COMPENSATION", "SUCCEEDED"));
        }
        if (recovery.implicitRollbackExecuted()) {
            outcomes.add(new ScenarioExecutionReport.RecoverySubOutcome("IMPLICIT_SAGA_ROLLBACK", "SUCCEEDED"));
        }
        return List.copyOf(outcomes);
    }

    private ScenarioExecutionReport.LifecycleEvent event(List<ScenarioExecutionReport.LifecycleEvent> events,
                                                         ParticipantState participant,
                                                         String type,
                                                         String actionId,
                                                         String outcome,
                                                         Throwable failure) {
        return new ScenarioExecutionReport.LifecycleEvent(
                events.size(), participant.saga.deterministicId(), type, actionId, outcome,
                failure == null ? null : failure.getClass().getName(),
                failure == null ? null : failure.getMessage());
    }

    private ScenarioExecutionReport report(ScenarioExecutorOptions options,
                                           String attemptId,
                                           String terminalStatus,
                                           WorkloadPlan workload,
                                           FaultScenario scenario,
                                           String providerMode,
                                           TraceMetadata trace,
                                           List<ScenarioExecutionReport.FaultSlot> faultSlots,
                                           List<ScenarioExecutionReport.PlannedAction> plannedActions,
                                           List<ScenarioExecutionReport.ActionOutcome> actualActions,
                                           List<ScenarioExecutionReport.LifecycleEvent> lifecycleEvents,
                                           List<ParticipantState> participantStates,
                                           List<ScenarioExecutionReport.Blocker> blockers) {
        String packagePath = manifestPath(options.packagePath()).toString();
        ScenarioExecutionReport.RuntimeMetadata runtimeMetadata = new ScenarioExecutionReport.RuntimeMetadata(
                options.applicationBase(), options.applicationId(), options.springApplicationClass(), options.springProfiles(),
                options.mavenProfile(), packagePath, options.faultScenarioId(),
                "PERSISTED_FAULT_SCENARIO", options.dryRun());
        List<ScenarioExecutionReport.Participant> participants = participantStates.stream()
                .map(participant -> new ScenarioExecutionReport.Participant(
                        participant.saga.deterministicId(), participant.saga.sagaFqn(),
                        participant.input == null ? null : participant.input.deterministicId(),
                        participant.materializationState, participant.startupState, participant.finalState,
                        participant.skippedForwardActions, participant.blockers))
                .toList();
        List<ScenarioExecutionReport.ActionOutcome> completedActualActions = completeNotReachedEventOutcomes(
                providerMode, trace, plannedActions, actualActions);
        return new ScenarioExecutionReport(
                ScenarioExecutionReport.SCHEMA_VERSION, attemptId, terminalStatus, packagePath,
                workload == null ? null : workload.deterministicId(),
                scenario == null ? options.faultScenarioId() : scenario.deterministicId(),
                workload == null ? null : workload.kind().name(),
                scenario == null ? null : scenario.assignedVector(), providerMode, trace.scheduleConformance(),
                trace.deviationActionId(), trace.deviationPlannedPosition(), trace.deviationPolicy(),
                trace.hardStopActionId(), trace.hardStopReason(), runtimeMetadata,
                participantStates.isEmpty() ? null : participantStates.get(0).prerequisiteSetup,
                participantStates.isEmpty() ? null : participantStates.get(0).sourceSetup,
                faultSlots, plannedActions, completedActualActions, lifecycleEvents, participants, blockers);
    }

    private List<ScenarioExecutionReport.ActionOutcome> completeNotReachedEventOutcomes(
            String providerMode,
            TraceMetadata trace,
            List<ScenarioExecutionReport.PlannedAction> plannedActions,
            List<ScenarioExecutionReport.ActionOutcome> actualActions) {
        if (!"IN_MEMORY_FAULT_VECTOR".equals(providerMode) || trace.hardStopReason() == null) {
            return actualActions;
        }
        List<ScenarioExecutionReport.ActionOutcome> completed = new ArrayList<>(actualActions);
        Set<String> actualIds = actualActions.stream().map(ScenarioExecutionReport.ActionOutcome::actionId)
                .collect(java.util.stream.Collectors.toSet());
        for (ScenarioExecutionReport.PlannedAction planned : plannedActions) {
            if (!"EVENT_CONSEQUENCE".equals(planned.kind()) || actualIds.contains(planned.actionId())) continue;
            completed.add(new ScenarioExecutionReport.ActionOutcome(
                    planned.actionId(), planned.kind(), planned.sagaInstanceId(), null, null,
                    planned.sourceEventConsequenceId(), planned.sourceScheduledStepId(), planned.sourceStepId(),
                    planned.runtimeStepName(), null, planned.sourceEventConsequenceId(), planned.plannedPosition(),
                    completed.size(), "NOT_REACHED", "NOT_RUN", "NOT_APPLICABLE", null, null, List.of(),
                    null, "scenario hard-stopped before this event consequence"));
        }
        return List.copyOf(completed);
    }

    private String aggregateStatus(List<ParticipantState> participants) {
        boolean committed = participants.stream().anyMatch(participant -> "COMMITTED".equals(participant.finalState));
        boolean compensated = participants.stream().anyMatch(participant -> "COMPENSATED".equals(participant.finalState));
        if (committed && compensated) return "PARTIAL_COMPENSATED";
        if (compensated) return "COMPENSATED";
        return "SUCCESS";
    }

    private boolean terminal(String state) {
        return "COMMITTED".equals(state) || "COMPENSATED".equals(state) || "COMPENSATION_FAILED".equals(state);
    }

    private Object instantiate(Class<?> type, List<Object> arguments) throws ReflectiveOperationException {
        List<Constructor<?>> constructors = java.util.Arrays.stream(type.getConstructors())
                .sorted(Comparator.comparing(Constructor::toGenericString))
                .toList();
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() != arguments.size()
                    || nullTargetsPrimitive(constructor.getParameterTypes(), arguments)) {
                continue;
            }
            try {
                return constructor.newInstance(arguments.toArray());
            } catch (IllegalArgumentException incompatibleArguments) {
                // Reflection rejected this overload's invocation conversions; try the next overload.
            }
        }

        List<String> typedInvocationRejections = new ArrayList<>();
        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() != arguments.size()) {
                continue;
            }
            try {
                Object[] converted = exactIntegralInvocationArguments(
                        constructor.getParameterTypes(), arguments);
                return constructor.newInstance(converted);
            } catch (TypedInvocationRejection rejection) {
                typedInvocationRejections.add(constructor.toGenericString() + ": " + rejection.getMessage());
            } catch (IllegalArgumentException incompatibleArguments) {
                typedInvocationRejections.add(constructor.toGenericString()
                        + ": reflection rejected the exactly converted argument list");
            }
        }

        String argumentTypes = arguments.stream()
                .map(argument -> argument == null ? "null" : argument.getClass().getName())
                .collect(java.util.stream.Collectors.joining(", ", "[", "]"));
        String available = constructors.stream()
                .map(Constructor::toGenericString)
                .collect(java.util.stream.Collectors.joining("; ", "[", "]"));
        String rejections = typedInvocationRejections.isEmpty()
                ? ""
                : "; typed invocation rejections " + typedInvocationRejections;
        throw new NoSuchMethodException("No compatible constructor for " + type.getName()
                + " with persisted argument types " + argumentTypes
                + "; available public constructors " + available + rejections);
    }

    private Object[] exactIntegralInvocationArguments(Class<?>[] parameterTypes,
                                                      List<Object> arguments)
            throws TypedInvocationRejection {
        Object[] converted = new Object[arguments.size()];
        for (int index = 0; index < parameterTypes.length; index++) {
            Class<?> targetType = parameterTypes[index];
            Object argument = arguments.get(index);
            if (argument == null) {
                if (targetType.isPrimitive()) {
                    throw new TypedInvocationRejection("argument " + index
                            + " is null and cannot target primitive " + targetType.getTypeName());
                }
                converted[index] = null;
                continue;
            }
            if (reflectionAccepts(targetType, argument.getClass())) {
                converted[index] = argument;
                continue;
            }
            Class<?> integralTarget = integralTarget(targetType);
            if (!(argument instanceof Number number) || integralTarget == null) {
                throw new TypedInvocationRejection("argument " + index + " has persisted type "
                        + argument.getClass().getName() + " and cannot target " + targetType.getTypeName()
                        + ": unsupported coercion; only exact numeric conversion to byte, short, int, long,"
                        + " or BigInteger is supported");
            }
            converted[index] = exactIntegralValue(index, number, integralTarget);
        }
        return converted;
    }

    private Object exactIntegralValue(int argumentIndex,
                                      Number value,
                                      Class<?> targetType)
            throws TypedInvocationRejection {
        BigInteger integralValue;
        try {
            if (value instanceof BigInteger bigInteger) {
                integralValue = bigInteger;
            } else if (value instanceof BigDecimal bigDecimal) {
                integralValue = bigDecimal.toBigIntegerExact();
            } else if (value instanceof Byte || value instanceof Short
                    || value instanceof Integer || value instanceof Long) {
                integralValue = BigInteger.valueOf(value.longValue());
            } else if (value instanceof Float floating) {
                if (!Float.isFinite(floating)) {
                    throw new TypedInvocationRejection("argument " + argumentIndex + " numeric value " + value
                            + " (" + value.getClass().getName() + ") is non-finite and cannot target "
                            + targetType.getName());
                }
                integralValue = BigDecimal.valueOf(floating.doubleValue()).toBigIntegerExact();
            } else if (value instanceof Double floating) {
                if (!Double.isFinite(floating)) {
                    throw new TypedInvocationRejection("argument " + argumentIndex + " numeric value " + value
                            + " (" + value.getClass().getName() + ") is non-finite and cannot target "
                            + targetType.getName());
                }
                integralValue = BigDecimal.valueOf(floating).toBigIntegerExact();
            } else {
                throw new TypedInvocationRejection("argument " + argumentIndex + " has unsupported numeric type "
                        + value.getClass().getName() + " for exact conversion to " + targetType.getName());
            }
        } catch (ArithmeticException fractional) {
            throw new TypedInvocationRejection("argument " + argumentIndex + " numeric value " + value
                    + " (" + value.getClass().getName() + ") is fractional and cannot be converted exactly to "
                    + targetType.getName());
        }

        try {
            if (targetType == Byte.class) return integralValue.byteValueExact();
            if (targetType == Short.class) return integralValue.shortValueExact();
            if (targetType == Integer.class) return integralValue.intValueExact();
            if (targetType == Long.class) return integralValue.longValueExact();
            return integralValue;
        } catch (ArithmeticException overflow) {
            throw new TypedInvocationRejection("argument " + argumentIndex + " numeric value " + value
                    + " (" + value.getClass().getName() + ") is outside the range of " + targetType.getName());
        }
    }

    private Class<?> integralTarget(Class<?> type) {
        if (type == byte.class || type == Byte.class) return Byte.class;
        if (type == short.class || type == Short.class) return Short.class;
        if (type == int.class || type == Integer.class) return Integer.class;
        if (type == long.class || type == Long.class) return Long.class;
        if (type == BigInteger.class) return BigInteger.class;
        return null;
    }

    private boolean reflectionAccepts(Class<?> targetType, Class<?> argumentType) {
        if (!targetType.isPrimitive()) return targetType.isAssignableFrom(argumentType);
        if (targetType == boolean.class) return argumentType == Boolean.class;
        if (targetType == byte.class) return argumentType == Byte.class;
        if (targetType == short.class) return argumentType == Byte.class || argumentType == Short.class;
        if (targetType == char.class) return argumentType == Character.class;
        if (targetType == int.class) {
            return argumentType == Byte.class || argumentType == Short.class
                    || argumentType == Character.class || argumentType == Integer.class;
        }
        if (targetType == long.class) {
            return argumentType == Byte.class || argumentType == Short.class
                    || argumentType == Character.class || argumentType == Integer.class
                    || argumentType == Long.class;
        }
        if (targetType == float.class) {
            return argumentType == Byte.class || argumentType == Short.class
                    || argumentType == Character.class || argumentType == Integer.class
                    || argumentType == Long.class || argumentType == Float.class;
        }
        if (targetType == double.class) {
            return argumentType == Byte.class || argumentType == Short.class
                    || argumentType == Character.class || argumentType == Integer.class
                    || argumentType == Long.class || argumentType == Float.class
                    || argumentType == Double.class;
        }
        return false;
    }

    private boolean nullTargetsPrimitive(Class<?>[] parameterTypes, List<Object> arguments) {
        for (int index = 0; index < parameterTypes.length; index++) {
            if (arguments.get(index) == null && parameterTypes[index].isPrimitive()) {
                return true;
            }
        }
        return false;
    }

    private ScenarioExecutionReport.Blocker blocker(WorkloadPlan workload,
                                                     FaultScenario scenario,
                                                     ParticipantState participant,
                                                     FaultScenarioAction action,
                                                     String sourceScheduledStepId,
                                                     String reason,
                                                     Throwable failure) {
        return blocker(workload, scenario, participant, action, sourceScheduledStepId, reason, failureDetails(failure));
    }

    private ScenarioExecutionReport.Blocker blocker(WorkloadPlan workload,
                                                     FaultScenario scenario,
                                                     ParticipantState participant,
                                                     FaultScenarioAction action,
                                                     String sourceScheduledStepId,
                                                     String reason,
                                                     String message) {
        return new ScenarioExecutionReport.Blocker(
                workload == null ? null : workload.deterministicId(),
                scenario == null ? null : scenario.deterministicId(),
                participant == null || participant.input == null ? null : participant.input.deterministicId(),
                null,
                action == null ? null : action.deterministicId(),
                sourceScheduledStepId,
                reason,
                message);
    }

    private Throwable unwrap(Throwable failure) {
        if (failure == null) return null;
        Throwable current = failure;
        while (current.getCause() != null
                && (current instanceof InvocationTargetException || current instanceof CompletionException)) {
            current = current.getCause();
        }
        return current;
    }

    private String failureDetails(Throwable failure) {
        if (failure == null) return null;
        return failure.getClass().getName() + (failure.getMessage() == null ? "" : ": " + failure.getMessage());
    }

    private Path manifestPath(Path configured) {
        return Files.isDirectory(configured) ? configured.resolve("scenario-catalog-manifest.json") : configured;
    }

    private void rejectPackageOutputAlias(Path packagePath,
                                          Path outputPath,
                                          ScenarioCatalogPackageReader.PackageContents packageContents,
                                          String reportKind) {
        rejectPackageOutputAlias(packagePath, outputPath, List.of(
                packageContents.workloadCatalogPath(), packageContents.faultScenarioCatalogPath(),
                packageContents.accountingPath(), packageContents.rejectedInputsPath()), reportKind);
    }

    private void rejectPackageOutputAlias(Path packagePath,
                                          Path outputPath,
                                          ScenarioCatalogPackageReader.SelectedPackageContents packageContents,
                                          String reportKind) {
        rejectPackageOutputAlias(packagePath, outputPath, List.of(
                packageContents.workloadCatalogPath(), packageContents.faultScenarioCatalogPath(),
                packageContents.accountingPath(), packageContents.rejectedInputsPath()), reportKind);
    }

    private void rejectPackageOutputAlias(Path packagePath,
                                          Path outputPath,
                                          List<Path> linkedArtifacts,
                                          String reportKind) {
        if (outputPath == null) return;
        Path output = outputPath.toAbsolutePath().normalize();
        List<Path> packageInputs = new ArrayList<>();
        packageInputs.add(manifestPath(packagePath).toAbsolutePath().normalize());
        packageInputs.addAll(linkedArtifacts);
        for (Path packageInput : packageInputs) {
            Path normalizedInput = packageInput.toAbsolutePath().normalize();
            if (output.equals(normalizedInput) || sameFile(output, normalizedInput)) {
                throw new IllegalArgumentException(reportKind + " output path must not alias scenario package input "
                        + normalizedInput);
            }
        }
        if (isDynamicEnrichmentArtifact(output)) {
            throw new IllegalArgumentException(reportKind + " output path must not overwrite an existing "
                    + "recognized v3 dynamic-enrichment artifact " + output);
        }
    }

    private void rejectExecutionOutputAlias(Path executionOutputPath, Path impactOutputPath) {
        if (executionOutputPath == null || impactOutputPath == null) return;
        Path executionOutput = outputIdentity(executionOutputPath);
        Path impactOutput = outputIdentity(impactOutputPath);
        if (executionOutput.equals(impactOutput)) {
            throw new IllegalArgumentException("Scenario impact report output path must not alias scenario execution report "
                    + executionOutput);
        }
    }

    private Path outputIdentity(Path configuredOutput) {
        Path absolute = configuredOutput.toAbsolutePath().normalize();
        List<Path> missingParts = new ArrayList<>();
        Path existing = absolute;
        while (existing != null && !Files.exists(existing)) {
            missingParts.add(existing.getFileName());
            existing = existing.getParent();
        }
        if (existing == null) {
            return absolute;
        }
        try {
            Path identity = existing.toRealPath();
            for (int index = missingParts.size() - 1; index >= 0; index--) {
                identity = identity.resolve(missingParts.get(index));
            }
            return identity.normalize();
        } catch (IOException failure) {
            throw new IllegalArgumentException("Cannot safely resolve report output path " + absolute, failure);
        }
    }

    private boolean isDynamicEnrichmentArtifact(Path output) {
        if (!Files.isRegularFile(output)) return false;
        try {
            JsonNode artifact = mapper.readTree(output.toFile());
            if (artifact == null || !artifact.isObject()) return false;
            String schema = artifact.path("schemaVersion").asText(artifact.path("schema").asText(null));
            return WorkloadDynamicEvidenceRecord.SCHEMA_VERSION.equals(schema)
                    || EnrichedScenarioCatalogWriter.MANIFEST_SCHEMA.equals(schema)
                    || EnrichedScenarioCatalogWriter.JOIN_REPORT_SCHEMA.equals(schema);
        } catch (IOException ignored) {
            return false;
        }
    }

    private boolean sameFile(Path output, Path packageInput) {
        if (!Files.exists(output) || !Files.exists(packageInput)) return false;
        try {
            return Files.isSameFile(output, packageInput);
        } catch (IOException failure) {
            throw new IllegalArgumentException("Cannot safely validate scenario execution report output path " + output, failure);
        }
    }

    private ScenarioExecutionReport reportWriteFailure(ScenarioExecutionReport report, Throwable failure) {
        List<ScenarioExecutionReport.Blocker> blockers = new ArrayList<>(report.blockers());
        blockers.add(new ScenarioExecutionReport.Blocker(
                report.workloadPlanId(), report.faultScenarioId(), null, null, null, null,
                "REPORT_WRITE_FAILED", failureDetails(unwrap(failure))));
        return new ScenarioExecutionReport(
                report.schemaVersion(), report.executionAttemptId(), "REPORT_WRITE_FAILED",
                report.packageManifestPath(), report.workloadPlanId(), report.faultScenarioId(),
                report.scenarioKind(), report.assignedVector(), report.providerMode(),
                report.actualActions().isEmpty() ? null : "INCOMPLETE",
                report.deviationActionId(), report.deviationPlannedPosition(), report.deviationPolicy(),
                null, "REPORT_WRITE_FAILED", report.runtimeMetadata(), report.prerequisiteSetup(), report.sourceSetup(),
                report.faultSlots(), report.plannedActions(), report.actualActions(), report.lifecycleEvents(),
                report.participants(), blockers);
    }

    private void writeReport(ScenarioExecutorOptions options, ScenarioExecutionReport report) {
        if (options.outputPath() == null) return;
        try {
            write(options.outputPath(), report);
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to write scenario execution report", failure);
        }
    }

    private List<ScenarioImpactReport.InvariantViolationFinding> findings(ImpactV1Collector collector) {
        return collector == null ? List.of() : collector.findings();
    }

    private void writeImpactReport(ScenarioExecutorOptions options,
                                   ScenarioExecutionReport executionReport,
                                   List<ScenarioImpactReport.InvariantViolationFinding> findings) {
        if (options.impactOutputPath() == null) return;
        try {
            write(options.impactOutputPath(), ScenarioImpactReport.evaluate(executionReport, findings));
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to write scenario impact report", failure);
        }
    }

    private void writePreflightReport(ScenarioSetupPreflightOptions options,
                                      ScenarioSetupPreflightReport report) {
        if (options.outputPath() == null) return;
        try {
            write(options.outputPath(), report);
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to write scenario setup preflight report", failure);
        }
    }

    private void write(Path configuredOutput, Object report) throws IOException {
        Path output = configuredOutput.toAbsolutePath().normalize();
        Path parent = output.getParent();
        if (parent != null) Files.createDirectories(parent);
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report);
    }

    private ScenarioSetupPreflightReport.ParticipantResult preflightParticipant(
            WorkloadPlan workload,
            String workloadStatus,
            ParticipantState participant) {
        List<ScenarioExecutionReport.Blocker> blockers = participant.blockers;
        if (blockers.isEmpty() && !"STARTUP_READY".equals(participant.startupState)) {
            blockers = List.of(new ScenarioExecutionReport.Blocker(
                    workload.deterministicId(), null,
                    participant.input == null ? null : participant.input.deterministicId(), null, null, null,
                    "SETUP_NOT_COMPLETED",
                    "Saga startup was not attempted because workload setup stopped at " + workloadStatus));
        }
        return new ScenarioSetupPreflightReport.ParticipantResult(
                participant.saga.deterministicId(), participant.saga.sagaFqn(),
                participant.input == null ? null : participant.input.deterministicId(),
                "STARTUP_READY".equals(participant.startupState), participant.materializationState,
                participant.startupState, blockers);
    }

    private record PrerequisiteResult(
            boolean success,
            Map<String, Object> bindings,
            ScenarioExecutionReport.PrerequisiteSetup report,
            List<ScenarioExecutionReport.Blocker> blockers) {
        private static PrerequisiteResult success(Map<String, Object> bindings,
                                                  ScenarioExecutionReport.PrerequisiteSetup report) {
            return new PrerequisiteResult(true, bindings, report, List.of());
        }

        private static PrerequisiteResult failure(ScenarioExecutionReport.PrerequisiteSetup report,
                                                  List<ScenarioExecutionReport.Blocker> blockers) {
            return new PrerequisiteResult(false, Map.of(), report, blockers);
        }
    }

    private record SetupResult(
            String status,
            List<ParticipantState> participants,
            ScenarioExecutionReport.SourceSetup sourceSetup,
            List<ScenarioExecutionReport.Blocker> blockers) {
    }

    private record ResolvedContract(
            List<ScenarioExecutionReport.FaultSlot> faultSlots,
            List<ScenarioExecutionReport.PlannedAction> plannedActions,
            Map<String, ResolvedAction> actionsById) {
    }

    private record ResolvedAction(
            FaultScenarioAction action,
            ForwardFaultSlot faultSlot,
            CompensationCheckpoint checkpoint,
            EventConsequence eventConsequence,
            ScheduledStep source,
            String sourceScheduledStepId,
            String sourceStepId,
            String runtimeStepName,
            String compensationEvidenceClass) {
    }

    private record RuntimeRecoveryReference(
            String checkpointId,
            String sourceScheduledStepId,
            String sourceStepId,
            String runtimeOccurrenceId,
            String evidenceClass) {
    }

    private record EventActionResult(
            boolean completed,
            ScenarioExecutionReport.ActionOutcome outcome,
            String reason,
            String message) {
        private static EventActionResult completed(ScenarioExecutionReport.ActionOutcome outcome) {
            return new EventActionResult(true, outcome, null, null);
        }

        private static EventActionResult failed(ScenarioExecutionReport.ActionOutcome outcome,
                                                String reason,
                                                String message) {
            return new EventActionResult(false, outcome, reason, message);
        }
    }

    private record FallbackResult(boolean completed, String hardStopActionId, String hardStopReason) {
        private static FallbackResult success() {
            return new FallbackResult(true, null, null);
        }
    }

    private record TraceMetadata(
            String scheduleConformance,
            String deviationActionId,
            Integer deviationPlannedPosition,
            String deviationPolicy,
            String hardStopActionId,
            String hardStopReason) {
        private static TraceMetadata none() {
            return new TraceMetadata(null, null, null, null, null, null);
        }

        private static TraceMetadata hardStop(String reason) {
            return new TraceMetadata(null, null, null, null, null, reason);
        }
    }

    private static final class BaselineFailure extends RuntimeException {
        private final String reason;

        private BaselineFailure(String reason, String message) {
            super(message);
            this.reason = reason;
        }
    }

    private static final class TypedInvocationRejection extends Exception {
        private TypedInvocationRejection(String message) {
            super(message);
        }
    }

    private static final class ParticipantState {
        private final SagaInstance saga;
        private final InputVariant input;
        private String materializationState = "NOT_ATTEMPTED";
        private String startupState = "NOT_ATTEMPTED";
        private String finalState = "NOT_STARTED";
        private UnitOfWork unitOfWork;
        private List<Object> materializedArguments = List.of();
        private WorkflowFunctionality functionality;
        private int completedCompensations;
        private boolean runtimeDeviation;
        private ScenarioExecutionReport.PrerequisiteSetup prerequisiteSetup;
        private ScenarioExecutionReport.SourceSetup sourceSetup;
        private final List<ScenarioExecutionReport.SkippedForwardAction> skippedForwardActions = new ArrayList<>();
        private final List<ScenarioExecutionReport.Blocker> blockers = new ArrayList<>();

        private ParticipantState(SagaInstance saga, InputVariant input) {
            this.saga = saga;
            this.input = input;
        }
    }

    private static final class MutableFaultSlot {
        private final ScenarioExecutionReport.FaultSlot slot;
        private String state;
        private String reason;

        private MutableFaultSlot(ScenarioExecutionReport.FaultSlot slot) {
            this.slot = slot;
            this.state = slot.state();
            this.reason = slot.reason();
        }

        private ScenarioExecutionReport.FaultSlot snapshot() {
            return new ScenarioExecutionReport.FaultSlot(
                    slot.slotIndex(), slot.faultSlotId(), slot.scheduledStepId(), slot.stepId(), slot.scheduleOrder(),
                    slot.sagaInstanceId(), slot.runtimeStepName(), slot.assignedBit(), state, reason);
        }
    }
}
