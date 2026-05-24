package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.course

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.states.CourseSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.coordination.sagas.UpdateCourseFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateCourseTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

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

    def "updateCourse: getCourseStep acquires IN_UPDATE_COURSE semantic lock before updateCourseStep runs"() {
        given: 'updateCourse workflow pauses after getCourseStep has acquired IN_UPDATE_COURSE lock'
        def uow1 = unitOfWorkService.createUnitOfWork("updateCourse")
        def func1 = new UpdateCourseFunctionalitySagas(
                unitOfWorkService, courseDto.aggregateId, "New Name", COURSE_TYPE_EXTERNAL, uow1, commandGateway)
        func1.executeUntilStep("getCourseStep", uow1)

        expect: 'course saga state is IN_UPDATE_COURSE'
        sagaStateOf(courseDto.aggregateId) == CourseSagaState.IN_UPDATE_COURSE

        when: 'workflow resumes; updateCourseStep always throws COURSE_FIELDS_IMMUTABLE'
        func1.resumeWorkflow(uow1)

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == QuizzesFullErrorMessage.COURSE_FIELDS_IMMUTABLE
    }
}
