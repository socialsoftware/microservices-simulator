package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.course

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.states.CourseSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.sagas.DeleteCourseFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteCourseTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

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

    def "deleteCourse: getCourseStep acquires IN_DELETE_COURSE semantic lock before deletion completes"() {
        given: 'deleteCourse workflow pauses after getCourseStep has acquired IN_DELETE_COURSE lock'
        def uow1 = unitOfWorkService.createUnitOfWork("deleteCourse")
        def func1 = new DeleteCourseFunctionalitySagas(
                unitOfWorkService, courseDto.aggregateId, uow1, commandGateway)
        func1.executeUntilStep("getCourseStep", uow1)

        expect: 'course saga state is IN_DELETE_COURSE'
        sagaStateOf(courseDto.aggregateId) == CourseSagaState.IN_DELETE_COURSE

        when: 'workflow resumes and completes'
        func1.resumeWorkflow(uow1)

        then:
        noExceptionThrown()

        when: 'course is no longer retrievable after deletion'
        def uow2 = unitOfWorkService.createUnitOfWork("verify")
        unitOfWorkService.aggregateLoadAndRegisterRead(courseDto.aggregateId, uow2)

        then:
        thrown(SimulatorException)
    }
}
