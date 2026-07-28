package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor

import spock.lang.Specification

import java.nio.charset.StandardCharsets
import java.nio.file.Path

class ScenarioExecutorWrapperSpec extends Specification {
    def 'wrapper rejects ambiguous mode values and combinations before build work'() {
        when:
        def result = runWrapper(environment)

        then:
        result.exitCode == 2
        result.output.contains(expectedMessage)
        !result.output.contains('Installing simulator dependency')
        !result.output.contains('Building verifier executor classes')

        where:
        environment                                                        | expectedMessage
        [PREFLIGHT: 'TRUE']                                                 | "PREFLIGHT must be exactly 'true' or 'false'"
        [DRY_RUN: 'yes']                                                    | "DRY_RUN must be exactly 'true' or 'false'"
        [PREFLIGHT: 'true', DRY_RUN: 'true']                                | 'PREFLIGHT=true cannot be combined with DRY_RUN=true'
        [PREFLIGHT: 'true', DRY_RUN: 'false', FAULT_SCENARIO_ID: 'scenario'] | 'PREFLIGHT=true cannot be combined with FAULT_SCENARIO_ID'
        [PREFLIGHT: 'true', DRY_RUN: 'false', IMPACT_OUTPUT_PATH: '/tmp/impact.json'] | 'PREFLIGHT=true cannot be combined with IMPACT_OUTPUT_PATH'
    }

    private static Result runWrapper(Map<String, String> environment) {
        Path script = Path.of(System.getProperty('user.dir')).resolve('scripts/run-scenario-executor.sh')
        def processBuilder = new ProcessBuilder('bash', script.toString()).redirectErrorStream(true)
        processBuilder.environment().keySet().removeAll(['PREFLIGHT', 'DRY_RUN', 'FAULT_SCENARIO_ID', 'IMPACT_OUTPUT_PATH'])
        processBuilder.environment().putAll(environment)
        def process = processBuilder.start()
        def output = new String(process.inputStream.readAllBytes(), StandardCharsets.UTF_8)
        new Result(process.waitFor(), output)
    }

    private record Result(int exitCode, String output) {
    }
}
