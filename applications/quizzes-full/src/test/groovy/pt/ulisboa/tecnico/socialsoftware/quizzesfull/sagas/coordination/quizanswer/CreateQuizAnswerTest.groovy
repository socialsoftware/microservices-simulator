package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.quizanswer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.TestPropertySource
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.sagas.states.QuizSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.coordination.sagas.CreateQuizAnswerFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas.states.UserSagaState

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.UNIQUE_QUIZ_ANSWER_PER_STUDENT

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateQuizAnswerTest extends QuizzesFullSpockTest {

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
    }

    def "createQuizAnswer: success"() {
        when:
        def result = createQuizAnswer(quizId, userId)

        then:
        result.aggregateId != null
        result.quizAggregateId == quizId
        result.userAggregateId == userId
        result.executionAggregateId == executionId
        result.userName == USER_NAME_1
        result.userUsername == USER_USERNAME_1
        result.completed == false
        result.creationDate != null
        result.answerDate != null
        result.questionAnswerIds.isEmpty()

        and: 'the quiz answer is persisted and retrievable'
        def checkUow = unitOfWorkService.createUnitOfWork("check")
        def dto = quizAnswerService.getQuizAnswerById(result.aggregateId, checkUow)
        dto.aggregateId == result.aggregateId
        dto.completed == false
    }

    def "createQuizAnswer: UNIQUE_QUIZ_ANSWER_PER_STUDENT violation"() {
        given:
        createQuizAnswer(quizId, userId)

        when:
        createQuizAnswer(quizId, userId)

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == UNIQUE_QUIZ_ANSWER_PER_STUDENT
    }

    def "createQuizAnswer: getQuizStep acquires READ_QUIZ semantic lock"() {
        given: 'a createQuizAnswer workflow pauses after getQuizStep has acquired READ_QUIZ lock'
        def uow = unitOfWorkService.createUnitOfWork("createQuizAnswer")
        def func = new CreateQuizAnswerFunctionalitySagas(
                unitOfWorkService, quizId, userId, uow, commandGateway)
        func.executeUntilStep("getQuizStep", uow)

        expect: 'quiz saga state is READ_QUIZ'
        sagaStateOf(quizId) == QuizSagaState.READ_QUIZ

        when: 'workflow resumes and completes'
        func.resumeWorkflow(uow)

        then:
        noExceptionThrown()

        and: 'quiz answer was created'
        func.getCreatedQuizAnswerDto().aggregateId != null
        func.getCreatedQuizAnswerDto().quizAggregateId == quizId
    }

    def "createQuizAnswer: getUserStep acquires READ_USER semantic lock"() {
        given: 'a createQuizAnswer workflow pauses after getUserStep has acquired READ_USER lock'
        def uow = unitOfWorkService.createUnitOfWork("createQuizAnswer")
        def func = new CreateQuizAnswerFunctionalitySagas(
                unitOfWorkService, quizId, userId, uow, commandGateway)
        func.executeUntilStep("getUserStep", uow)

        expect: 'user saga state is READ_USER'
        sagaStateOf(userId) == UserSagaState.READ_USER

        when: 'workflow resumes and completes'
        func.resumeWorkflow(uow)

        then:
        noExceptionThrown()

        and: 'quiz answer was created'
        func.getCreatedQuizAnswerDto().aggregateId != null
        func.getCreatedQuizAnswerDto().userAggregateId == userId
    }
}
