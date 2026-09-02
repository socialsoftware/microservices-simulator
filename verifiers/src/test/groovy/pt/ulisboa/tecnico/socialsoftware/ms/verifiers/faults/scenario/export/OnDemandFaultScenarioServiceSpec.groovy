package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.*
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence
import spock.lang.Specification
import spock.lang.Unroll

import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger

class OnDemandFaultScenarioServiceSpec extends Specification {
    def 'production writer emits the exact executable shape for setup schedules faults and requests'() {
        given:
        def source = executableShapeWorkload(true)
        def provider = executableShapeWorkload(false)
        def scenario = RecoveryScheduleGenerator.generate(source, '0000', 1).faultScenarios().first()
        def fixture = CurrentPackageFixture.write([source, provider], [scenario], 1)

        when:
        def current = new ScenarioCatalogPackageReader().readCurrent(fixture.manifest)
        def sourceRecord = current.workloadRecords().find { it.path('id').asText() == fixture.workloads[0].deterministicId() }
        def providerRecord = current.workloadRecords().find { it.path('id').asText() == fixture.workloads[1].deterministicId() }
        def sourceSetup = current.setupRecords().find { it.path('id').asText() == sourceRecord.path('setup').asText() }
        def providerSetup = current.setupRecords().find { it.path('id').asText() == providerRecord.path('setup').asText() }
        def fault = current.faultScenarioRecords().first()

        then:
        sourceSetup.path('kind').asText() == 'sourceDerived'
        sourceSetup.path('actions')*.path('call')*.asText() == ['example.Fixture#create():java.lang.Long']
        sourceSetup.path('bindings')*.path('input')*.asText() == [fixture.workloads[0].acceptedInputs().first().deterministicId()]
        providerSetup.path('kind').asText() == 'providerBacked'
        providerSetup.path('provider').asText() == 'fixture-provider@1'
        sourceRecord.path('schedule')*.path('kind')*.asText() == ['step', 'event', 'step', 'step', 'step']
        sourceRecord.path('schedule')[1].path('triggeringStep').asText() == 's1'
        fault.path('actions')*.fieldNames()*.next() == ['step', 'event', 'step', 'step', 'step']
        current.requestRecords().isEmpty()
        fixture.requests.bytes.length == 0
    }

    def 'writer and reader retain exact occurrence suffixes for repeated same-name Saga steps'() {
        given:
        def workload = repeatedNameWorkload()
        def scenario = RecoveryScheduleGenerator.generate(workload, '01', 20).faultScenarios()
                .find { it.actions()*.kind().contains(FaultScenarioActionKind.COMPENSATION) }
        def fixture = CurrentPackageFixture.write([workload], [scenario], 20)

        when:
        def raw = new ScenarioCatalogPackageReader().readCurrent(fixture.manifest)
        def execution = new ScenarioCatalogPackageReader().readCurrentForExecution(fixture.manifest)
        def projected = execution.workloadPlans().first()
        def projectedScenario = execution.faultScenarios().first()

        then:
        raw.workloadRecords().first().path('schedule')*.path('sagaStep')*.asText() == ['same#0', 'same#1']
        projected.forwardSchedule()*.stepId() == ['example.RepeatedSaga::same#0', 'example.RepeatedSaga::same#1']
        def compensation = projectedScenario.actions().find { it.kind() == FaultScenarioActionKind.COMPENSATION }
        def completedForward = projectedScenario.actions().find { action ->
            action.kind() == FaultScenarioActionKind.FORWARD &&
                    action.occurrenceId() == compensation.occurrenceId()
        }
        completedForward != null
        projected.compensationCheckpoints().find {
            it.deterministicId() == compensation.sourceCompensationCheckpointId()
        }.stepId() == 'example.RepeatedSaga::same#0'
    }

