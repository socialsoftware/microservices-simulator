package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.persistence.EntityManager
import org.springframework.core.env.Environment
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.test.util.ReflectionTestUtils
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventApplicationService
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventSubscription
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorDomainException
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorBoundaryContext
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorProviderHolder
import pt.ulisboa.tecnico.socialsoftware.ms.faults.InMemoryFaultVectorProvider
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceEvent
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceNoopRecorder
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventReplayCoordinator
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.IVersionService
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.export.EnrichedScenarioCatalogWriter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.WorkloadDynamicEvidenceRecord
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.FaultScenarioValidator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.RecoveryScheduleGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioIdGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export.ScenarioCatalogPackageReader
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

class ScenarioExecutorSpec extends Specification {
    private static final ObjectMapper MAPPER = new ObjectMapper()

    def setupSpec() {
        TraceManager.init('scenario-executor-spec')
        TraceManager.getInstance().startRootSpan()
    }

    def cleanupSpec() {
        TraceManager.getInstance().endRootSpan()
        TraceManager.getInstance().forceFlush()
    }

    def setup() {
        FixtureWorkflow.reset()
        FixtureEventHandling.reset()
        FaultVectorProviderHolder.clear()
    }

    def cleanup() {
        FaultVectorProviderHolder.clear()
    }

