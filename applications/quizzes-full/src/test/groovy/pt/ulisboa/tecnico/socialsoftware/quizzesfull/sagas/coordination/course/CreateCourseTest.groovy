package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseType

@DataJpaTest
@Import(LocalBeanConfiguration)
class CreateCourseTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def "createCourse: success"() {
        when:
        CourseDto result = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)

        then:
        result != null
        result.name == COURSE_NAME_1
        result.type == COURSE_TYPE_TECNICO
        result.aggregateId != null
        result.executionCount == 0
        result.questionCount == 0
    }

    def "createCourse: success with EXTERNAL type"() {
        when:
        CourseDto result = createCourse(COURSE_NAME_2, COURSE_TYPE_EXTERNAL)

        then:
        result != null
        result.name == COURSE_NAME_2
        result.type == COURSE_TYPE_EXTERNAL
    }
}