    def 'smaller then larger cap adds only absent schedules and deduplicates exact identity'() {
        given:
        def workload = workload()
        def generated = new WorkloadGenerationResult(WorkloadPlan.SCHEMA_VERSION,
                new ScenarioGeneratorConfig(), [workload], [], [:], [])
        def eager = EagerFaultScenarioGenerator.generate(generated, new RecoveryScheduleCap(3))
        def fixture = CurrentPackageFixture.write([workload], eager.faultScenarios(), 3)
        def service = new OnDemandFaultScenarioService()

        when:
        def smaller = service.request(new OnDemandFaultScenarioRequest(fixture.manifest, workload.deterministicId(), '0011', '1'))
        def afterSmall = new ScenarioCatalogPackageReader().readCurrent(fixture.manifest)
        def idsAfterSmall = afterSmall.faultScenarioRecords()*.path('id')*.asText().toSet()
        def larger = service.request(new OnDemandFaultScenarioRequest(fixture.manifest, workload.deterministicId(), '0011', '3'))
        def repeated = service.request(new OnDemandFaultScenarioRequest(fixture.manifest, workload.deterministicId(), '0011', '3'))
        def current = new ScenarioCatalogPackageReader().readCurrent(fixture.manifest)
        def idsAfterLarge = current.faultScenarioRecords()*.path('id')*.asText().toSet()

        then:
        smaller.status() == OnDemandFaultScenarioResult.Status.PERSISTED
        smaller.writtenScheduleCount() == 1
        larger.status() == OnDemandFaultScenarioResult.Status.PERSISTED
        larger.writtenScheduleCount() == 3
        larger.addedFaultScenarioCount() == idsAfterLarge.size() - idsAfterSmall.size()
        idsAfterLarge.containsAll(idsAfterSmall)
        repeated.status() == OnDemandFaultScenarioResult.Status.DEDUPLICATED
        current.requestRecords()*.path('effectiveRecoveryScheduleCap')*.asInt() == [1, 3]
        current.requestRecords().every { !it.has('id') }
        current.accounting().path('faultScenarios').path('initial') == afterSmall.accounting().path('faultScenarios').path('initial')
        current.accounting().path('faultScenarios').path('current').path('written').asInt() == idsAfterLarge.size()
    }

