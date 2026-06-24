package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest

// Course's only P1 intra-invariants are COURSE_TYPE_FINAL and COURSE_NAME_FINAL,
// both enforced structurally via final fields (no verifyInvariants() body to exercise).
// No state-based P1 invariant remains, so this file keeps only the happy-path case.
@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CourseIntraInvariantTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "create course"() {
        when:
        def courseDto = courseFunctionalities.createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)

        then:
        courseDto.name == COURSE_NAME_1
        courseDto.type == COURSE_TYPE_TECNICO
    }
}
