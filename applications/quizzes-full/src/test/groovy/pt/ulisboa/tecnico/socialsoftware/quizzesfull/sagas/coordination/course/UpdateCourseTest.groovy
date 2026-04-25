package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException

@DataJpaTest
@org.springframework.transaction.annotation.Transactional
@Import(LocalBeanConfiguration)
class UpdateCourseTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    CourseDto courseDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
    }

    // UpdateCourse always fails: COURSE_NAME_FINAL and COURSE_TYPE_FINAL are P1 final fields
    def "updateCourse: COURSE_FIELDS_IMMUTABLE — name and type are P1 final fields"() {
        when:
        courseFunctionalities.updateCourse(courseDto.aggregateId, "New Name", COURSE_TYPE_EXTERNAL)

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == QuizzesFullErrorMessage.COURSE_FIELDS_IMMUTABLE
    }
}
