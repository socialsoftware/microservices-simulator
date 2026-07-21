package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.execution

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionRepository

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateExecutionCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    ExecutionRepository executionRepository

    def courseDto

    def setup() {
        loadBehaviorScripts()
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "createExecution: fault on createExecutionStep compensates the lock acquired by getCourseStep"() {
        // getCourseStep has already set CourseSagaState.READ_COURSE and registered its
        // compensation by the time createExecutionStep's injected fault fires. The Execution
        // doesn't exist yet at this point in the saga, so the released lock is on Course.
        when:
        createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the Course semantic lock back to NOT_IN_SAGA'
        sagaStateOf(courseDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: no execution was created'
        executionRepository.findAll().every { it.acronym != ACRONYM_1 }
    }
}
