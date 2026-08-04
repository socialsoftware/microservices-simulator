package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.quiz

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateQuizCompensationTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    def courseDto
    def executionDto
    def questionDto1
    def questionDto2
    def quizDto
    def originalAvailableDate

    def setup() {
        loadBehaviorScripts()
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionDto = createExecution(courseDto.aggregateId, ACRONYM_1, ACADEMIC_TERM_1)
        questionDto1 = createQuestion(courseDto.aggregateId, [], "Quiz Question 1", "Describe sorting.")
        questionDto2 = createQuestion(courseDto.aggregateId, [], "Quiz Question 2", "Describe searching.")
        quizDto = createQuiz(executionDto.aggregateId, [questionDto1.aggregateId])
        originalAvailableDate = quizDto.availableDate
    }

    def cleanup() {
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def "updateQuiz: fault on getQuestionsStep compensates the lock acquired by getQuizStep"() {
        // getQuizStep has already set QuizSagaState.IN_UPDATE_QUIZ and registered its compensation
        // by the time getQuestionsStep's injected fault fires, so this exercises the saga's own
        // compensate transition (IN_UPDATE_QUIZ -> NOT_IN_SAGA) with no second saga involved.
        when:
        quizFunctionalities.updateQuiz(quizDto.aggregateId, LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(4), LocalDateTime.now().plusDays(5),
                [questionDto2.aggregateId])

        then: 'the injected fault surfaces to the caller'
        thrown(SimulatorException)

        and: 'compensation released the semantic lock back to NOT_IN_SAGA'
        sagaStateOf(quizDto.aggregateId) == GenericSagaState.NOT_IN_SAGA

        and: 'the mutation never ran: quiz still has its original dates and questions'
        def reread = quizFunctionalities.getQuizById(quizDto.aggregateId)
        reread.availableDate == originalAvailableDate
        reread.questionIds.contains(questionDto1.aggregateId)
        !reread.questionIds.contains(questionDto2.aggregateId)
    }
}
