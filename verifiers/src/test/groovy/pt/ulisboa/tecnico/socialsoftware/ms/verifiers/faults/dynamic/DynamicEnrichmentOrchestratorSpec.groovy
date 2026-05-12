package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic

import com.fasterxml.jackson.databind.ObjectMapper
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.*
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class DynamicEnrichmentOrchestratorSpec extends spock.lang.Specification {

    private static final String GENERATED_AT = '2026-05-01T00:00:00Z'
    private static final String TEST_CLASS = 'com.example.quiz.CreateTournamentFunctionalitySagasTest'
    private static final String SECOND_TEST_CLASS = 'com.example.quiz.UpdateTournamentFunctionalitySagasTest'
    private final ObjectMapper mapper = new ObjectMapper()

    @TempDir
    Path tempDir

    def 'command args include dynamic evidence properties and class output dir'() {
        given:
        def appDir = tempDir.resolve('applications/quizzes')
        Files.createDirectories(appDir.resolve('src/test/groovy/com/example/quiz'))
        def runDir = tempDir.resolve('runs/quizzes-1')
        def runner = new FakeProcessRunner([new ProcessRunner.ProcessResult(0, 'ok', '', false)], { ProcessRunner.ProcessCommand command ->
            def evidenceDir = runDir.resolve('dynamic-evidence').resolve(DynamicEnrichmentOrchestrator.safeTestClassDirectoryName(TEST_CLASS))
            assert Files.exists(evidenceDir.resolve(DynamicInputMapWriter.FILE_NAME))
            def inputMap = mapper.readTree(Files.readString(evidenceDir.resolve(DynamicInputMapWriter.FILE_NAME)))
            assert inputMap.path('inputCount').asInt() == 1
            assert inputMap.path('inputs')[0].path('inputVariantId').asText() == 'input-1'
        })
        def orchestrator = new DynamicEnrichmentOrchestrator(runner)

        when:
        def result = orchestrator.run(config(), appDir, 'quizzes', runDir, [TEST_CLASS], [scenarioPlan()], runDir.resolve('scenario-catalog.jsonl'), GENERATED_AT)

        then:
        result.testRuns().size() == 1
        runner.commands.size() == 1
        runner.commands[0].workingDirectory() == appDir
        runner.commands[0].arguments() == [
                'mvn', '-Ptest-sagas', 'test', "-Dtest=${TEST_CLASS}",
                '-Dsimulator.dynamic-evidence.enabled=true',
                '-Dsimulator.dynamic-evidence.test-context.enabled=true',
                '-Djunit.platform.listeners.autodetection.enabled=true',
                "-Dsimulator.dynamic-evidence.output-dir=${runDir.resolve('dynamic-evidence').resolve(DynamicEnrichmentOrchestrator.safeTestClassDirectoryName(TEST_CLASS))}",
                '-Dsimulator.dynamic-evidence.application-name=quizzes'
        ]
        result.testRuns()[0].status() == 'PASSED'
        Files.exists(runDir.resolve('dynamic-evidence').resolve(DynamicEnrichmentOrchestrator.safeTestClassDirectoryName(TEST_CLASS)).resolve('test-run.json'))
        Files.exists(runDir.resolve('dynamic-evidence').resolve(DynamicEnrichmentOrchestrator.safeTestClassDirectoryName(TEST_CLASS)).resolve(DynamicInputMapWriter.FILE_NAME))
        Files.readString(runDir.resolve('dynamic-evidence').resolve(DynamicEnrichmentOrchestrator.safeTestClassDirectoryName(TEST_CLASS)).resolve('maven-output.log')).contains('ok')
    }

    def 'dynamic evidence and sidecar paths cannot escape the run directory when orchestrator is used directly'() {
        given:
        def appDir = tempDir.resolve('applications/quizzes')
        Files.createDirectories(appDir)
        def runDir = tempDir.resolve('run')
        def runner = new FakeProcessRunner([new ProcessRunner.ProcessResult(0, 'ok', '', false)])
        def orchestrator = new DynamicEnrichmentOrchestrator(runner)

        when:
        orchestrator.run(config(true, '../escape-dynamic-evidence'), appDir, 'quizzes', runDir, [TEST_CLASS], [scenarioPlan()], runDir.resolve('scenario-catalog.jsonl'), GENERATED_AT)

        then:
        def ex = thrown(IllegalArgumentException)
        ex.message.contains('must stay under verifier run directory')
        runner.commands.isEmpty()
    }

    def 'partial mode continues after fake failed class and marks partial report artifacts'() {
        given:
        def appDir = tempDir.resolve('applications/quizzes')
        Files.createDirectories(appDir)
        def runDir = tempDir.resolve('run')
        def runner = new FakeProcessRunner([
                new ProcessRunner.ProcessResult(1, 'failed output', 'boom', false),
                new ProcessRunner.ProcessResult(0, 'passed output', '', false)
        ])
        def orchestrator = new DynamicEnrichmentOrchestrator(runner)

        when:
        orchestrator.run(config(true), appDir, 'quizzes', runDir, [TEST_CLASS, SECOND_TEST_CLASS], [scenarioPlan()], runDir.resolve('scenario-catalog.jsonl'), GENERATED_AT)

        then:
        runner.commands.size() == 2
        def report = mapper.readTree(Files.readString(runDir.resolve('dynamic-evidence-join-report.json')))
        report.path('runStatus').asText() == 'PARTIAL'
        report.path('counts').path('testClassesFailed').asInt() == 1
        report.path('counts').path('testClassesPassed').asInt() == 1
        report.path('testRuns')*.path('status')*.asText() == ['FAILED', 'PASSED']
        Files.exists(runDir.resolve('dynamic-evidence').resolve(DynamicEnrichmentOrchestrator.safeTestClassDirectoryName(TEST_CLASS)).resolve('test-run.json'))
    }

    def 'strict mode fails on fake failed class after preserving artifacts'() {
        given:
        def appDir = tempDir.resolve('applications/quizzes')
        Files.createDirectories(appDir)
        def runDir = tempDir.resolve('run')
        def runner = new FakeProcessRunner([new ProcessRunner.ProcessResult(1, 'failed output', 'boom', false)])
        def orchestrator = new DynamicEnrichmentOrchestrator(runner)

        when:
        orchestrator.run(config(false), appDir, 'quizzes', runDir, [TEST_CLASS], [scenarioPlan()], runDir.resolve('scenario-catalog.jsonl'), GENERATED_AT)

        then:
        def ex = thrown(IllegalStateException)
        ex.message.contains('Dynamic enrichment test run failed')
        runner.commands.size() == 1
        Files.exists(runDir.resolve('dynamic-evidence-join-report.json'))
        mapper.readTree(Files.readString(runDir.resolve('dynamic-evidence-join-report.json'))).path('runStatus').asText() == 'PARTIAL'
        Files.exists(runDir.resolve('dynamic-evidence').resolve(DynamicEnrichmentOrchestrator.safeTestClassDirectoryName(TEST_CLASS)).resolve('test-run.json'))
    }

    def 'orchestration writes sidecar artifacts from fake evidence in same run directory'() {
        given:
        def appDir = tempDir.resolve('applications/quizzes')
        Files.createDirectories(appDir)
        def runDir = tempDir.resolve('run')
        def runner = new FakeProcessRunner([new ProcessRunner.ProcessResult(0, 'ok', '', false)], { ProcessRunner.ProcessCommand command ->
            def outDirArg = command.arguments().find { it.startsWith('-Dsimulator.dynamic-evidence.output-dir=') }
            def evidenceDir = Path.of(outDirArg.substring(outDirArg.indexOf('=') + 1))
            Files.createDirectories(evidenceDir)
            Files.writeString(evidenceDir.resolve('dynamic-evidence.jsonl'), dynamicEvidenceLine() + System.lineSeparator())
        })
        def orchestrator = new DynamicEnrichmentOrchestrator(runner)

        when:
        orchestrator.run(config(), appDir, 'quizzes', runDir, [TEST_CLASS], [scenarioPlan()], runDir.resolve('scenario-catalog.jsonl'), GENERATED_AT)

        then:
        Files.exists(runDir.resolve('scenario-catalog-enriched.jsonl'))
        Files.exists(runDir.resolve('scenario-catalog-enriched-manifest.json'))
        Files.exists(runDir.resolve('dynamic-evidence-join-report.json'))
        Files.exists(runDir.resolve('dynamic-evidence').resolve(DynamicEnrichmentOrchestrator.safeTestClassDirectoryName(TEST_CLASS)).resolve('dynamic-evidence.jsonl'))
        mapper.readTree(Files.readString(runDir.resolve('dynamic-evidence-join-report.json'))).path('counts').path('dynamicEventsRead').asInt() == 1
        mapper.readTree(Files.readAllLines(runDir.resolve('scenario-catalog-enriched.jsonl'))[0]).path('dynamicEvidence').path('joinStatus').asText() == 'MATCHED_HIGH_CONFIDENCE'
    }

    private DynamicEnrichmentConfig config(boolean partial = true,
                                            String dynamicEvidenceSubdir = 'dynamic-evidence',
                                            String enrichedCatalogPath = 'scenario-catalog-enriched.jsonl',
                                            String enrichedManifestPath = 'scenario-catalog-enriched-manifest.json',
                                            String joinReportPath = 'dynamic-evidence-join-report.json') {
        new DynamicEnrichmentConfig(true, partial, dynamicEvidenceSubdir, enrichedCatalogPath, enrichedManifestPath, joinReportPath, 'src/test/groovy', [], [], [], 300, new DynamicEnrichmentConfig.DynamicEnrichmentMavenConfig('mvn', 'test-sagas'))
    }

    private ScenarioPlan scenarioPlan() {
        def sagaFqn = 'com.example.quiz.CreateTournamentFunctionalitySagas'
        def input = new InputVariant('input-1', sagaFqn, TEST_CLASS, 'createsTournament', 'binding', InputResolutionStatus.RESOLVED, 'source', 'prov', ['arg[0]: 11'], [:], [])
        def saga = new SagaInstance('saga-1', sagaFqn, 'input-1', [])
        def step = new ScheduledStep('step-1', 'saga-1', sagaFqn + '::getCourseExecutionStep', 0, [])
        new ScenarioPlan(ScenarioPlan.SCHEMA_VERSION, 'scenario-1', ScenarioKind.SINGLE_SAGA, [saga], [input], [step], null, [], [])
    }

    private String dynamicEvidenceLine() {
        mapper.writeValueAsString([
                schemaVersion: 'microservices-simulator.dynamic-evidence.v1', eventId: 'event-1', eventKind: 'STEP_STARTED',
                applicationName: 'quizzes', functionalityName: 'CreateTournamentFunctionalitySagas', stepName: 'getCourseExecutionStep',
                testClassFqn: TEST_CLASS, testMethodName: 'createsTournament', testDisplayName: 'createsTournament', testUniqueId: TEST_CLASS + '#createsTournament',
                payload: [stepPhase: 'FORWARD']
        ])
    }

    private static class FakeProcessRunner implements ProcessRunner {
        final List<ProcessRunner.ProcessCommand> commands = []
        private final Queue<ProcessRunner.ProcessResult> results
        private final Closure sideEffect

        FakeProcessRunner(List<ProcessRunner.ProcessResult> results, Closure sideEffect = {}) {
            this.results = new ArrayDeque<>(results)
            this.sideEffect = sideEffect
        }

        @Override
        ProcessRunner.ProcessResult run(ProcessRunner.ProcessCommand command) {
            commands << command
            sideEffect.call(command)
            return results.remove()
        }
    }
}
