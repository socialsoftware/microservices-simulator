package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.quizanswer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.domain.QuizzesFull2DomainConstants
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuestionAnswerDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.sagas.SagaQuizAnswer
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.notification.handling.QuizAnswerEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.notification.handling.QuizEventHandling

@DataJpaTest
@Transactional
@Import(QuizAnswerInterInvariantTest.LocalBeanConfiguration)
class QuizAnswerInterInvariantTest extends QuizzesFull2SpockTest {

    public static final String OTHER_USER_NAME = "Carol White"
    public static final String OTHER_USER_USERNAME = "carol"
    public static final String UPDATED_USER_NAME = "Alice A. Smith"
    public static final String STALE_USER_NAME = "Alice Smyth"
    public static final String OTHER_QUESTION_TITLE = "Graph traversal"
    public static final String OTHER_QUESTION_CONTENT = "What is the complexity of BFS?"
    public static final String UPDATED_QUESTION_TITLE = "Merge sort revisited"
    public static final String UPDATED_QUESTION_CONTENT = "What is the average complexity of merge sort?"
    public static final String OTHER_EXECUTION_ACRONYM = "SE-02"

    @Autowired
    QuizAnswerEventHandling quizAnswerEventHandling

    @Autowired
    QuizEventHandling quizEventHandling

    Integer courseAggregateId
    Integer executionAggregateId
    Integer userAggregateId
    Integer questionAggregateId
    Integer quizAggregateId
    Integer quizAnswerAggregateId

