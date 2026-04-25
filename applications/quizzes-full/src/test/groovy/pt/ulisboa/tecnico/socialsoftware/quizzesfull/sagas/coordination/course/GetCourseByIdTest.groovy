package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto

@DataJpaTest
@org.springframework.transaction.annotation.Transactional
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
        result.executionCount == 0
        result.questionCount == 0
    }

    def "getCourseById: not found throws exception"() {
        when:
        courseFunctionalities.getCourseById(999999)

        then:
        thrown(SimulatorException)
    }
}
