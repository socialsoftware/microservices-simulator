package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.behaviour

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic.DynamicEvidenceRecorder
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest

import java.nio.file.Files
import java.nio.file.Path

@DataJpaTest
@ImportAutoConfiguration(DynamicEvidenceConfiguration)
class DynamicEvidenceDisabledSmokeTest extends QuizzesSpockTest {
    private static final Path tempRoot = Files.createTempDirectory('quizzes-dynamic-evidence-disabled-')
    private static final Path disabledOutputDir = tempRoot.resolve('disabled-output')

    @Autowired
    private DynamicEvidenceRecorder dynamicEvidenceRecorder

    @DynamicPropertySource
    static void dynamicEvidenceProperties(DynamicPropertyRegistry registry) {
        registry.add('simulator.dynamic-evidence.enabled') { false }
        registry.add('simulator.dynamic-evidence.output-dir') { disabledOutputDir.toString() }
    }

    def cleanupSpec() {
        deleteRecursively(tempRoot)
    }

    def 'normal quizzes test path does not create dynamic evidence artifacts when disabled'() {
        when: 'a normal Quizzes helper path runs with dynamic evidence disabled'
        createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)
        dynamicEvidenceRecorder.close()

        then: 'no dynamic evidence output directory or artifacts are created'
        !dynamicEvidenceRecorder.enabled
        !Files.exists(disabledOutputDir)
        !Files.exists(disabledOutputDir.resolve('dynamic-evidence.jsonl'))
        !Files.exists(disabledOutputDir.resolve('dynamic-evidence-manifest.json'))
    }

    private static void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) {
            return
        }
        Files.walk(path)
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().findAndRegisterModules()
        }
    }
}
