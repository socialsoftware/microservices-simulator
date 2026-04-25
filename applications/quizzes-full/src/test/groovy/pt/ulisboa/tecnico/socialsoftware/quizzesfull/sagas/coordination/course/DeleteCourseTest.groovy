package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto

@DataJpaTest
@Import(LocalBeanConfiguration)
class DeleteCourseTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    CourseDto courseDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
    }

    def "deleteCourse: success"() {
        when:
        courseFunctionalities.deleteCourse(courseDto.aggregateId)

        and: 'verify course is no longer retrievable'
        def uow = unitOfWorkService.createUnitOfWork("verify")
        unitOfWorkService.aggregateLoadAndRegisterRead(courseDto.aggregateId, uow)

        then:
        def ex = thrown(SimulatorException)
        ex.errorMessage == SimulatorErrorMessage.AGGREGATE_NOT_FOUND
    }
}
