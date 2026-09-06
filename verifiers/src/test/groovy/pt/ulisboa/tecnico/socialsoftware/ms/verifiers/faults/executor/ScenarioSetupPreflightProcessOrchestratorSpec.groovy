package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import com.fasterxml.jackson.databind.ObjectMapper
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class ScenarioSetupPreflightProcessOrchestratorSpec extends Specification {
    private static final ObjectMapper MAPPER = new ObjectMapper()
    private static final Duration TEST_TIMEOUT = Duration.ofSeconds(3)
    private static final Duration TERMINATION_TIMEOUT = Duration.ofMillis(500)

    @TempDir
    Path temporaryDirectory

    def 'state-only source setup with explicit empty participant bindings is accepted after setup and startup'() {
        given:
        Path output = temporaryDirectory.resolve('state-only.json')
        def opts = options(output)
        def starter = { List<String> command, Path workingDirectory ->
            writeWorkerReport(command, opts, ['state-only'] as Set<String>, 'STATE_ONLY')
            quickProcess(0)
        } as ScenarioSetupPreflightProcessOrchestrator.WorkerProcessStarter

        when:
        int status = orchestrator(starter).run(originalArgs(output, opts.packagePath()), opts,
                new ScenarioExecutor.PreflightPlan(['state-only'] as Set<String>, ['state-only'] as Set<String>))

        then:
        status == 0
        def report = MAPPER.readValue(output.toFile(), ScenarioSetupPreflightReport)
        report.terminalStatus() == 'SUCCESS'
        report.participantCount() == 1
        def workload = report.workloads()[0]
        workload.status() == 'SETUP_READY'
        workload.sourceSetup().actions()*.status() == ['SUCCEEDED']
        workload.sourceSetup().pendingEventsCleared() == 0
        workload.sourceSetup().emptyPendingEventBaseline()
        workload.sourceSetup().failureReason() == null
        workload.sourceSetup().failureMessage() == null
        workload.sourceSetup().participantBindings().isEmpty()
        workload.participants()[0].setupReady()
        workload.participants()[0].materializationState() == 'MATERIALIZED'
        workload.participants()[0].startupState() == 'STARTUP_READY'
        report.runtimeMetadata().applicationId() == 'fixture'
        report.runtimeMetadata().springApplicationClass() == 'fixture.Application'
        report.runtimeMetadata().springProfiles() == 'test,sagas,local'
        report.runtimeMetadata().mavenProfile() == 'test-sagas'
        report.runtimeMetadata().packageManifestPath() == opts.packagePath().toAbsolutePath().normalize().toString()
        report.runtimeMetadata().faultScenarioId() == null
        !report.runtimeMetadata().dryRun()
        def sourceSetup = MAPPER.readTree(output.toFile()).path('workloads').get(0).path('sourceSetup')
        sourceSetup.has('participantBindings')
        sourceSetup.path('participantBindings').isArray()
        sourceSetup.path('participantBindings').size() == 0
        noWorkerFragments(output.parent)
    }

    def 'incomplete source setup success evidence remains rejected'() {
        given:
        Path output = temporaryDirectory.resolve("${variant}.json")
        def opts = options(output)
        def starter = { List<String> command, Path workingDirectory ->
            writeWorkerReport(command, opts, ['source-a'] as Set<String>, variant)
            quickProcess(0)
        } as ScenarioSetupPreflightProcessOrchestrator.WorkerProcessStarter

        when:
        int status = orchestrator(starter).run(originalArgs(output, opts.packagePath()), opts, sourcePlan())

        then:
        status == 1
        def report = MAPPER.readValue(output.toFile(), ScenarioSetupPreflightReport)
        report.terminalStatus() == 'SETUP_FAILED'
        report.participantCount() == 0
        report.workloads()[0].status() == 'FRESH_STATE_ISOLATION_FAILED'
        report.workloads()[0].blockers()[0].message().contains(expectedMessage)
        noWorkerFragments(output.parent)

        where:
        variant              | expectedMessage
        'UNRESOLVED_BINDING' | 'worker source setup did not complete successfully'
        'FAILED_ACTION'      | 'worker source setup did not complete successfully'
        'MISSING_ACTIONS'    | 'worker source setup did not complete successfully'
        'STARTUP_FAILED'     | 'worker participant result is inconsistent'
        'MISSING_BINDINGS'   | 'worker report source setup participant bindings are missing or malformed'
        'NULL_BINDINGS'      | 'worker report source setup participant bindings are missing or malformed'
        'NON_ARRAY_BINDINGS' | 'worker report source setup participant bindings are missing or malformed'
        'MISSING_ACTION_LIST' | 'worker source setup actions are missing or malformed'
        'NULL_ACTION_LIST'    | 'worker source setup actions are missing or malformed'
        'NON_ARRAY_ACTIONS'   | 'worker source setup actions are missing or malformed'
    }

    def 'source candidates use separate real processes while provider-backed candidates retain one batch and fragments are cleaned'() {
        given:
        def commands = []
        def processes = []
        def workingDirectories = []
        Path applicationDirectory = temporaryDirectory.resolve('relative-application')
        Files.createDirectories(applicationDirectory)
        Path relativeApplication = Path.of('').toAbsolutePath().normalize().relativize(applicationDirectory)
        Path output = temporaryDirectory.resolve('aggregate.json')
        Path relativeOutput = Path.of('').toAbsolutePath().normalize().relativize(output)
        Path relativePackage = Path.of('').toAbsolutePath().normalize().relativize(temporaryDirectory.resolve('manifest.json'))
        def options = options(relativeOutput, relativePackage, relativeApplication.toString())
        def starter = { List<String> command, Path workingDirectory ->
            commands << command
            workingDirectories << workingDirectory
            writeWorkerReport(command, options, ['source-a', 'source-b'] as Set<String>)
            Process process = quickProcess(0)
            processes << process
            process
        } as ScenarioSetupPreflightProcessOrchestrator.WorkerProcessStarter
        def plan = new ScenarioExecutor.PreflightPlan(
                ['provider-a', 'provider-b', 'source-a', 'source-b'] as Set<String>,
                ['source-a', 'source-b'] as Set<String>)

        when:
        def status = orchestrator(starter).run(originalArgs(relativeOutput, relativePackage), options, plan)

        then:
        status == 0
        processes.size() == 3
        processes.every { !it.isAlive() }
        commands.collect { selectedIds(it) } == [
                ['provider-a', 'provider-b'], ['source-a'], ['source-b']
        ]
        commands.every { Path.of(argument(it, '--package-path')).isAbsolute() }
        commands.every { Path.of(argument(it, '--output-path')).isAbsolute() }
        workingDirectories == [applicationDirectory.toAbsolutePath().normalize()] * 3
        def aggregate = MAPPER.readValue(output.toFile(), ScenarioSetupPreflightReport)
        aggregate.terminalStatus() == 'SUCCESS'
        aggregate.packageManifestPath() == temporaryDirectory.resolve('manifest.json').toString()
        aggregate.workloads()*.workloadPlanId() == ['provider-a', 'provider-b', 'source-a', 'source-b']
        noWorkerFragments(output.parent)
    }

    def 'timed out worker is terminated and reported as fresh-state isolation failure'() {
        given:
        AtomicReference<Process> child = new AtomicReference<>()
        def starter = { List<String> command, Path workingDirectory ->
            Process process = stubbornProcess()
            child.set(process)
            process
        } as ScenarioSetupPreflightProcessOrchestrator.WorkerProcessStarter
        Path output = temporaryDirectory.resolve('timeout.json')

        when:
        int status = orchestrator(starter, Duration.ofMillis(100)).run(
                originalArgs(output, temporaryDirectory.resolve('manifest.json')),
                options(output), sourcePlan())

        then:
        status == 1
        !child.get().isAlive()
        def report = MAPPER.readValue(output.toFile(), ScenarioSetupPreflightReport)
        report.workloads()[0].status() == 'FRESH_STATE_ISOLATION_FAILED'
        report.workloads()[0].blockers()[0].message().contains('TimeoutException')
        noWorkerFragments(output.parent)
    }

    def 'interrupt terminates worker and restores parent thread interrupt status'() {
        given:
        CountDownLatch started = new CountDownLatch(1)
        AtomicReference<Process> child = new AtomicReference<>()
        AtomicBoolean interruptedOnReturn = new AtomicBoolean(false)
        AtomicReference<Throwable> threadFailure = new AtomicReference<>()
        Path output = temporaryDirectory.resolve('interrupted.json')
        def starter = { List<String> command, Path workingDirectory ->
            Process process = sleepingProcess()
            child.set(process)
            started.countDown()
            process
        } as ScenarioSetupPreflightProcessOrchestrator.WorkerProcessStarter
        Thread parent = new Thread({
            try {
                orchestrator(starter).run(originalArgs(output, temporaryDirectory.resolve('manifest.json')),
                        options(output), sourcePlan())
                interruptedOnReturn.set(Thread.currentThread().isInterrupted())
            } catch (Throwable failure) {
                threadFailure.set(failure)
            }
        })

        when:
        parent.start()
        assert started.await(2, TimeUnit.SECONDS)
        parent.interrupt()
        parent.join(3000)

        then:
        !parent.isAlive()
        threadFailure.get() == null
        interruptedOnReturn.get()
        !child.get().isAlive()
        MAPPER.readValue(output.toFile(), ScenarioSetupPreflightReport)
                .workloads()[0].status() == 'FRESH_STATE_ISOLATION_FAILED'
        noWorkerFragments(output.parent)
    }

    def 'nonzero worker exit rejects a success-looking report without ingestion and cleans fragments'() {
        given:
        Path output = temporaryDirectory.resolve('nonzero.json')
        def opts = options(output)
        def starter = { List<String> command, Path workingDirectory ->
            writeWorkerReport(command, opts, ['source-a'] as Set<String>)
            quickProcess(7)
        } as ScenarioSetupPreflightProcessOrchestrator.WorkerProcessStarter

        when:
        int status = orchestrator(starter).run(originalArgs(output, opts.packagePath()), opts, sourcePlan())

        then:
        status == 1
        def report = MAPPER.readValue(output.toFile(), ScenarioSetupPreflightReport)
        report.participantCount() == 0
        report.workloads()[0].status() == 'FRESH_STATE_ISOLATION_FAILED'
        report.workloads()[0].blockers()[0].message().contains('unexpected worker exit status 7')
        noWorkerFragments(output.parent)
    }

    def 'expected worker failure exit preserves validated source setup diagnosis'() {
        given:
        Path output = temporaryDirectory.resolve('expected-failure.json')
        def opts = options(output)
        def starter = { List<String> command, Path workingDirectory ->
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(
                    Path.of(argument(command, '--output-path')).toFile(),
                    failedSourceWorkerReport(opts))
            quickProcess(1)
        } as ScenarioSetupPreflightProcessOrchestrator.WorkerProcessStarter

        when:
        int status = orchestrator(starter).run(originalArgs(output, opts.packagePath()), opts, sourcePlan())

        then:
        status == 1
        def report = MAPPER.readValue(output.toFile(), ScenarioSetupPreflightReport)
        report.terminalStatus() == 'SETUP_FAILED'
        report.participantCount() == 1
        report.workloads()[0].status() == 'SETUP_METHOD_NOT_AUTHORIZED'
        report.workloads()[0].sourceSetup().status() == 'FAILED'
        report.workloads()[0].sourceSetup().failureReason() == 'SETUP_METHOD_NOT_AUTHORIZED'
        report.workloads()[0].blockers()[0].reason() == 'SETUP_METHOD_NOT_AUTHORIZED'
        noWorkerFragments(output.parent)
    }

    def 'worker report and exit status must describe the same outcome'() {
        given:
        Path output = temporaryDirectory.resolve("inconsistent-${variant}.json")
        def opts = options(output)
        def starter = { List<String> command, Path workingDirectory ->
            def report = variant == 'failure-with-zero'
                    ? failedSourceWorkerReport(opts)
                    : workerReport(['source-a'], opts, ['source-a'] as Set<String>)
            MAPPER.writeValue(Path.of(argument(command, '--output-path')).toFile(), report)
            quickProcess(workerExit)
        } as ScenarioSetupPreflightProcessOrchestrator.WorkerProcessStarter

        when:
        int status = orchestrator(starter).run(originalArgs(output, opts.packagePath()), opts, sourcePlan())

        then:
        status == 1
        def report = MAPPER.readValue(output.toFile(), ScenarioSetupPreflightReport)
        report.workloads()[0].status() == 'FRESH_STATE_ISOLATION_FAILED'
        report.workloads()[0].blockers()[0].message().contains('is inconsistent with terminal status')

        where:
        variant             | workerExit
        'failure-with-zero' | 0
        'success-with-one'  | 1
    }

    def 'inconsistent expected failure evidence remains an isolation failure'() {
        given:
        Path output = temporaryDirectory.resolve("invalid-failure-${variant}.json")
        def opts = options(output)
        def starter = { List<String> command, Path workingDirectory ->
            def valid = failedSourceWorkerReport(opts)
            def workload = valid.workloads()[0]
            def sourceSetup = workload.sourceSetup()
            if (variant in ['wrong-reason', 'unknown-reason']) {
                sourceSetup = new ScenarioExecutionReport.SourceSetup(
                        'FAILED', sourceSetup.durationNanos(), sourceSetup.pendingEventsCleared(),
                        sourceSetup.emptyPendingEventBaseline(), sourceSetup.actions(),
                        sourceSetup.participantBindings(),
                        variant == 'unknown-reason' ? 'MADE_UP' : 'DIFFERENT_REASON',
                        sourceSetup.failureMessage())
            }
            if (variant == 'preaction-progress') {
                def action = ready('source-a', true).sourceSetup().actions()[0]
                sourceSetup = new ScenarioExecutionReport.SourceSetup(
                        'FAILED', sourceSetup.durationNanos(), sourceSetup.pendingEventsCleared(),
                        sourceSetup.emptyPendingEventBaseline(), [action], [],
                        sourceSetup.failureReason(), sourceSetup.failureMessage())
            }
            def blockers = variant == 'missing-blocker' ? [] : workload.blockers()
            def status = variant == 'unknown-reason' ? 'MADE_UP' : workload.status()
            if (variant == 'unknown-reason') {
                blockers = [new ScenarioExecutionReport.Blocker(
                        'source-a', null, null, null, null, null, 'MADE_UP', 'invented')]
            }
            def invalidWorkload = new ScenarioSetupPreflightReport.WorkloadResult(
                    workload.workloadPlanId(), status, workload.setupDurationNanos(),
                    sourceSetup, workload.participants(), blockers)
            def invalid = new ScenarioSetupPreflightReport(
                    valid.schemaVersion(), valid.preflightAttemptId(), valid.terminalStatus(),
                    valid.packageManifestPath(), valid.candidateSelection(), valid.candidateCount(),
                    valid.participantCount(), valid.setupDurationNanos(), valid.runtimeMetadata(),
                    [invalidWorkload])
            MAPPER.writeValue(Path.of(argument(command, '--output-path')).toFile(), invalid)
            quickProcess(1)
        } as ScenarioSetupPreflightProcessOrchestrator.WorkerProcessStarter

        when:
        int status = orchestrator(starter).run(originalArgs(output, opts.packagePath()), opts, sourcePlan())

        then:
        status == 1
        def report = MAPPER.readValue(output.toFile(), ScenarioSetupPreflightReport)
        report.workloads()[0].status() == 'FRESH_STATE_ISOLATION_FAILED'

        where:
        variant << ['wrong-reason', 'unknown-reason', 'missing-blocker', 'preaction-progress']
    }

    def 'phase failure blockers must agree with participant state'() {
        given:
        Path output = temporaryDirectory.resolve('wrong-phase-blocker.json')
        def opts = options(output)
        def starter = { List<String> command, Path workingDirectory ->
            def wrongBlocker = new ScenarioExecutionReport.Blocker(
                    'source-a', null, 'source-a-input', null, null, null,
                    'STARTUP_FAILED', 'wrong phase')
            def participant = new ScenarioSetupPreflightReport.ParticipantResult(
                    'source-a-participant', 'fixture.Saga', 'source-a-input', false,
                    'MATERIALIZATION_FAILED', 'NOT_ATTEMPTED', [wrongBlocker])
            def workload = new ScenarioSetupPreflightReport.WorkloadResult(
                    'source-a', 'MATERIALIZATION_FAILED', 1L,
                    ready('source-a', true).sourceSetup(), [participant], [wrongBlocker])
            def valid = workerReport(['source-a'], opts, ['source-a'] as Set<String>)
            def report = new ScenarioSetupPreflightReport(
                    valid.schemaVersion(), valid.preflightAttemptId(), 'SETUP_FAILED',
                    valid.packageManifestPath(), valid.candidateSelection(), 1, 1, 1L,
                    valid.runtimeMetadata(), [workload])
            MAPPER.writeValue(Path.of(argument(command, '--output-path')).toFile(), report)
            quickProcess(1)
        } as ScenarioSetupPreflightProcessOrchestrator.WorkerProcessStarter

        expect:
        orchestrator(starter).run(originalArgs(output, opts.packagePath()), opts, sourcePlan()) == 1
        MAPPER.readValue(output.toFile(), ScenarioSetupPreflightReport)
                .workloads()[0].status() == 'FRESH_STATE_ISOLATION_FAILED'
    }

    def 'failed binding is omitted rather than reported as unresolved progress'() {
        given:
        Path output = temporaryDirectory.resolve('unresolved-binding-progress.json')
        def opts = options(output)
        def starter = { List<String> command, Path workingDirectory ->
            def base = failedSourceWorkerReport(opts)
            def readySetup = ready('source-a', true).sourceSetup()
            def resolved = readySetup.participantBindings()[0]
            def impossible = new ScenarioExecutionReport.SetupParticipantBindingOutcome(
                    resolved.inputVariantId(), resolved.argumentIndex(), resolved.sourceActionId(),
                    resolved.propertyName(), 'UNRESOLVED', null, null, null)
            def reason = 'SETUP_PARTICIPANT_MATERIALIZATION_FAILED'
            def message = 'binding materialization failed'
            def sourceSetup = new ScenarioExecutionReport.SourceSetup(
                    'FAILED', 1L, 0L, false, readySetup.actions(), [impossible], reason, message)
            def blocker = new ScenarioExecutionReport.Blocker(
                    'source-a', null, null, null, null, null, reason, message)
            def workload = new ScenarioSetupPreflightReport.WorkloadResult(
                    'source-a', reason, 1L, sourceSetup, base.workloads()[0].participants(), [blocker])
            def report = new ScenarioSetupPreflightReport(
                    base.schemaVersion(), base.preflightAttemptId(), 'SETUP_FAILED',
                    base.packageManifestPath(), base.candidateSelection(), 1, 1, 1L,
                    base.runtimeMetadata(), [workload])
            MAPPER.writeValue(Path.of(argument(command, '--output-path')).toFile(), report)
            quickProcess(1)
        } as ScenarioSetupPreflightProcessOrchestrator.WorkerProcessStarter

        expect:
        orchestrator(starter).run(originalArgs(output, opts.packagePath()), opts, sourcePlan()) == 1
        MAPPER.readValue(output.toFile(), ScenarioSetupPreflightReport)
                .workloads()[0].status() == 'FRESH_STATE_ISOLATION_FAILED'
    }

    def 'mismatched participant identity cannot preserve a worker diagnosis'() {
        given:
        Path output = temporaryDirectory.resolve('wrong-participant.json')
        def opts = options(output)
        def starter = { List<String> command, Path workingDirectory ->
            def valid = failedSourceWorkerReport(opts)
            def workload = valid.workloads()[0]
            def participant = workload.participants()[0]
            def wrong = new ScenarioSetupPreflightReport.ParticipantResult(
                    participant.sagaInstanceId(), 'fixture.OtherSaga', participant.inputVariantId(),
                    participant.setupReady(), participant.materializationState(), participant.startupState(),
                    participant.blockers())
            def changed = new ScenarioSetupPreflightReport.WorkloadResult(
                    workload.workloadPlanId(), workload.status(), workload.setupDurationNanos(),
                    workload.sourceSetup(), [wrong], workload.blockers())
            def invalid = new ScenarioSetupPreflightReport(
                    valid.schemaVersion(), valid.preflightAttemptId(), valid.terminalStatus(),
                    valid.packageManifestPath(), valid.candidateSelection(), valid.candidateCount(),
                    valid.participantCount(), valid.setupDurationNanos(), valid.runtimeMetadata(), [changed])
            MAPPER.writeValue(Path.of(argument(command, '--output-path')).toFile(), invalid)
            quickProcess(1)
        } as ScenarioSetupPreflightProcessOrchestrator.WorkerProcessStarter

        expect:
        orchestrator(starter).run(originalArgs(output, opts.packagePath()), opts, sourcePlan()) == 1
        MAPPER.readValue(output.toFile(), ScenarioSetupPreflightReport)
                .workloads()[0].status() == 'FRESH_STATE_ISOLATION_FAILED'
    }

    def 'invalid worker envelope cannot produce aggregate success and fragments are cleaned'() {
        given:
        Path output = temporaryDirectory.resolve('invalid-envelope.json')
        def opts = options(output)
        def starter = { List<String> command, Path workingDirectory ->
            Path workerOutput = Path.of(argument(command, '--output-path'))
            def valid = workerReport(selectedIds(command), opts, ['source-a'] as Set<String>)
            def invalid = new ScenarioSetupPreflightReport(
                    'microservices-simulator.scenario-setup-preflight-report.v0', valid.preflightAttemptId(),
                    valid.terminalStatus(), valid.packageManifestPath(), valid.candidateSelection(),
                    valid.candidateCount(), valid.participantCount(), valid.setupDurationNanos(),
                    valid.runtimeMetadata(), valid.workloads())
            MAPPER.writeValue(workerOutput.toFile(), invalid)
            quickProcess(0)
        } as ScenarioSetupPreflightProcessOrchestrator.WorkerProcessStarter

        when:
        int status = orchestrator(starter).run(originalArgs(output, opts.packagePath()), opts, sourcePlan())

        then:
        status == 1
        def report = MAPPER.readValue(output.toFile(), ScenarioSetupPreflightReport)
        report.workloads()[0].status() == 'FRESH_STATE_ISOLATION_FAILED'
        report.workloads()[0].blockers()[0].message().contains('unexpected worker report schema')
        noWorkerFragments(output.parent)
    }

    def 'worker launch failure is reported and worker directory is recursively cleaned'() {
        given:
        Path output = temporaryDirectory.resolve('launch-failure.json')
        def starter = { List<String> command, Path workingDirectory ->
            throw new IOException('fresh process unavailable')
        } as ScenarioSetupPreflightProcessOrchestrator.WorkerProcessStarter

        when:
        int status = orchestrator(starter).run(
                originalArgs(output, temporaryDirectory.resolve('manifest.json')),
                options(output), sourcePlan())

        then:
        status == 1
        MAPPER.readValue(output.toFile(), ScenarioSetupPreflightReport)
                .workloads()[0].blockers()[0].message().contains('fresh process unavailable')
        noWorkerFragments(output.parent)
    }

    private ScenarioSetupPreflightProcessOrchestrator orchestrator(
            ScenarioSetupPreflightProcessOrchestrator.WorkerProcessStarter starter,
            Duration timeout = TEST_TIMEOUT) {
        new ScenarioSetupPreflightProcessOrchestrator(starter, MAPPER, timeout, TERMINATION_TIMEOUT)
    }

    private ScenarioSetupPreflightOptions options(Path output,
                                                  Path packagePath = temporaryDirectory.resolve('manifest.json'),
                                                  String applicationBase = null) {
        new ScenarioSetupPreflightOptions(packagePath, output, applicationBase,
                'fixture', 'fixture.Application', 'test,sagas,local', 'test-sagas')
    }

    private static ScenarioExecutor.PreflightPlan sourcePlan() {
        def participant = new ScenarioExecutor.ExpectedParticipant(
                'source-a-participant', 'fixture.Saga', 'source-a-input')
        def action = new ScenarioExecutor.ExpectedSetupAction(
                'action-1', 0, 'fixture#create', 'java.lang.Object')
        def binding = new ScenarioExecutor.ExpectedSetupBinding(
                'source-a-input', 0, 'action-1', 'aggregateId')
        def workload = new ScenarioExecutor.ExpectedWorkload(
                [participant], [action], [binding])
        new ScenarioExecutor.PreflightPlan(['source-a'] as Set<String>, ['source-a'] as Set<String>,
                ['source-a': workload])
    }

    private static String[] originalArgs(Path output, Path packagePath) {
        ['--preflight', '--spring-application-class', 'fixture.Application',
         '--package-path', packagePath.toString(), '--output-path', output.toString()] as String[]
    }

    private static void writeWorkerReport(List<String> command,
                                          ScenarioSetupPreflightOptions options,
                                          Set<String> sourceIds,
                                          String evidence = 'RESOLVED') {
        Path output = Path.of(argument(command, '--output-path'))
        def report = workerReport(selectedIds(command), options, sourceIds, evidence)
        if (evidence in ['MISSING_BINDINGS', 'NULL_BINDINGS', 'NON_ARRAY_BINDINGS',
                         'MISSING_ACTION_LIST', 'NULL_ACTION_LIST', 'NON_ARRAY_ACTIONS']) {
            def envelope = MAPPER.valueToTree(report)
            def sourceSetup = envelope.path('workloads').get(0).path('sourceSetup')
            if (evidence == 'MISSING_BINDINGS') sourceSetup.remove('participantBindings')
            if (evidence == 'NULL_BINDINGS') sourceSetup.putNull('participantBindings')
            if (evidence == 'NON_ARRAY_BINDINGS') sourceSetup.put('participantBindings', 'malformed')
            if (evidence == 'MISSING_ACTION_LIST') sourceSetup.remove('actions')
            if (evidence == 'NULL_ACTION_LIST') sourceSetup.putNull('actions')
            if (evidence == 'NON_ARRAY_ACTIONS') sourceSetup.put('actions', 'malformed')
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), envelope)
        } else {
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(output.toFile(), report)
        }
    }

    private static ScenarioSetupPreflightReport workerReport(List<String> ids,
                                                              ScenarioSetupPreflightOptions options,
                                                              Set<String> sourceIds,
                                                              String evidence = 'RESOLVED') {
        String packagePath = options.packagePath().toAbsolutePath().normalize().toString()
        def workloads = ids.collect { ready(it, sourceIds.contains(it), evidence) }
        def metadata = new ScenarioExecutionReport.RuntimeMetadata(
                options.applicationBase(), options.applicationId(), options.springApplicationClass(),
                options.springProfiles(), options.mavenProfile(), packagePath, null,
                'STATIC_MATERIALIZABILITY_CANDIDATE_PREFLIGHT', false)
        new ScenarioSetupPreflightReport(ScenarioSetupPreflightReport.SCHEMA_VERSION, 'worker-attempt',
                'SUCCESS', packagePath, ScenarioSetupPreflightReport.CANDIDATE_SELECTION,
                workloads.size(), workloads.size(), 1L, metadata, workloads)
    }

    private static ScenarioSetupPreflightReport failedSourceWorkerReport(
            ScenarioSetupPreflightOptions options) {
        String packagePath = options.packagePath().toAbsolutePath().normalize().toString()
        def setupBlocker = new ScenarioExecutionReport.Blocker(
                'source-a', null, null, null, null, null,
                'SETUP_METHOD_NOT_AUTHORIZED', 'fixture#create is not authorized')
        def participantBlocker = new ScenarioExecutionReport.Blocker(
                'source-a', null, 'source-a-input', null, null, null,
                'SETUP_NOT_COMPLETED',
                'Saga startup was not attempted because workload setup stopped at SETUP_METHOD_NOT_AUTHORIZED')
        def sourceSetup = new ScenarioExecutionReport.SourceSetup(
                'FAILED', 1L, 0L, false, [], [],
                'SETUP_METHOD_NOT_AUTHORIZED', 'fixture#create is not authorized')
        def participant = new ScenarioSetupPreflightReport.ParticipantResult(
                'source-a-participant', 'fixture.Saga', 'source-a-input', false,
                'NOT_ATTEMPTED', 'NOT_ATTEMPTED', [participantBlocker])
        def workload = new ScenarioSetupPreflightReport.WorkloadResult(
                'source-a', 'SETUP_METHOD_NOT_AUTHORIZED', 1L,
                sourceSetup, [participant], [setupBlocker])
        def metadata = new ScenarioExecutionReport.RuntimeMetadata(
                options.applicationBase(), options.applicationId(), options.springApplicationClass(),
                options.springProfiles(), options.mavenProfile(), packagePath, null,
                'STATIC_MATERIALIZABILITY_CANDIDATE_PREFLIGHT', false)
        new ScenarioSetupPreflightReport(ScenarioSetupPreflightReport.SCHEMA_VERSION, 'worker-attempt',
                'SETUP_FAILED', packagePath, ScenarioSetupPreflightReport.CANDIDATE_SELECTION,
                1, 1, 1L, metadata, [workload])
    }

    private static ScenarioSetupPreflightReport.WorkloadResult ready(String id,
                                                                      boolean sourceSetup,
                                                                      String evidence = 'RESOLVED') {
        boolean startupReady = evidence != 'STARTUP_FAILED'
        def participant = new ScenarioSetupPreflightReport.ParticipantResult(
                "${id}-participant", 'fixture.Saga', "${id}-input", startupReady,
                'MATERIALIZED', startupReady ? 'STARTUP_READY' : 'STARTUP_FAILED',
                startupReady ? [] : [new ScenarioExecutionReport.Blocker(
                        id, null, "${id}-input", null, null, null,
                        'STARTUP_FAILED', 'fixture startup failed')])
        String actionStatus = evidence == 'FAILED_ACTION' ? 'FAILED' : 'SUCCEEDED'
        def action = new ScenarioExecutionReport.SetupActionOutcome(
                'action-1', 0, 'fixture#create', actionStatus, 'java.lang.Object',
                actionStatus == 'SUCCEEDED' ? 'java.lang.Object' : null,
                actionStatus == 'SUCCEEDED' ? 'action-1-result' : null,
                actionStatus == 'SUCCEEDED' ? '1' : null)
        def binding = new ScenarioExecutionReport.SetupParticipantBindingOutcome(
                "${id}-input", 0, 'action-1', 'aggregateId',
                evidence == 'UNRESOLVED_BINDING' ? 'UNRESOLVED' : 'RESOLVED',
                'java.lang.Integer', 'action-1-result', '1')
        def setupActions = evidence == 'MISSING_ACTIONS' ? [] : [action]
        def setupBindings = evidence == 'STATE_ONLY' ? []
                : evidence == 'MISSING_BINDINGS' ? null : [binding]
        def setup = sourceSetup
                ? new ScenarioExecutionReport.SourceSetup(
                'SUCCEEDED', 1L, 0L, true, setupActions, setupBindings, null, null)
                : null
        new ScenarioSetupPreflightReport.WorkloadResult(id, 'SETUP_READY', 1L, setup, [participant], [])
    }

    private static List<String> selectedIds(List<String> command) {
        argument(command, '--preflight-workload-ids').split(',').toList().sort()
    }

    private static String argument(List<String> command, String option) {
        command[command.indexOf(option) + 1]
    }

    private static Process quickProcess(int status) {
        new ProcessBuilder('python3', '-c', "import sys; sys.exit(${status})").start()
    }

    private static Process sleepingProcess() {
        new ProcessBuilder('python3', '-c', 'import time; time.sleep(30)').start()
    }

    private static Process stubbornProcess() {
        Process process = new ProcessBuilder('python3', '-u', '-c',
                'import signal,time; signal.signal(signal.SIGTERM, signal.SIG_IGN); print("ready", flush=True); time.sleep(30)')
                .redirectErrorStream(true)
                .start()
        assert process.inputStream.newReader().readLine() == 'ready'
        process
    }

    private static boolean noWorkerFragments(Path parent) {
        try (def paths = Files.list(parent)) {
            !paths.anyMatch { it.fileName.toString().startsWith('.setup-preflight-workers-') }
        }
    }
}
