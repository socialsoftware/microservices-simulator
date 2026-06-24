package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class GetCourseByIdTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "getCourseById: success"() {
        given:
        CourseDto created = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)

        when:
        CourseDto result = courseFunctionalities.getCourseById(created.aggregateId)

        then:
        result != null
        result.aggregateId == created.aggregateId
        result.name == COURSE_NAME_1
        result.type == COURSE_TYPE_TECNICO
    }

    def "getCourseById: not found throws exception"() {
        when:
        courseFunctionalities.getCourseById(999999)

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }
}
