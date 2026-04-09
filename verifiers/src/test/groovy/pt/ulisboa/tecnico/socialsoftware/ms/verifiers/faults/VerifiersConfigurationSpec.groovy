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
    }
}
