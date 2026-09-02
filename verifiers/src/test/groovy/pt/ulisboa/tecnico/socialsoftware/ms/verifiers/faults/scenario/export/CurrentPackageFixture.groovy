package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.InputVariantNormalizer
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ScenarioModelAdapterResult
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*

import java.nio.file.Files
import java.nio.file.Path

/** Test-only construction boundary for current executable packages. */
final class CurrentPackageFixture {
    private CurrentPackageFixture() {}

    static Map write(List<WorkloadPlan> workloads,
                     List<FaultScenario> faults,
                     int cap = 20,
                     Set<String> materializable = workloads*.deterministicId() as Set<String>,
                     Path requestedDirectory = null) {
        Path directory = Files.createTempDirectory(Path.of('/private/tmp'), 'current-executable-package-')
        Files.createDirectories(directory)
        def originalWorkloads = new ArrayList<>(workloads)
        workloads = workloads.collect { normalizeInputReferences(it) }
        def normalizedMaterializable = originalWorkloads.withIndex().findAll { original, index ->
            materializable.contains(original.deterministicId())
        }.collect { original, index -> workloads[index].deterministicId() } as Set<String>
        def inputs = workloads.collectMany { it.acceptedInputs() }
                .unique { it.deterministicId() }
        def sagaDefinitions = workloads.collectMany { workload ->
            workload.participants().collect { participant ->
                def steps = workload.forwardSchedule()
                        .findAll { it.sagaInstanceId() == participant.deterministicId() }
                        .collect { scheduled ->
                            String local = scheduled.stepId().contains('::')
                                    ? scheduled.stepId().substring(scheduled.stepId().lastIndexOf('::') + 2)
                                    : scheduled.stepId()
                            boolean compensatable = workload.compensationCheckpoints()
                                    .any { it.sourceScheduledStepId() == scheduled.deterministicId() }
                            new StepDefinition(scheduled.deterministicId(), scheduled.stepId(),
                                    local.replaceFirst('#\\d+$', ''), scheduled.scheduleOrder(), [], [], [],
                                    compensatable, true, true,
                                    compensatable ? CompensationEvidenceClass.EXPLICIT_COMPENSATION : null,
                                    [], [])
                        }
                new SagaDefinition(participant.sagaFqn(), steps, [])
            }
        }.groupBy { it.sagaFqn() }.collect { saga, definitions ->
            def steps = definitions.collectMany { it.steps() }
                    .unique { it.stepKey() }
                    .sort { it.orderIndex() }
            new SagaDefinition(saga, steps, [])
        }
        def eventDefinitions = workloads.collectMany { workload ->
            workload.eventConsequences().collect { event ->
                def trigger = workload.forwardSchedule().find { it.deterministicId() == event.triggerScheduledStepId() }
                def participant = workload.participants().find { it.deterministicId() == trigger?.sagaInstanceId() }
                new EventConsequenceDefinition(participant?.sagaFqn(), trigger?.stepId(), event.emissionSite(),
                        event.eventHandlingClassFqn(), event.eventHandlingMethodName(), event.eventHandlerClassFqn(),
                        event.eventProcessingClassFqn(), event.eventProcessingMethodName(), event.facadeClassFqn(),
                        event.facadeMethodName(), event.downstreamSagaFqn(), event.deliveryPolicy(), event.diagnostics())
            }
        }
        eventDefinitions*.downstreamSagaFqn().findAll { it }.unique().each { downstream ->
            if (!sagaDefinitions.any { it.sagaFqn() == downstream }) sagaDefinitions << new SagaDefinition(downstream, [], [])
        }
        def model = new ScenarioModelAdapterResult(sagaDefinitions, inputs, eventDefinitions, [], [:], [])
        def config = new ScenarioGeneratorConfig()
        def workloadGeneration = new WorkloadGenerationResult(WorkloadPlan.SCHEMA_VERSION, config,
                workloads, [], [workloadsGenerated: workloads.size()], [])
        def materializability = workloads.collect {
            new WorkloadMaterializability(it.deterministicId(), normalizedMaterializable.contains(it.deterministicId()),
                    normalizedMaterializable.contains(it.deterministicId()) ? [] : ['fixture excluded'])
        }
        def generation = new EagerFaultScenarioGenerationResult(workloadGeneration, cap, faults,
                materializability, [])
        new ExecutableArtifactWriter().write(model, 'fixture', generation, directory)
        [directory: directory,
         manifest: directory.resolve('scenario-catalog-manifest.json'),
         workloadPath: directory.resolve(ExecutableArtifactWriter.DEFAULT_WORKLOAD_FILE),
         faultScenario: directory.resolve(ExecutableArtifactWriter.DEFAULT_FAULT_FILE),
         accounting: directory.resolve(StaticAnalysisArtifactWriter.DEFAULT_ACCOUNTING_FILE),
         requests: directory.resolve(ExecutableArtifactWriter.DEFAULT_REQUEST_FILE),
         rejected: directory.resolve(ExecutableArtifactWriter.DEFAULT_REQUEST_FILE),
         workloads: workloads]
    }