    def 'all-zero persisted scenario replays every action and commits each participant at its own final forward action'() {
        given:
        def workload = workload(['left', 'right'], [
                ['left', 'first'], ['right', 'first'], ['left', 'second'], ['right', 'second']
        ])
        def scenario = scenarios(workload, '0000')[0]
        def packageFixture = writePackage(workload, [scenario])
        def before = packageChecksums(packageFixture.directory)
        def output = packageFixture.directory.resolve('reports/execution-report.json')
        def service = new TrackingSagaUnitOfWorkService()

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, output, scenario.deterministicId()), runtime(service))

        then:
        report.schemaVersion() == 'microservices-simulator.scenario-execution-report.v5'
        report.executionAttemptId()
        report.workloadPlanId() == workload.deterministicId()
        report.faultScenarioId() == scenario.deterministicId()
        report.assignedVector() == '0000'
        report.scheduleConformance() == 'EXACT'
        report.terminalStatus() == 'SUCCESS'
        report.plannedActions()*.actionId() == scenario.actions()*.deterministicId()
        report.actualActions()*.actionId() == scenario.actions()*.deterministicId()
        report.actualActions()*.plannedPosition() == [0, 1, 2, 3]
        report.actualActions()*.actualPosition() == [0, 1, 2, 3]
        report.actualActions()*.bodyOutcome().unique() == ['SUCCEEDED']
        report.actualActions()*.commitOutcome() == ['NOT_RUN', 'NOT_RUN', 'SUCCEEDED', 'SUCCEEDED']
        report.participants()*.finalState() == ['COMMITTED', 'COMMITTED']
        report.lifecycleEvents()*.type() == ['AUTOMATIC_COMMIT', 'AUTOMATIC_COMMIT']
        report.lifecycleEvents()*.sagaInstanceId() == ['left', 'right']
        report.lifecycleEvents()*.actionId() == [scenario.actions()[2].deterministicId(), scenario.actions()[3].deterministicId()]
        report.faultSlots()*.state().unique() == ['NOT_ASSIGNED']
        FixtureWorkflow.BODIES == ['left:first', 'right:first', 'left:second', 'right:second']
        service.commitCounts == [left: 1, right: 1]
        !FaultVectorProviderHolder.active
        packageChecksums(packageFixture.directory) == before
        Files.isRegularFile(output)
        def json = MAPPER.readTree(output.toFile())
        json.path('schemaVersion').asText() == 'microservices-simulator.scenario-execution-report.v5'
        json.path('plannedActions').size() == 4
        json.path('actualActions').size() == 4
    }

    def 'ImpactV1 sidecar counts invariant signals in capture order and isolates sequential attempts'() {
        given:
        def workload = workload(['solo'], [['solo', 'first'], ['solo', 'second']])
        def scenario = scenarios(workload, '00')[0]
        def packageFixture = writePackage(workload, [scenario])
        def before = packageChecksums(packageFixture.directory)
        def executionOutput = packageFixture.directory.resolve('reports/execution-with-impact.json')
        def impactOutput = packageFixture.directory.resolve('reports/impact-v1.json')
        FixtureWorkflow.recordInvariantSignals('solo', 'first', 1)
        FixtureWorkflow.recordInvariantSignals('solo', 'second', 2)

        when:
        def report = new ScenarioExecutor().execute(
                options(packageFixture.manifest, executionOutput, scenario.deterministicId(), impactOutput),
                runtime(new TrackingSagaUnitOfWorkService()))
        def impact = MAPPER.readTree(impactOutput.toFile())

        then:
        report.terminalStatus() == 'SUCCESS'
        impact.path('schemaVersion').asText() == 'microservices-simulator.scenario-impact-report.v1'
        impact.path('impactModel').asText() == 'ImpactV1'
        impact.path('evaluationStatus').asText() == 'EVALUATED'
        impact.path('notEvaluatedReason').isNull()
        impact.path('executionAttemptId').asText() == report.executionAttemptId()
        impact.path('workloadPlanId').asText() == workload.deterministicId()
        impact.path('faultScenarioId').asText() == scenario.deterministicId()
        impact.path('invariantViolationCount').asInt() == 3
        impact.path('impactScore').asInt() == 3
        impact.path('findings')*.path('sequence')*.asLong() == [1L, 2L, 3L]
        impact.path('findings')*.path('aggregateType')*.asText().unique() == ['DummyAggregate']
        impact.path('findings')*.path('aggregateId')*.asText() == ['1', '1', '2']
        impact.path('findings')*.path('runtimeStepName')*.asText() == ['first', 'second', 'second']
        impact.path('findings')*.path('executionAttemptId')*.asText().unique() == [report.executionAttemptId()]
        impact.path('findings')*.path('workloadPlanId')*.asText().unique() == [workload.deterministicId()]
        packageChecksums(packageFixture.directory) == before

        when: 'a later attempt executes without configured invariant signals'
        FixtureWorkflow.reset()
        def secondImpactOutput = packageFixture.directory.resolve('reports/impact-v1-second.json')
        def secondReport = new ScenarioExecutor().execute(
                options(packageFixture.manifest, null, scenario.deterministicId(), secondImpactOutput),
                runtime(new TrackingSagaUnitOfWorkService()))
        def secondImpact = MAPPER.readTree(secondImpactOutput.toFile())

        then:
        secondReport.terminalStatus() == 'SUCCESS'
        secondImpact.path('executionAttemptId').asText() == secondReport.executionAttemptId()
        secondImpact.path('invariantViolationCount').asInt() == 0
        secondImpact.path('impactScore').asInt() == 0
        secondImpact.path('findings').isEmpty()
        packageChecksums(packageFixture.directory) == before
    }

    def 'ImpactV1 collector rejects missing mismatched and delayed prior-attempt signals'() {
        given:
        def collector = new ImpactV1Collector(new DynamicEvidenceNoopRecorder(), 'attempt-b', 'workload-b')
        def event = { String attemptId, String workloadId, String aggregateId ->
            DynamicEvidenceEvent.of('INVARIANT_VIOLATION', 'DummyFunctionalitySagas', 'invocation', 'step', 1L, [
                    executionAttemptId: attemptId,
                    workloadPlanId: workloadId,
                    aggregateType: 'DummyAggregate',
                    aggregateId: aggregateId,
                    sourceMethod: 'SagaUnitOfWorkService.registerChanged',
                    verificationMethod: 'Aggregate.verifyInvariants'
            ])
        }

        when:
        collector.record(event('attempt-a', 'workload-a', 'prior'))
        collector.record(event(null, null, 'missing'))
        collector.record(event('attempt-b', 'workload-other', 'other-workload'))
        collector.record(event('attempt-b', 'workload-b', 'first'))
        collector.record(event('attempt-b', 'workload-b', 'second'))

        then:
        collector.findings()*.sequence() == [1L, 2L]
        collector.findings()*.aggregateId() == ['first', 'second']
        collector.findings()*.executionAttemptId().unique() == ['attempt-b']
        collector.findings()*.workloadPlanId().unique() == ['workload-b']
    }

    def 'a rejected dummyapp-shaped aggregate write produces evaluated ImpactV1 score one'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        def impactOutput = packageFixture.directory.resolve('reports/rejected-write-impact.json')
        FixtureWorkflow.rejectInvariantOnWrite('solo', 'first')

        when:
        def report = new ScenarioExecutor().execute(
                options(packageFixture.manifest, null, scenario.deterministicId(), impactOutput),
                runtime(new TrackingSagaUnitOfWorkService()))
        def impact = MAPPER.readTree(impactOutput.toFile())

        then:
        report.terminalStatus() == 'COMPENSATED'
        report.scheduleConformance() == 'DEVIATED'
        report.actualActions()*.status() == ['FAILED']
        report.actualActions()*.faultOrigin() == ['UNASSIGNED_RUNTIME']
        impact.path('evaluationStatus').asText() == 'EVALUATED'
        impact.path('invariantViolationCount').asInt() == 1
        impact.path('impactScore').asInt() == 1
        impact.path('findings').size() == 1
        impact.path('findings')[0].path('aggregateType').asText() == 'DummyAggregate'
        impact.path('findings')[0].path('sourceMethod').asText() == 'SagaUnitOfWorkService.registerChanged'
        impact.path('findings')[0].path('verificationMethod').asText() == 'Aggregate.verifyInvariants'
        impact.path('findings')[0].path('executionAttemptId').asText() == report.executionAttemptId()
        impact.path('findings')[0].path('workloadPlanId').asText() == workload.deterministicId()
    }

    def 'assigned fault and successful no-work compensation have zero ImpactV1'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '1')[0]
        def packageFixture = writePackage(workload, [scenario])
        def impactOutput = packageFixture.directory.resolve('reports/safe-compensation-impact.json')

        when:
        def report = new ScenarioExecutor().execute(
                options(packageFixture.manifest, null, scenario.deterministicId(), impactOutput),
                runtime(new TrackingSagaUnitOfWorkService()))
        def impact = MAPPER.readTree(impactOutput.toFile())

        then:
        report.terminalStatus() == 'COMPENSATED'
        report.faultSlots()*.state() == ['REALIZED']
        report.lifecycleEvents()*.type() == ['ABORTED', 'NO_COMPENSATION_WORK', 'COMPENSATED']
        impact.path('evaluationStatus').asText() == 'EVALUATED'
        impact.path('invariantViolationCount').asInt() == 0
        impact.path('impactScore').asInt() == 0
        impact.path('findings').isEmpty()
    }

    def 'setup failure is not evaluated by ImpactV1'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']], 'solo')
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        def impactOutput = packageFixture.directory.resolve('reports/not-evaluated-impact.json')

        when:
        def report = new ScenarioExecutor().execute(
                options(packageFixture.manifest, null, scenario.deterministicId(), impactOutput),
                runtime(new TrackingSagaUnitOfWorkService()))
        def impact = MAPPER.readTree(impactOutput.toFile())

        then:
        report.terminalStatus() == 'MATERIALIZATION_FAILED'
        impact.path('evaluationStatus').asText() == 'NOT_EVALUATED'
        impact.path('notEvaluatedReason').asText() == 'MATERIALIZATION_FAILED'
        impact.path('invariantViolationCount').isNull()
        impact.path('impactScore').isNull()
        impact.path('findings').isEmpty()
    }

    def 'assigned pre-body fault follows persisted interleaving and advances each compensation action once'() {
        given:
        def workload = workload(['left', 'right'], [
                ['left', 'first'], ['left', 'second'], ['left', 'third'], ['right', 'first'], ['right', 'second']
        ])
        def scenario = scenarios(workload, '01100').find { candidate ->
            candidate.actions()*.kind() == [
                    FaultScenarioActionKind.FORWARD,
                    FaultScenarioActionKind.FORWARD,
                    FaultScenarioActionKind.FORWARD,
                    FaultScenarioActionKind.COMPENSATION,
                    FaultScenarioActionKind.FORWARD
            ]
        }
        assert scenario != null
        def packageFixture = writePackage(workload, [scenario])
        def output = packageFixture.directory.resolve('reports/assigned-interleaving.json')

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, output, scenario.deterministicId()), runtime(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'PARTIAL_COMPENSATED'
        report.scheduleConformance() == 'EXACT'
        report.actualActions()*.actionId() == scenario.actions()*.deterministicId()
        report.actualActions()*.status() == ['COMPLETED', 'ASSIGNED_FAULT', 'COMPLETED', 'COMPENSATED', 'COMPLETED']
        report.actualActions()[1].bodyOutcome() == 'NOT_RUN'
        report.actualActions()[1].commitOutcome() == 'NOT_RUN'
        report.actualActions()[1].faultOrigin() == 'ASSIGNED'
        report.actualActions()[3].compensationEvidenceClass() == 'EXPLICIT_COMPENSATION'
        report.actualActions()[3].recoverySubOutcomes()*.kind() == ['EXPLICIT_COMPENSATION']
        report.participants().find { it.sagaInstanceId() == 'left' }.finalState() == 'COMPENSATED'
        report.participants().find { it.sagaInstanceId() == 'right' }.finalState() == 'COMMITTED'
        report.participants().find { it.sagaInstanceId() == 'left' }.skippedForwardActions()*.runtimeStepName() == ['third']
        report.participants().find { it.sagaInstanceId() == 'left' }.skippedForwardActions()*.state() == ['MASKED']
        report.faultSlots()*.state() == ['NOT_ASSIGNED', 'REALIZED', 'MASKED', 'NOT_ASSIGNED', 'NOT_ASSIGNED']
        FixtureWorkflow.BODIES == ['left:first', 'right:first', 'right:second']
        FixtureWorkflow.COMPENSATIONS == ['left:first']
        FixtureWorkflow.UNIT_OF_WORKS.left.executedSteps == ['first']
        report.lifecycleEvents()*.type() == ['ABORTED', 'COMPENSATED', 'AUTOMATIC_COMMIT']
        MAPPER.readTree(output.toFile()).path('actualActions')*.path('status')*.asText() ==
                ['COMPLETED', 'ASSIGNED_FAULT', 'COMPLETED', 'COMPENSATED', 'COMPLETED']
        !FaultVectorProviderHolder.active
    }

    def 'branched saga executes one persisted body at a time and does not run a later assigned target early'() {
        given:
        def workload = workload(['solo'], [['solo', 'first'], ['solo', 'second'], ['solo', 'third']])
        def scenario = scenarios(workload, '001').find { candidate ->
            candidate.actions().take(3)*.kind() == [
                    FaultScenarioActionKind.FORWARD,
                    FaultScenarioActionKind.FORWARD,
                    FaultScenarioActionKind.FORWARD
            ]
        }
        assert scenario != null
        def packageFixture = writePackage(workload, [scenario])

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, null, scenario.deterministicId()), runtime(new TrackingSagaUnitOfWorkService()))

        then:
        report.scheduleConformance() == 'EXACT'
        report.plannedActions()*.actionId() == scenario.actions()*.deterministicId()
        report.actualActions()*.actionId() == scenario.actions()*.deterministicId()
        report.actualActions()*.plannedPosition() == (0..<scenario.actions().size()).toList()
        report.actualActions()*.actualPosition() == (0..<scenario.actions().size()).toList()
        report.actualActions().take(3)*.status() == ['COMPLETED', 'COMPLETED', 'ASSIGNED_FAULT']
        report.actualActions()[2].bodyOutcome() == 'NOT_RUN'
        FixtureWorkflow.BODIES == ['solo:first', 'solo:second']
        !FixtureWorkflow.BODIES.contains('solo:third')
        FixtureWorkflow.UNIT_OF_WORKS.solo.executedSteps == ['first', 'second']
    }

    def 'assigned fault at first and final forward action runs no body or commit and reports no compensation work'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '1')[0]
        def packageFixture = writePackage(workload, [scenario])
        def service = new TrackingSagaUnitOfWorkService()

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, null, scenario.deterministicId()), runtime(service))

        then:
        report.terminalStatus() == 'COMPENSATED'
        report.scheduleConformance() == 'EXACT'
        report.actualActions()*.status() == ['ASSIGNED_FAULT']
        report.actualActions()[0].bodyOutcome() == 'NOT_RUN'
        report.actualActions()[0].commitOutcome() == 'NOT_RUN'
        report.participants()[0].finalState() == 'COMPENSATED'
        report.lifecycleEvents()*.type() == ['ABORTED', 'NO_COMPENSATION_WORK', 'COMPENSATED']
        report.actualActions().findAll { it.kind() == 'COMPENSATION' }.isEmpty()
        FixtureWorkflow.BODIES.isEmpty()
        FixtureWorkflow.UNIT_OF_WORKS.solo.executedSteps.isEmpty()
        service.commitCounts.isEmpty()
    }

    def 'materialization gate fails before startup and measured actions for every participant'() {
        given:
        def workload = workload(['ready', 'blocked'], [['ready', 'first'], ['blocked', 'first']], 'blocked')
        def scenario = scenarios(workload, '00')[0]
        def packageFixture = writePackage(workload, [scenario])

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, null, scenario.deterministicId()), runtime(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'MATERIALIZATION_FAILED'
        report.scheduleConformance() == null
        report.hardStopReason() == 'MATERIALIZATION_FAILED'
        report.actualActions().isEmpty()
        report.participants().find { it.sagaInstanceId() == 'ready' }.materializationState() == 'MATERIALIZED'
        report.participants().find { it.sagaInstanceId() == 'blocked' }.materializationState() == 'MATERIALIZATION_FAILED'
        report.participants()*.startupState().unique() == ['NOT_ATTEMPTED']
        FixtureWorkflow.constructorCalls == 0
        FixtureWorkflow.BODIES.isEmpty()
    }

    def 'startup gate fails before any measured action after all participants materialize'() {
        given:
        def workload = workload(['ready', 'broken'], [['ready', 'first'], ['broken', 'first']], null, 'broken')
        def scenario = scenarios(workload, '00')[0]
        def packageFixture = writePackage(workload, [scenario])

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, null, scenario.deterministicId()), runtime(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'STARTUP_FAILED'
        report.scheduleConformance() == null
        report.hardStopReason() == 'STARTUP_FAILED'
        report.actualActions().isEmpty()
        report.participants()*.materializationState().unique() == ['MATERIALIZED']
        report.participants().find { it.sagaInstanceId() == 'ready' }.startupState() == 'STARTUP_READY'
        report.participants().find { it.sagaInstanceId() == 'broken' }.startupState() == 'STARTUP_FAILED'
        FixtureWorkflow.BODIES.isEmpty()
    }

    def 'setup preflight and normal execution share exact successful setup while preflight runs zero workflow actions'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        def before = packageChecksums(packageFixture.directory)
        def output = packageFixture.directory.resolve('reports/setup-preflight.json')
        def preflightService = new TrackingSagaUnitOfWorkService()
        def preflightContext = new TrackingRuntimeContext(preflightService)

        when:
        def preflight = new ScenarioExecutor().preflight(
                new ScenarioSetupPreflightOptions(packageFixture.manifest, output), preflightContext)

        then:
        preflight.schemaVersion() == 'microservices-simulator.scenario-setup-preflight-report.v1'
        preflight.terminalStatus() == 'SUCCESS'
        preflight.candidateSelection() == 'MANIFEST_DECLARED_MATERIALIZABLE'
        preflight.candidateCount() == 1
        preflight.participantCount() == 1
        preflight.workloads()*.workloadPlanId() == [workload.deterministicId()]
        preflight.workloads()[0].status() == 'SETUP_READY'
        preflight.workloads()[0].participants()[0].setupReady()
        preflight.workloads()[0].participants()[0].materializationState() == 'MATERIALIZED'
        preflight.workloads()[0].participants()[0].startupState() == 'STARTUP_READY'
        preflight.workloads()[0].blockers().isEmpty()
        preflightContext.unitOfWorkCreations == 1
        preflightContext.beanRequests == [SagaUnitOfWorkService]
        FixtureWorkflow.constructorCalls == 1
        FixtureWorkflow.BODIES.isEmpty()
        FixtureWorkflow.COMPENSATIONS.isEmpty()
        preflightService.commitCounts.isEmpty()
        packageChecksums(packageFixture.directory) == before
        Files.isRegularFile(output)
        MAPPER.readTree(output.toFile()).path('workloads').first().path('status').asText() == 'SETUP_READY'

        when:
        def preflightParticipant = preflight.workloads()[0].participants()[0]
        FixtureWorkflow.reset()
        def executionContext = new TrackingRuntimeContext(new TrackingSagaUnitOfWorkService())
        def execution = new ScenarioExecutor().execute(
                options(packageFixture.manifest, null, scenario.deterministicId()), executionContext)

        then:
        execution.terminalStatus() == 'SUCCESS'
        execution.participants()[0].materializationState() == preflightParticipant.materializationState()
        execution.participants()[0].startupState() == preflightParticipant.startupState()
        executionContext.unitOfWorkCreations == preflightContext.unitOfWorkCreations
        FixtureWorkflow.constructorCalls == 1
        FixtureWorkflow.BODIES == ['solo:first']
    }

    def 'source setup executes twelve actions once in order and reuses one retained Tournament for both participants'() {
        given:
        def workload = sourceSetupWorkload()
        def scenario = scenarios(workload, '00')[0]
        def packageFixture = writePackage(workload, [scenario])
        def before = packageChecksums(packageFixture.directory)
        def service = new TrackingSagaUnitOfWorkService()
        def dispatcher = new FixtureSourceSetupDispatcher(1000, service.fixtureEventService)
        def runtime = new TrackingRuntimeContext(service, [:], [], [dispatcher])
        def gate = activateEventReplay()

        when:
        def report
        try {
            report = new ScenarioExecutor().preflightIsolatedAttempt(
                    new ScenarioSetupPreflightOptions(packageFixture.manifest, null), runtime,
                    [workload.deterministicId()] as Set<String>)
        } finally {
            gate.close()
            System.clearProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY)
        }

        then:
        report.terminalStatus() == 'SUCCESS'
        report.candidateCount() == 1
        report.workloads()[0].status() == 'SETUP_READY'
        def setup = report.workloads()[0].sourceSetup()
        setup.status() == 'SUCCEEDED'
        setup.actions()*.actionId() == (1..12).collect { "setup-action-${it}".toString() }
        setup.actions()*.orderIndex() == (0..11).toList()
        setup.actions()*.status().unique() == ['SUCCEEDED']
        dispatcher.invokedMethodKeys == workload.setupPlan().actions()*.methodKey()
        dispatcher.invocationCount == 12
        dispatcher.activationEffects == 2
        dispatcher.enrollmentEffects == 2
        dispatcher.tournamentCreations == 1
        dispatcher.faultProviderActive.every { !it }
        dispatcher.faultBoundaries.every { it == null }
        setup.pendingEventsCleared() == 12
        setup.emptyPendingEventBaseline()
        service.fixtureEventService.eventCountForReplay() == 0
        def tournamentAction = setup.actions().find { it.actionId() == 'setup-action-12' }
        tournamentAction.retainedResultId()
        tournamentAction.aggregateId() == '1007'
        def tournamentBindings = setup.participantBindings().findAll {
            it.sourceActionId() == 'setup-action-12' && it.propertyName() == 'aggregateId'
        }
        tournamentBindings.size() == 2
        tournamentBindings*.retainedResultId().unique() == [tournamentAction.retainedResultId()]
        tournamentBindings*.resolvedValue().unique() == ['1007']
        FixtureWorkflow.CONSTRUCTOR_PARTICIPANTS == [1007, 1007]
        FixtureWorkflow.constructorCalls == 2
        FixtureWorkflow.BODIES.isEmpty()
        packageChecksums(packageFixture.directory) == before
    }

    def 'in-process batch preflight rejects source setup before cross-candidate persistent state can be mutated'() {
        given:
        def first = sourceSetupWorkload()
        def secondActions = new ArrayList<>(first.setupPlan().actions())
        def changed = secondActions[0]
        secondActions[0] = new SetupAction(changed.actionId(), changed.orderIndex(),
                changed.sourceOccurrence() + ':second-candidate', changed.methodKey(), changed.arguments(),
                changed.declaredResultTypeFqn(), changed.voidResult(), changed.blockers())
        def second = reidentifyWorkload(first, first.acceptedInputs(), null,
                new SetupPlan(SetupPlan.SCHEMA_VERSION, secondActions,
                        first.setupPlan().participantBindings(), []))
        assert first.deterministicId() != second.deterministicId()
        def firstScenario = scenarios(first, '00')[0]
        def secondScenario = scenarios(second, '00')[0]
        def packageFixture = writePackage([first, second], [firstScenario, secondScenario],
                [first.deterministicId(), second.deterministicId()] as Set<String>)
        def service = new TrackingSagaUnitOfWorkService()
        def dispatcher = new FixtureSourceSetupDispatcher(1500, service.fixtureEventService)
        def runtime = new TrackingRuntimeContext(service, [:], [], [dispatcher])

        when:
        new ScenarioExecutor().preflight(
                new ScenarioSetupPreflightOptions(packageFixture.manifest, null), runtime)

        then:
        def error = thrown(IllegalStateException)
        error.message.contains('one fresh process per workload')
        dispatcher.invocationCount == 0
        dispatcher.tournamentCreations == 0
        runtime.unitOfWorkCreations == 0
        FixtureWorkflow.constructorCalls == 0
    }

    def 'fresh source-setup attempts receive fresh IDs and setup finishes before target fault injection'() {
        given:
        def workload = sourceSetupWorkload()
        def scenario = scenarios(workload, '10')[0]
        def packageFixture = writePackage(workload, [scenario])
        def before = packageChecksums(packageFixture.directory)

        when:
        def first = executeWithSourceSetup(packageFixture, scenario, 2000)
        def firstId = first.sourceSetup().actions().find { it.actionId() == 'setup-action-12' }.aggregateId()
        FixtureWorkflow.reset()
        def second = executeWithSourceSetup(packageFixture, scenario, 3000)
        def secondId = second.sourceSetup().actions().find { it.actionId() == 'setup-action-12' }.aggregateId()

        then:
        first.sourceSetup().status() == 'SUCCEEDED'
        second.sourceSetup().status() == 'SUCCEEDED'
        first.sourceSetup().actions()*.status().unique() == ['SUCCEEDED']
        second.sourceSetup().actions()*.status().unique() == ['SUCCEEDED']
        firstId == '2007'
        secondId == '3007'
        firstId != secondId
        first.providerMode() == 'IN_MEMORY_FAULT_VECTOR'
        second.providerMode() == 'IN_MEMORY_FAULT_VECTOR'
        first.faultSlots()[0].state() == 'REALIZED'
        second.faultSlots()[0].state() == 'REALIZED'
        packageChecksums(packageFixture.directory) == before
    }

    def 'unauthorized setup method null result wrong result type and cleanup failure stop before target startup'() {
        given:
        def workload = sourceSetupWorkload()
        def scenario = scenarios(workload, '00')[0]
        def packageFixture = writePackage(workload, [scenario])
        def impact = packageFixture.directory.resolve("reports/setup-${mode}-impact.json".toString())
        def service = new TrackingSagaUnitOfWorkService()
        if (mode == 'CLEANUP') service.fixtureEventService.retainEventsOnClear = true
        def dispatcher = new FixtureSourceSetupDispatcher(4000, service.fixtureEventService, mode)
        def runtime = new TrackingRuntimeContext(service, [:], [], [dispatcher])
        def gate = activateEventReplay()

        when:
        def report
        try {
            report = new ScenarioExecutor().execute(
                    options(packageFixture.manifest, null, scenario.deterministicId(), impact), runtime)
        } finally {
            gate.close()
            System.clearProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY)
        }

        then:
        report.terminalStatus() == expectedStatus
        report.sourceSetup().status() == 'FAILED'
        report.sourceSetup().failureReason() == expectedStatus
        report.actualActions().isEmpty()
        report.participants()*.startupState().unique() == ['NOT_ATTEMPTED']
        FixtureWorkflow.constructorCalls == 0
        FixtureWorkflow.BODIES.isEmpty()
        MAPPER.readTree(impact.toFile()).path('evaluationStatus').asText() == 'NOT_EVALUATED'
        MAPPER.readTree(impact.toFile()).path('notEvaluatedReason').asText() == expectedStatus
        if (mode == 'UNAUTHORIZED') {
            assert dispatcher.invocationCount == 0
        }

        where:
        mode           || expectedStatus
        'UNAUTHORIZED' || 'SETUP_METHOD_NOT_AUTHORIZED'
        'NULL_RESULT'  || 'SETUP_NULL_RESULT'
        'WRONG_TYPE'   || 'SETUP_RESULT_TYPE_MISMATCH'
        'CLEANUP'      || 'SETUP_PENDING_EVENT_BASELINE_NOT_EMPTY'
    }

    def 'invalid setup property type order and mixed provider configuration are rejected before any setup or target invocation'() {
        given:
        def validWorkload = sourceSetupWorkload()
        def workload = malformedSourceSetupWorkload(validWorkload, mutation)
        def validScenario = scenarios(validWorkload, '00')[0]
        def withoutId = new FaultScenario(FaultScenario.SCHEMA_VERSION, null, workload.deterministicId(),
                validScenario.assignedVector(), validScenario.actions())
        def scenario = new FaultScenario(withoutId.schemaVersion(), ScenarioIdGenerator.faultScenarioId(withoutId),
                withoutId.workloadPlanId(), withoutId.assignedVector(), withoutId.actions())
        def packageFixture = writePackage(workload, [scenario])
        def service = new TrackingSagaUnitOfWorkService()
        def dispatcher = new FixtureSourceSetupDispatcher(5000, service.fixtureEventService)
        def runtime = new TrackingRuntimeContext(service, [:], [], [dispatcher])

        when:
        new ScenarioExecutor().execute(
                options(packageFixture.manifest, null, scenario.deterministicId()), runtime)

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains(expectedDiagnostic)
        dispatcher.invocationCount == 0
        FixtureWorkflow.constructorCalls == 0
        FixtureWorkflow.BODIES.isEmpty()

        where:
        mutation             || expectedDiagnostic
        'PROPERTY'           || 'UNSUPPORTED_SETUP_RESULT_PROPERTY'
        'TYPE'               || 'INCOMPATIBLE_SETUP_RESULT_REFERENCE'
        'ORDER'              || 'INVALID_SETUP_ACTION_ORDER'
        'MIXED'              || 'MIXED_PREREQUISITE_AND_SETUP'
        'COLLECTION_ELEMENT' || 'INCOMPATIBLE_SETUP_LITERAL'
    }

    def 'setup preflight preserves reflection unboxing and primitive widening'() {
        given:
        WideningArgumentWorkflow.received = null
        def workload = workload(['widening'], [['widening', 'first']], null, 'widening', null,
                WideningArgumentWorkflow.name, byteConstructorRecipe('7'))
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])

        when:
        def report = new ScenarioExecutor().preflight(
                new ScenarioSetupPreflightOptions(packageFixture.manifest, null),
                new TrackingRuntimeContext(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'SUCCESS'
        report.workloads()[0].status() == 'SETUP_READY'
        WideningArgumentWorkflow.received == 7L
        FixtureWorkflow.BODIES.isEmpty()
    }

    def 'setup preflight rejects null for primitive overload and continues to reference overload'() {
        given:
        NullOverloadWorkflow.selected = null
        def workload = workload(['nullable'], [['nullable', 'first']], null, 'nullable', null,
                NullOverloadWorkflow.name, literalRecipe(null))
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])

        when:
        def report = new ScenarioExecutor().preflight(
                new ScenarioSetupPreflightOptions(packageFixture.manifest, null),
                new TrackingRuntimeContext(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'SUCCESS'
        report.workloads()[0].status() == 'SETUP_READY'
        NullOverloadWorkflow.selected == 'reference'
        FixtureWorkflow.BODIES.isEmpty()
    }

    def 'setup preflight continues overload search after reflection rejects an argument list'() {
        given:
        OverloadSearchWorkflow.selected = null
        def workload = workload(['overload'], [['overload', 'first']], null, 'overload', null,
                OverloadSearchWorkflow.name, literalRecipe(7))
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])

        when:
        def report = new ScenarioExecutor().preflight(
                new ScenarioSetupPreflightOptions(packageFixture.manifest, null),
                new TrackingRuntimeContext(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'SUCCESS'
        report.workloads()[0].status() == 'SETUP_READY'
        OverloadSearchWorkflow.selected == 'number'
    }

    def 'setup preflight restores persisted integral value to exact #targetLabel constructor type'() {
        given:
        IntegralArgumentCapture.received = null
        def workload = workload(['typed'], [['typed', 'first']], null, 'typed', null,
                workflowClass.name, literalRecipe(persistedValue))
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])

        when:
        def report = new ScenarioExecutor().preflight(
                new ScenarioSetupPreflightOptions(packageFixture.manifest, null),
                new TrackingRuntimeContext(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'SUCCESS'
        report.workloads()[0].status() == 'SETUP_READY'
        IntegralArgumentCapture.received.class == expectedType
        IntegralArgumentCapture.received == expectedValue
        FixtureWorkflow.BODIES.isEmpty()

        where:
        targetLabel        | workflowClass                    | persistedValue                              || expectedType | expectedValue
        'Byte'             | ByteArgumentWorkflow             | 127                                         || Byte         | Byte.valueOf((byte) 127)
        'Short'            | ShortArgumentWorkflow            | 32767                                       || Short        | Short.valueOf((short) 32767)
        'Integer'          | IntegerArgumentWorkflow          | 2147483647                                  || Integer      | Integer.MAX_VALUE
        'primitive int'    | PrimitiveIntegerArgumentWorkflow | 7                                           || Integer      | 7
        'Long'             | LongArgumentWorkflow             | new BigInteger('9223372036854775807')        || Long         | Long.MAX_VALUE
        'integral decimal' | IntegerArgumentWorkflow          | new BigDecimal('7.0')                       || Integer      | 7
    }

    def 'setup preflight rejects #rejectionKind typed numeric invocation with a precise diagnostic'() {
        given:
        def workload = workload(['rejected'], [['rejected', 'first']], null, 'rejected', null,
                workflowClass.name, literalRecipe(persistedValue))
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])

        when:
        def report = new ScenarioExecutor().preflight(
                new ScenarioSetupPreflightOptions(packageFixture.manifest, null),
                new TrackingRuntimeContext(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'SETUP_FAILED'
        report.workloads()[0].status() == 'STARTUP_FAILED'
        report.workloads()[0].blockers()*.reason() == ['STARTUP_FAILED']
        report.workloads()[0].blockers()[0].inputVariantId() == 'rejected-input'
        report.workloads()[0].blockers()[0].message().contains('No compatible constructor for ' + workflowClass.name)
        report.workloads()[0].blockers()[0].message().contains(expectedDiagnostic)
        FixtureWorkflow.BODIES.isEmpty()
        FixtureWorkflow.COMPENSATIONS.isEmpty()

        where:
        rejectionKind         | workflowClass                    | persistedValue                       || expectedDiagnostic
        'overflow'            | IntegerArgumentWorkflow          | new BigInteger('2147483648')          || 'numeric value 2147483648 (java.math.BigInteger) is outside the range of java.lang.Integer'
        'fractional value'    | IntegerArgumentWorkflow          | new BigDecimal('7.5')                 || 'numeric value 7.5 (java.math.BigDecimal) is fractional and cannot be converted exactly to java.lang.Integer'
        'null-to-primitive'   | PrimitiveIntegerArgumentWorkflow | null                                  || 'argument 0 is null and cannot target primitive int'
        'unsupported coercion'| IntegerArgumentWorkflow          | '7'                                   || 'persisted type java.lang.String and cannot target java.lang.Integer: unsupported coercion'
    }

    def 'setup preflight continues typed overload search after exact conversion rejects a narrower target'() {
        given:
        ExactConversionOverloadWorkflow.selected = null
        def workload = workload(['overload'], [['overload', 'first']], null, 'overload', null,
                ExactConversionOverloadWorkflow.name, literalRecipe(new BigInteger('2147483648')))
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])

        when:
        def report = new ScenarioExecutor().preflight(
                new ScenarioSetupPreflightOptions(packageFixture.manifest, null),
                new TrackingRuntimeContext(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'SUCCESS'
        report.workloads()[0].status() == 'SETUP_READY'
        ExactConversionOverloadWorkflow.selected == 'long'
    }

    def 'setup preflight does not relabel a #invocationPath constructor-thrown failure as overload incompatibility'() {
        given:
        def workload = workload(['throwing'], [['throwing', 'first']], null, 'throwing', null,
                workflowClass.name, literalRecipe(7))
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])

        when:
        def report = new ScenarioExecutor().preflight(
                new ScenarioSetupPreflightOptions(packageFixture.manifest, null),
                new TrackingRuntimeContext(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'SETUP_FAILED'
        report.workloads()[0].blockers()[0].message().contains('constructor body failed')
        !report.workloads()[0].blockers()[0].message().contains('No compatible constructor')

        where:
        invocationPath       | workflowClass
        'reflection-direct'  | ThrowingConstructorWorkflow
        'exactly-converted'  | ThrowingIntegerConstructorWorkflow
    }

    def 'batch setup preflight checks every declared candidate through one supplied runtime context'() {
        given:
        def first = workload(['first'], [['first', 'first']])
        def second = workload(['second'], [['second', 'first']])
        def excluded = workload(['excluded'], [['excluded', 'first']])
        def firstScenario = scenarios(first, '0')[0]
        def secondScenario = scenarios(second, '0')[0]
        def excludedScenario = scenarios(excluded, '0')[0]
        def packageFixture = writePackage(
                [first, second, excluded], [firstScenario, secondScenario, excludedScenario],
                [first.deterministicId(), second.deterministicId()] as Set<String>)
        def before = packageChecksums(packageFixture.directory)
        def suppliedContext = new TrackingRuntimeContext(new TrackingSagaUnitOfWorkService())

        when:
        def report = new ScenarioExecutor().preflight(
                new ScenarioSetupPreflightOptions(packageFixture.manifest, null), suppliedContext)

        then:
        report.terminalStatus() == 'SUCCESS'
        report.candidateCount() == 2
        report.participantCount() == 2
        report.workloads()*.workloadPlanId() == [first.deterministicId(), second.deterministicId()].sort()
        report.workloads()*.status().unique() == ['SETUP_READY']
        report.workloads()*.setupDurationNanos().every { it >= 0 }
        suppliedContext.unitOfWorkCreations == 2
        suppliedContext.functionalityNames.size() == 2
        suppliedContext.beanRequests == [SagaUnitOfWorkService, SagaUnitOfWorkService]
        FixtureWorkflow.constructorCalls == 2
        FixtureWorkflow.BODIES.isEmpty()
        packageChecksums(packageFixture.directory) == before
    }

    def 'setup preflight rejects missing and duplicate manifest materializability rows before setup'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        def manifest = MAPPER.readTree(packageFixture.manifest.toFile())
        def rows = manifest.withArray('workloadMaterializability')
        if (mutation == 'missing') {
            rows.remove(0)
        } else if (mutation == 'duplicate') {
            rows.add(rows.get(0).deepCopy())
        } else {
            def extra = rows.get(0).deepCopy()
            extra.put('workloadPlanId', 'missing-workload')
            rows.add(extra)
        }
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(packageFixture.manifest.toFile(), manifest)
        def context = new TrackingRuntimeContext(new TrackingSagaUnitOfWorkService())

        when:
        new ScenarioExecutor().preflight(
                new ScenarioSetupPreflightOptions(packageFixture.manifest, null), context)

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains(expectedMessage)
        context.unitOfWorkCreations == 0
        FixtureWorkflow.constructorCalls == 0
        FixtureWorkflow.BODIES.isEmpty()

        where:
        mutation    | expectedMessage
        'missing'   | 'materializability rows do not match WorkloadPlans'
        'duplicate' | 'Duplicate manifest materializability row'
        'extra'     | 'materializability rows do not match WorkloadPlans'
    }

    def 'setup preflight rejects an available inconsistent materializable candidate count'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        def manifest = MAPPER.readTree(packageFixture.manifest.toFile())
        manifest.path('counts').put('materializableWorkloadPlans', '0')
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(packageFixture.manifest.toFile(), manifest)
        def context = new TrackingRuntimeContext(new TrackingSagaUnitOfWorkService())

        when:
        new ScenarioExecutor().preflight(
                new ScenarioSetupPreflightOptions(packageFixture.manifest, null), context)

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('Manifest count mismatch for materializableWorkloadPlans')
        context.unitOfWorkCreations == 0
        FixtureWorkflow.constructorCalls == 0
    }

    def 'zero-bit body failure recovers runtime checkpoints immediately and continues a surviving participant'() {
        given:
        def workload = workload(['left', 'right'], [
                ['left', 'first'], ['right', 'first'], ['left', 'second'], ['left', 'third'], ['right', 'second']
        ], null, null, 'left:second')
        def scenario = scenarios(workload, '00010')[0]
        def survivorActionPosition = scenario.actions().findIndexOf { action ->
            action.kind() == FaultScenarioActionKind.FORWARD && action.sagaInstanceId() == 'right' &&
                    workload.faultSlots().find { it.deterministicId() == action.sourceFaultSlotId() }.runtimeStepName() == 'second'
        }
        assert survivorActionPosition > 2
        def packageFixture = writePackage(workload, [scenario])
        def before = packageChecksums(packageFixture.directory)
        def output = packageFixture.directory.resolve('reports/deviated-runtime-fallback.json')
        def service = new TrackingSagaUnitOfWorkService()
        FixtureWorkflow.recordImplicitState('left', 'second')
        FixtureWorkflow.failBodyWithDomainException('left', 'second')

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, output, scenario.deterministicId()), runtime(service))

        then:
        report.terminalStatus() == 'PARTIAL_COMPENSATED'
        report.scheduleConformance() == 'DEVIATED'
        report.deviationActionId() == scenario.actions()[2].deterministicId()
        report.deviationPlannedPosition() == 2
        report.deviationPolicy() == 'IMMEDIATE_CHECKPOINT_RECOVERY_AND_CONTINUE'
        report.hardStopActionId() == null
        report.actualActions()*.status() == ['COMPLETED', 'COMPLETED', 'FAILED', 'COMPENSATED', 'COMPENSATED', 'COMPLETED']
        report.actualActions()[2].faultOrigin() == 'UNASSIGNED_RUNTIME'
        report.actualActions()[3].sourceCompensationCheckpointId() == null
        report.actualActions()[3].runtimeOccurrenceId()
        report.actualActions()[3].runtimeStepName() == 'second'
        report.actualActions()[3].plannedPosition() == null
        report.actualActions()[3].recoverySubOutcomes()*.kind() == ['IMPLICIT_SAGA_ROLLBACK']
        report.actualActions()[4].runtimeStepName() == 'first'
        report.actualActions()[4].sourceCompensationCheckpointId()
        report.actualActions()[4].recoverySubOutcomes()*.kind() == ['EXPLICIT_COMPENSATION']
        report.actualActions()[5].actionId() == scenario.actions()[survivorActionPosition].deterministicId()
        report.actualActions()[5].plannedPosition() == survivorActionPosition
        report.actualActions()[5].actualPosition() == 5
        report.participants()*.finalState() == ['COMPENSATED', 'COMMITTED']
        report.participants().find { it.sagaInstanceId() == 'left' }.skippedForwardActions()*.runtimeStepName() == ['third']
        report.participants().find { it.sagaInstanceId() == 'left' }.skippedForwardActions()*.state() == ['MASKED']
        report.faultSlots()*.state() == ['NOT_ASSIGNED', 'NOT_ASSIGNED', 'NOT_ASSIGNED', 'MASKED', 'NOT_ASSIGNED']
        report.lifecycleEvents()*.type() == ['ABORTED', 'COMPENSATED', 'AUTOMATIC_COMMIT']
        FixtureWorkflow.BODIES == ['left:first', 'right:first', 'left:second', 'right:second']
        FixtureWorkflow.COMPENSATIONS == ['left:first']
        service.implicitRollbacks == ['left:second']
        packageChecksums(packageFixture.directory) == before
        def json = MAPPER.readTree(output.toFile())
        json.path('scheduleConformance').asText() == 'DEVIATED'
        json.path('deviationPolicy').asText() == 'IMMEDIATE_CHECKPOINT_RECOVERY_AND_CONTINUE'
        def runtimeOnlyJson = json.path('actualActions').find { action ->
            action.path('runtimeStepName').asText() == 'second' && action.path('kind').asText() == 'COMPENSATION'
        }
        runtimeOnlyJson.path('sourceCompensationCheckpointId').isNull()
    }

    def 'multiple zero-bit fallbacks preserve the first schedule deviation point'() {
        given:
        def workload = workload(['left', 'right'], [['left', 'first'], ['right', 'first']])
        def scenario = scenarios(workload, '00')[0]
        def packageFixture = writePackage(workload, [scenario])
        def service = new TrackingSagaUnitOfWorkService(failCommitDomainFor: 'right')
        FixtureWorkflow.failBodyWithDomainException('left', 'first')

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, null, scenario.deterministicId()), runtime(service))

        then:
        report.terminalStatus() == 'COMPENSATED'
        report.scheduleConformance() == 'DEVIATED'
        report.deviationActionId() == scenario.actions()[0].deterministicId()
        report.deviationPlannedPosition() == 0
        report.deviationPolicy() == 'IMMEDIATE_CHECKPOINT_RECOVERY_AND_CONTINUE'
        report.actualActions()*.status() == ['FAILED', 'COMMIT_FAILED', 'COMPENSATED']
        report.actualActions().take(2)*.faultOrigin() == ['UNASSIGNED_RUNTIME', 'UNASSIGNED_RUNTIME']
        report.actualActions().take(2)*.actionId() == scenario.actions()*.deterministicId()
        report.participants()*.finalState() == ['COMPENSATED', 'COMPENSATED']
        FixtureWorkflow.BODIES == ['left:first', 'right:first']
        FixtureWorkflow.COMPENSATIONS == ['right:first']
        report.lifecycleEvents()*.type() == [
                'ABORTED', 'NO_COMPENSATION_WORK', 'COMPENSATED',
                'ABORTED', 'COMPENSATED'
        ]
    }

    def 'partially failed body reports explicit recovery only when runtime registration completed'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        FixtureWorkflow.recordImplicitState('solo', 'first')
        FixtureWorkflow.registerExplicitBeforeBodyFailure('solo', 'first')
        FixtureWorkflow.failBodyWithDomainException('solo', 'first')
        def service = new TrackingSagaUnitOfWorkService()

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, null, scenario.deterministicId()), runtime(service))

        then:
        report.terminalStatus() == 'COMPENSATED'
        report.scheduleConformance() == 'DEVIATED'
        report.actualActions()*.status() == ['FAILED', 'COMPENSATED']
        report.actualActions()[1].recoverySubOutcomes()*.kind() == ['EXPLICIT_COMPENSATION', 'IMPLICIT_SAGA_ROLLBACK']
        report.actualActions()[1].recoverySubOutcomes()*.status() == ['SUCCEEDED', 'SUCCEEDED']
        FixtureWorkflow.COMPENSATIONS == ['solo:first']
        service.implicitRollbacks == ['solo:first']
    }

    def 'zero-bit failure with no runtime recovery work emits no-work lifecycle and no recovery action'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        FixtureWorkflow.failBodyWithDomainException('solo', 'first')

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, null, scenario.deterministicId()), runtime(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'COMPENSATED'
        report.scheduleConformance() == 'DEVIATED'
        report.actualActions()*.status() == ['FAILED']
        report.lifecycleEvents()*.type() == ['ABORTED', 'NO_COMPENSATION_WORK', 'COMPENSATED']
    }

    def 'zero-bit commit domain failure preserves body outcome, recovers, and continues a survivor'() {
        given:
        def workload = workload(['left', 'right'], [
                ['left', 'first'], ['right', 'first'], ['left', 'second'], ['right', 'second']
        ])
        def scenario = scenarios(workload, '0000')[0]
        def packageFixture = writePackage(workload, [scenario])
        def service = new TrackingSagaUnitOfWorkService(failCommitDomainFor: 'left')

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, null, scenario.deterministicId()), runtime(service))

        then:
        report.terminalStatus() == 'PARTIAL_COMPENSATED'
        report.scheduleConformance() == 'DEVIATED'
        report.actualActions()*.status() == ['COMPLETED', 'COMPLETED', 'COMMIT_FAILED', 'COMPENSATED', 'COMPENSATED', 'COMPLETED']
        report.actualActions()[2].bodyOutcome() == 'SUCCEEDED'
        report.actualActions()[2].commitOutcome() == 'FAILED'
        report.actualActions()[2].faultOrigin() == 'UNASSIGNED_RUNTIME'
        report.actualActions()[2].exceptionMessage() == 'fixture commit domain failure'
        report.actualActions()[3..4]*.runtimeStepName() == ['second', 'first']
        report.actualActions()[5].runtimeStepName() == 'second'
        report.participants()*.finalState() == ['COMPENSATED', 'COMMITTED']
        FixtureWorkflow.BODIES == ['left:first', 'right:first', 'left:second', 'right:second']
        FixtureWorkflow.COMPENSATIONS == ['left:second', 'left:first']
    }

    def 'plain and service-unavailable SimulatorException body failures hard-stop without fallback or survivor continuation'() {
        given:
        def workload = workload(['left', 'right'], [['left', 'first'], ['right', 'first']])
        def scenario = scenarios(workload, '00')[0]
        def packageFixture = writePackage(workload, [scenario])
        if (failureKind == 'plain') {
            FixtureWorkflow.failBodyWithPlainSimulatorException('left', 'first')
        } else {
            FixtureWorkflow.failBodyWithServiceUnavailableException('left', 'first')
        }

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, null, scenario.deterministicId()), runtime(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'UNEXPECTED_EXECUTION_FAILURE'
        report.scheduleConformance() == 'INCOMPLETE'
        report.actualActions()*.status() == ['INFRASTRUCTURE_FAILED']
        report.actualActions()[0].faultOrigin() == null
        report.actualActions()[0].exceptionClass() == SimulatorException.name
        report.hardStopActionId() == scenario.actions()[0].deterministicId()
        report.hardStopReason() == 'FORWARD_INFRASTRUCTURE_FAILURE'
        report.lifecycleEvents().isEmpty()
        FixtureWorkflow.COMPENSATIONS.isEmpty()
        FixtureWorkflow.BODIES == ['left:first']
        report.participants().find { it.sagaInstanceId() == 'right' }.skippedForwardActions()*.runtimeStepName() == ['first']
        report.participants().find { it.sagaInstanceId() == 'right' }.skippedForwardActions()*.state() == ['NOT_EXECUTED_HARD_STOP']

        where:
        failureKind << ['plain', 'service-unavailable']
    }

    def 'plain SimulatorException during commit hard-stops without fallback or survivor continuation'() {
        given:
        def workload = workload(['left', 'right'], [['left', 'first'], ['right', 'first']])
        def scenario = scenarios(workload, '00')[0]
        def packageFixture = writePackage(workload, [scenario])
        def service = new TrackingSagaUnitOfWorkService(failCommitPlainSimulatorFor: 'left')

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, null, scenario.deterministicId()), runtime(service))

        then:
        report.terminalStatus() == 'UNEXPECTED_EXECUTION_FAILURE'
        report.scheduleConformance() == 'INCOMPLETE'
        report.actualActions()*.status() == ['COMMIT_INFRASTRUCTURE_FAILED']
        report.actualActions()[0].bodyOutcome() == 'SUCCEEDED'
        report.actualActions()[0].faultOrigin() == null
        report.hardStopReason() == 'COMMIT_INFRASTRUCTURE_FAILURE'
        report.lifecycleEvents().isEmpty()
        FixtureWorkflow.COMPENSATIONS.isEmpty()
        FixtureWorkflow.BODIES == ['left:first']
        !FixtureWorkflow.BODIES.contains('right:first')
        report.participants().find { it.sagaInstanceId() == 'right' }.skippedForwardActions()*.state() == ['NOT_EXECUTED_HARD_STOP']
    }

    def 'leaked assigned-fault exception hard-stops and is never relabeled as unassigned runtime'() {
        given:
        def workload = workload(['left', 'right'], [['left', 'first'], ['right', 'first']])
        def scenario = scenarios(workload, '00')[0]
        def packageFixture = writePackage(workload, [scenario])
        FixtureWorkflow.leakAssignedFaultException('left', 'first')

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, null, scenario.deterministicId()), runtime(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'UNEXPECTED_EXECUTION_FAILURE'
        report.scheduleConformance() == 'INCOMPLETE'
        report.actualActions()*.status() == ['INFRASTRUCTURE_FAILED']
        report.actualActions()[0].faultOrigin() == null
        report.actualActions()[0].exceptionClass().endsWith('FaultVectorInjectedFaultException')
        report.hardStopReason() == 'FORWARD_INFRASTRUCTURE_FAILURE'
        report.deviationActionId() == null
        FixtureWorkflow.COMPENSATIONS.isEmpty()
        FixtureWorkflow.BODIES == ['left:first']
        !FixtureWorkflow.BODIES.contains('right:first')
    }

    def 'non-domain body and commit failures are infrastructure hard stops without fallback'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        def service = new TrackingSagaUnitOfWorkService(failCommitInfrastructureFor: phase == 'commit' ? 'solo' : null)
        if (phase == 'body') FixtureWorkflow.failBodyWithInfrastructureException('solo', 'first')

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, null, scenario.deterministicId()), runtime(service))

        then:
        report.terminalStatus() == 'UNEXPECTED_EXECUTION_FAILURE'
        report.scheduleConformance() == 'INCOMPLETE'
        report.actualActions()*.status() == [phase == 'body' ? 'INFRASTRUCTURE_FAILED' : 'COMMIT_INFRASTRUCTURE_FAILED']
        report.actualActions()[0].faultOrigin() == null
        report.hardStopActionId() == scenario.actions()[0].deterministicId()
        report.hardStopReason() == (phase == 'body' ? 'FORWARD_INFRASTRUCTURE_FAILURE' : 'COMMIT_INFRASTRUCTURE_FAILURE')
        report.lifecycleEvents().isEmpty()
        FixtureWorkflow.COMPENSATIONS.isEmpty()

        where:
        phase << ['body', 'commit']
    }

    def 'scheduled compensation failure hard stops once and remains explicitly retryable later'() {
        given:
        def workload = workload(['left', 'right'], [
                ['left', 'first'], ['left', 'second'], ['right', 'first'], ['right', 'second']
        ])
        def scenario = scenarios(workload, '0100').find { candidate ->
            candidate.actions()*.kind() == [
                    FaultScenarioActionKind.FORWARD, FaultScenarioActionKind.FORWARD,
                    FaultScenarioActionKind.COMPENSATION, FaultScenarioActionKind.FORWARD,
                    FaultScenarioActionKind.FORWARD
            ]
        }
        assert scenario != null
        def packageFixture = writePackage(workload, [scenario])
        def before = packageChecksums(packageFixture.directory)
        def output = packageFixture.directory.resolve('reports/scheduled-compensation-failed.json')
        def service = new TrackingSagaUnitOfWorkService()
        FixtureWorkflow.failExplicitCompensation('left', 'first')

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, output, scenario.deterministicId()), runtime(service))

        then:
        report.terminalStatus() == 'COMPENSATION_FAILED'
        report.scheduleConformance() == 'INCOMPLETE'
        report.actualActions()*.status() == ['COMPLETED', 'ASSIGNED_FAULT', 'COMPENSATION_FAILED']
        report.actualActions()[2].recoverySubOutcomes()*.kind() == ['EXPLICIT_COMPENSATION']
        report.actualActions()[2].recoverySubOutcomes()*.status() == ['FAILED']
        report.hardStopActionId() == scenario.actions()[2].deterministicId()
        report.hardStopReason() == 'EXPLICIT_COMPENSATION_FAILED'
        FixtureWorkflow.COMPENSATION_ATTEMPTS['left:first'] == 1
        report.lifecycleEvents()*.type() == ['ABORTED', 'COMPENSATION_FAILED']
        report.participants().find { it.sagaInstanceId() == 'right' }.skippedForwardActions()*.runtimeStepName() == ['first', 'second']
        report.participants().find { it.sagaInstanceId() == 'right' }.skippedForwardActions()*.state() == ['NOT_EXECUTED_HARD_STOP', 'NOT_EXECUTED_HARD_STOP']
        !FixtureWorkflow.BODIES.any { it.startsWith('right:') }
        !FixtureWorkflow.UNIT_OF_WORKS.left.isCompensationExecuted('first')
        packageChecksums(packageFixture.directory) == before
        def json = MAPPER.readTree(output.toFile())
        json.path('scheduleConformance').asText() == 'INCOMPLETE'
        json.path('hardStopReason').asText() == 'EXPLICIT_COMPENSATION_FAILED'
        json.path('actualActions').last().path('recoverySubOutcomes').first().path('status').asText() == 'FAILED'

        when:
        FixtureWorkflow.allowExplicitCompensation('left', 'first')
        def retry = service.recoverStepForExecutor(FixtureWorkflow.UNIT_OF_WORKS.left, 'first')

        then:
        retry.explicitCompensationExecuted()
        FixtureWorkflow.COMPENSATION_ATTEMPTS['left:first'] == 2
        FixtureWorkflow.UNIT_OF_WORKS.left.isCompensationExecuted('first')
    }

    def 'fallback explicit compensation failure hard stops before survivor continuation'() {
        given:
        def workload = workload(['left', 'right'], [
                ['left', 'first'], ['right', 'first'], ['left', 'second'], ['right', 'second']
        ])
        def scenario = scenarios(workload, '0000')[0]
        def packageFixture = writePackage(workload, [scenario])
        FixtureWorkflow.failBodyWithDomainException('left', 'second')
        FixtureWorkflow.failExplicitCompensation('left', 'first')

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, null, scenario.deterministicId()), runtime(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'COMPENSATION_FAILED'
        report.scheduleConformance() == 'INCOMPLETE'
        report.deviationActionId() == scenario.actions()[2].deterministicId()
        report.actualActions()*.status() == ['COMPLETED', 'COMPLETED', 'FAILED', 'COMPENSATION_FAILED']
        report.actualActions()[3].recoverySubOutcomes()*.kind() == ['EXPLICIT_COMPENSATION']
        report.actualActions()[3].recoverySubOutcomes()*.status() == ['FAILED']
        report.hardStopActionId() == report.actualActions()[3].actionId()
        report.hardStopReason() == 'EXPLICIT_COMPENSATION_FAILED'
        FixtureWorkflow.COMPENSATION_ATTEMPTS['left:first'] == 1
        report.lifecycleEvents()*.type() == ['ABORTED', 'COMPENSATION_FAILED']
        report.participants().find { it.sagaInstanceId() == 'right' }.skippedForwardActions()*.runtimeStepName() == ['second']
        report.participants().find { it.sagaInstanceId() == 'right' }.skippedForwardActions()*.state() == ['NOT_EXECUTED_HARD_STOP']
        !FixtureWorkflow.BODIES.contains('right:second')
    }

    def 'fallback implicit rollback failure retains successful explicit prefix and stops'() {
        given:
        def workload = workload(['left', 'right'], [
                ['left', 'first'], ['right', 'first'], ['left', 'second'], ['right', 'second']
        ])
        def scenario = scenarios(workload, '0000')[0]
        def packageFixture = writePackage(workload, [scenario])
        def service = new TrackingSagaUnitOfWorkService(failImplicitFor: 'left:first')
        FixtureWorkflow.recordImplicitState('left', 'first')
        FixtureWorkflow.failBodyWithDomainException('left', 'second')

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, null, scenario.deterministicId()), runtime(service))

        then:
        report.terminalStatus() == 'COMPENSATION_FAILED'
        report.scheduleConformance() == 'INCOMPLETE'
        report.actualActions()*.status() == ['COMPLETED', 'COMPLETED', 'FAILED', 'COMPENSATION_FAILED']
        report.actualActions()[3].recoverySubOutcomes()*.kind() == ['EXPLICIT_COMPENSATION', 'IMPLICIT_SAGA_ROLLBACK']
        report.actualActions()[3].recoverySubOutcomes()*.status() == ['SUCCEEDED', 'FAILED']
        FixtureWorkflow.UNIT_OF_WORKS.left.isCompensationExecuted('first')
        !FixtureWorkflow.UNIT_OF_WORKS.left.isStepAborted('first')
        service.implicitAttempts['left:first'] == 1
        report.lifecycleEvents()*.type() == ['ABORTED', 'COMPENSATION_FAILED']
        report.participants().find { it.sagaInstanceId() == 'right' }.skippedForwardActions()*.state() == ['NOT_EXECUTED_HARD_STOP']
        !FixtureWorkflow.BODIES.contains('right:second')
    }

    def 'report writing failure remains infrastructure and retains an incomplete measured prefix in memory'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        def before = packageChecksums(packageFixture.directory)
        def nonDirectory = packageFixture.directory.resolve('not-a-directory')
        Files.writeString(nonDirectory, 'occupied')
        def output = nonDirectory.resolve('report.json')
        def impactOutput = packageFixture.directory.resolve('reports/report-write-failed-impact.json')

        when:
        new ScenarioExecutor().execute(
                options(packageFixture.manifest, output, scenario.deterministicId(), impactOutput),
                runtime(new TrackingSagaUnitOfWorkService()))

        then:
        def failure = thrown(ScenarioReportWriteException)
        failure.report().terminalStatus() == 'REPORT_WRITE_FAILED'
        failure.report().scheduleConformance() == 'INCOMPLETE'
        failure.report().actualActions()*.status() == ['COMPLETED']
        failure.report().hardStopActionId() == null
        failure.report().hardStopReason() == 'REPORT_WRITE_FAILED'
        failure.report().blockers()*.reason().contains('REPORT_WRITE_FAILED')
        def impact = MAPPER.readTree(impactOutput.toFile())
        impact.path('evaluationStatus').asText() == 'NOT_EVALUATED'
        impact.path('notEvaluatedReason').asText() == 'REPORT_WRITE_FAILED'
        impact.path('impactScore').isNull()
        packageChecksums(packageFixture.directory) == before
    }

    def 'provider installation failure is an infrastructure stop before measured execution'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        def impactOutput = packageFixture.directory.resolve('reports/provider-failure-impact.json')
        def occupiedProvider = FaultVectorProviderHolder.install(new InMemoryFaultVectorProvider([:]))

        when:
        def report
        try {
            report = new ScenarioExecutor().execute(
                    options(packageFixture.manifest, null, scenario.deterministicId(), impactOutput),
                    runtime(new TrackingSagaUnitOfWorkService()))
        } finally {
            occupiedProvider.close()
        }

        then:
        report.terminalStatus() == 'UNEXPECTED_EXECUTION_FAILURE'
        report.scheduleConformance() == null
        report.actualActions().isEmpty()
        report.hardStopReason() == 'EXECUTOR_INFRASTRUCTURE_FAILURE'
        def impact = MAPPER.readTree(impactOutput.toFile())
        impact.path('evaluationStatus').asText() == 'NOT_EVALUATED'
        impact.path('notEvaluatedReason').asText() == 'UNEXPECTED_EXECUTION_FAILURE'
        impact.path('impactScore').isNull()
        FixtureWorkflow.BODIES.isEmpty()
    }

    def 'dry run serializes the full planned contract without measured actions or conformance'() {
        given:
        def workload = workload(['solo'], [['solo', 'first'], ['solo', 'second']])
        def scenario = scenarios(workload, '10')[0]
        def packageFixture = writePackage(workload, [scenario])
        def output = packageFixture.directory.resolve('dry-run.json')
        def impactOutput = packageFixture.directory.resolve('dry-run-impact.json')
        def before = packageChecksums(packageFixture.directory)

        when:
        def report = new ScenarioExecutor().execute(
                new ScenarioExecutorOptions(packageFixture.manifest, output, scenario.deterministicId(), true,
                        null, null, null, null, null, impactOutput),
                runtime(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'DRY_RUN'
        report.scheduleConformance() == null
        report.plannedActions()*.actionId() == scenario.actions()*.deterministicId()
        report.actualActions().isEmpty()
        report.participants()*.materializationState().unique() == ['NOT_ATTEMPTED']
        FixtureWorkflow.constructorCalls == 0
        packageChecksums(packageFixture.directory) == before
        !MAPPER.readTree(output.toFile()).has('scheduleConformance')
        def impact = MAPPER.readTree(impactOutput.toFile())
        impact.path('evaluationStatus').asText() == 'NOT_EVALUATED'
        impact.path('notEvaluatedReason').asText() == 'DRY_RUN'
        impact.path('impactScore').isNull()
    }

    def 'report output cannot alias package artifact #artifactName during dryRun=#dryRun'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        def before = packageChecksums(packageFixture.directory)
        def output = packageFixture.directory.resolve(artifactName)

        when:
        new ScenarioExecutor().execute(
                new ScenarioExecutorOptions(packageFixture.manifest, output, scenario.deterministicId(), dryRun),
                runtime(new TrackingSagaUnitOfWorkService()))

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('must not alias scenario package input')
        packageChecksums(packageFixture.directory) == before
        FixtureWorkflow.constructorCalls == 0
        FixtureWorkflow.BODIES.isEmpty()

        where:
        [dryRun, artifactName] << [[false, true], [
                'workload-catalog.jsonl',
                'fault-scenario-catalog.jsonl',
                'scenario-catalog-manifest.json',
                'scenario-space-accounting.json',
                'workload-catalog-rejected-inputs.jsonl'
        ]].combinations()
    }

    def 'impact output cannot alias the execution report or a package input'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        def before = packageChecksums(packageFixture.directory)
        def output = packageFixture.directory.resolve('reports/execution.json')

        when:
        new ScenarioExecutor().execute(
                options(packageFixture.manifest, output, scenario.deterministicId(), output),
                runtime(new TrackingSagaUnitOfWorkService()))

        then:
        def reportAliasError = thrown(IllegalArgumentException)
        reportAliasError.message.contains('must not alias scenario execution report')
        packageChecksums(packageFixture.directory) == before
        FixtureWorkflow.constructorCalls == 0

        when:
        new ScenarioExecutor().execute(
                options(packageFixture.manifest, output, scenario.deterministicId(), packageFixture.manifest),
                runtime(new TrackingSagaUnitOfWorkService()))

        then:
        def packageAliasError = thrown(IllegalArgumentException)
        packageAliasError.message.contains('must not alias scenario package input')
        packageChecksums(packageFixture.directory) == before
        FixtureWorkflow.constructorCalls == 0

        when: 'two absent outputs resolve to the same leaf through symlinked parents'
        def realOutputDirectory = packageFixture.directory.resolve('real-reports')
        Files.createDirectories(realOutputDirectory)
        def executionAliasDirectory = packageFixture.directory.resolve('execution-reports-link')
        def impactAliasDirectory = packageFixture.directory.resolve('impact-reports-link')
        Files.createSymbolicLink(executionAliasDirectory, realOutputDirectory.fileName)
        Files.createSymbolicLink(impactAliasDirectory, realOutputDirectory.fileName)
        def aliasedExecutionOutput = executionAliasDirectory.resolve('shared.json')
        def aliasedImpactOutput = impactAliasDirectory.resolve('shared.json')
        new ScenarioExecutor().execute(
                options(packageFixture.manifest, aliasedExecutionOutput, scenario.deterministicId(), aliasedImpactOutput),
                runtime(new TrackingSagaUnitOfWorkService()))

        then:
        def parentAliasError = thrown(IllegalArgumentException)
        parentAliasError.message.contains('must not alias scenario execution report')
        !Files.exists(realOutputDirectory.resolve('shared.json'))
        packageChecksums(packageFixture.directory) == before
        FixtureWorkflow.constructorCalls == 0
    }

    def 'report output cannot alias a package input through normalized or symbolic paths'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def normalizedFixture = writePackage(workload, [scenario])
        def normalizedBefore = packageChecksums(normalizedFixture.directory)
        def normalizedOutput = normalizedFixture.directory.resolve('missing/../scenario-catalog-manifest.json')

        when:
        new ScenarioExecutor().execute(
                new ScenarioExecutorOptions(normalizedFixture.manifest, normalizedOutput, scenario.deterministicId(), false),
                runtime(new TrackingSagaUnitOfWorkService()))

        then:
        def normalizedError = thrown(IllegalArgumentException)
        normalizedError.message.contains('must not alias scenario package input')
        packageChecksums(normalizedFixture.directory) == normalizedBefore
        FixtureWorkflow.constructorCalls == 0
        FixtureWorkflow.BODIES.isEmpty()

        when:
        FixtureWorkflow.reset()
        def symbolicFixture = writePackage(workload, [scenario])
        def symbolicBefore = packageChecksums(symbolicFixture.directory)
        def symbolicOutput = symbolicFixture.directory.resolve('manifest-output-link.json')
        Files.createSymbolicLink(symbolicOutput, symbolicFixture.manifest.fileName)
        new ScenarioExecutor().execute(
                new ScenarioExecutorOptions(symbolicFixture.manifest, symbolicOutput, scenario.deterministicId(), true),
                runtime(new TrackingSagaUnitOfWorkService()))

        then:
        def symbolicError = thrown(IllegalArgumentException)
        symbolicError.message.contains('must not alias scenario package input')
        packageChecksums(symbolicFixture.directory) == symbolicBefore
        FixtureWorkflow.constructorCalls == 0
        FixtureWorkflow.BODIES.isEmpty()
    }

    def 'report output cannot overwrite custom v3 dynamic-enrichment #artifactName during dryRun=#dryRun'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        def packageBefore = packageChecksums(packageFixture.directory)
        def dynamicArtifacts = writeDynamicArtifacts(packageFixture.directory.resolve('custom-enrichment'), workload.deterministicId())
        def dynamicBefore = dynamicChecksums(dynamicArtifacts)

        when:
        new ScenarioExecutor().execute(
                new ScenarioExecutorOptions(packageFixture.manifest, dynamicArtifacts[artifactName], scenario.deterministicId(), dryRun),
                runtime(new TrackingSagaUnitOfWorkService()))

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('v3 dynamic-enrichment artifact')
        packageChecksums(packageFixture.directory) == packageBefore
        dynamicChecksums(dynamicArtifacts) == dynamicBefore
        FixtureWorkflow.constructorCalls == 0
        FixtureWorkflow.BODIES.isEmpty()

        where:
        [dryRun, artifactName] << [[false, true], ['sidecar', 'manifest', 'joinReport']].combinations()
    }

    def 'report output cannot overwrite a v3 dynamic-enrichment artifact through #aliasKind alias'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        def dynamicRoot = packageFixture.directory.resolve('custom-enrichment')
        def dynamicArtifacts = writeDynamicArtifacts(dynamicRoot, workload.deterministicId())
        def dynamicBefore = dynamicChecksums(dynamicArtifacts)
        def aliasRoot = packageFixture.directory.resolve('aliases')
        Files.createDirectories(aliasRoot)
        Path output
        if (aliasKind == 'NORMALIZED') {
            output = dynamicRoot.resolve('missing/../workload-dynamic-evidence-manifest.json')
        } else if (aliasKind == 'SYMBOLIC') {
            output = aliasRoot.resolve('sidecar-link.jsonl')
            Files.createSymbolicLink(output, dynamicArtifacts.sidecar)
        } else {
            output = aliasRoot.resolve('join-report-hard-link.json')
            Files.createLink(output, dynamicArtifacts.joinReport)
        }

        when:
        new ScenarioExecutor().execute(
                new ScenarioExecutorOptions(packageFixture.manifest, output, scenario.deterministicId(), false),
                runtime(new TrackingSagaUnitOfWorkService()))

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('v3 dynamic-enrichment artifact')
        dynamicChecksums(dynamicArtifacts) == dynamicBefore
        FixtureWorkflow.constructorCalls == 0
        FixtureWorkflow.BODIES.isEmpty()

        where:
        aliasKind << ['NORMALIZED', 'SYMBOLIC', 'HARD_LINK']
    }

    def 'selection requires one persisted FaultScenario id and v2 records are rejected'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])

        when:
        def missing = new ScenarioExecutor().execute(options(packageFixture.manifest, null, 'missing-id'), runtime(new TrackingSagaUnitOfWorkService()))

        then:
        missing.terminalStatus() == 'SELECTION_FAILED'
        missing.scheduleConformance() == null
        missing.hardStopReason() == 'MISSING_FAULT_SCENARIO_ID'
        missing.actualActions().isEmpty()
        missing.blockers()*.reason() == ['MISSING_FAULT_SCENARIO_ID']

        when:
        def v2 = packageFixture.directory.resolve('scenario-catalog.jsonl')
        Files.writeString(v2, '{"schemaVersion":"microservices-simulator.scenario-catalog.v2"}')
        new ScenarioExecutor().execute(options(v2, null, scenario.deterministicId()), runtime(new TrackingSagaUnitOfWorkService()))

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('latest v5 or explicit valid v4 packages are required')
        error.message.contains('v3 catalogs are not supported')
    }

    @Unroll
    def 'selected execution reports #reason without whole-package fallback and still rejects package output aliases'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        def valid = new ScenarioCatalogPackageReader().readSelected(
                packageFixture.manifest, workload.deterministicId(), scenario.deterministicId())
        def selected = new ScenarioCatalogPackageReader.SelectedPackageContents(
                valid.manifest(), missingKind == 'workload' ? null : valid.workloadPlan(),
                missingKind == 'fault' ? null : valid.faultScenario(), valid.workloadCatalogPath(),
                valid.faultScenarioCatalogPath(), valid.rejectedInputsPath(), valid.accountingPath())
        int wholeReads = 0
        def catalogReader = new ScenarioCatalogReader({ Path ignored ->
            wholeReads++
            throw new AssertionError('selected execution must not invoke whole-package reading')
        } as ScenarioCatalogReader.WholePackageReader, { Path ignored, String ignoredWorkload, String ignoredFault ->
            selected
        } as ScenarioCatalogReader.SelectedPackageReader)
        def executor = new ScenarioExecutor(
                catalogReader, new ScenarioMaterializer(), new ScenarioSetupRunner(), MAPPER)
        def service = new TrackingSagaUnitOfWorkService()
        def runtimeContext = runtime(service)

        when:
        def report = executor.execute(
                options(packageFixture.manifest, null, scenario.deterministicId()), runtimeContext)

        then:
        report.terminalStatus() == 'SELECTION_FAILED'
        report.hardStopReason() == reason
        report.blockers()*.reason() == [reason]
        report.actualActions().isEmpty()
        wholeReads == 0
        runtimeContext.unitOfWorkCreations == 0
        FixtureWorkflow.constructorCalls == 0
        FixtureWorkflow.BODIES.isEmpty()

        when:
        executor.execute(options(packageFixture.manifest, valid.workloadCatalogPath(),
                scenario.deterministicId()), runtimeContext)

        then:
        def aliasFailure = thrown(IllegalArgumentException)
        aliasFailure.message.contains('must not alias scenario package input')
        wholeReads == 0
        runtimeContext.unitOfWorkCreations == 0
        FixtureWorkflow.constructorCalls == 0
        FixtureWorkflow.BODIES.isEmpty()

        where:
        missingKind || reason
        'fault'     || 'MISSING_FAULT_SCENARIO_ID'
        'workload'  || 'MISSING_WORKLOAD_PLAN_ID'
    }

    def 'executor rejects checksum-mismatched fault-scenario content before selection or execution'() {
        given:
        def workload = workload(['solo'], [['solo', 'first']])
        def scenario = scenarios(workload, '0')[0]
        def packageFixture = writePackage(workload, [scenario])
        Files.write(packageFixture.directory.resolve('fault-scenario-catalog.jsonl'), '\n'.bytes,
                java.nio.file.StandardOpenOption.APPEND)

        when:
        new ScenarioExecutor().execute(
                options(packageFixture.manifest, null, scenario.deterministicId()),
                runtime(new TrackingSagaUnitOfWorkService()))

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('FAULT_SCENARIO_CATALOG')
        error.message.contains('checksum mismatch')
        FixtureWorkflow.constructorCalls == 0
        FixtureWorkflow.BODIES.isEmpty()
    }

    def 'executor package boundary rejects checksum-current repeated participant runtime step names before execution'() {
        given:
        def workload = workload(['solo'], [['solo', 'first'], ['solo', 'second']])
        def scenario = scenarios(workload, '00')[0]
        def packageFixture = writePackage(workload, [scenario])
        def workloadPath = packageFixture.directory.resolve('workload-catalog.jsonl')
        def workloadJson = MAPPER.readTree(Files.readAllLines(workloadPath).first())
        def firstStep = workloadJson.path('forwardSchedule').get(0)
        def repeatedStep = workloadJson.path('forwardSchedule').get(1)
        repeatedStep.put('stepId', firstStep.path('stepId').asText())
        repeatedStep.put('runtimeStepName', firstStep.path('runtimeStepName').asText())
        Files.writeString(workloadPath, MAPPER.writeValueAsString(workloadJson) + '\n')
        def manifest = MAPPER.readTree(Files.readString(packageFixture.manifest))
        manifest.path('workloadCatalog').put('sha256', sha256(workloadPath))
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(packageFixture.manifest.toFile(), manifest)

        when:
        new ScenarioExecutor().execute(
                options(packageFixture.manifest, null, scenario.deterministicId()),
                runtime(new TrackingSagaUnitOfWorkService()))

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('DUPLICATE_PARTICIPANT_RUNTIME_STEP_NAME')
        FixtureWorkflow.constructorCalls == 0
        FixtureWorkflow.BODIES.isEmpty()
    }

    def 'pure action validation rejects duplicate, premature, reverse-order, and residual-forward violations before execution'() {
        given:
        def workload = workload(['left', 'right'], [
                ['left', 'first'], ['left', 'second'], ['left', 'third'], ['right', 'first']
        ])
        def generated = scenarios(workload, '0010')[0]
        def forwards = generated.actions().findAll { it.kind() == FaultScenarioActionKind.FORWARD }
        def compensations = generated.actions().findAll { it.kind() == FaultScenarioActionKind.COMPENSATION }
        assert compensations.size() == 2
        def validator = new FaultScenarioValidator()
        def cases = [
                [[generated.actions()[0], generated.actions()[0]], 'DUPLICATE_OR_MISSING_ACTION_ID'],
                [[compensations[0]] + forwards + compensations.drop(1), 'COMPENSATION_NOT_ENABLED'],
                [forwards + compensations.reverse(), 'COMPENSATION_ORDER_VIOLATION'],
                [[forwards[1], forwards[0]] + forwards.drop(2) + compensations, 'RESIDUAL_FORWARD_ORDER_VIOLATION']
        ]

        expect:
        cases.every { mutation, expected ->
            codes(validator.validate(reidentified(generated, mutation as List<FaultScenarioAction>), workload)).contains(expected)
        }
    }

    def 'pure action validation rejects dangling and misowned action references'() {
        given:
        def workload = workload(['left', 'right'], [['left', 'first'], ['right', 'first']])
        def generated = scenarios(workload, '00')[0]
        def original = generated.actions()[0]
        def dangling = action(FaultScenarioActionKind.FORWARD, 'left', 'missing-slot', null, original.occurrenceId())
        def misowned = action(FaultScenarioActionKind.FORWARD, 'right', original.sourceFaultSlotId(), null, original.occurrenceId())
        def danglingCheckpoint = action(FaultScenarioActionKind.COMPENSATION, 'left', null, 'missing-checkpoint', original.occurrenceId())
        def validator = new FaultScenarioValidator()

        expect:
        codes(validator.validate(reidentified(generated, [dangling] + generated.actions().drop(1)), workload)).contains('MALFORMED_FORWARD_ACTION')
        codes(validator.validate(reidentified(generated, [misowned] + generated.actions().drop(1)), workload)).contains('MALFORMED_FORWARD_ACTION')
        codes(validator.validate(reidentified(generated, generated.actions() + danglingCheckpoint), workload)).contains('MALFORMED_COMPENSATION_ACTION')
    }

    def 'CLI rejects a direct execution-time vector overlay'() {
        when:
        ScenarioExecutorCli.validateInvocation([
                'spring-application-class': 'example.Application',
                'package-path': '/tmp/scenario-catalog-manifest.json',
                'fault-scenario-id': 'persisted-id',
                'output-path': '/tmp/report.json',
                'fault-vector': '01'
        ])

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('Unsupported executor option --fault-vector')
        ScenarioExecutorOptions.recordComponents*.name == [
                'packagePath', 'outputPath', 'faultScenarioId', 'dryRun', 'applicationBase', 'applicationId',
                'springApplicationClass', 'springProfiles', 'mavenProfile', 'impactOutputPath'
        ]
    }

    def 'CLI accepts optional setup preflight without a FaultScenario and keeps it separate from dry run'() {
        expect:
        ScenarioExecutorCli.validateInvocation([
                'spring-application-class': 'example.Application',
                'package-path': '/tmp/scenario-catalog-manifest.json',
                'output-path': '/tmp/setup-preflight.json',
                'preflight': 'true'
        ]) == null

        when:
        ScenarioExecutorCli.validateInvocation([
                'spring-application-class': 'example.Application',
                'package-path': '/tmp/scenario-catalog-manifest.json',
                'output-path': '/tmp/setup-preflight.json',
                'preflight': 'true',
                'dry-run': 'true'
        ])

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('cannot be combined')

        when:
        ScenarioExecutorCli.validateInvocation([
                'spring-application-class': 'example.Application',
                'package-path': '/tmp/scenario-catalog-manifest.json',
                'output-path': '/tmp/setup-preflight.json',
                'preflight': 'true',
                'fault-scenario-id': 'must-not-be-ignored'
        ])

        then:
        def faultSelectionError = thrown(IllegalArgumentException)
        faultSelectionError.message.contains('do not pass --fault-scenario-id')

        when:
        ScenarioExecutorCli.validateInvocation([
                'spring-application-class': 'example.Application',
                'package-path': '/tmp/scenario-catalog-manifest.json',
                'output-path': '/tmp/setup-preflight.json',
                'impact-output-path': '/tmp/impact.json',
                'preflight': 'true'
        ])

        then:
        def impactOutputError = thrown(IllegalArgumentException)
        impactOutputError.message.contains('--impact-output-path is an execution output')
    }

    def 'CLI rejects a missing impact output path value'() {
        given:
        def required = [
                '--spring-application-class', 'example.Application',
                '--package-path', '/tmp/scenario-catalog-manifest.json',
                '--output-path', '/tmp/report.json',
                '--fault-scenario-id', 'persisted-id'
        ]

        when:
        ScenarioExecutorCli.validateInvocation(ScenarioExecutorCli.parse((required + ['--impact-output-path']) as String[]))

        then:
        def bareError = thrown(IllegalArgumentException)
        bareError.message.contains('--impact-output-path requires an explicit path value')

        when:
        ScenarioExecutorCli.validateInvocation([
                'spring-application-class': 'example.Application',
                'package-path': '/tmp/scenario-catalog-manifest.json',
                'output-path': '/tmp/report.json',
                'fault-scenario-id': 'persisted-id',
                'impact-output-path': ' '
        ])

        then:
        def blankError = thrown(IllegalArgumentException)
        blankError.message.contains('--impact-output-path requires an explicit path value')
    }

    def 'CLI rejects non-canonical boolean option values before mode selection'() {
        given:
        def options = [
                'spring-application-class': 'example.Application',
                'package-path': '/tmp/scenario-catalog-manifest.json',
                'output-path': '/tmp/report.json',
                'fault-scenario-id': 'persisted-id'
        ]
        options[option] = value

        when:
        ScenarioExecutorCli.validateInvocation(options)

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains("--${option} must be exactly 'true' or 'false'".toString())

        where:
        option      | value
        'preflight' | 'TRUE'
        'preflight' | 'tru'
        'dry-run'   | 'FALSE'
        'dry-run'   | 'yes'
    }

    def 'event consequence executes synchronously outside fault boundaries and masks on its trigger fault'() {
        given:
        def workload = eventWorkload()
        def successScenario = scenarios(workload, '00').find { scenario ->
            scenario.actions()*.kind() == [FaultScenarioActionKind.FORWARD,
                                          FaultScenarioActionKind.EVENT_CONSEQUENCE,
                                          FaultScenarioActionKind.FORWARD]
        }
        def packageFixture = writePackage(workload, [successScenario])
        def before = packageChecksums(packageFixture.directory)
        def service = new TrackingSagaUnitOfWorkService()
        def handling = new FixtureEventHandling(service.fixtureEventService, 1, 'SUCCESS')
        def runtime = new TrackingRuntimeContext(service, [(FixtureEventHandling): handling])
        FixtureWorkflow.emitEvents('solo', 'first', 1)
        def gate = activateEventReplay()

        when:
        def report
        try {
            report = new ScenarioExecutor().execute(
                    options(packageFixture.manifest, packageFixture.directory.resolve('reports/event-success.json'),
                            successScenario.deterministicId()), runtime)
        } finally {
            gate.close()
            System.clearProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY)
        }

        then:
        report.schemaVersion() == 'microservices-simulator.scenario-execution-report.v5'
        report.terminalStatus() == 'SUCCESS'
        report.scheduleConformance() == 'EXACT'
        report.actualActions()*.kind() == ['FORWARD', 'EVENT_CONSEQUENCE', 'FORWARD']
        report.actualActions()*.status() == ['COMPLETED', 'COMPLETED', 'COMPLETED']
        report.actualActions()[1].sourceFaultSlotId() == null
        report.actualActions()[1].sourceCompensationCheckpointId() == null
        report.actualActions()[1].eventEvidence().eventTypeFqn() == FixtureEvent.name
        report.actualActions()[1].eventEvidence().subscriberAggregateId() == 99
        FixtureEventHandling.ORDER == ['handler-start', 'downstream-saga-complete', 'handler-return']
        FixtureEventHandling.BOUNDARIES == [null]
        packageChecksums(packageFixture.directory) == before

        when: 'the trigger is assigned a pre-body fault'
        FixtureWorkflow.reset()
        FixtureEventHandling.reset()
        def maskedScenario = scenarios(workload, '10').find { scenario ->
            scenario.actions().any { it.kind() == FaultScenarioActionKind.EVENT_CONSEQUENCE }
        }
        def maskedPackage = writePackage(workload, [maskedScenario])
        def maskedBefore = packageChecksums(maskedPackage.directory)
        def maskedService = new TrackingSagaUnitOfWorkService()
        def maskedRuntime = new TrackingRuntimeContext(maskedService,
                [(FixtureEventHandling): new FixtureEventHandling(maskedService.fixtureEventService, 1, 'SUCCESS')])
        FixtureWorkflow.emitEvents('solo', 'first', 1)
        gate = activateEventReplay()
        def masked
        try {
            masked = new ScenarioExecutor().execute(
                    options(maskedPackage.manifest, null, maskedScenario.deterministicId()), maskedRuntime)
        } finally {
            gate.close()
            System.clearProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY)
        }

        then:
        masked.actualActions().find { it.kind() == 'EVENT_CONSEQUENCE' }.status() == 'MASKED_BY_TRIGGER_FAULT'
        FixtureEventHandling.ORDER.isEmpty()
        masked.faultSlots()[0].state() == 'REALIZED'
        packageChecksums(maskedPackage.directory) == maskedBefore
    }

    def 'event consequence is causally masked when its trigger fails at runtime'() {
        given:
        def workload = eventWorkload()
        def scenario = scenarios(workload, '00').find { it.actions()*.kind().contains(FaultScenarioActionKind.EVENT_CONSEQUENCE) }
        def packageFixture = writePackage(workload, [scenario])
        def service = new TrackingSagaUnitOfWorkService()
        def runtime = new TrackingRuntimeContext(service,
                [(FixtureEventHandling): new FixtureEventHandling(service.fixtureEventService, 1, 'SUCCESS')])
        FixtureWorkflow.failBodyWithDomainException('solo', 'first')
        def gate = activateEventReplay()

        when:
        def report
        try {
            report = new ScenarioExecutor().execute(
                    options(packageFixture.manifest, null, scenario.deterministicId()), runtime)
        } finally {
            gate.close()
            System.clearProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY)
        }

        then:
        report.actualActions().find { it.kind() == 'EVENT_CONSEQUENCE' }.status() == 'MASKED_BY_TRIGGER_FAILURE'
        FixtureEventHandling.ORDER.isEmpty()
        report.scheduleConformance() == 'DEVIATED'
    }

    def 'trigger failure after matching event emission hard stops without dispatch and preserves event occurrence identity'() {
        given:
        def workload = eventWorkload()
        def scenario = scenarios(workload, '00').find {
            it.actions()*.kind().contains(FaultScenarioActionKind.EVENT_CONSEQUENCE)
        }
        def eventAction = scenario.actions().find { it.kind() == FaultScenarioActionKind.EVENT_CONSEQUENCE }
        def packageFixture = writePackage(workload, [scenario])
        def impact = packageFixture.directory.resolve('reports/trigger-failed-after-emission-impact.json')
        def service = new TrackingSagaUnitOfWorkService()
        def runtime = new TrackingRuntimeContext(service,
                [(FixtureEventHandling): new FixtureEventHandling(service.fixtureEventService, 1, 'SUCCESS')])
        FixtureWorkflow.emitEvents('solo', 'first', 1)
        FixtureWorkflow.failBodyWithDomainException('solo', 'first')
        def gate = activateEventReplay()

        when:
        def report
        try {
            report = new ScenarioExecutor().execute(
                    options(packageFixture.manifest, null, scenario.deterministicId(), impact), runtime)
        } finally {
            gate.close()
            System.clearProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY)
        }

        then:
        report.terminalStatus() == 'UNEXPECTED_EXECUTION_FAILURE'
        report.scheduleConformance() == 'INCOMPLETE'
        report.hardStopReason() == 'TRIGGER_FAILED_AFTER_EVENT_EMISSION'
        report.actualActions().find { it.kind() == 'FORWARD' }.status() ==
                'TRIGGER_FAILED_AFTER_EVENT_EMISSION'
        def eventOutcome = report.actualActions().find { it.kind() == 'EVENT_CONSEQUENCE' }
        eventOutcome.status() == 'NOT_REACHED'
        eventOutcome.runtimeOccurrenceId() == eventAction.occurrenceId()
        eventOutcome.runtimeOccurrenceId() == workload.eventConsequences().first().deterministicId()
        FixtureEventHandling.ORDER.isEmpty()
        MAPPER.readTree(impact.toFile()).path('evaluationStatus').asText() == 'NOT_EVALUATED'
    }

    def 'trigger commit failure after matching event emission also hard stops before event dispatch'() {
        given:
        def workload = eventWorkload(1)
        def scenario = scenarios(workload, '00').find {
            it.actions()*.kind().contains(FaultScenarioActionKind.EVENT_CONSEQUENCE)
        }
        def packageFixture = writePackage(workload, [scenario])
        def impact = packageFixture.directory.resolve('reports/trigger-commit-failed-after-emission-impact.json')
        def service = new TrackingSagaUnitOfWorkService(failCommitDomainFor: 'solo')
        def runtime = new TrackingRuntimeContext(service,
                [(FixtureEventHandling): new FixtureEventHandling(service.fixtureEventService, 1, 'SUCCESS')])
        FixtureWorkflow.emitEvents('solo', 'second', 1)
        def gate = activateEventReplay()

        when:
        def report
        try {
            report = new ScenarioExecutor().execute(
                    options(packageFixture.manifest, null, scenario.deterministicId(), impact), runtime)
        } finally {
            gate.close()
            System.clearProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY)
        }

        then:
        report.terminalStatus() == 'UNEXPECTED_EXECUTION_FAILURE'
        report.hardStopReason() == 'TRIGGER_FAILED_AFTER_EVENT_EMISSION'
        report.actualActions().find { it.runtimeStepName() == 'second' }.with {
            status() == 'TRIGGER_FAILED_AFTER_EVENT_EMISSION' &&
                    bodyOutcome() == 'SUCCEEDED' && commitOutcome() == 'FAILED'
        }
        report.actualActions().find { it.kind() == 'EVENT_CONSEQUENCE' }.status() == 'NOT_REACHED'
        FixtureEventHandling.ORDER.isEmpty()
        MAPPER.readTree(impact.toFile()).path('evaluationStatus').asText() == 'NOT_EVALUATED'
    }

    def 'event consequence hard stops every exact replay failure classification and leaves impact unevaluated'() {
        given:
        def workload = eventWorkload()
        def scenario = scenarios(workload, '00').find { it.actions()*.kind().contains(FaultScenarioActionKind.EVENT_CONSEQUENCE) }
        def packageFixture = writePackage(workload, [scenario])
        def impact = packageFixture.directory.resolve("reports/event-${expected}.impact.json".toString())
        def service = new TrackingSagaUnitOfWorkService()
        def handling = new FixtureEventHandling(service.fixtureEventService, subscribers, handlerMode)
        def runtime = new TrackingRuntimeContext(service, [(FixtureEventHandling): handling])
        FixtureWorkflow.emitEvents('solo', 'first', emissions)
        def gate = activateEventReplay()

        when:
        def report
        try {
            report = new ScenarioExecutor().execute(
                    options(packageFixture.manifest, null, scenario.deterministicId(), impact), runtime)
        } finally {
            gate.close()
            System.clearProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY)
        }

        then:
        report.terminalStatus() == 'UNEXPECTED_EXECUTION_FAILURE'
        report.scheduleConformance() == 'INCOMPLETE'
        report.hardStopReason() == expected
        report.actualActions().find { it.kind() == 'EVENT_CONSEQUENCE' }.status() == expected
        MAPPER.readTree(impact.toFile()).path('evaluationStatus').asText() == 'NOT_EVALUATED'

        where:
        emissions | subscribers | handlerMode || expected
        0         | 1           | 'SUCCESS'   || 'EXPECTED_EVENT_NOT_EMITTED'
        2         | 1           | 'SUCCESS'   || 'MULTIPLE_MATCHING_EVENTS_UNSUPPORTED'
        1         | 0           | 'SUCCESS'   || 'SELECTED_SUBSCRIBER_NOT_FOUND'
        1         | 2           | 'SUCCESS'   || 'MULTIPLE_MATCHING_SUBSCRIBERS_UNSUPPORTED'
        1         | 1           | 'FAIL'      || 'EVENT_CONSEQUENCE_FAILED'
        1         | 1           | 'RECURSIVE' || 'RECURSIVE_EVENT_CONSEQUENCE_UNSUPPORTED'
    }

    def 'event workload without the pre-start replay gate hard stops before measured actions'() {
        given:
        def workload = eventWorkload()
        def scenario = scenarios(workload, '00').find { it.actions()*.kind().contains(FaultScenarioActionKind.EVENT_CONSEQUENCE) }
        def packageFixture = writePackage(workload, [scenario])
        def impact = packageFixture.directory.resolve('reports/replay-control-impact.json')

        when:
        def report = new ScenarioExecutor().execute(
                options(packageFixture.manifest, null, scenario.deterministicId(), impact),
                runtime(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'UNEXPECTED_EXECUTION_FAILURE'
        report.hardStopReason() == 'EVENT_REPLAY_CONTROL_FAILED'
        report.actualActions().find { it.kind() == 'EVENT_CONSEQUENCE' }.status() == 'NOT_REACHED'
        MAPPER.readTree(impact.toFile()).path('evaluationStatus').asText() == 'NOT_EVALUATED'
    }

    def 'explicit valid v4 prerequisite provider resolves baseline binding clears pending events and fails before measurement'() {
        given:
        def workload = prerequisiteWorkload(true)
        def scenario = scenarios(workload, '00')[0]
        def packageFixture = writePackage(workload, [scenario])
        def before = packageChecksums(packageFixture.directory)
        def service = new TrackingSagaUnitOfWorkService()
        def provider = new FixturePrerequisiteProvider('fixture-provider', '1', providerMode, service.fixtureEventService)
        def runtime = new TrackingRuntimeContext(service, [:], includeProvider ? [provider] : [])
        def impact = packageFixture.directory.resolve("reports/prerequisite-${providerMode}-${includeProvider}.json".toString())
        def gate = activateEventReplay()

        when:
        def report
        try {
            report = new ScenarioExecutor().execute(
                    options(packageFixture.manifest, null, scenario.deterministicId(), impact), runtime)
        } finally {
            gate.close()
            System.clearProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY)
        }

        then:
        workload.schemaVersion() == WorkloadPlan.LEGACY_V4_SCHEMA_VERSION
        report.terminalStatus() == terminal
        report.sourceSetup() == null
        report.prerequisiteSetup().status() == setupStatus
        report.actualActions().size() == measuredActions
        MAPPER.readTree(impact.toFile()).path('evaluationStatus').asText() == impactStatus
        packageChecksums(packageFixture.directory) == before
        if (terminal == 'SUCCESS') {
            assert report.prerequisiteSetup().pendingEventsCleared() == 1
            assert report.prerequisiteSetup().emptyPendingEventBaseline()
            assert report.prerequisiteSetup().bindings()*.status() == ['RESOLVED']
            assert FixtureWorkflow.BODIES[0] == 'bound:first'
        } else {
            assert report.hardStopReason() == 'PREREQUISITE_BASELINE_FAILED'
        }

        where:
        providerMode    | includeProvider || terminal                       | setupStatus | measuredActions | impactStatus
        'SUCCESS'       | true            || 'SUCCESS'                      | 'SUCCEEDED' | 2               | 'EVALUATED'
        'MISSING_KEY'   | true            || 'PREREQUISITE_BASELINE_FAILED' | 'FAILED'    | 0               | 'NOT_EVALUATED'
        'WRONG_TYPE'    | true            || 'PREREQUISITE_BASELINE_FAILED' | 'FAILED'    | 0               | 'NOT_EVALUATED'
        'SUCCESS'       | false           || 'PREREQUISITE_BASELINE_FAILED' | 'FAILED'    | 0               | 'NOT_EVALUATED'
    }

    def 'CLI activates simulator replay control before Spring context startup'() {
        when:
        def activation = ScenarioExecutorCli.activateReplayMode()

        then:
        System.getProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY) == 'true'
        EventReplayCoordinator.active

        cleanup:
        activation?.close()
        System.clearProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY)
    }

    def 'CLI success vocabulary accepts only complete measured outcomes and dry run'() {
        expect:
        ScenarioExecutorCli.exitCodeFor(status) == code

        where:
        status                    || code
        'SUCCESS'                 || 0
        'COMPENSATED'             || 0
        'PARTIAL_COMPENSATED'     || 0
        'DRY_RUN'                 || 0
        'SELECTION_FAILED'        || 1
        'MATERIALIZATION_FAILED'  || 1
        'UNEXPECTED_EXECUTION_FAILURE' || 1
        'COMPENSATION_FAILED'     || 1
    }

    private static EventReplayCoordinator.Activation activateEventReplay() {
        System.setProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY, 'true')
        EventReplayCoordinator.activate()
    }

    private static ScenarioExecutionReport executeWithSourceSetup(Map packageFixture,
                                                                  FaultScenario scenario,
                                                                  int firstRuntimeId) {
        def service = new TrackingSagaUnitOfWorkService()
        def dispatcher = new FixtureSourceSetupDispatcher(firstRuntimeId, service.fixtureEventService)
        def runtime = new TrackingRuntimeContext(service, [:], [], [dispatcher])
        def gate = activateEventReplay()
        try {
            def report = new ScenarioExecutor().execute(
                    options(packageFixture.manifest as Path, null, scenario.deterministicId()), runtime)
            assert dispatcher.invocationCount == 12
            assert dispatcher.faultProviderActive.every { !it }
            assert dispatcher.faultBoundaries.every { it == null }
            return report
        } finally {
            gate.close()
            System.clearProperty(EventReplayCoordinator.REPLAY_MODE_PROPERTY)
        }
    }

    private static WorkloadPlan sourceSetupWorkload() {
        def base = workload(['left', 'right'], [['left', 'first'], ['right', 'first']])
        def inputs = base.acceptedInputs().collect { original ->
            def arguments = new ArrayList<>(original.inputRecipe().arguments())
            arguments[0] = new InputRecipeArgument(0, Integer.name, InputResolutionStatus.UNRESOLVED,
                    false, ['source setup binding required'], 'shared Tournament',
                    InputRecipeNode.builder('unresolved').executorReady(false)
                            .blockers(['source setup binding required']).build())
            def recipe = new InputRecipe(InputRecipe.SCHEMA_VERSION, null, false,
                    ['source setup binding required'], arguments)
            new InputVariant(original.deterministicId(), original.sagaFqn(), original.sourceClassFqn(),
                    original.sourceMethodName(), original.sourceBindingName(), original.callContextMethodName(),
                    original.inputRole(), original.fixtureOrigin(), original.resolutionStatus(), original.sourceMode(),
                    original.sourceModeConfidence(), original.sourceModeEvidence(), original.stableSourceText(),
                    original.provenanceText(), original.owners(), original.constructorArgumentSummaries(),
                    original.logicalKeyBindings(), original.warnings(), recipe)
        }
        def actions = [
                setupAction(1, FixtureSourceSetupDispatcher.CREATE, [setupLiteral(1)], FixtureSetupDto.name),
                setupAction(2, FixtureSourceSetupDispatcher.CREATE, [setupLiteral(2)], FixtureSetupDto.name),
                setupAction(3, FixtureSourceSetupDispatcher.ACTIVATE,
                        [SetupValueRecipe.actionProperty('setup-action-2', 'aggregateId', Integer.name)], 'void'),
                setupAction(4, FixtureSourceSetupDispatcher.CREATE, [setupLiteral(4)], FixtureSetupDto.name),
                setupAction(5, FixtureSourceSetupDispatcher.ACTIVATE,
                        [SetupValueRecipe.actionProperty('setup-action-4', 'aggregateId', Integer.name)], 'void'),
                setupAction(6, FixtureSourceSetupDispatcher.ENROLL,
                        [SetupValueRecipe.actionProperty('setup-action-1', 'aggregateId', Integer.name),
                         SetupValueRecipe.actionProperty('setup-action-2', 'aggregateId', Integer.name)], 'void'),
                setupAction(7, FixtureSourceSetupDispatcher.ENROLL,
                        [SetupValueRecipe.actionProperty('setup-action-1', 'aggregateId', Integer.name),
                         SetupValueRecipe.actionProperty('setup-action-4', 'aggregateId', Integer.name)], 'void'),
                setupAction(8, FixtureSourceSetupDispatcher.CREATE, [setupLiteral(8)], FixtureSetupDto.name),
                setupAction(9, FixtureSourceSetupDispatcher.CREATE, [setupLiteral(9)], FixtureSetupDto.name),
                setupAction(10, FixtureSourceSetupDispatcher.CREATE, [setupLiteral(10)], FixtureSetupDto.name),
                setupAction(11, FixtureSourceSetupDispatcher.CREATE, [setupLiteral(11)], FixtureSetupDto.name),
                setupAction(12, FixtureSourceSetupDispatcher.CREATE_TOURNAMENT,
                        [SetupValueRecipe.actionProperty('setup-action-1', 'courseAggregateId', Integer.name),
                         SetupValueRecipe.actionProperty('setup-action-4', 'aggregateId', Integer.name),
                         new SetupValueRecipe(SetupValueKind.LIST, 'java.util.List', null, null,
                                 null, [], [], [setupLiteral(8), setupLiteral(9)],
                                 null, null, null, [])], FixtureTournamentDto.name)
        ]
        def bindings = inputs.collect { input ->
            new SetupParticipantBinding(input.deterministicId(), 0, Integer.name,
                    SetupValueRecipe.actionProperty('setup-action-12', 'aggregateId', Integer.name), [])
        }
        def setup = new SetupPlan(SetupPlan.SCHEMA_VERSION, actions, bindings, [])
        reidentifyWorkload(base, inputs, null, setup)
    }

    private static WorkloadPlan malformedSourceSetupWorkload(WorkloadPlan base, String mutation) {
        def actions = new ArrayList<>(base.setupPlan().actions())
        def bindings = new ArrayList<>(base.setupPlan().participantBindings())
        def prerequisite = null
        if (mutation == 'PROPERTY') {
            def binding = bindings[0]
            bindings[0] = new SetupParticipantBinding(binding.inputVariantId(), binding.argumentIndex(),
                    binding.expectedTypeFqn(),
                    SetupValueRecipe.actionProperty('setup-action-12', 'quiz', Integer.name), [])
        } else if (mutation == 'TYPE') {
            def binding = bindings[0]
            bindings[0] = new SetupParticipantBinding(binding.inputVariantId(), binding.argumentIndex(),
                    String.name, SetupValueRecipe.actionProperty('setup-action-12', 'aggregateId', String.name), [])
        } else if (mutation == 'ORDER') {
            def action = actions[0]
            actions[0] = new SetupAction(action.actionId(), 1, action.sourceOccurrence(), action.methodKey(),
                    action.arguments(), action.declaredResultTypeFqn(), action.voidResult(), action.blockers())
        } else if (mutation == 'COLLECTION_ELEMENT') {
            def action = actions[11]
            def arguments = new ArrayList<>(action.arguments())
            arguments[2] = new SetupArgument(2, 'java.util.List<java.lang.Integer>',
                    new SetupValueRecipe(SetupValueKind.LIST, 'java.util.List', null, null,
                            null, [], [], [new SetupValueRecipe(SetupValueKind.LITERAL, String.name,
                            'string', 'wrong', null, [], [], [], null, null, null, [])],
                            null, null, null, []), [])
            actions[11] = new SetupAction(action.actionId(), action.orderIndex(), action.sourceOccurrence(),
                    action.methodKey(), arguments, action.declaredResultTypeFqn(), action.voidResult(), action.blockers())
        } else if (mutation == 'MIXED') {
            prerequisite = new PrerequisiteBaseline('fixture-provider', '1', [])
        }
        def setup = new SetupPlan(SetupPlan.SCHEMA_VERSION, actions, bindings, [])
        reidentifyWorkload(base, base.acceptedInputs(), prerequisite, setup)
    }

    private static WorkloadPlan reidentifyWorkload(WorkloadPlan base,
                                                   List<InputVariant> inputs,
                                                   PrerequisiteBaseline prerequisite,
                                                   SetupPlan setup) {
        def withoutId = new WorkloadPlan(base.schemaVersion(), null, base.kind(), base.executionShape(),
                base.participants(), inputs, base.forwardSchedule(), base.eventConsequences(), base.normalSchedule(),
                prerequisite, setup, base.conflictEvidence(), base.faultSlots(), base.compensationCheckpoints(),
                base.warnings())
        new WorkloadPlan(withoutId.schemaVersion(), ScenarioIdGenerator.workloadPlanId(withoutId), withoutId.kind(),
                withoutId.executionShape(), withoutId.participants(), withoutId.acceptedInputs(),
                withoutId.forwardSchedule(), withoutId.eventConsequences(), withoutId.normalSchedule(),
                withoutId.prerequisiteBaseline(), withoutId.setupPlan(), withoutId.conflictEvidence(),
                withoutId.faultSlots(), withoutId.compensationCheckpoints(), withoutId.warnings())
    }

    private static SetupAction setupAction(int oneBasedOrder,
                                           String methodKey,
                                           List<SetupValueRecipe> values,
                                           String resultType) {
        def arguments = values.withIndex().collect { value, index ->
            new SetupArgument(index, setupMethodParameterTypes(methodKey)[index], value, [])
        }
        new SetupAction("setup-action-${oneBasedOrder}".toString(), oneBasedOrder - 1,
                "fixture-source:${oneBasedOrder}".toString(), methodKey, arguments,
                resultType, resultType == 'void', [])
    }

    private static List<String> setupMethodParameterTypes(String methodKey) {
        def parameters = methodKey.substring(methodKey.indexOf('(') + 1, methodKey.indexOf(')'))
        parameters ? parameters.split(',') as List<String> : []
    }

    private static SetupValueRecipe setupLiteral(int value) {
        new SetupValueRecipe(SetupValueKind.LITERAL, Integer.name, 'integer', value,
                null, [], [], [], null, null, null, [])
    }

    private static WorkloadPlan eventWorkload(int triggerIndex = 0) {
        def base = workload(['solo'], [['solo', 'first'], ['solo', 'second']])
        def trigger = base.forwardSchedule()[triggerIndex]
        def site = new EventEmissionSite(
                ScenarioIdGenerator.eventEmissionSiteId('dummyapp.FixtureService', 'emit()', 0, FixtureEvent.name),
                'dummyapp.FixtureService', 'emit()', 0, FixtureEvent.name, ['dummyapp runtime fixture'])
        def consequenceId = ScenarioIdGenerator.eventConsequenceId(
                trigger.deterministicId(), site, FixtureEventHandling.name, 'handleFixtureEvents',
                FixtureEventHandler.name, 'dummyapp.FixtureEventProcessing', 'process',
                'dummyapp.FixtureFacade', 'startSaga', 'dummyapp.DownstreamFunctionalitySagas',
                EventConsequenceDefinition.UNIQUE_MATCHING_SUBSCRIBER)
        def consequence = new EventConsequence(consequenceId, trigger.deterministicId(), site, FixtureEvent.name,
                FixtureEventHandling.name, 'handleFixtureEvents', FixtureEventHandler.name,
                'dummyapp.FixtureEventProcessing', 'process', 'dummyapp.FixtureFacade', 'startSaga',
                'dummyapp.DownstreamFunctionalitySagas', EventConsequenceDefinition.UNIQUE_MATCHING_SUBSCRIBER, [])
        def withoutId = new WorkloadPlan(base.schemaVersion(), null, base.kind(), base.executionShape(),
                base.participants(), base.acceptedInputs(), base.forwardSchedule(), [consequence],
                triggerIndex == 0
                        ? [NormalActionRef.forward(0, base.forwardSchedule()[0].deterministicId()),
                           NormalActionRef.eventConsequence(1, consequenceId),
                           NormalActionRef.forward(2, base.forwardSchedule()[1].deterministicId())]
                        : [NormalActionRef.forward(0, base.forwardSchedule()[0].deterministicId()),
                           NormalActionRef.forward(1, base.forwardSchedule()[1].deterministicId()),
                           NormalActionRef.eventConsequence(2, consequenceId)],
                null, base.conflictEvidence(), base.faultSlots(), base.compensationCheckpoints(), base.warnings())
        new WorkloadPlan(withoutId.schemaVersion(), ScenarioIdGenerator.workloadPlanId(withoutId), withoutId.kind(),
                withoutId.executionShape(), withoutId.participants(), withoutId.acceptedInputs(),
                withoutId.forwardSchedule(), withoutId.eventConsequences(), withoutId.normalSchedule(),
                withoutId.prerequisiteBaseline(), withoutId.conflictEvidence(), withoutId.faultSlots(),
                withoutId.compensationCheckpoints(), withoutId.warnings())
    }

    private static WorkloadPlan prerequisiteWorkload(boolean legacyV4 = false) {
        def base = workload(['solo'], [['solo', 'first'], ['solo', 'second']])
        def oldInput = base.acceptedInputs()[0]
        def bindingNode = InputRecipeNode.builder('baseline_binding').executorReady(true)
                .bindingKey('participant').bindingTypeFqn(String.name).build()
        def arguments = new ArrayList<>(oldInput.inputRecipe().arguments())
        arguments[0] = new InputRecipeArgument(0, String.name, InputResolutionStatus.RESOLVED,
                true, [], 'provider binding', bindingNode)
        def recipe = new InputRecipe(InputRecipe.SCHEMA_VERSION, null, true, [], arguments)
        def input = new InputVariant(oldInput.deterministicId(), oldInput.sagaFqn(), oldInput.sourceClassFqn(),
                oldInput.sourceMethodName(), oldInput.sourceBindingName(), oldInput.callContextMethodName(),
                oldInput.inputRole(), oldInput.fixtureOrigin(), oldInput.resolutionStatus(), oldInput.sourceMode(),
                oldInput.sourceModeConfidence(), oldInput.sourceModeEvidence(), oldInput.stableSourceText(),
                oldInput.provenanceText(), oldInput.owners(), oldInput.constructorArgumentSummaries(),
                oldInput.logicalKeyBindings(), oldInput.warnings(), recipe)
        def baseline = new PrerequisiteBaseline('fixture-provider', '1',
                [new BaselineBindingRequirement('participant', String.name)])
        def schemaVersion = legacyV4 ? WorkloadPlan.LEGACY_V4_SCHEMA_VERSION : base.schemaVersion()
        def withoutId = new WorkloadPlan(schemaVersion, null, base.kind(), base.executionShape(),
                base.participants(), [input], base.forwardSchedule(), base.eventConsequences(), base.normalSchedule(),
                baseline, base.conflictEvidence(), base.faultSlots(), base.compensationCheckpoints(), base.warnings())
        new WorkloadPlan(withoutId.schemaVersion(), ScenarioIdGenerator.workloadPlanId(withoutId), withoutId.kind(),
                withoutId.executionShape(), withoutId.participants(), withoutId.acceptedInputs(),
                withoutId.forwardSchedule(), withoutId.eventConsequences(), withoutId.normalSchedule(),
                withoutId.prerequisiteBaseline(), withoutId.conflictEvidence(), withoutId.faultSlots(),
                withoutId.compensationCheckpoints(), withoutId.warnings())
    }

    private static Set<String> codes(FaultScenarioValidator.ValidationResult result) {
        result.diagnostics()*.code() as Set<String>
    }

    private static FaultScenarioAction action(FaultScenarioActionKind kind,
                                              String participantId,
                                              String faultSlotId,
                                              String checkpointId,
                                              String occurrenceId) {
        def withoutId = new FaultScenarioAction(null, kind, participantId, faultSlotId, checkpointId, occurrenceId)
        new FaultScenarioAction(ScenarioIdGenerator.faultScenarioActionId(withoutId), kind, participantId,
                faultSlotId, checkpointId, occurrenceId)
    }

    private static FaultScenario reidentified(FaultScenario original, List<FaultScenarioAction> actions) {
        def withoutId = new FaultScenario(FaultScenario.SCHEMA_VERSION, null, original.workloadPlanId(), original.assignedVector(), actions)
        new FaultScenario(withoutId.schemaVersion(), ScenarioIdGenerator.faultScenarioId(withoutId), withoutId.workloadPlanId(), withoutId.assignedVector(), withoutId.actions())
    }

    private static List<FaultScenario> scenarios(WorkloadPlan workload, String vector) {
        RecoveryScheduleGenerator.generate(workload, vector, 20).faultScenarios()
    }

    private static WorkloadPlan workload(List<String> participantIds,
                                         List<List<String>> scheduleShape,
                                         String blockedParticipant = null,
                                         String startupFailureParticipant = null,
                                         String omittedCheckpoint = null,
                                         String startupFailureSagaFqn = MissingExecuteWorkflow.name,
                                         InputRecipeNode startupArgumentRecipe = null) {
        def sagaFqns = participantIds.collectEntries { id ->
            [(id): startupFailureParticipant == id ? startupFailureSagaFqn : FixtureWorkflow.name]
        }
        def inputs = participantIds.collect { id ->
            input(id, sagaFqns[id], blockedParticipant == id,
                    startupFailureParticipant == id ? startupArgumentRecipe : null)
        }
        def participants = participantIds.collect { id ->
            new SagaInstance(id, sagaFqns[id], "${id}-input".toString(), [])
        }
        def schedule = scheduleShape.withIndex().collect { shape, index ->
            def participantId = shape[0]
            def runtimeName = shape[1]
            new ScheduledStep("${participantId}-${runtimeName}-${index}".toString(), participantId,
                    "${FixtureWorkflow.name}::${runtimeName}#${index}".toString(), index, runtimeName, [])
        }
        def slots = schedule.withIndex().collect { step, index ->
            new ForwardFaultSlot("slot-${step.deterministicId()}".toString(), index, step.deterministicId(),
                    step.sagaInstanceId(), step.stepId(), step.runtimeStepName(), step.deterministicId())
        }
        def checkpoints = schedule.findAll { step ->
            "${step.sagaInstanceId()}:${step.runtimeStepName()}".toString() != omittedCheckpoint
        }.withIndex().collect { step, index ->
            new CompensationCheckpoint("checkpoint-${step.deterministicId()}".toString(), index,
                    step.sagaInstanceId(), step.deterministicId(), step.stepId(), step.runtimeStepName(),
                    step.deterministicId(), CompensationEvidenceClass.EXPLICIT_COMPENSATION, [], [], [])
        }
        def withoutId = new WorkloadPlan(WorkloadPlan.SCHEMA_VERSION, null,
                participantIds.size() == 1 ? ScenarioKind.SINGLE_SAGA : ScenarioKind.MULTI_SAGA,
                WorkloadExecutionShape.SAGA_LOCAL, participants, inputs, schedule, [], slots, checkpoints, [])
        new WorkloadPlan(withoutId.schemaVersion(), ScenarioIdGenerator.workloadPlanId(withoutId), withoutId.kind(),
                withoutId.executionShape(), withoutId.participants(), withoutId.acceptedInputs(), withoutId.forwardSchedule(),
                withoutId.conflictEvidence(), withoutId.faultSlots(), withoutId.compensationCheckpoints(), withoutId.warnings())
    }

    private static InputVariant input(String participantId,
                                      String sagaFqn,
                                      boolean blocked,
                                      InputRecipeNode argumentRecipe = null) {
        def valueNode = blocked
                ? InputRecipeNode.builder('unresolved').executorReady(false).build()
                : argumentRecipe ?: literalRecipe(participantId)
        def arguments = [
                new InputRecipeArgument(0, 'java.lang.Object',
                        blocked ? InputResolutionStatus.UNRESOLVED : InputResolutionStatus.RESOLVED,
                        !blocked, blocked ? ['fixture blocker'] : [], 'participant', valueNode),
                new InputRecipeArgument(1, SagaUnitOfWorkService.name, InputResolutionStatus.UNRESOLVED, false, [], 'service',
                        InputRecipeNode.builder('placeholder').expectedTypeFqn(SagaUnitOfWorkService.name).executorReady(false).build()),
                new InputRecipeArgument(2, SagaUnitOfWork.name, InputResolutionStatus.UNRESOLVED, false, [], 'unitOfWork',
                        InputRecipeNode.builder('call_result').executorReady(false).build())
        ]
        new InputVariant("${participantId}-input".toString(), sagaFqn, 'Fixture', 'build', 'fixture',
                blocked ? InputResolutionStatus.UNRESOLVED : InputResolutionStatus.RESOLVED,
                SourceMode.UNKNOWN, SourceModeConfidence.UNKNOWN, blocked ? ['fixture blocker'] : [],
                'source', 'provenance', [], ['participant'], [:], [],
                new InputRecipe(InputRecipe.SCHEMA_VERSION, null, !blocked, blocked ? ['fixture blocker'] : [], arguments))
    }

    private static InputRecipeNode literalRecipe(Object value) {
        InputRecipeNode.builder('literal').executorReady(true).literalKind('value').value(value).build()
    }

    private static InputRecipeNode byteConstructorRecipe(String value) {
        def argument = new InputRecipeArgument(
                0, String.name, InputResolutionStatus.RESOLVED, true, [], 'value', literalRecipe(value))
        InputRecipeNode.builder('constructor')
                .executorReady(true)
                .targetTypeFqn(Byte.name)
                .arguments([argument])
                .build()
    }

    private static ScenarioExecutorOptions options(Path manifest, Path output, String scenarioId) {
        options(manifest, output, scenarioId, null)
    }

    private static ScenarioExecutorOptions options(Path manifest, Path output, String scenarioId, Path impactOutput) {
        new ScenarioExecutorOptions(manifest, output, scenarioId, false,
                'dummyapp', 'dummyapp', 'example.Application', 'test,sagas,local', 'test-sagas', impactOutput)
    }

    private static ScenarioRuntimeContext runtime(SagaUnitOfWorkService service) {
        new TrackingRuntimeContext(service)
    }

    private static Map writePackage(WorkloadPlan workload, List<FaultScenario> faultScenarios) {
        writePackage([workload], faultScenarios)
    }

    private static Map writePackage(List<WorkloadPlan> workloads, List<FaultScenario> faultScenarios) {
        writePackage(workloads, faultScenarios, workloads*.deterministicId() as Set<String>)
    }

    private static Map writePackage(List<WorkloadPlan> workloads,
                                    List<FaultScenario> faultScenarios,
                                    Set<String> materializableWorkloadIds) {
        Path directory = Files.createTempDirectory('v3-executor-package')
        Path workloadPath = directory.resolve('workload-catalog.jsonl')
        Path faultPath = directory.resolve('fault-scenario-catalog.jsonl')
        Path accountingPath = directory.resolve('scenario-space-accounting.json')
        Path rejectedPath = directory.resolve('workload-catalog-rejected-inputs.jsonl')
        Path manifestPath = directory.resolve('scenario-catalog-manifest.json')
        Files.write(workloadPath, workloads.collect { MAPPER.writeValueAsString(it) })
        Files.write(faultPath, faultScenarios.collect { MAPPER.writeValueAsString(it) })
        Files.writeString(accountingPath, MAPPER.writeValueAsString([schemaVersion: ScenarioSpaceAccountingReport.SCHEMA_VERSION]))
        Files.writeString(rejectedPath, '')
        def materializability = workloads.collect {
            new WorkloadMaterializability(
                    it.deterministicId(), materializableWorkloadIds.contains(it.deterministicId()),
                    materializableWorkloadIds.contains(it.deterministicId()) ? [] : ['fixture excluded'])
        }
        def workloadSchemas = workloads*.schemaVersion().unique()
        assert workloadSchemas.size() == 1
        def workloadSchema = workloadSchemas.first()
        def manifestSchema = workloadSchema == WorkloadPlan.LEGACY_V4_SCHEMA_VERSION
                ? ScenarioCatalogManifest.LEGACY_V4_SCHEMA_VERSION
                : ScenarioCatalogManifest.SCHEMA_VERSION
        def manifest = new ScenarioCatalogManifest(
                manifestSchema, '2026-07-20T00:00:00Z', new ScenarioGeneratorConfig(),
                'TEST', 'TEST', 20, 'TEST', materializability, [
                        workloadsExported: workloads.size().toString(),
                        materializableWorkloadPlans: materializableWorkloadIds.size().toString(),
                        nonMaterializableWorkloadPlans: (workloads.size() - materializableWorkloadIds.size()).toString()
                ], [],
                artifact('WORKLOAD_CATALOG', workloadSchema, workloadPath, workloads.size()),
                artifact('FAULT_SCENARIO_CATALOG', FaultScenario.SCHEMA_VERSION, faultPath, faultScenarios.size()),
                artifact('SCENARIO_SPACE_ACCOUNTING', ScenarioSpaceAccountingReport.SCHEMA_VERSION, accountingPath, 1),
                artifact('REJECTED_INPUT_DIAGNOSTIC', 'test.rejected.v1', rejectedPath, 0),
                [:], [:], [:])
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), manifest)
        [directory: directory, manifest: manifestPath]
    }

    private static ScenarioCatalogManifest.ArtifactMetadata artifact(String kind, String schema, Path path, int count) {
        new ScenarioCatalogManifest.ArtifactMetadata(kind, schema, path.fileName.toString(), count.toString(), sha256(path))
    }

    private static Map<String, Path> writeDynamicArtifacts(Path directory, String workloadPlanId) {
        Files.createDirectories(directory)
        def sidecar = directory.resolve('workload-dynamic-evidence.jsonl')
        def manifest = directory.resolve('workload-dynamic-evidence-manifest.json')
        def joinReport = directory.resolve('dynamic-evidence-join-report.json')
        Files.writeString(sidecar, MAPPER.writeValueAsString([
                schemaVersion: WorkloadDynamicEvidenceRecord.SCHEMA_VERSION,
                workloadPlanId: workloadPlanId,
                inputVariantIds: [],
                dynamicEvidence: [joinStatus: 'NOT_COVERED']
        ]) + System.lineSeparator())
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(manifest.toFile(), [
                schema: EnrichedScenarioCatalogWriter.MANIFEST_SCHEMA,
                sourceWorkloadCatalogPath: 'workload-catalog.jsonl',
                sidecarPath: sidecar.toString()
        ])
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(joinReport.toFile(), [
                schema: EnrichedScenarioCatalogWriter.JOIN_REPORT_SCHEMA,
                sidecarPath: sidecar.toString(),
                runStatus: 'COMPLETE'
        ])
        [sidecar: sidecar, manifest: manifest, joinReport: joinReport]
    }

    private static Map<String, String> dynamicChecksums(Map<String, Path> artifacts) {
        artifacts.collectEntries { name, path -> [(name): sha256(path)] }
    }

    private static Map<String, String> packageChecksums(Path directory) {
        ['workload-catalog.jsonl', 'fault-scenario-catalog.jsonl', 'scenario-catalog-manifest.json',
         'scenario-space-accounting.json', 'workload-catalog-rejected-inputs.jsonl'].collectEntries { name ->
            [(name): sha256(directory.resolve(name))]
        }
    }

    private static String sha256(Path path) {
        MessageDigest.getInstance('SHA-256').digest(Files.readAllBytes(path)).encodeHex().toString()
    }

    static class WideningArgumentWorkflow extends WorkflowFunctionality {
        static Long received

        WideningArgumentWorkflow(long participant,
                                 SagaUnitOfWorkService unitOfWorkService,
                                 SagaUnitOfWork unitOfWork) {
            received = participant
        }
    }

    static class NullOverloadWorkflow extends WorkflowFunctionality {
        static String selected

        NullOverloadWorkflow(int participant,
                             SagaUnitOfWorkService unitOfWorkService,
                             SagaUnitOfWork unitOfWork) {
            selected = 'primitive'
        }

        NullOverloadWorkflow(Object participant,
                             SagaUnitOfWorkService unitOfWorkService,
                             SagaUnitOfWork unitOfWork) {
            selected = 'reference'
        }
    }

    static class OverloadSearchWorkflow extends WorkflowFunctionality {
        static String selected

        OverloadSearchWorkflow(Integer participant,
                               SagaUnitOfWorkService unitOfWorkService,
                               SagaUnitOfWork unitOfWork) {
            selected = 'integer'
        }

        OverloadSearchWorkflow(Number participant,
                               SagaUnitOfWorkService unitOfWorkService,
                               SagaUnitOfWork unitOfWork) {
            selected = 'number'
        }
    }

    static class IntegralArgumentCapture {
        static Object received
    }

    static class ByteArgumentWorkflow extends WorkflowFunctionality {
        ByteArgumentWorkflow(Byte participant,
                             SagaUnitOfWorkService unitOfWorkService,
                             SagaUnitOfWork unitOfWork) {
            IntegralArgumentCapture.received = participant
        }
    }

    static class ShortArgumentWorkflow extends WorkflowFunctionality {
        ShortArgumentWorkflow(Short participant,
                              SagaUnitOfWorkService unitOfWorkService,
                              SagaUnitOfWork unitOfWork) {
            IntegralArgumentCapture.received = participant
        }
    }

    static class IntegerArgumentWorkflow extends WorkflowFunctionality {
        IntegerArgumentWorkflow(Integer participant,
                                SagaUnitOfWorkService unitOfWorkService,
                                SagaUnitOfWork unitOfWork) {
            IntegralArgumentCapture.received = participant
        }
    }

    static class PrimitiveIntegerArgumentWorkflow extends WorkflowFunctionality {
        PrimitiveIntegerArgumentWorkflow(int participant,
                                         SagaUnitOfWorkService unitOfWorkService,
                                         SagaUnitOfWork unitOfWork) {
            IntegralArgumentCapture.received = participant
        }
    }

    static class LongArgumentWorkflow extends WorkflowFunctionality {
        LongArgumentWorkflow(Long participant,
                             SagaUnitOfWorkService unitOfWorkService,
                             SagaUnitOfWork unitOfWork) {
            IntegralArgumentCapture.received = participant
        }
    }

    static class ExactConversionOverloadWorkflow extends WorkflowFunctionality {
        static String selected

        ExactConversionOverloadWorkflow(Integer participant,
                                        SagaUnitOfWorkService unitOfWorkService,
                                        SagaUnitOfWork unitOfWork) {
            selected = 'integer'
        }

        ExactConversionOverloadWorkflow(Long participant,
                                        SagaUnitOfWorkService unitOfWorkService,
                                        SagaUnitOfWork unitOfWork) {
            selected = 'long'
        }
    }

    static class ThrowingConstructorWorkflow extends WorkflowFunctionality {
        ThrowingConstructorWorkflow(Object participant,
                                    SagaUnitOfWorkService unitOfWorkService,
                                    SagaUnitOfWork unitOfWork) {
            throw new IllegalArgumentException('constructor body failed')
        }
    }

    static class ThrowingIntegerConstructorWorkflow extends WorkflowFunctionality {
        ThrowingIntegerConstructorWorkflow(Integer participant,
                                           SagaUnitOfWorkService unitOfWorkService,
                                           SagaUnitOfWork unitOfWork) {
            throw new IllegalArgumentException('constructor body failed after exact conversion')
        }
    }

    static class FixtureEventHandling {
        static final List<String> ORDER = []
        static final List<FaultVectorBoundaryContext> BOUNDARIES = []
        private final EventApplicationService eventApplicationService
        private final int subscriberCount
        private final String mode

        FixtureEventHandling(EventService eventService, int subscriberCount, String mode) {
            this.subscriberCount = subscriberCount
            this.mode = mode
            this.eventApplicationService = new EventApplicationService()
            ReflectionTestUtils.setField(this.eventApplicationService, 'eventService', eventService)
        }

        void handleFixtureEvents() {
            eventApplicationService.handleSubscribedEvent(FixtureEvent, new FixtureEventHandler(subscriberCount, mode))
        }

        static void reset() {
            ORDER.clear()
            BOUNDARIES.clear()
        }
    }

    static class FixtureEventHandler extends EventHandler {
        private final int subscriberCount
        private final String mode

        FixtureEventHandler(int subscriberCount, String mode) {
            super(mock(JpaRepository))
            this.subscriberCount = subscriberCount
            this.mode = mode
        }

        @Override
        Set<Integer> getAggregateIds() {
            subscriberCount == 0 ? [] as Set : (99..<(99 + subscriberCount)) as Set
        }

        @Override
        Set<EventSubscription> getEventSubscriptions(Integer subscriberAggregateId,
                                                       Class<? extends pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event> eventClass) {
            [new EventSubscription(Math.abs('solo'.hashCode()), 0L, FixtureEvent.simpleName) {}] as Set
        }

        @Override
        void handleEvent(Integer subscriberAggregateId,
                         pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event event) {
            FixtureEventHandling.ORDER.add('handler-start')
            FixtureEventHandling.BOUNDARIES.add(FaultVectorProviderHolder.currentBoundary().orElse(null))
            if (mode == 'RECURSIVE') EventReplayCoordinator.beforeEventRegistration()
            if (mode == 'FAIL') throw new IllegalStateException('dummyapp-labelled handler failure')
            FixtureEventHandling.ORDER.add('downstream-saga-complete')
            FixtureEventHandling.ORDER.add('handler-return')
        }
    }

    static class FixtureSetupDto {
        Integer aggregateId
        Integer courseAggregateId

        FixtureSetupDto(Integer aggregateId) {
            this.aggregateId = aggregateId
            this.courseAggregateId = aggregateId
        }
    }

    static class FixtureTournamentDto extends FixtureSetupDto {
        FixtureTournamentDto(Integer aggregateId) {
            super(aggregateId)
        }
    }

    private static class FixtureSourceSetupDispatcher implements ScenarioSetupActionDispatcher {
        static final String CREATE = "fixture.ClosedFacade#create(java.lang.Integer):${FixtureSetupDto.name}".toString()
        static final String ACTIVATE = 'fixture.ClosedFacade#activate(java.lang.Integer):void'
        static final String ENROLL = 'fixture.ClosedFacade#enroll(java.lang.Integer,java.lang.Integer):void'
        static final String CREATE_TOURNAMENT = "fixture.ClosedFacade#createTournament(java.lang.Integer,java.lang.Integer,java.util.List<java.lang.Integer>):${FixtureTournamentDto.name}".toString()

        final TrackingEventService eventService
        final String mode
        final List<String> invokedMethodKeys = []
        final List<Boolean> faultProviderActive = []
        final List<FaultVectorBoundaryContext> faultBoundaries = []
        int nextRuntimeId
        int invocationCount
        int activationEffects
        int enrollmentEffects
        int tournamentCreations

        FixtureSourceSetupDispatcher(int firstRuntimeId,
                                     TrackingEventService eventService,
                                     String mode = 'SUCCESS') {
            this.nextRuntimeId = firstRuntimeId
            this.eventService = eventService
            this.mode = mode
        }

        @Override
        Map<String, SetupMethod> setupMethods() {
            def methods = [
                    (CREATE): new SetupMethod(CREATE, FixtureSetupDto.name, false,
                            { List<Object> arguments ->
                                beforeInvocation(CREATE)
                                new FixtureSetupDto(nextRuntimeId++)
                            } as Invocation),
                    (ACTIVATE): new SetupMethod(ACTIVATE, 'void', true,
                            { List<Object> arguments ->
                                beforeInvocation(ACTIVATE)
                                activationEffects++
                                null
                            } as Invocation),
                    (ENROLL): new SetupMethod(ENROLL, 'void', true,
                            { List<Object> arguments ->
                                beforeInvocation(ENROLL)
                                enrollmentEffects++
                                null
                            } as Invocation),
                    (CREATE_TOURNAMENT): new SetupMethod(CREATE_TOURNAMENT,
                            FixtureTournamentDto.name, false,
                            { List<Object> arguments ->
                                beforeInvocation(CREATE_TOURNAMENT)
                                assert arguments[2] == [8, 9]
                                assert arguments[2].every { it instanceof Integer }
                                tournamentCreations++
                                if (mode == 'NULL_RESULT') return null
                                if (mode == 'WRONG_TYPE') return 'not-a-tournament'
                                new FixtureTournamentDto(nextRuntimeId++)
                            } as Invocation)
            ]
            if (mode == 'UNAUTHORIZED') methods.remove(CREATE_TOURNAMENT)
            methods
        }

        private void beforeInvocation(String methodKey) {
            invocationCount++
            invokedMethodKeys.add(methodKey)
            faultProviderActive.add(FaultVectorProviderHolder.active)
            faultBoundaries.add(FaultVectorProviderHolder.currentBoundary().orElse(null))
            def event = new FixtureEvent(invocationCount)
            event.publisherAggregateVersion = 1L
            event.published = true
            eventService.saveEvent(event)
        }
    }

    private static class FixturePrerequisiteProvider implements ScenarioPrerequisiteProvider {
        private final String id
        private final String version
        private final String mode
        private final TrackingEventService fixtureEventService

        FixturePrerequisiteProvider(String id, String version, String mode, TrackingEventService eventService) {
            this.id = id
            this.version = version
            this.mode = mode
            this.fixtureEventService = eventService
        }

        @Override
        String providerId() { id }

        @Override
        String providerVersion() { version }

        @Override
        ScenarioPrerequisiteResult prepare(ScenarioRuntimeContext runtimeContext,
                                           List<BaselineBindingRequirement> requiredBindings) {
            def event = new FixtureEvent(1)
            event.publisherAggregateVersion = 1L
            event.published = true
            fixtureEventService.saveEvent(event)
            def bindings = switch (mode) {
                case 'MISSING_KEY' -> [:]
                case 'WRONG_TYPE' -> [participant: 42]
                default -> [participant: 'bound']
            }
            new ScenarioPrerequisiteResult(bindings, [fixture: 'dummyapp-labelled'])
        }
    }

    private static class TrackingRuntimeContext implements ScenarioRuntimeContext {
        private final TrackingSagaUnitOfWorkService service
        private final Map<Class<?>, Object> extraBeans
        private final List<ScenarioPrerequisiteProvider> prerequisiteProviders
        private final List<ScenarioSetupActionDispatcher> setupDispatchers
        int unitOfWorkCreations
        List<Class<?>> beanRequests = []
        List<String> functionalityNames = []

        TrackingRuntimeContext(SagaUnitOfWorkService service,
                               Map<Class<?>, Object> extraBeans = [:],
                               List<ScenarioPrerequisiteProvider> prerequisiteProviders = [],
                               List<ScenarioSetupActionDispatcher> setupDispatchers = []) {
            this.service = (TrackingSagaUnitOfWorkService) service
            this.extraBeans = extraBeans
            this.prerequisiteProviders = prerequisiteProviders
            this.setupDispatchers = setupDispatchers
        }

        @Override
        Object bean(Class<?> type) {
            beanRequests.add(type)
            if (type == SagaUnitOfWorkService) return service
            if (type == EventService) return service.fixtureEventService
            extraBeans[type]
        }

        @Override
        def <T> List<T> beans(Class<T> type) {
            if (type == ScenarioPrerequisiteProvider) return prerequisiteProviders as List<T>
            if (type == ScenarioSetupActionDispatcher) return setupDispatchers as List<T>
            []
        }

        @Override
        Object createSagaUnitOfWork(String functionalityName) {
            unitOfWorkCreations++
            functionalityNames.add(functionalityName)
            new SagaUnitOfWork(0L, functionalityName)
        }
    }

    private static class TrackingSagaUnitOfWorkService extends SagaUnitOfWorkService {
        final TrackingEventService fixtureEventService = new TrackingEventService()
        Map<String, Integer> commitCounts = [:].withDefault { 0 }
        Map<String, Integer> implicitAttempts = [:].withDefault { 0 }
        List<String> implicitRollbacks = []
        String failCommitDomainFor
        String failCommitPlainSimulatorFor
        String failCommitInfrastructureFor
        String failImplicitFor

        TrackingSagaUnitOfWorkService(Map values = [:]) {
            def versionService = mock(IVersionService)
            when(versionService.incrementAndGetVersionNumber()).thenReturn(1L, 2L, 3L)
            ReflectionTestUtils.setField(this, 'versionService', versionService)
            ReflectionTestUtils.setField(this, 'entityManager', mock(EntityManager))
            ReflectionTestUtils.setField(this, 'eventService', fixtureEventService)
            def environment = mock(Environment)
            when(environment.activeProfiles).thenReturn(['local'] as String[])
            ReflectionTestUtils.setField(this, 'environment', environment)
            this.failCommitDomainFor = values.failCommitDomainFor
            this.failCommitPlainSimulatorFor = values.failCommitPlainSimulatorFor
            this.failCommitInfrastructureFor = values.failCommitInfrastructureFor
            this.failImplicitFor = values.failImplicitFor
        }

        @Override
        void commit(SagaUnitOfWork unitOfWork) {
            commitCounts[unitOfWork.functionalityName] = commitCounts[unitOfWork.functionalityName] + 1
            if (unitOfWork.functionalityName == failCommitDomainFor) {
                throw new SimulatorDomainException('fixture commit domain failure')
            }
            if (unitOfWork.functionalityName == failCommitPlainSimulatorFor) {
                throw new SimulatorException('fixture unmarked commit failure')
            }
            if (unitOfWork.functionalityName == failCommitInfrastructureFor) {
                throw new IllegalStateException('fixture commit infrastructure failure')
            }
        }

        @Override
        void sendAbortCommandsForStep(SagaUnitOfWork unitOfWork, String stepName) {
            def key = "${unitOfWork.functionalityName}:${stepName}".toString()
            implicitAttempts[key] = implicitAttempts[key] + 1
            implicitRollbacks.add(key)
            if (key == failImplicitFor) {
                throw new IllegalStateException("fixture implicit rollback failure ${key}".toString())
            }
        }
    }

    private static class TrackingEventService extends EventService {
        final Map<Integer, pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event> events = [:]
        int nextId = 1
        boolean retainEventsOnClear

        @Override
        void saveEvent(pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event event) {
            if (event.id == null) event.id = nextId++
            events[event.id] = event
        }

        @Override
        pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event getEventForReplay(Integer eventId) {
            events[eventId]
        }

        @Override
        long eventCountForReplay() {
            events.size()
        }

        @Override
        void clearEventsForReplay() {
            if (!retainEventsOnClear) events.clear()
        }
    }
}
