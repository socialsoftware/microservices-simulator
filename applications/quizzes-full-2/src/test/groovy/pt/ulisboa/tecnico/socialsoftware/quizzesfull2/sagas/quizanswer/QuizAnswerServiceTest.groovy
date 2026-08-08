package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.quizanswer

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception

@DataJpaTest
@Transactional
@Import(QuizAnswerServiceTest.LocalBeanConfiguration)
class QuizAnswerServiceTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999
    public static final String SECOND_QUIZ_TITLE = "Graph quiz"
    public static final String SECOND_USER_NAME = "Bob Jones"
    public static final String SECOND_USER_USERNAME = "bob"

    def "getQuizAnswerById: reads back the persisted quiz answer through a fresh UnitOfWork"() {
        // Spec: plan.md §7 QuizAnswer — GetQuizAnswerById(quizAnswerAggregateId); fields creationDate,
        // answerDate, completed and the quiz / student / execution snapshots
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)
        def quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)

        when:
        flushAndClear()
        def result = quizAnswerService.getQuizAnswerById(quizAnswerAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == quizAnswerAggregateId
        result.creationDate != null
        result.answerDate != null
        !result.completed
        result.quizAggregateId == quizAggregateId
        result.quizVersion != null
        result.userAggregateId == userAggregateId
        result.userName == USER_NAME
        result.userVersion != null
        result.executionAggregateId == executionAggregateId
        result.executionVersion != null
        result.questionAnswers.isEmpty()
        result.version != null
    }

    def "getQuizAnswerById: unknown aggregate id is not found"() {
        // Spec: plan.md §7 QuizAnswer — Path A (aggregateLoadAndRegisterRead)
        when:
        quizAnswerService.getQuizAnswerById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "getQuizAnswerForStudentAndQuiz: returns the quiz answer of that student for that quiz"() {
        // Spec: plan.md §7 QuizAnswer — GetQuizAnswerForStudentAndQuiz(userAggregateId, quizAggregateId)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)
        def quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)

        when:
        flushAndClear()
        def result = quizAnswerService.getQuizAnswerForStudentAndQuiz(userAggregateId, quizAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == quizAnswerAggregateId
        result.quizAggregateId == quizAggregateId
        result.userAggregateId == userAggregateId
        result.userName == USER_NAME
        result.executionAggregateId == executionAggregateId
        !result.completed
    }

    def "getQuizAnswerForStudentAndQuiz: ignores the same student's answer to another quiz"() {
        // Spec: plan.md §7 QuizAnswer — the lookup is keyed on the (student, quiz) pair
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)
        def otherQuizAggregateId = createQuiz(executionAggregateId, SECOND_QUIZ_TITLE)
        createQuizAnswer(otherQuizAggregateId, userAggregateId, executionAggregateId)
        def quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)

        when:
        flushAndClear()
        def result = quizAnswerService.getQuizAnswerForStudentAndQuiz(userAggregateId, quizAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == quizAnswerAggregateId
    }

    def "getQuizAnswerForStudentAndQuiz: ignores another student's answer to the same quiz"() {
        // Spec: plan.md §7 QuizAnswer — the lookup is keyed on the (student, quiz) pair
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def otherUserAggregateId = createActiveUser(SECOND_USER_NAME, SECOND_USER_USERNAME)
        def quizAggregateId = createQuiz(executionAggregateId)
        createQuizAnswer(quizAggregateId, otherUserAggregateId, executionAggregateId)
        def quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)

        when:
        flushAndClear()
        def result = quizAnswerService.getQuizAnswerForStudentAndQuiz(userAggregateId, quizAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == quizAnswerAggregateId
        result.userName == USER_NAME
    }

    def "getQuizAnswerForStudentAndQuiz: the student has not answered the quiz"() {
        // Spec: plan.md §7 QuizAnswer — Path B (custom-repository lookup returning Optional)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)

        when:
        quizAnswerService.getQuizAnswerForStudentAndQuiz(userAggregateId, quizAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.QUIZ_ANSWER_NOT_FOUND
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