    private static WorkloadPlan normalizeInputReferences(WorkloadPlan workload) {
        def normalizedByOldId = workload.acceptedInputs().collectEntries { input ->
            [(input.deterministicId()): InputVariantNormalizer.normalizeForArtifact(input)]
        }
        def participants = workload.participants().collect { participant ->
            def normalized = normalizedByOldId[participant.inputVariantId()]
            new SagaInstance(participant.deterministicId(), participant.sagaFqn(),
                    normalized?.deterministicId() ?: participant.inputVariantId(), participant.warnings())
        }
        def setup = workload.setupPlan()
        if (setup != null) {
            def bindings = setup.participantBindings().collect { binding ->
                def normalized = normalizedByOldId[binding.inputVariantId()]
                new SetupParticipantBinding(normalized?.deterministicId() ?: binding.inputVariantId(),
                        binding.argumentIndex(), binding.expectedTypeFqn(), binding.value(), binding.blockers())
            }.unique { binding -> "${binding.inputVariantId()}#${binding.argumentIndex()}" }
            setup = new SetupPlan(setup.schemaVersion(), setup.actions(), bindings, setup.blockers())
        }
        def occurrences = [:].withDefault { 0 }
        def stepIds = [:]
        def schedule = workload.forwardSchedule().collect { scheduled ->
            String local = scheduled.stepId().contains('::')
                    ? scheduled.stepId().substring(scheduled.stepId().lastIndexOf('::') + 2)
                    : scheduled.stepId()
            local = local.replaceFirst('#\\d+$', '')
            String key = "${scheduled.sagaInstanceId()}\u0000${local}"
            local = "${local}#${occurrences[key]++}"
            String saga = participants.find { it.deterministicId() == scheduled.sagaInstanceId() }.sagaFqn()
            String exact = "${saga}::${local}"
            stepIds[scheduled.deterministicId()] = exact
            new ScheduledStep(scheduled.deterministicId(), scheduled.sagaInstanceId(), exact,
                    scheduled.scheduleOrder(), scheduled.runtimeStepName(), scheduled.warnings())
        }
        def slots = workload.faultSlots().collect { slot ->
            new ForwardFaultSlot(slot.deterministicId(), slot.slotIndex(), slot.scheduledStepId(),
                    slot.sagaInstanceId(), stepIds[slot.scheduledStepId()], slot.runtimeStepName(), slot.occurrenceId())
        }
        def checkpoints = workload.compensationCheckpoints().collect { checkpoint ->
            new CompensationCheckpoint(checkpoint.deterministicId(), checkpoint.checkpointIndex(),
                    checkpoint.sagaInstanceId(), checkpoint.sourceScheduledStepId(),
                    stepIds[checkpoint.sourceScheduledStepId()], checkpoint.runtimeStepName(),
                    checkpoint.occurrenceId(), checkpoint.evidenceClass(), checkpoint.forwardFootprints(),
                    checkpoint.compensationFootprints(), checkpoint.warnings())
        }
        new WorkloadPlan(workload.schemaVersion(), workload.deterministicId(), workload.kind(),
                workload.executionShape(), participants,
                (normalizedByOldId.values() as List).unique { it.deterministicId() },
                schedule, workload.eventConsequences(), workload.normalSchedule(),
                workload.prerequisiteBaseline(), setup, workload.conflictEvidence(), slots,
                checkpoints, workload.warnings())
    }
}
