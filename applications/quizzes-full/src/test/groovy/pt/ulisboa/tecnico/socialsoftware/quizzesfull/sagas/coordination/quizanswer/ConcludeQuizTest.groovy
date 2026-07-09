package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.quizanswer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.sagas.states.QuizAnswerSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.sagas.ConcludeQuizFunctionalitySagas

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class ConcludeQuizTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    Integer courseId
    Integer userId
    Integer executionId
    Integer topicId
    Integer questionId
    Integer quizId
    Integer quizAnswerId

    def setup() {
        def course = createCourse("Software Engineering", "TECNICO")
        courseId = course.aggregateId

        def user = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        userId = user.aggregateId

        def execution = createExecution(courseId, "SE2024", "1st Semester 2024")
        executionId = execution.aggregateId

        executionFunctionalities.enrollStudentInExecution(executionId, userId)

        def topic = createTopic(courseId, "Topic A")
        topicId = topic.aggregateId

        def question = createQuestion(courseId, [topicId], "Q1", "Content")
        questionId = question.aggregateId

        def quiz = createQuiz(executionId, [questionId])
        quizId = quiz.aggregateId

        def quizAnswer = createQuizAnswer(quizId, userId)
        quizAnswerId = quizAnswer.aggregateId
    }

    def "concludeQuiz: success"() {
        // Orchestration outcome only — persistence/field-level assertions are owned by QuizAnswerServiceTest.
        when:
        quizAnswerFunctionalities.concludeQuiz(quizAnswerId)

        then:
        noExceptionThrown()
        sagaStateOf(quizAnswerId) == GenericSagaState.NOT_IN_SAGA
    }

    def "concludeQuiz: getQuizAnswerStep acquires IN_CONCLUDE_QUIZ semantic lock"() {
        given: 'a concludeQuiz workflow pauses after getQuizAnswerStep has acquired IN_CONCLUDE_QUIZ lock'
        def uow = unitOfWorkService.createUnitOfWork("concludeQuiz")
        def func = new ConcludeQuizFunctionalitySagas(
                unitOfWorkService, quizAnswerId, uow, commandGateway)
        func.executeUntilStep("getQuizAnswerStep", uow)

        expect: 'quiz answer saga state is IN_CONCLUDE_QUIZ'
        sagaStateOf(quizAnswerId) == QuizAnswerSagaState.IN_CONCLUDE_QUIZ

        when: 'workflow resumes and completes'
        func.resumeWorkflow(uow)

        then:
        noExceptionThrown()
    }
}
