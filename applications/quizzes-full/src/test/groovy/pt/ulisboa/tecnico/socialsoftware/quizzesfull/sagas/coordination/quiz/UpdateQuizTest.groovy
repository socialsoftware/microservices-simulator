package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.quiz

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.Quiz
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.sagas.states.QuizSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.coordination.sagas.UpdateQuizFunctionalitySagas

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class UpdateQuizTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    def courseDto
    def executionDto
    def questionDto1
    def questionDto2
    def quizDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionDto = createExecution(courseDto.aggregateId, "SE-2025", "2025/2026")
        questionDto1 = createQuestion(courseDto.aggregateId, [], "Quiz Question 1", "Describe sorting.")
        questionDto2 = createQuestion(courseDto.aggregateId, [], "Quiz Question 2", "Describe searching.")
        quizDto = createQuiz(executionDto.aggregateId, [questionDto1.aggregateId])
    }

    def "updateQuiz: success — dates and questions updated"() {
        given:
        def newAvailableDate = LocalDateTime.now().plusDays(2)
        def newConclusionDate = LocalDateTime.now().plusDays(4)
        def newResultsDate = LocalDateTime.now().plusDays(5)

        when:
        quizFunctionalities.updateQuiz(quizDto.aggregateId, newAvailableDate, newConclusionDate,
                newResultsDate, [questionDto2.aggregateId])

        then:
        def uow = unitOfWorkService.createUnitOfWork("verify")
        Quiz fetched = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizDto.aggregateId, uow)
        fetched.availableDate == newAvailableDate
        fetched.questions.any { it.questionAggregateId == questionDto2.aggregateId }
        !fetched.questions.any { it.questionAggregateId == questionDto1.aggregateId }
    }

    def "updateQuiz: violates QUIZ_DATE_ORDERING — availableDate after conclusionDate"() {
        when:
        quizFunctionalities.updateQuiz(quizDto.aggregateId,
                LocalDateTime.now().plusDays(5),
                LocalDateTime.now().plusDays(2),
                LocalDateTime.now().plusDays(6),
                [questionDto1.aggregateId])

        then:
        thrown(QuizzesFullException)
    }

    def "updateQuiz: getQuizStep acquires IN_UPDATE_QUIZ semantic lock"() {
        given: 'updateQuiz workflow pauses after getQuizStep has acquired IN_UPDATE_QUIZ lock'
        def newAvailableDate = LocalDateTime.now().plusDays(2)
        def newConclusionDate = LocalDateTime.now().plusDays(4)
        def newResultsDate = LocalDateTime.now().plusDays(5)
        def uow = unitOfWorkService.createUnitOfWork("updateQuiz")
        def func = new UpdateQuizFunctionalitySagas(
                unitOfWorkService, quizDto.aggregateId, newAvailableDate, newConclusionDate,
                newResultsDate, [questionDto1.aggregateId], uow, commandGateway)
        func.executeUntilStep("getQuizStep", uow)

        expect: 'quiz saga state is IN_UPDATE_QUIZ'
        sagaStateOf(quizDto.aggregateId) == QuizSagaState.IN_UPDATE_QUIZ

        when: 'workflow resumes and completes'
        func.resumeWorkflow(uow)

        then:
        noExceptionThrown()

        and: 'quiz dates were updated'
        def uow2 = unitOfWorkService.createUnitOfWork("verify")
        Quiz fetched = (Quiz) unitOfWorkService.aggregateLoadAndRegisterRead(quizDto.aggregateId, uow2)
        fetched.availableDate == newAvailableDate
    }
}
