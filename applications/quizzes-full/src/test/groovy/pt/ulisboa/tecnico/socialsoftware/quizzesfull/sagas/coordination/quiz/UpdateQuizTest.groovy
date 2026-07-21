package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.quiz

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
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

    def "updateQuiz: success"() {
        // Spec: plan.md §6 Quiz — UpdateQuiz; orchestration outcome only, persistence in QuizServiceTest.
        given:
        def newAvailableDate = LocalDateTime.now().plusDays(2)
        def newConclusionDate = LocalDateTime.now().plusDays(4)
        def newResultsDate = LocalDateTime.now().plusDays(5)

        when:
        quizFunctionalities.updateQuiz(quizDto.aggregateId, newAvailableDate, newConclusionDate,
                newResultsDate, [questionDto2.aggregateId])

        then:
        noExceptionThrown()
        sagaStateOf(quizDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
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
        sagaStateOf(quizDto.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

}
