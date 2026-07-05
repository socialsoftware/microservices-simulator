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
class UpdateExecutionCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    public static final String ACRONYM_2 = "SE02"
    public static final String ACADEMIC_TERM_2 = "2nd Semester 2024/2025"

    def courseDto
    def executionDto

    def setup() {
        loadBehaviorScripts()
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updateExecution: fault on updateExecutionStep compensates the lock acquired by getExecutionStep"() {
        // getExecutionStep has already set ExecutionSagaState.IN_UPDATE_EXECUTION and registered its
        // compensation by the time updateExecutionStep's injected fault fires, so this exercises the
        // saga's own compensate transition (IN_UPDATE_EXECUTION -> NOT_IN_SAGA) with no second saga involved.
        when:
        executionFunctionalities.updateExecution(executionDto.aggregateId, ACRONYM_2, ACADEMIC_TERM_2)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(executionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: fields are unchanged on read-back'
        def reread = executionFunctionalities.getExecutionById(executionDto.aggregateId)
        reread.acronym == ACRONYM_1
        reread.academicTerm == ACADEMIC_TERM_1
    }
}
