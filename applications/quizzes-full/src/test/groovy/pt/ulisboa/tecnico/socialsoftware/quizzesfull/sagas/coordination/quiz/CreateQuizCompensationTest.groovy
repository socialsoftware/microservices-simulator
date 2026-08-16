package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.quiz

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizRepository

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateQuizCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    QuizRepository quizRepository

    def courseDto
    def executionDto
    def questionDto

    def setup() {
        loadBehaviorScripts()
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        questionDto = createQuestion(courseDto.aggregateId, [], "Quiz Question", "Describe sorting.")
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "createQuiz: fault on getQuestionsStep compensates the lock acquired by getExecutionStep"() {
        // getExecutionStep has already set ExecutionSagaState.READ_EXECUTION and registered its
        // compensation by the time getQuestionsStep's injected fault fires. The Quiz doesn't exist
        // yet at this point in the saga, so the released lock is on Execution.
        given:
        def availableDate = LocalDateTime.now().plusDays(1)
        def conclusionDate = LocalDateTime.now().plusDays(2)
        def resultsDate = LocalDateTime.now().plusDays(3)

        when:
        quizFunctionalities.createQuiz("Test Quiz", availableDate, conclusionDate, resultsDate,
                "GENERATED", executionDto.aggregateId, [questionDto.aggregateId])

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the Execution semantic lock back to NOT_IN_SAGA'
        sagaStateOf(executionDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: no quiz was created'
        quizRepository.findAll().isEmpty()
    }
}
