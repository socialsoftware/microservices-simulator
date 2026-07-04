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

    // updateCourse has no successful traversal — COURSE_FIELDS_IMMUTABLE is unconditional
    // (asserted in CourseServiceTest); this lock-acquisition case is the meaningful T4 coverage.
    def "updateCourse: getCourseStep acquires IN_UPDATE_COURSE semantic lock before updateCourseStep runs"() {
        given: 'updateCourse workflow pauses after getCourseStep has acquired IN_UPDATE_COURSE lock'
        def uow1 = unitOfWorkService.createUnitOfWork("updateCourse")
        def func1 = new UpdateCourseFunctionalitySagas(
                unitOfWorkService, courseDto.aggregateId, "New Name", COURSE_TYPE_EXTERNAL, uow1, commandGateway)
        func1.executeUntilStep("getCourseStep", uow1)

        expect: 'course saga state is IN_UPDATE_COURSE'
        sagaStateOf(courseDto.aggregateId) == CourseSagaState.IN_UPDATE_COURSE

        when: 'workflow resumes; updateCourseStep always throws (message asserted in CourseServiceTest)'
        func1.resumeWorkflow(uow1)

        then:
        thrown(QuizzesFullException)
    }
}
