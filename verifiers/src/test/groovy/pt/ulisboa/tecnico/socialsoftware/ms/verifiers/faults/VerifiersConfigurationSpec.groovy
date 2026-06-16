package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults

import spock.lang.Specification

import java.nio.file.Files
import java.nio.file.Path

class VerifiersConfigurationSpec extends Specification {

    def 'test profile defaults to dummyapp'() {
        expect:
        Files.readString(Path.of('src/main/resources/application-test.yaml')).contains(
                'application-base-dir: ${VERIFIERS_APPLICATION_BASE_DIR:dummyapp}'
        )
        Files.readString(Path.of('src/main/resources/application-test.yaml')).contains('enabled: false')
        Files.readString(Path.of('src/main/resources/application-test.yaml')).contains(
                'output-root: ${VERIFIERS_OUTPUT_ROOT:output}'
        )
    }

    def 'scenario catalog defaults are bounded'() {
        expect:
        def applicationYaml = Files.readString(Path.of('src/main/resources/application.yaml'))
        applicationYaml.contains('enabled: false')
        applicationYaml.contains('max-saga-set-size: 1')
        applicationYaml.contains('max-catalog-scenarios: 100')
        applicationYaml.contains('output-root: ${VERIFIERS_OUTPUT_ROOT:output}')
    }

    def 'dynamic enrichment defaults are disabled and bounded'() {
        expect:
        def applicationYaml = Files.readString(Path.of('src/main/resources/application.yaml'))
        applicationYaml.contains('dynamic-enrichment:')
        applicationYaml.contains('allow-partial-test-run: true')
        applicationYaml.contains('test-source-root: src/test/groovy')
        applicationYaml.contains('exclude-test-classes:')
        applicationYaml.contains('CreateTournamentDynamicEvidenceSmokeTest')
        applicationYaml.contains('DynamicEvidenceDisabledSmokeTest')
        applicationYaml.contains('profile: test-sagas')
    }
}
