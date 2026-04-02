package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.behaviour.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.quizzes.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities

@DataJpaTest
class CreateCourseExecutionCompensationTest extends QuizzesSpockTest {
    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities

    @Autowired
    private CourseRepository courseRepository

    private CourseExecutionDto courseExecutionDto

    def setup() {
        loadBehaviorScripts()
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "saga compensation deletes newly created course when createCourseExecutionStep fails"() {
        when: 'creating a course execution with a brand-new course name that will fail in step 3'
        courseExecutionFunctionalities.createCourseExecution(new CourseExecutionDto(
            name: 'BRAND_NEW_COURSE',
            type: COURSE_EXECUTION_TYPE,
            acronym: 'NEW_SAGA_TEST',
            academicTerm: COURSE_EXECUTION_ACADEMIC_TERM,
            endDate: DateHandler.toISOString(TIME_4)
        ))

        then: 'the injected fault is thrown'
        thrown(SimulatorException)

        and: 'the newly created course was compensated (deleted or not persisted)'
        def createdCourse = courseRepository.findAll().find { it.getName() == 'BRAND_NEW_COURSE' }
        createdCourse == null || createdCourse.getState() == Aggregate.AggregateState.DELETED
    }

    def "saga compensation preserves pre-existing course when createCourseExecutionStep fails"() {
        given: 'find the existing course before the saga'
        def existingCourse = courseRepository.findAll().find { it.getName() == COURSE_EXECUTION_NAME }

        when: 'creating a course execution reusing the existing course name that will fail in step 3'
        courseExecutionFunctionalities.createCourseExecution(new CourseExecutionDto(
            name: COURSE_EXECUTION_NAME,
            type: COURSE_EXECUTION_TYPE,
            acronym: 'REUSE_SAGA_TEST',
            academicTerm: COURSE_EXECUTION_ACADEMIC_TERM,
            endDate: DateHandler.toISOString(TIME_4)
        ))

        then: 'the injected fault is thrown'
        thrown(SimulatorException)

        and: 'the pre-existing course is untouched (not deleted)'
        def courseAfterSaga = courseRepository.findAll().find { it.getName() == COURSE_EXECUTION_NAME && it.getId() == existingCourse.getId() }
        courseAfterSaga != null
        courseAfterSaga.getState() == Aggregate.AggregateState.ACTIVE
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
