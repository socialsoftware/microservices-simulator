package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import spock.lang.Specification

import java.nio.file.Path

class ScenarioExecutorOrchestratorSpec extends Specification {
    def 'orchestrator validates runtime inputs and builds persisted-id package command without a vector overlay'() {
        given:
        def calls = []
        def runner = { List<String> command, Path workingDirectory -> calls << [command, workingDirectory]; 0 } as ScenarioExecutorOrchestrator.ProcessRunner
        def config = new ScenarioExecutorOrchestrator.Config(
                Path.of('/tmp/app'), 'com.example.Application', 'test-sagas', 'local,sagas',
                Path.of('/tmp/run/scenario-catalog-manifest.json'), Path.of('/tmp/out/execution-report.json'),
                'fault-scenario-1', 'target/classes:verifiers.jar')

        when:
        def status = new ScenarioExecutorOrchestrator(runner).run(config)

        then:
        status == 0
        calls[0][0] == ['mvn', '-P', 'test-sagas', 'test-compile']
        calls[0][1] == Path.of('/tmp/app')
        calls[1][0].containsAll([
                'java', '-cp', 'target/classes:verifiers.jar',
                'pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorCli',
                '--spring-profiles', 'local,sagas', '--application-base', '/tmp/app', '--application-id', 'app',
                '--maven-profile', 'test-sagas', '--package-path', '/tmp/run/scenario-catalog-manifest.json',
                '--output-path', '/tmp/out/execution-report.json', '--fault-scenario-id', 'fault-scenario-1'])
        !calls[1][0].contains('--fault-vector')
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

    def 'orchestrator rejects missing persisted FaultScenario id'() {
        when:
        new ScenarioExecutorOrchestrator({ List<String> command, Path workingDirectory -> 0 } as ScenarioExecutorOrchestrator.ProcessRunner)
                .run(new ScenarioExecutorOrchestrator.Config(
                        Path.of('/tmp/app'), 'App', 'test', 'local', Path.of('/tmp/manifest.json'),
                        Path.of('/tmp/report.json'), null, 'cp'))

        then:
        def error = thrown(IllegalArgumentException)
        error.message.contains('FaultScenario id')
    }

    private static ScenarioExecutorOrchestrator.Config validConfig() {
        new ScenarioExecutorOrchestrator.Config(
                Path.of('/tmp/app'), 'App', 'test', 'local', Path.of('/tmp/manifest.json'),
                Path.of('/tmp/report.json'), 'fault-scenario-1', 'cp')
    }
}