    def 'default cap comes from accounting and a failed request is atomic'() {
        given:
        def workload = workload()
        def fixture = CurrentPackageFixture.write([workload], [], 2)
        def before = snapshot(fixture)

        when:
        def invalid = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, workload.deterministicId(), 'x011', null))

        then:
        invalid.status() == OnDemandFaultScenarioResult.Status.REJECTED
        snapshot(fixture) == before

        when:
        def persisted = new OnDemandFaultScenarioService().request(new OnDemandFaultScenarioRequest(
                fixture.manifest, workload.deterministicId(), '0011', null))

        then:
        persisted.status() == OnDemandFaultScenarioResult.Status.PERSISTED
        persisted.recoveryScheduleCap() == 2
        new ScenarioCatalogPackageReader().readCurrent(fixture.manifest)
                .requestRecords().first().path('effectiveRecoveryScheduleCap').asInt() == 2
    }

    def 'missing manifest is rejected as a current package request'() {
        expect:
        new OnDemandFaultScenarioService().request(null).diagnostics().first().message() ==
                'A current scenario package manifest path is required'
    }

    def 'process local serialization uses one package identity for aliased manifest paths'() {
        given:
        def workload = workload()
        def fixture = CurrentPackageFixture.write([workload], [], 2)
        def aliasedManifest = fixture.manifest.parent.resolve('unused').resolve('..').resolve(fixture.manifest.fileName)
        def firstAcquired = new CountDownLatch(1)
        def releaseFirst = new CountDownLatch(1)
        def opens = new AtomicInteger()
        def closes = new AtomicInteger()
        def provider = { Path ignored ->
            def sequence = opens.incrementAndGet()
            [acquire: {
                if (sequence == 1) {
                    firstAcquired.countDown()
                    assert releaseFirst.await(5, TimeUnit.SECONDS)
                }
            }, close: { closes.incrementAndGet() }] as OnDemandFaultScenarioService.PackageLockHandle
        } as OnDemandFaultScenarioService.PackageLockProvider
        def service = new OnDemandFaultScenarioService(provider)
        def request = new OnDemandFaultScenarioRequest(fixture.manifest, workload.deterministicId(), '0011', '2')
        def aliasRequest = new OnDemandFaultScenarioRequest(aliasedManifest, workload.deterministicId(), '0011', '2')
        def executor = Executors.newFixedThreadPool(2)

        when:
        def first = executor.submit({ service.request(request) } as Callable<OnDemandFaultScenarioResult>)
        assert firstAcquired.await(5, TimeUnit.SECONDS)
        def second = executor.submit({ service.request(aliasRequest) } as Callable<OnDemandFaultScenarioResult>)
        try {
            try {
                second.get(200, TimeUnit.MILLISECONDS)
                assert false: 'aliased request entered while the first package request still held the process-local lock'
            } catch (TimeoutException expected) {
                // Expected: the canonical package identity serializes the aliased request.
            }
            assert opens.get() == 1
        } finally {
            releaseFirst.countDown()
        }
        def results = [first.get(10, TimeUnit.SECONDS), second.get(10, TimeUnit.SECONDS)]
        executor.shutdownNow()

        then:
        results*.status().toSet() == [OnDemandFaultScenarioResult.Status.PERSISTED,
                                     OnDemandFaultScenarioResult.Status.DEDUPLICATED] as Set
        opens.get() == 2
        closes.get() == 2
    }

    def 'NIO package lock is acquired at the canonical package path and released after an aliased request'() {
        given:
        def workload = workload()
        def fixture = CurrentPackageFixture.write([workload], [], 1)
        def aliasedManifest = fixture.manifest.parent.resolve('unused').resolve('..').resolve(fixture.manifest.fileName)
        def observed = []
        def provider = new OnDemandFaultScenarioService.NioPackageLockProvider({ Path path -> observed << path })

        when:
        def result = new OnDemandFaultScenarioService(provider).request(new OnDemandFaultScenarioRequest(
                aliasedManifest, workload.deterministicId(), '0011', '1'))
        def lockPath = fixture.manifest.parent.toRealPath().resolve(OnDemandFaultScenarioService.PACKAGE_LOCK_FILE_NAME)
        def channel = FileChannel.open(lockPath, StandardOpenOption.WRITE)
        def reacquired = channel.tryLock()

        then:
        result.status() == OnDemandFaultScenarioResult.Status.PERSISTED
        observed == [lockPath]
        Files.isRegularFile(lockPath)
        reacquired != null

        cleanup:
        reacquired?.close()
        channel?.close()
    }

    @Unroll
    def 'package lock #failurePoint failure is contained and closes any opened resource'() {
        given:
        def workload = workload()
        def fixture = CurrentPackageFixture.write([workload], [], 1)
        def closes = new AtomicInteger()
        def provider = { Path ignored ->
            if (failurePoint == 'resource') throw new IOException('resource failure')
            [acquire: {
                if (failurePoint == 'acquisition') throw new IOException('acquisition failure')
            }, close: {
                closes.incrementAndGet()
                if (failurePoint == 'release') throw new IOException('release failure')
            }] as OnDemandFaultScenarioService.PackageLockHandle
        } as OnDemandFaultScenarioService.PackageLockProvider

        when:
        def result = new OnDemandFaultScenarioService(provider).request(new OnDemandFaultScenarioRequest(
                fixture.manifest, workload.deterministicId(), '0011', '1'))

        then:
        result.status() == expectedStatus
        result.diagnostics()*.code() == expectedDiagnostics
        closes.get() == expectedCloses
        new ScenarioCatalogPackageReader().readCurrent(fixture.manifest) != null

        where:
        failurePoint  || expectedStatus                                         | expectedDiagnostics       | expectedCloses
        'resource'    || OnDemandFaultScenarioResult.Status.PERSISTENCE_FAILED | ['PACKAGE_LOCK_FAILED']   | 0
        'acquisition' || OnDemandFaultScenarioResult.Status.PERSISTENCE_FAILED | ['PACKAGE_LOCK_FAILED']   | 1
        'release'     || OnDemandFaultScenarioResult.Status.PERSISTED          | []                        | 1
    }

    def 'atomic move fallback publishes a valid current revision'() {
        given:
        def workload = workload()
        def fixture = CurrentPackageFixture.write([workload], [], 1)
        def atomicAttempts = new AtomicInteger()
        def fallbacks = new AtomicInteger()
        def mover = [atomicMove: { Path source, Path target ->
            atomicAttempts.incrementAndGet()
            throw new AtomicMoveNotSupportedException(source.toString(), target.toString(), 'fixture')
        }, fallbackMove: { Path source, Path target ->
            fallbacks.incrementAndGet()
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
        }] as OnDemandFaultScenarioService.FileMover
        def service = operationalService({ ignored -> } as OnDemandFaultScenarioService.FailureInjector, mover)

        when:
        def result = service.request(new OnDemandFaultScenarioRequest(
                fixture.manifest, workload.deterministicId(), '0011', '1'))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.PERSISTED
        atomicAttempts.get() == 4
        fallbacks.get() == 4
        new ScenarioCatalogPackageReader().readCurrent(fixture.manifest).requestRecords().size() == 1
    }

    def 'staging cleanup failure does not invalidate an already published current revision'() {
        given:
        def workload = workload()
        def fixture = CurrentPackageFixture.write([workload], [], 1)
        def cleanupAttempts = new AtomicInteger()
        def cleaner = { Path ignored ->
            cleanupAttempts.incrementAndGet()
            throw new IOException('cleanup failure')
        } as OnDemandFaultScenarioService.TemporaryFileCleaner
        def service = new OnDemandFaultScenarioService(
                generator(), { ignored -> } as OnDemandFaultScenarioService.FailureInjector,
                new OnDemandFaultScenarioService.NioFileMover(), cleaner)

        when:
        def result = service.request(new OnDemandFaultScenarioRequest(
                fixture.manifest, workload.deterministicId(), '0011', '1'))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.PERSISTED
        cleanupAttempts.get() == 5
        new ScenarioCatalogPackageReader().readCurrent(fixture.manifest).requestRecords().size() == 1
        hasFile(fixture.manifest.parent) { it.fileName.toString().startsWith('.current-validation-') }
    }

    @Unroll
    def 'failure at current publication boundary #boundary preserves every mutable package byte'() {
        given:
        def workload = workload()
        def fixture = CurrentPackageFixture.write([workload], [], 2)
        def before = snapshot(fixture)
        def injector = { OnDemandFaultScenarioService.Boundary actual ->
            if (actual == boundary) throw new IOException("failure at ${boundary}".toString())
        } as OnDemandFaultScenarioService.FailureInjector
        def service = operationalService(injector, new OnDemandFaultScenarioService.NioFileMover())

        when:
        def result = service.request(new OnDemandFaultScenarioRequest(
                fixture.manifest, workload.deterministicId(), '0011', '2'))

        then:
        result.status() == OnDemandFaultScenarioResult.Status.PERSISTENCE_FAILED
        result.diagnostics()*.code() == ['PACKAGE_REVISION_FAILED']
        snapshot(fixture) == before
        new ScenarioCatalogPackageReader().readCurrent(fixture.manifest).requestRecords().isEmpty()
        !hasFile(fixture.manifest.parent) { it.fileName.toString().endsWith('.tmp') }

        where:
        boundary << OnDemandFaultScenarioService.Boundary.values()
    }

    def 'request and fault record ordering is deterministic across equivalent current packages'() {
        given:
        def workload = workload()
        def left = CurrentPackageFixture.write([workload], [], 2)
        def right = CurrentPackageFixture.write([workload], [], 2)
        def requests = [['1010', '1'], ['0011', null], ['0101', '2']]

        when:
        [left, right].each { fixture ->
            def service = new OnDemandFaultScenarioService()
            requests.each { values ->
                def result = service.request(new OnDemandFaultScenarioRequest(
                        fixture.manifest, workload.deterministicId(), values[0], values[1]))
                assert result.status() == OnDemandFaultScenarioResult.Status.PERSISTED
            }
        }
        def leftCurrent = new ScenarioCatalogPackageReader().readCurrent(left.manifest)

        then:
        left.faultScenario.bytes == right.faultScenario.bytes
        left.requests.bytes == right.requests.bytes
        left.accounting.bytes == right.accounting.bytes
        left.manifest.bytes == right.manifest.bytes
        leftCurrent.faultScenarioRecords()*.path('id')*.asText() ==
                leftCurrent.faultScenarioRecords()*.path('id')*.asText().sort()
        leftCurrent.requestRecords()*.path('faultVector')*.asText() == requests*.get(0)
    }

    private static OnDemandFaultScenarioService operationalService(
            OnDemandFaultScenarioService.FailureInjector injector,
            OnDemandFaultScenarioService.FileMover mover) {
        new OnDemandFaultScenarioService(generator(), injector, mover)
    }

    private static OnDemandFaultScenarioService.RecoveryScheduleSource generator() {
        { WorkloadPlan plan, String vector, int cap -> RecoveryScheduleGenerator.generate(plan, vector, cap) }
                as OnDemandFaultScenarioService.RecoveryScheduleSource
    }

    private static boolean hasFile(Path directory, Closure<Boolean> predicate) {
        def stream = Files.list(directory)
        try {
            return stream.anyMatch { Path path -> predicate.call(path) }
        } finally {
            stream.close()
        }
    }

    private static Map snapshot(Map fixture) {
        [manifest: fixture.manifest.bytes, accounting: fixture.accounting.bytes,
         faults: fixture.faultScenario.bytes, requests: fixture.requests.bytes]
    }

    private static WorkloadPlan workload() {
        def inputs = ['a', 'b'].collect { owner -> input(owner) }
        def participants = ['a', 'b'].collect { owner -> new SagaInstance(owner,
                "example.${owner.toUpperCase()}Saga".toString(), "input-${owner}".toString(), []) }
        def shape = [['a', 'first'], ['b', 'first'], ['a', 'second'], ['b', 'second']]
        def schedule = shape.withIndex().collect { row, index -> new ScheduledStep("forward-${index}".toString(),
                row[0], "example.${row[0].toUpperCase()}Saga::${row[1]}".toString(), index, row[1], []) }
        def slots = schedule.withIndex().collect { step, index -> new ForwardFaultSlot("slot-${index}".toString(),
                index, step.deterministicId(), step.sagaInstanceId(), step.stepId(), step.runtimeStepName(), step.deterministicId()) }
        def checkpoints = schedule.take(2).withIndex().collect { step, index -> new CompensationCheckpoint(
                "checkpoint-${index}".toString(), index, step.sagaInstanceId(), step.deterministicId(), step.stepId(),
                step.runtimeStepName(), step.deterministicId(), CompensationEvidenceClass.EXPLICIT_COMPENSATION, [], [], []) }
        def withoutId = new WorkloadPlan(WorkloadPlan.SCHEMA_VERSION, null, ScenarioKind.MULTI_SAGA,
                WorkloadExecutionShape.SAGA_LOCAL, participants, inputs, schedule, [], slots, checkpoints, [])
        new WorkloadPlan(withoutId.schemaVersion(), ScenarioIdGenerator.workloadPlanId(withoutId), withoutId.kind(),
                withoutId.executionShape(), withoutId.participants(), withoutId.acceptedInputs(), withoutId.forwardSchedule(),
                withoutId.conflictEvidence(), withoutId.faultSlots(), withoutId.compensationCheckpoints(), withoutId.warnings())
    }

    private static WorkloadPlan repeatedNameWorkload() {
        def accepted = input('repeated', 'example.RepeatedSaga')
        def participant = new SagaInstance('repeated', 'example.RepeatedSaga', accepted.deterministicId(), [])
        def schedule = (0..1).collect { index -> new ScheduledStep("forward-${index}".toString(),
                participant.deterministicId(), "example.RepeatedSaga::same#${index}".toString(), index, 'same', []) }
        def slots = schedule.withIndex().collect { step, index -> new ForwardFaultSlot("slot-${index}".toString(),
                index, step.deterministicId(), participant.deterministicId(), step.stepId(), 'same', step.deterministicId()) }
        def checkpoints = schedule.withIndex().collect { step, index -> new CompensationCheckpoint(
                "checkpoint-${index}".toString(), index, participant.deterministicId(), step.deterministicId(),
                step.stepId(), 'same', step.deterministicId(), CompensationEvidenceClass.EXPLICIT_COMPENSATION,
                [], [], []) }
        def withoutId = new WorkloadPlan(WorkloadPlan.SCHEMA_VERSION, null, ScenarioKind.SINGLE_SAGA,
                WorkloadExecutionShape.SAGA_LOCAL, [participant], [accepted], schedule, [], slots, checkpoints, [])
        new WorkloadPlan(withoutId.schemaVersion(), ScenarioIdGenerator.workloadPlanId(withoutId), withoutId.kind(),
                withoutId.executionShape(), withoutId.participants(), withoutId.acceptedInputs(),
                withoutId.forwardSchedule(), withoutId.conflictEvidence(), withoutId.faultSlots(),
                withoutId.compensationCheckpoints(), withoutId.warnings())
    }

    private static WorkloadPlan executableShapeWorkload(boolean sourceDerived) {
        def base = workload()
        def trigger = base.forwardSchedule().first()
        def site = new EventEmissionSite(ScenarioIdGenerator.eventEmissionSiteId(
                'example.AService', 'first()', 0, 'example.CreatedEvent'),
                'example.AService', 'first()', 0, 'example.CreatedEvent', [])
        def event = new EventConsequence(null, trigger.deterministicId(), site,
                'example.CreatedEvent', 'example.EventHandling', 'handle', 'example.Handler',
                'example.EventProcessing', 'process', 'example.Facade', 'continueFlow',
                'example.DownstreamSaga', 'UNIQUE_MATCHING_SUBSCRIBER', [])
        event = new EventConsequence(ScenarioIdGenerator.eventConsequenceId(
                event.triggerScheduledStepId(), event.emissionSite(), event.eventHandlingClassFqn(),
                event.eventHandlingMethodName(), event.eventHandlerClassFqn(), event.eventProcessingClassFqn(),
                event.eventProcessingMethodName(), event.facadeClassFqn(), event.facadeMethodName(),
                event.downstreamSagaFqn(), event.deliveryPolicy()), event.triggerScheduledStepId(),
                event.emissionSite(), event.eventTypeFqn(), event.eventHandlingClassFqn(),
                event.eventHandlingMethodName(), event.eventHandlerClassFqn(), event.eventProcessingClassFqn(),
                event.eventProcessingMethodName(), event.facadeClassFqn(), event.facadeMethodName(),
                event.downstreamSagaFqn(), event.deliveryPolicy(), [])
        def normal = [NormalActionRef.forward(0, trigger.deterministicId()),
                      NormalActionRef.eventConsequence(1, event.deterministicId())]
        base.forwardSchedule().drop(1).eachWithIndex { step, index ->
            normal << NormalActionRef.forward(index + 2, step.deterministicId())
        }
        def setup = null
        def prerequisite = null
        if (sourceDerived) {
            def action = new SetupAction('setup-action-1', 0, 'fixture:1',
                    'example.Fixture#create():java.lang.Long', [], Long.name, false, [])
            def binding = new SetupParticipantBinding(base.acceptedInputs().first().deterministicId(), 0,
                    Long.name, SetupValueRecipe.actionResult(action.actionId(), Long.name), [])
            setup = new SetupPlan(SetupPlan.SCHEMA_VERSION, [action], [binding], [])
        } else {
            prerequisite = new PrerequisiteBaseline('fixture-provider', '1', [])
        }
        def withoutId = new WorkloadPlan(base.schemaVersion(), null, base.kind(), base.executionShape(),
                base.participants(), base.acceptedInputs(), base.forwardSchedule(), [event], normal,
                prerequisite, setup, base.conflictEvidence(), base.faultSlots(),
                base.compensationCheckpoints(), [])
        new WorkloadPlan(withoutId.schemaVersion(), ScenarioIdGenerator.workloadPlanId(withoutId),
                withoutId.kind(), withoutId.executionShape(), withoutId.participants(),
                withoutId.acceptedInputs(), withoutId.forwardSchedule(), withoutId.eventConsequences(),
                withoutId.normalSchedule(), withoutId.prerequisiteBaseline(), withoutId.setupPlan(),
                withoutId.conflictEvidence(), withoutId.faultSlots(), withoutId.compensationCheckpoints(), [])
    }

    private static InputVariant input(String owner, String sagaFqn = null) {
        def node = InputRecipeNode.builder('literal').executorReady(true).literalKind('integer').value(1L).targetTypeFqn(Long.name).build()
        def argument = new InputRecipeArgument(0, Long.name, InputResolutionStatus.RESOLVED, true, [], 'argument 0', node)
        new InputVariant("input-${owner}".toString(), sagaFqn ?: "example.${owner.toUpperCase()}Saga".toString(),
                'example.CurrentSpec', 'fixture', owner, null, InputRole.FEATURE_UNDER_TEST, FixtureOrigin.DIRECT_FEATURE,
                InputResolutionStatus.RESOLVED, SourceMode.SAGAS, SourceModeConfidence.TEST_CONFIGURATION, [], 'source', 'provenance',
                [], [], [:], [], new InputRecipe(InputRecipe.SCHEMA_VERSION, null, true, [], [argument]))
    }
}
