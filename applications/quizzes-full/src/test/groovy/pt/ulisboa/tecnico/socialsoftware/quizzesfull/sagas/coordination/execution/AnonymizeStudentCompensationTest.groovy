package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.execution

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
class AnonymizeStudentCompensationTest extends QuizzesFullSpockTest {

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
        executionFunctionalities.enrollStudentInExecution(executionDto.aggregateId, userDto.aggregateId)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "anonymizeStudent: fault on anonymizeStudentInExecutionStep compensates the lock acquired by getExecutionStep"() {
        // getExecutionStep has already set ExecutionSagaState.IN_ANONYMIZE_STUDENT and registered
        // its compensation by the time anonymizeStudentInExecutionStep's injected fault fires.
        // The downstream anonymizeUserStep never runs, so it needs no CSV row.
        when:
        executionFunctionalities.anonymizeStudent(executionDto.aggregateId, userDto.aggregateId)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(executionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: student is not anonymized on read-back'
        def reread = executionFunctionalities.getStudentByExecutionIdAndUserId(executionDto.aggregateId, userDto.aggregateId)
        reread.userName == USER_NAME_1
        reread.userUsername == USER_USERNAME_1
    }
}
