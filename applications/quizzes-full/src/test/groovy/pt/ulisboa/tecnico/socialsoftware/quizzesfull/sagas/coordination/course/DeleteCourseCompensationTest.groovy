package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.course

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class DeleteCourseCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def courseDto

    def setup() {
        loadBehaviorScripts()
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "deleteCourse: fault on deleteCourseStep compensates the lock acquired by getCourseStep"() {
        // getCourseStep has already set CourseSagaState.IN_DELETE_COURSE and registered its
        // compensation by the time deleteCourseStep's injected fault fires, so this exercises the
        // saga's own compensate transition (IN_DELETE_COURSE -> NOT_IN_SAGA) with no second saga involved.
        when:
        courseFunctionalities.deleteCourse(courseDto.aggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(courseDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: course is still readable, not deleted'
        def reread = courseFunctionalities.getCourseById(courseDto.aggregateId)
        reread.aggregateId == courseDto.aggregateId
        reread.name == COURSE_NAME_1
    }
}
