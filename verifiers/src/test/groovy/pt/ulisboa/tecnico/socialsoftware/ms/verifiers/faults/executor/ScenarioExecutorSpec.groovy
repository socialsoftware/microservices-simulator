package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.faults.FaultVectorProviderHolder
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.TraceManager
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.export.EnrichedScenarioCatalogWriter
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.WorkloadDynamicEvidenceRecord
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.FaultScenarioValidator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.RecoveryScheduleGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioIdGenerator
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence
import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

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
        report.schemaVersion() == 'microservices-simulator.scenario-execution-report.v4'
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
        json.path('schemaVersion').asText() == 'microservices-simulator.scenario-execution-report.v4'
        json.path('plannedActions').size() == 4
        json.path('actualActions').size() == 4
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
        report.actualActions().isEmpty()
        report.participants()*.materializationState().unique() == ['MATERIALIZED']
        report.participants().find { it.sagaInstanceId() == 'ready' }.startupState() == 'STARTUP_READY'
        report.participants().find { it.sagaInstanceId() == 'broken' }.startupState() == 'STARTUP_FAILED'
        FixtureWorkflow.BODIES.isEmpty()
    }

    def 'controlled commit failure reports body success separately and leaves recovery checkpoints unconsumed'() {
        given:
        def workload = workload(['solo'], [['solo', 'first'], ['solo', 'second']])
        def scenario = scenarios(workload, '00')[0]
        def packageFixture = writePackage(workload, [scenario])
        def service = new TrackingSagaUnitOfWorkService(failCommitFor: 'solo')

        when:
        def report = new ScenarioExecutor().execute(options(packageFixture.manifest, null, scenario.deterministicId()), runtime(service))

        then:
        report.terminalStatus() == 'UNEXPECTED_EXECUTION_FAILURE'
        report.scheduleConformance() == 'INCOMPLETE'
        report.actualActions()*.status() == ['COMPLETED', 'COMMIT_FAILED']
        report.actualActions()[1].bodyOutcome() == 'SUCCEEDED'
        report.actualActions()[1].commitOutcome() == 'FAILED'
        report.actualActions()[1].faultOrigin() == 'UNASSIGNED_RUNTIME'
        report.actualActions()[1].exceptionMessage() == 'fixture commit failure'
        report.participants()[0].finalState() == 'ABORTED'
        FixtureWorkflow.BODIES == ['solo:first', 'solo:second']
        FixtureWorkflow.COMPENSATIONS.isEmpty()
        !FixtureWorkflow.UNIT_OF_WORKS.solo.isCompensationExecuted('first')
        !FixtureWorkflow.UNIT_OF_WORKS.solo.isCompensationExecuted('second')
    }

    def 'dry run serializes the full planned contract without measured actions or conformance'() {
        given:
        def workload = workload(['solo'], [['solo', 'first'], ['solo', 'second']])
        def scenario = scenarios(workload, '10')[0]
        def packageFixture = writePackage(workload, [scenario])
        def output = packageFixture.directory.resolve('dry-run.json')
        def before = packageChecksums(packageFixture.directory)

        when:
        def report = new ScenarioExecutor().execute(new ScenarioExecutorOptions(packageFixture.manifest, output, scenario.deterministicId(), true), runtime(new TrackingSagaUnitOfWorkService()))

        then:
        report.terminalStatus() == 'DRY_RUN'
        report.scheduleConformance() == null
        report.plannedActions()*.actionId() == scenario.actions()*.deterministicId()
        report.actualActions().isEmpty()
        report.participants()*.materializationState().unique() == ['NOT_ATTEMPTED']
        FixtureWorkflow.constructorCalls == 0
        packageChecksums(packageFixture.directory) == before
        !MAPPER.readTree(output.toFile()).has('scheduleConformance')
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
        missing.actualActions().isEmpty()
        missing.blockers()*.reason() == ['MISSING_FAULT_SCENARIO_ID']

        when:
        def v2 = packageFixture.directory.resolve('scenario-catalog.jsonl')
        Files.writeString(v2, '{"schemaVersion":"microservices-simulator.scenario-catalog.v2"}')
        new ScenarioExecutor().execute(options(v2, null, scenario.deterministicId()), runtime(new TrackingSagaUnitOfWorkService()))

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('v3 WorkloadPlan/FaultScenario packages are required')
        error.message.contains('v2 catalogs are not supported')
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
                'springApplicationClass', 'springProfiles', 'mavenProfile'
        ]
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
                                         String startupFailureParticipant = null) {
        def sagaFqns = participantIds.collectEntries { id ->
            [(id): startupFailureParticipant == id ? MissingExecuteWorkflow.name : FixtureWorkflow.name]
        }
        def inputs = participantIds.collect { id -> input(id, sagaFqns[id], blockedParticipant == id) }
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
        def checkpoints = schedule.withIndex().collect { step, index ->
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

    private static InputVariant input(String participantId, String sagaFqn, boolean blocked) {
        def valueNode = blocked
                ? InputRecipeNode.builder('unresolved').executorReady(false).build()
                : InputRecipeNode.builder('literal').executorReady(true).literalKind('value').value(participantId).build()
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

    private static ScenarioExecutorOptions options(Path manifest, Path output, String scenarioId) {
        new ScenarioExecutorOptions(manifest, output, scenarioId, false,
                'dummyapp', 'dummyapp', 'example.Application', 'test,sagas,local', 'test-sagas')
    }

    private static ScenarioRuntimeContext runtime(SagaUnitOfWorkService service) {
        new ScenarioRuntimeContext() {
            @Override
            Object bean(Class<?> type) {
                type == SagaUnitOfWorkService ? service : null
            }
        }
    }

    private static Map writePackage(WorkloadPlan workload, List<FaultScenario> faultScenarios) {
        Path directory = Files.createTempDirectory('v3-executor-package')
        Path workloadPath = directory.resolve('workload-catalog.jsonl')
        Path faultPath = directory.resolve('fault-scenario-catalog.jsonl')
        Path accountingPath = directory.resolve('scenario-space-accounting.json')
        Path rejectedPath = directory.resolve('workload-catalog-rejected-inputs.jsonl')
        Path manifestPath = directory.resolve('scenario-catalog-manifest.json')
        Files.write(workloadPath, [MAPPER.writeValueAsString(workload)])
        Files.write(faultPath, faultScenarios.collect { MAPPER.writeValueAsString(it) })
        Files.writeString(accountingPath, MAPPER.writeValueAsString([schemaVersion: ScenarioSpaceAccountingReport.SCHEMA_VERSION]))
        Files.writeString(rejectedPath, '')
        def manifest = new ScenarioCatalogManifest(
                ScenarioCatalogManifest.SCHEMA_VERSION, '2026-07-20T00:00:00Z', new ScenarioGeneratorConfig(),
                'TEST', 'TEST', 20, 'TEST', [], [:], [],
                artifact('WORKLOAD_CATALOG', WorkloadPlan.SCHEMA_VERSION, workloadPath, 1),
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

    private static class TrackingSagaUnitOfWorkService extends SagaUnitOfWorkService {
        Map<String, Integer> commitCounts = [:].withDefault { 0 }
        String failCommitFor

        TrackingSagaUnitOfWorkService(Map values = [:]) {
            this.failCommitFor = values.failCommitFor
        }

        @Override
        void commit(SagaUnitOfWork unitOfWork) {
            commitCounts[unitOfWork.functionalityName] = commitCounts[unitOfWork.functionalityName] + 1
            if (unitOfWork.functionalityName == failCommitFor) {
                throw new IllegalStateException('fixture commit failure')
            }
        }
    }
}
