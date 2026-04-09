package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults

import org.springframework.boot.SpringApplication
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

class ScenarioGeneratorApplicationSpec extends Specification {

    @TempDir
    Path tempDir

    def 'application starts when configured application directory exists'() {
        given:
        def applicationsRoot = tempDir.resolve('applications')
        def applicationBaseDir = 'dummyapp'

        Files.createDirectories(applicationsRoot.resolve(applicationBaseDir))

        and:
        def app = new SpringApplication(ScenarioGeneratorApplication)

        when:
        def context = app.run(
                "--verifiers.applications-root=${applicationsRoot}",
                "--verifiers.application-base-dir=${applicationBaseDir}"
        )

        then:
        noExceptionThrown()

        cleanup:
        context?.close()
    }
}
