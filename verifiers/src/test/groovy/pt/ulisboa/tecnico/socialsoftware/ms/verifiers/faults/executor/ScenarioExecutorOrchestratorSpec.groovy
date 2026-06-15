package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import spock.lang.Specification

import java.nio.file.Path

class ScenarioExecutorOrchestratorSpec extends Specification {
    def 'orchestrator validates runtime inputs and builds prepare plus forked executor commands'() {
        given:
        def calls = []
        def runner = { List<String> command, Path workingDirectory -> calls << [command, workingDirectory]; 0 } as ScenarioExecutorOrchestrator.ProcessRunner
        def config = new ScenarioExecutorOrchestrator.Config(Path.of('/tmp/app'), 'com.example.Application', 'test-sagas', 'local,sagas', Path.of('/tmp/run/scenario-catalog.jsonl'), Path.of('/tmp/out/execution-report.json'), 'scenario-1', 'target/classes:verifiers.jar')

        when:
        def status = new ScenarioExecutorOrchestrator(runner).run(config)

        then:
        status == 0
        calls[0][0] == ['mvn', '-P', 'test-sagas', 'test-compile']
        calls[0][1] == Path.of('/tmp/app')
        calls[1][0].containsAll(['java', '-cp', 'target/classes:verifiers.jar', 'pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorCli', '--spring-profiles', 'local,sagas', '--catalog-path', '/tmp/run/scenario-catalog.jsonl', '--output-path', '/tmp/out/execution-report.json', '--scenario-id', 'scenario-1'])
        calls[1][1] == Path.of('/tmp/app')
    }

    def 'orchestrator maps preparation failure without launching execution'() {
        given:
        def calls = []
        def runner = { List<String> command, Path workingDirectory -> calls << command; 7 } as ScenarioExecutorOrchestrator.ProcessRunner

        expect:
        new ScenarioExecutorOrchestrator(runner).run(validConfig()) == 7
        calls.size() == 1
    }

    def 'orchestrator rejects missing required output path'() {
        when:
        new ScenarioExecutorOrchestrator({ List<String> command, Path workingDirectory -> 0 } as ScenarioExecutorOrchestrator.ProcessRunner)
                .run(new ScenarioExecutorOrchestrator.Config(Path.of('/tmp/app'), 'App', 'test', 'local', Path.of('/tmp/catalog.jsonl'), null, null, 'cp'))

        then:
        thrown(IllegalArgumentException)
    }

    private static ScenarioExecutorOrchestrator.Config validConfig() {
        new ScenarioExecutorOrchestrator.Config(Path.of('/tmp/app'), 'App', 'test', 'local', Path.of('/tmp/catalog.jsonl'), Path.of('/tmp/report.json'), null, 'cp')
    }
}
