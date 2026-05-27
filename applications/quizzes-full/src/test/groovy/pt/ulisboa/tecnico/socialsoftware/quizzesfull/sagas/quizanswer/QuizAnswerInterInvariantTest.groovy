package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.quizanswer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.aggregate.QuizAnswer
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quizanswer.notification.handling.QuizAnswerEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.InterInvariantTestBase

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class QuizAnswerInterInvariantTest extends InterInvariantTestBase {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    QuizAnswerEventHandling quizAnswerEventHandling

    def setup() {
        buildFixture([Stage.QUIZ, Stage.ENROLLMENT] as Set)
    }

    // ─── USER_EXISTS — DeleteUserEvent ───────────────────────────────────────

    def "quizAnswer is deleted on DeleteUserEvent"() {
        given:
        def quizAnswer = createQuizAnswer(quizId, userId)

        when: 'user is deleted, publishing DeleteUserEvent'
        userFunctionalities.deleteUser(userId)

        and: 'quizAnswer polls for delete user events'
        quizAnswerEventHandling.handleDeleteUserEvents()

        and: 'attempt to load the now-deleted quizAnswer'
        loadForCheck(quizAnswer.aggregateId, QuizAnswer)

        then:
        thrown(SimulatorException)
    }

    def "quizAnswer ignores DeleteUserEvent for unrelated user"() {
        given:
        def user2 = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        def quizAnswer = createQuizAnswer(quizId, userId)

        when: 'an unrelated user is deleted'
        userFunctionalities.deleteUser(user2.aggregateId)

        and: 'quizAnswer polls for delete user events'
        quizAnswerEventHandling.handleDeleteUserEvents()

        then: 'quizAnswer is still active'
        def unchanged = loadForCheck(quizAnswer.aggregateId, QuizAnswer)
        unchanged.state == AggregateState.ACTIVE
    }

    // ─── USER_EXISTS — UpdateStudentNameEvent ─────────────────────────────────

    def "quizAnswer reflects UpdateStudentNameEvent"() {
        given:
        def quizAnswer = createQuizAnswer(quizId, userId)

        when: 'user name is updated, publishing UpdateStudentNameEvent'
        def uow = unitOfWorkService.createUnitOfWork("updateUserName")
        userService.updateUserName(userId, USER_NAME_2, uow)
        unitOfWorkService.commit(uow)

        and: 'quizAnswer polls for update student name events'
        quizAnswerEventHandling.handleUpdateStudentNameEvents()

        then: 'cached user name in quizAnswer is updated'
        def updated = loadForCheck(quizAnswer.aggregateId, QuizAnswer)
        updated.userName == USER_NAME_2
    }

    def "quizAnswer ignores UpdateStudentNameEvent for unrelated user"() {
        given:
        def user2 = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        def quizAnswer = createQuizAnswer(quizId, userId)

        when: 'an unrelated user name is updated'
        def uow = unitOfWorkService.createUnitOfWork("updateUserName")
        userService.updateUserName(user2.aggregateId, "New Name", uow)
        unitOfWorkService.commit(uow)

        and: 'quizAnswer polls for update student name events'
        quizAnswerEventHandling.handleUpdateStudentNameEvents()

        then: 'cached user name in quizAnswer is unchanged'
        def unchanged = loadForCheck(quizAnswer.aggregateId, QuizAnswer)
        unchanged.userName == USER_NAME_1
    }

    // ─── USER_EXISTS — AnonymizeStudentEvent ──────────────────────────────────

    def "quizAnswer reflects AnonymizeStudentEvent"() {
        given:
        def quizAnswer = createQuizAnswer(quizId, userId)

        when: 'user is anonymized, publishing AnonymizeStudentEvent'
        def uow = unitOfWorkService.createUnitOfWork("anonymizeUser")
        userService.anonymizeUser(userId, uow)
        unitOfWorkService.commit(uow)

        and: 'quizAnswer polls for anonymize student events'
        quizAnswerEventHandling.handleAnonymizeStudentEvents()

        then: 'cached user fields in quizAnswer are anonymized'
        def updated = loadForCheck(quizAnswer.aggregateId, QuizAnswer)
        updated.userName == "ANONYMOUS"
        updated.userUsername == "ANONYMOUS"
    }

    def "quizAnswer ignores AnonymizeStudentEvent for unrelated user"() {
        given:
        def user2 = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        def quizAnswer = createQuizAnswer(quizId, userId)

        when: 'an unrelated user is anonymized'
        def uow = unitOfWorkService.createUnitOfWork("anonymizeUser")
        userService.anonymizeUser(user2.aggregateId, uow)
        unitOfWorkService.commit(uow)

        and: 'quizAnswer polls for anonymize student events'
        quizAnswerEventHandling.handleAnonymizeStudentEvents()

        then: 'cached user fields in quizAnswer are unchanged'
        def unchanged = loadForCheck(quizAnswer.aggregateId, QuizAnswer)
        unchanged.userName == USER_NAME_1
        unchanged.userUsername == USER_USERNAME_1
    }

    // ─── USER_EXISTS — DisenrollStudentFromCourseExecutionEvent ──────────────

    def "quizAnswer is deleted on DisenrollStudentFromCourseExecutionEvent"() {
        given:
        def quizAnswer = createQuizAnswer(quizId, userId)

        when: 'student is disenrolled, publishing DisenrollStudentFromCourseExecutionEvent'
        executionFunctionalities.disenrollStudent(executionId, userId)

        and: 'quizAnswer polls for disenroll student events'
        quizAnswerEventHandling.handleDisenrollStudentFromCourseExecutionEvents()

        and: 'attempt to load the now-deleted quizAnswer'
        loadForCheck(quizAnswer.aggregateId, QuizAnswer)

        then:
        thrown(SimulatorException)
    }

    def "quizAnswer ignores DisenrollStudentFromCourseExecutionEvent for unrelated student"() {
        given:
        def user2 = createUser(USER_NAME_2, "janedoe", STUDENT_ROLE)
        executionFunctionalities.enrollStudentInExecution(executionId, user2.aggregateId)
        def quizAnswer = createQuizAnswer(quizId, userId)

        when: 'an unrelated student is disenrolled'
        executionFunctionalities.disenrollStudent(executionId, user2.aggregateId)

        and: 'quizAnswer polls for disenroll student events'
        quizAnswerEventHandling.handleDisenrollStudentFromCourseExecutionEvents()

        then: 'quizAnswer for user1 is still active'
        def unchanged = loadForCheck(quizAnswer.aggregateId, QuizAnswer)
        unchanged.state == AggregateState.ACTIVE
    }

    // ─── QUESTION_EXISTS — UpdateQuestionEvent ───────────────────────────────

    def "quizAnswer reflects UpdateQuestionEvent"() {
        given:
        def quizAnswer = createQuizAnswer(quizId, userId)
        quizAnswerFunctionalities.answerQuestion(quizAnswer.aggregateId, questionId, 1, 30)
        def before = unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswer.aggregateId, unitOfWorkService.createUnitOfWork("before")) as QuizAnswer
        def versionBefore = before.questionAnswers.find { it.questionAggregateId == questionId }.questionVersion

        when: 'question is updated, publishing UpdateQuestionEvent'
        questionFunctionalities.updateQuestion(questionId, "Updated Title", "Updated Content", [topicId])

        and: 'quizAnswer polls for update question events'
        quizAnswerEventHandling.handleUpdateQuestionEvents()

        then: 'cached questionVersion in questionAnswer is updated to the new version'
        def updated = loadForCheck(quizAnswer.aggregateId, QuizAnswer)
        def qa = updated.questionAnswers.find { it.questionAggregateId == questionId }
        qa != null && qa.questionVersion > versionBefore
    }

    def "quizAnswer ignores UpdateQuestionEvent for unrelated question"() {
        given:
        def question2 = createQuestion(courseId, [topicId], "Q2 Title", "Q2 Content")
        def quizAnswer = createQuizAnswer(quizId, userId)
        quizAnswerFunctionalities.answerQuestion(quizAnswer.aggregateId, questionId, 1, 30)
        def before = unitOfWorkService.aggregateLoadAndRegisterRead(quizAnswer.aggregateId, unitOfWorkService.createUnitOfWork("before")) as QuizAnswer
        def versionBefore = before.questionAnswers.find { it.questionAggregateId == questionId }.questionVersion

        when: 'an unrelated question is updated'
        questionFunctionalities.updateQuestion(question2.aggregateId, "Q2 Updated", "Q2 Updated Content", [topicId])

        and: 'quizAnswer polls for update question events'
        quizAnswerEventHandling.handleUpdateQuestionEvents()

        then: 'questionVersion for the answered question is unchanged'
        def unchanged = loadForCheck(quizAnswer.aggregateId, QuizAnswer)
        def qa = unchanged.questionAnswers.find { it.questionAggregateId == questionId }
        qa != null && qa.questionVersion == versionBefore
    }

    // ─── COURSE_EXECUTION_EXISTS — DeleteCourseExecutionEvent ─────────────────

    def "quizAnswer is deleted on DeleteCourseExecutionEvent"() {
        given:
        def quizAnswer = createQuizAnswer(quizId, userId)

        when: 'execution is deleted, publishing DeleteCourseExecutionEvent'
        executionFunctionalities.deleteExecution(executionId)

        and: 'quizAnswer polls for delete course execution events'
        quizAnswerEventHandling.handleDeleteCourseExecutionEvents()

        and: 'attempt to load the now-deleted quizAnswer'
        loadForCheck(quizAnswer.aggregateId, QuizAnswer)

        then:
        thrown(SimulatorException)
    }

    def "quizAnswer ignores DeleteCourseExecutionEvent for unrelated execution"() {
        given:
        def execution2 = createExecution(courseId, "EA002", "2nd Semester 2024/25")
        def quizAnswer = createQuizAnswer(quizId, userId)

        when: 'an unrelated execution is deleted'
        executionFunctionalities.deleteExecution(execution2.aggregateId)

        and: 'quizAnswer polls for delete course execution events'
        quizAnswerEventHandling.handleDeleteCourseExecutionEvents()

        then: 'quizAnswer is still active'
        def unchanged = loadForCheck(quizAnswer.aggregateId, QuizAnswer)
        unchanged.state == AggregateState.ACTIVE
    }

    // ─── QUIZ_EXISTS — InvalidateQuizEvent ───────────────────────────────────

    def "quizAnswer is deleted on InvalidateQuizEvent"() {
        given:
        def quizAnswer = createQuizAnswer(quizId, userId)

        when: 'quiz is invalidated, publishing InvalidateQuizEvent'
        quizFunctionalities.invalidateQuizByEvent(quizId)

        and: 'quizAnswer polls for invalidate quiz events'
        quizAnswerEventHandling.handleInvalidateQuizEvents()

        and: 'attempt to load the now-deleted quizAnswer'
        loadForCheck(quizAnswer.aggregateId, QuizAnswer)

        then:
        thrown(SimulatorException)
    }

    def "quizAnswer ignores InvalidateQuizEvent for unrelated quiz"() {
        given:
        def quiz2 = createQuiz(executionId, [questionId])
        def quizAnswer = createQuizAnswer(quizId, userId)

        when: 'an unrelated quiz is invalidated'
        quizFunctionalities.invalidateQuizByEvent(quiz2.aggregateId)

        and: 'quizAnswer polls for invalidate quiz events'
        quizAnswerEventHandling.handleInvalidateQuizEvents()

        then: 'quizAnswer is still active'
        def unchanged = loadForCheck(quizAnswer.aggregateId, QuizAnswer)
        unchanged.state == AggregateState.ACTIVE
    }
}
