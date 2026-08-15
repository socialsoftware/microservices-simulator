package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.execution

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.COURSE_EXECUTION_STUDENT_NOT_FOUND

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class EnrollStudentInExecutionCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def courseDto
    def executionDto
    def userDto

    def setup() {
        loadBehaviorScripts()
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        userDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "enrollStudentInExecution: fault on enrollStudentStep compensates the lock acquired by getExecutionStep"() {
        // getExecutionStep depends on getUserStep (not a root step), so under the old fixed
        // two-pass ExecutionPlan this compensation could never be genuinely exercised - the
        // pass would fault enrollStudentStep before getExecutionStep's real body (and its lock
        // acquisition) ever ran. The topological worklist fix gates enrollStudentStep's fault
        // check on getExecutionStep's real completion, so the lock is genuinely held and then
        // genuinely released by compensation.
        when:
        executionFunctionalities.enrollStudentInExecution(executionDto.aggregateId, userDto.aggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(executionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        when: 'checking whether the student was ever enrolled'
        executionFunctionalities.getStudentByExecutionIdAndUserId(executionDto.aggregateId, userDto.aggregateId)

        then: 'the mutation never ran: the student was never enrolled'
        def ex = thrown(QuizzesFullException)
        ex.message == COURSE_EXECUTION_STUDENT_NOT_FOUND
    }
}