    def setup() {
        courseAggregateId = createCourse()
        executionAggregateId = createExecution(courseAggregateId)
        userAggregateId = createActiveUser()
        enrollStudentInExecution(executionAggregateId, userAggregateId)
        questionAggregateId = createQuestionWithOptions(courseAggregateId)
        quizAggregateId = createQuiz(executionAggregateId, QUIZ_TITLE, QUIZ_AVAILABLE_DATE,
                QUIZ_CONCLUSION_DATE, QUIZ_RESULTS_DATE, QUIZ_TYPE, [questionAggregateId])
        quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)
    }

    def "quiz answer updates the cached student name on UpdateStudentNameEvent"() {
        // Spec: plan.md §7 QuizAnswer — subscribed UpdateStudentNameEvent; grouping §2
        // QuizAnswer/User row caches (userName, userVersion). Payload field: updatedName.
        given:
        def versionBefore = quizAnswerOf(quizAnswerAggregateId).userVersion

        when: 'the student is renamed'
        userFunctionalities.updateUserName(userAggregateId, UPDATED_USER_NAME)

        and: 'the quiz answer polls for the event'
        quizAnswerEventHandling.handleUpdateStudentNameEvents()

        then: 'the cached student carries the payload and advances past the published version'
        def quizAnswer = quizAnswerOf(quizAnswerAggregateId)
        quizAnswer.userName == UPDATED_USER_NAME
        quizAnswer.userVersion > versionBefore
    }

    def "quiz answer ignores an UpdateStudentNameEvent for another user"() {
        // Spec: plan.md §7 QuizAnswer — the subscription is anchored on the cached student's user id
        given:
        def otherUserAggregateId = createActiveUser(OTHER_USER_NAME, OTHER_USER_USERNAME)
        def nameBefore = quizAnswerOf(quizAnswerAggregateId).userName
        def versionBefore = quizAnswerOf(quizAnswerAggregateId).userVersion

        when: 'an unrelated user is renamed'
        userFunctionalities.updateUserName(otherUserAggregateId, UPDATED_USER_NAME)

        and: 'the quiz answer polls for the event'
        quizAnswerEventHandling.handleUpdateStudentNameEvents()

        then: 'the cached student snapshot is untouched'
        def quizAnswer = quizAnswerOf(quizAnswerAggregateId)
        quizAnswer.userName == nameBefore
        quizAnswer.userVersion == versionBefore
    }

    def "quiz answer anonymizes the cached student on AnonymizeStudentEvent"() {
        // Spec: plan.md §7 QuizAnswer — subscribed AnonymizeStudentEvent; the student snapshot caches
        // no username, so the payload field that reaches it is name.
        given:
        def versionBefore = quizAnswerOf(quizAnswerAggregateId).userVersion

        when: 'the student is anonymized'
        userFunctionalities.anonymizeUser(userAggregateId)

        and: 'the quiz answer polls for the event'
        quizAnswerEventHandling.handleAnonymizeStudentEvents()

        then: 'the cached student name is anonymized and the version advances'
        def quizAnswer = quizAnswerOf(quizAnswerAggregateId)
        quizAnswer.userName == QuizzesFull2DomainConstants.ANONYMOUS
        quizAnswer.userVersion > versionBefore
    }

    def "quiz answer ignores an AnonymizeStudentEvent for another user"() {
        // Spec: plan.md §7 QuizAnswer — the subscription is anchored on the cached student's user id
        given:
        def otherUserAggregateId = createActiveUser(OTHER_USER_NAME, OTHER_USER_USERNAME)
        def nameBefore = quizAnswerOf(quizAnswerAggregateId).userName
        def versionBefore = quizAnswerOf(quizAnswerAggregateId).userVersion

        when: 'an unrelated user is anonymized'
        userFunctionalities.anonymizeUser(otherUserAggregateId)

        and: 'the quiz answer polls for the event'
        quizAnswerEventHandling.handleAnonymizeStudentEvents()

        then: 'the cached student snapshot is untouched'
        def quizAnswer = quizAnswerOf(quizAnswerAggregateId)
        quizAnswer.userName == nameBefore
        quizAnswer.userVersion == versionBefore
    }

    def "quiz answer is removed on DeleteUserEvent"() {
        // Spec: plan.md §3.2 — rule USER_EXISTS (QuizAnswer), P2: an answer session whose student was
        // deleted references a User that no longer exists, and the reference is structural.
        when: 'the student is deleted'
        userFunctionalities.deleteUser(userAggregateId)

        and: 'the quiz answer polls for the event'
        quizAnswerEventHandling.handleDeleteUserEvents()

        and: 'attempt to load the now-removed quiz answer'
        loadForCheck(quizAnswerAggregateId, SagaQuizAnswer)

        then: 'DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

    def "quiz answer ignores a DeleteUserEvent for another user"() {
        // Spec: plan.md §3.2 — rule USER_EXISTS (QuizAnswer) removes only the deleted student's session
        given:
        def otherUserAggregateId = createActiveUser(OTHER_USER_NAME, OTHER_USER_USERNAME)
        def versionBefore = quizAnswerOf(quizAnswerAggregateId).userVersion

        when: 'an unrelated user is deleted'
        userFunctionalities.deleteUser(otherUserAggregateId)

        and: 'the quiz answer polls for the event'
        quizAnswerEventHandling.handleDeleteUserEvents()

        then: 'the quiz answer survives with its cached student snapshot intact'
        def quizAnswer = quizAnswerOf(quizAnswerAggregateId)
        quizAnswer.userAggregateId == userAggregateId
        quizAnswer.userVersion == versionBefore
    }

    def "quiz answer advances the cached question version on UpdateQuestionEvent"() {
        // Spec: grouping §2 QuestionAnswer/Question row — correctOptionKey is seeded once and never
        // refreshed, so this subscription exists to track questionVersion.
        given:
        def versionBefore = questionAnswerOf(quizAnswerAggregateId, questionAggregateId).questionVersion

        when: 'the question is updated'
        questionFunctionalities.updateQuestion(questionAggregateId, UPDATED_QUESTION_TITLE,
                UPDATED_QUESTION_CONTENT, [])

        and: 'the quiz answer polls for the event'
        quizAnswerEventHandling.handleUpdateQuestionEvents()

        then: 'the cached question version advances past the published version'
        def questionAnswer = questionAnswerOf(quizAnswerAggregateId, questionAggregateId)
        questionAnswer.questionVersion > versionBefore
        questionAnswer.correctOptionKey == QUESTION_ANSWER_CORRECT_OPTION_KEY
    }

    def "quiz answer ignores an UpdateQuestionEvent for a question it does not answer"() {
        // Spec: plan.md §7 QuizAnswer — each QuestionAnswer anchors its own subscription
        given:
        def otherQuestionAggregateId = createQuestionWithOptions(courseAggregateId, OTHER_QUESTION_TITLE,
                OTHER_QUESTION_CONTENT)
        def versionBefore = questionAnswerOf(quizAnswerAggregateId, questionAggregateId).questionVersion

        when: 'a question the quiz answer does not cover is updated'
        questionFunctionalities.updateQuestion(otherQuestionAggregateId, UPDATED_QUESTION_TITLE,
                UPDATED_QUESTION_CONTENT, [])

        and: 'the quiz answer polls for the event'
        quizAnswerEventHandling.handleUpdateQuestionEvents()

        then: 'the cached question snapshot is untouched'
        questionAnswerOf(quizAnswerAggregateId, questionAggregateId).questionVersion == versionBefore
    }

    def "quiz answer is removed on DeleteCourseExecutionEvent"() {
        // Spec: plan.md §3.2 — rule COURSE_EXECUTION_EXISTS (QuizAnswer), P2: an answer session cannot
        // outlive the course execution it belongs to.
        when: 'the course execution is deleted'
        executionFunctionalities.deleteExecution(executionAggregateId)

        and: 'the quiz answer polls for the event'
        quizAnswerEventHandling.handleDeleteCourseExecutionEvents()

        and: 'attempt to load the now-removed quiz answer'
        loadForCheck(quizAnswerAggregateId, SagaQuizAnswer)

        then: 'DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

    def "quiz answer ignores a DeleteCourseExecutionEvent for another execution"() {
        // Spec: plan.md §7 QuizAnswer — the subscription is anchored on the cached execution snapshot
        given:
        def otherExecutionAggregateId = createExecution(courseAggregateId, OTHER_EXECUTION_ACRONYM)
        def versionBefore = quizAnswerOf(quizAnswerAggregateId).executionVersion

        when: 'another course execution is deleted'
        executionFunctionalities.deleteExecution(otherExecutionAggregateId)

        and: 'the quiz answer polls for the event'
        quizAnswerEventHandling.handleDeleteCourseExecutionEvents()

        then: 'the quiz answer survives with its cached execution snapshot intact'
        def quizAnswer = quizAnswerOf(quizAnswerAggregateId)
        quizAnswer.executionAggregateId == executionAggregateId
        quizAnswer.executionVersion == versionBefore
    }

    def "quiz answer is removed on DisenrollStudentFromCourseExecutionEvent for its own student"() {
        // Spec: plan.md §3.2 — rule USER_EXISTS (QuizAnswer), P2: the predicate covers a student that
        // was disenrolled, not only one that was deleted.
        when: 'the student is disenrolled from the execution'
        executionFunctionalities.disenrollStudent(executionAggregateId, userAggregateId)

        and: 'the quiz answer polls for the event'
        quizAnswerEventHandling.handleDisenrollStudentFromCourseExecutionEvents()

        and: 'attempt to load the now-removed quiz answer'
        loadForCheck(quizAnswerAggregateId, SagaQuizAnswer)

        then: 'DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

    def "quiz answer ignores a DisenrollStudentFromCourseExecutionEvent for another student"() {
        // Spec: session-d.md § Shared-anchor events — the event is anchored on the execution, so every
        // quiz answer of that execution receives it; the student check discriminates in the service.
        given:
        def otherUserAggregateId = createActiveUser(OTHER_USER_NAME, OTHER_USER_USERNAME)
        enrollStudentInExecution(executionAggregateId, otherUserAggregateId)
        def versionBefore = quizAnswerOf(quizAnswerAggregateId).executionVersion

        when: 'another student of the same execution is disenrolled'
        executionFunctionalities.disenrollStudent(executionAggregateId, otherUserAggregateId)

        and: 'the quiz answer polls for the event'
        quizAnswerEventHandling.handleDisenrollStudentFromCourseExecutionEvents()

        then: 'the quiz answer survives with its cached snapshots intact'
        def quizAnswer = quizAnswerOf(quizAnswerAggregateId)
        quizAnswer.userAggregateId == userAggregateId
        quizAnswer.executionVersion == versionBefore
    }

    def "quiz answer is removed on InvalidateQuizEvent"() {
        // Spec: plan.md §3.2 — rule QUIZ_EXISTS (QuizAnswer), P2: an invalidated quiz is treated as
        // deleted downstream, so its answer sessions cannot survive it.
        when: 'a question of the quiz is deleted and the quiz invalidates itself'
        questionFunctionalities.deleteQuestion(questionAggregateId)
        quizEventHandling.handleDeleteQuestionEvents()

        and: 'the quiz answer polls for the event'
        quizAnswerEventHandling.handleInvalidateQuizEvents()

        and: 'attempt to load the now-removed quiz answer'
        loadForCheck(quizAnswerAggregateId, SagaQuizAnswer)

        then: 'DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

    def "quiz answer ignores an InvalidateQuizEvent for another quiz"() {
        // Spec: plan.md §7 QuizAnswer — the subscription is anchored on the cached quiz snapshot
        given:
        def otherQuestionAggregateId = createQuestionWithOptions(courseAggregateId, OTHER_QUESTION_TITLE,
                OTHER_QUESTION_CONTENT)
        createQuiz(executionAggregateId, QUIZ_TITLE, QUIZ_AVAILABLE_DATE, QUIZ_CONCLUSION_DATE,
                QUIZ_RESULTS_DATE, QUIZ_TYPE, [otherQuestionAggregateId])
        def versionBefore = quizAnswerOf(quizAnswerAggregateId).quizVersion

        when: 'the other quiz invalidates itself'
        questionFunctionalities.deleteQuestion(otherQuestionAggregateId)
        quizEventHandling.handleDeleteQuestionEvents()

        and: 'the quiz answer polls for the event'
        quizAnswerEventHandling.handleInvalidateQuizEvents()

        then: 'the quiz answer survives with its cached quiz snapshot intact'
        def quizAnswer = quizAnswerOf(quizAnswerAggregateId)
        quizAnswer.quizAggregateId == quizAggregateId
        quizAnswer.quizVersion == versionBefore
    }

    def "quiz answer ignores a replayed UpdateStudentNameEvent"() {
        // Spec: events.md § "Advance the cached publisher version" — a poll that re-delivers an event
        // already folded in must write nothing, or the projection gains a version on every poll and
        // the backlog never converges.
        given:
        userFunctionalities.updateUserName(userAggregateId, UPDATED_USER_NAME)
        quizAnswerEventHandling.handleUpdateStudentNameEvents()
        def versionAfterFirstPoll = quizAnswerOf(quizAnswerAggregateId).version

        when: 'the quiz answer polls a second time'
        quizAnswerEventHandling.handleUpdateStudentNameEvents()

        then: 'the second poll writes nothing and the cached student still carries the payload'
        quizAnswerOf(quizAnswerAggregateId).version == versionAfterFirstPoll
        quizAnswerOf(quizAnswerAggregateId).userName == UPDATED_USER_NAME
    }

    def "quiz answer keeps the newest student name when a stale UpdateStudentNameEvent trails it"() {
        // Spec: events.md § "Advance the cached publisher version" — findUnprocessedEvents orders by
        // timestamp DESC, so one poll applies the newer rename first and then reaches the older one.
        when: 'the student is renamed twice before the quiz answer polls'
        userFunctionalities.updateUserName(userAggregateId, STALE_USER_NAME)
        userFunctionalities.updateUserName(userAggregateId, UPDATED_USER_NAME)

        and: 'the quiz answer drains both pending events in one poll'
        quizAnswerEventHandling.handleUpdateStudentNameEvents()

        then: 'the newest rename survives the older event that trails it'
        quizAnswerOf(quizAnswerAggregateId).userName == UPDATED_USER_NAME
    }

    private QuestionAnswerDto questionAnswerOf(Integer quizAnswerAggregateId, Integer questionAggregateId) {
        return quizAnswerOf(quizAnswerAggregateId).questionAnswers
                .find { it.questionAggregateId == questionAggregateId }
    }

    private quizAnswerOf(Integer quizAnswerAggregateId) {
        return quizAnswerService.getQuizAnswerById(quizAnswerAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
