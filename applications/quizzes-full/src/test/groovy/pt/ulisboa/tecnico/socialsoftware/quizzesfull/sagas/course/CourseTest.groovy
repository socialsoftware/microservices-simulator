package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest

@DataJpaTest
@org.springframework.transaction.annotation.Transactional
@Import(LocalBeanConfiguration)
class CourseTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "create course"() {
        when:
        def courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)

        then:
        courseDto.name == COURSE_NAME_1
        courseDto.type == COURSE_TYPE_TECNICO
        courseDto.executionCount == 0
        courseDto.questionCount == 0
    }
}
