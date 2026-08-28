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

    def 'source candidates use separate real processes while legacy candidates retain one batch and fragments are cleaned'() {
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
                ['legacy-a', 'legacy-b', 'source-a', 'source-b'] as Set<String>,
                ['source-a', 'source-b'] as Set<String>)

        when:
        def status = orchestrator(starter).run(originalArgs(relativeOutput, relativePackage), options, plan)

        then:
        status == 0
        processes.size() == 3
        processes.every { !it.isAlive() }
        commands.collect { selectedIds(it) } == [
                ['legacy-a', 'legacy-b'], ['source-a'], ['source-b']
        ]
        commands.every { Path.of(argument(it, '--package-path')).isAbsolute() }
        commands.every { Path.of(argument(it, '--output-path')).isAbsolute() }
        workingDirectories == [applicationDirectory.toAbsolutePath().normalize()] * 3
        def aggregate = MAPPER.readValue(output.toFile(), ScenarioSetupPreflightReport)
        aggregate.terminalStatus() == 'SUCCESS'
        aggregate.packageManifestPath() == temporaryDirectory.resolve('manifest.json').toString()
        aggregate.workloads()*.workloadPlanId() == ['legacy-a', 'legacy-b', 'source-a', 'source-b']
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
        report.workloads()[0].blockers()[0].message().contains('nonzero status 7')
        noWorkerFragments(output.parent)
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
        new ScenarioExecutor.PreflightPlan(['source-a'] as Set<String>, ['source-a'] as Set<String>)
    }

    private static String[] originalArgs(Path output, Path packagePath) {
        ['--preflight', '--spring-application-class', 'fixture.Application',
         '--package-path', packagePath.toString(), '--output-path', output.toString()] as String[]
    }

    private static void writeWorkerReport(List<String> command,
                                          ScenarioSetupPreflightOptions options,
                                          Set<String> sourceIds) {
        Path output = Path.of(argument(command, '--output-path'))
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(
                output.toFile(), workerReport(selectedIds(command), options, sourceIds))
    }

    private static ScenarioSetupPreflightReport workerReport(List<String> ids,
                                                              ScenarioSetupPreflightOptions options,
                                                              Set<String> sourceIds) {
        String packagePath = options.packagePath().toAbsolutePath().normalize().toString()
        def workloads = ids.collect { ready(it, sourceIds.contains(it)) }
        def metadata = new ScenarioExecutionReport.RuntimeMetadata(
                options.applicationBase(), options.applicationId(), options.springApplicationClass(),
                options.springProfiles(), options.mavenProfile(), packagePath, null,
                'STATIC_MATERIALIZABILITY_CANDIDATE_PREFLIGHT', false)
        new ScenarioSetupPreflightReport(ScenarioSetupPreflightReport.SCHEMA_VERSION, 'worker-attempt',
                'SUCCESS', packagePath, ScenarioSetupPreflightReport.CANDIDATE_SELECTION,
                workloads.size(), workloads.size(), 1L, metadata, workloads)
    }

    private static ScenarioSetupPreflightReport.WorkloadResult ready(String id, boolean sourceSetup) {
        def participant = new ScenarioSetupPreflightReport.ParticipantResult(
                "${id}-participant", 'fixture.Saga', "${id}-input", true,
                'MATERIALIZED', 'STARTUP_READY', [])
        def action = new ScenarioExecutionReport.SetupActionOutcome(
                'action-1', 0, 'fixture#create', 'SUCCEEDED', 'java.lang.Object',
                'java.lang.Object', 'action-1-result', '1')
        def binding = new ScenarioExecutionReport.SetupParticipantBindingOutcome(
                "${id}-input", 0, 'action-1', 'aggregateId', 'RESOLVED',
                'java.lang.Integer', 'action-1-result', '1')
        def setup = sourceSetup
                ? new ScenarioExecutionReport.SourceSetup(
                'SUCCEEDED', 1L, 0L, true, [action], [binding], null, null)
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
