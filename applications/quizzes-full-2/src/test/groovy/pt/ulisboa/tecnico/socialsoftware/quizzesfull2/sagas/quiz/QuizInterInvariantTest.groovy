package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.quiz

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.sagas.SagaQuiz
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.notification.handling.QuizEventHandling

@DataJpaTest
@Transactional
@Import(QuizInterInvariantTest.LocalBeanConfiguration)
class QuizInterInvariantTest extends QuizzesFull2SpockTest {

    public static final String OTHER_QUESTION_TITLE = "Graph traversal"
    public static final String OTHER_QUESTION_CONTENT = "What is the complexity of BFS?"
    public static final String UPDATED_QUESTION_TITLE = "Merge sort complexity"
    public static final String UPDATED_QUESTION_CONTENT = "What is the worst-case complexity of merge sort?"
    public static final String OTHER_EXECUTION_ACRONYM = "SE-02"

    @Autowired
    QuizEventHandling quizEventHandling

    def "quiz updates the cached question snapshot on UpdateQuestionEvent"() {
        // Spec: plan.md §6 Quiz — subscribed UpdateQuestionEvent; grouping §2 Quiz/Question row
        // caches (title, content, questionVersion). Payload fields: title, content.
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def questionAggregateId = createQuestion(courseAggregateId)
        def quizAggregateId = createQuizWithQuestions(executionAggregateId, [questionAggregateId])
        def versionBefore = questionOf(quizAggregateId, questionAggregateId).questionVersion

        when: 'the question is updated'
        questionFunctionalities.updateQuestion(questionAggregateId, UPDATED_QUESTION_TITLE,
                UPDATED_QUESTION_CONTENT, [])

        and: 'the quiz polls for the event'
        quizEventHandling.handleUpdateQuestionEvents()

        then: 'the cached question carries the payload and advances past the published version'
        def question = questionOf(quizAggregateId, questionAggregateId)
        question.title == UPDATED_QUESTION_TITLE
        question.content == UPDATED_QUESTION_CONTENT
        question.questionVersion > versionBefore
    }

    def "quiz ignores an UpdateQuestionEvent for a question it does not contain"() {
        // Spec: plan.md §6 Quiz — the subscription is anchored on each cached question's aggregate id
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def questionAggregateId = createQuestion(courseAggregateId)
        def unrelatedQuestionAggregateId = createQuestion(courseAggregateId, OTHER_QUESTION_TITLE,
                OTHER_QUESTION_CONTENT)
        def quizAggregateId = createQuizWithQuestions(executionAggregateId, [questionAggregateId])
        def titleBefore = questionOf(quizAggregateId, questionAggregateId).title
        def versionBefore = questionOf(quizAggregateId, questionAggregateId).questionVersion

        when: 'a question the quiz does not contain is updated'
        questionFunctionalities.updateQuestion(unrelatedQuestionAggregateId, UPDATED_QUESTION_TITLE,
                UPDATED_QUESTION_CONTENT, [])

        and: 'the quiz polls for the event'
        quizEventHandling.handleUpdateQuestionEvents()

        then: 'the cached question snapshot is untouched'
        def question = questionOf(quizAggregateId, questionAggregateId)
        question.title == titleBefore
        question.questionVersion == versionBefore
    }

    def "quiz invalidates itself on DeleteQuestionEvent"() {
        // Spec: plan.md §3.2 — rule QUESTION_EXISTS (Quiz), P2: a quiz whose question was soft-deleted
        // is no longer answerable, so the whole quiz is invalidated rather than the question dropped.
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def questionAggregateId = createQuestion(courseAggregateId)
        def quizAggregateId = createQuizWithQuestions(executionAggregateId, [questionAggregateId])

        when: 'the question is deleted'
        questionFunctionalities.deleteQuestion(questionAggregateId)

        and: 'the quiz polls for the event'
        quizEventHandling.handleDeleteQuestionEvents()

        and: 'attempt to load the now-invalidated quiz'
        loadForCheck(quizAggregateId, SagaQuiz)

        then: 'DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

    def "quiz ignores a DeleteQuestionEvent for a question it does not contain"() {
        // Spec: plan.md §3.2 — rule QUESTION_EXISTS (Quiz) invalidates only the quizzes that contain
        // the deleted question
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def questionAggregateId = createQuestion(courseAggregateId)
        def unrelatedQuestionAggregateId = createQuestion(courseAggregateId, OTHER_QUESTION_TITLE,
                OTHER_QUESTION_CONTENT)
        def quizAggregateId = createQuizWithQuestions(executionAggregateId, [questionAggregateId])
        def versionBefore = questionOf(quizAggregateId, questionAggregateId).questionVersion

        when: 'a question the quiz does not contain is deleted'
        questionFunctionalities.deleteQuestion(unrelatedQuestionAggregateId)

        and: 'the quiz polls for the event'
        quizEventHandling.handleDeleteQuestionEvents()

        then: 'the quiz survives with its cached question snapshot intact'
        def question = questionOf(quizAggregateId, questionAggregateId)
        question != null
        question.questionVersion == versionBefore
    }

    def "quiz is removed on DeleteCourseExecutionEvent"() {
        // Spec: plan.md §3.2 — rule COURSE_EXECUTION_EXISTS (Quiz), P2: a quiz cannot outlive the
        // course execution it belongs to.
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId)

        when: 'the course execution is deleted'
        executionFunctionalities.deleteExecution(executionAggregateId)

        and: 'the quiz polls for the event'
        quizEventHandling.handleDeleteCourseExecutionEvents()

        and: 'attempt to load the now-removed quiz'
        loadForCheck(quizAggregateId, SagaQuiz)

        then: 'DELETED aggregate is not loadable'
        thrown(SimulatorException)
    }

    def "quiz ignores a DeleteCourseExecutionEvent for another execution"() {
        // Spec: plan.md §6 Quiz — the subscription is anchored on the cached QuizExecution snapshot
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def unrelatedExecutionAggregateId = createExecution(courseAggregateId, OTHER_EXECUTION_ACRONYM)
        def quizAggregateId = createQuiz(executionAggregateId)
        def versionBefore = quizOf(quizAggregateId).executionVersion

        when: 'another course execution is deleted'
        executionFunctionalities.deleteExecution(unrelatedExecutionAggregateId)

        and: 'the quiz polls for the event'
        quizEventHandling.handleDeleteCourseExecutionEvents()

        then: 'the quiz survives with its cached execution snapshot intact'
        def quiz = quizOf(quizAggregateId)
        quiz.executionAggregateId == executionAggregateId
        quiz.executionVersion == versionBefore
    }

    def "quiz ignores a replayed UpdateQuestionEvent"() {
        // Spec: events.md § "Advance the cached publisher version" — a poll that re-delivers an event
        // already folded in must write nothing, or the projection gains a version on every poll and
        // the backlog never converges.
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def questionAggregateId = createQuestion(courseAggregateId)
        def quizAggregateId = createQuizWithQuestions(executionAggregateId, [questionAggregateId])
        questionFunctionalities.updateQuestion(questionAggregateId, UPDATED_QUESTION_TITLE,
                UPDATED_QUESTION_CONTENT, [])
        quizEventHandling.handleUpdateQuestionEvents()
        def versionAfterFirstPoll = quizOf(quizAggregateId).version

        when: 'the quiz polls a second time'
        quizEventHandling.handleUpdateQuestionEvents()

        then: 'the second poll writes nothing and the cached question still carries the payload'
        quizOf(quizAggregateId).version == versionAfterFirstPoll
        questionOf(quizAggregateId, questionAggregateId).title == UPDATED_QUESTION_TITLE
    }

    def "quiz keeps the newest question snapshot when a stale UpdateQuestionEvent trails it"() {
        // Spec: events.md § "Advance the cached publisher version" — findUnprocessedEvents orders by
        // timestamp DESC, so one poll applies the newer update first and then reaches the older one.
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def questionAggregateId = createQuestion(courseAggregateId)
        def quizAggregateId = createQuizWithQuestions(executionAggregateId, [questionAggregateId])

        when: 'the question is updated twice before the quiz polls'
        questionFunctionalities.updateQuestion(questionAggregateId, OTHER_QUESTION_TITLE,
                OTHER_QUESTION_CONTENT, [])
        questionFunctionalities.updateQuestion(questionAggregateId, UPDATED_QUESTION_TITLE,
                UPDATED_QUESTION_CONTENT, [])

        and: 'the quiz drains both pending events in one poll'
        quizEventHandling.handleUpdateQuestionEvents()

        then: 'the newest update survives the older event that trails it'
        def question = questionOf(quizAggregateId, questionAggregateId)
        question.title == UPDATED_QUESTION_TITLE
        question.content == UPDATED_QUESTION_CONTENT
    }

    private Integer createQuizWithQuestions(Integer executionAggregateId, List<Integer> questionAggregateIds) {
        return createQuiz(executionAggregateId, QUIZ_TITLE, QUIZ_AVAILABLE_DATE, QUIZ_CONCLUSION_DATE,
                QUIZ_RESULTS_DATE, QUIZ_TYPE, questionAggregateIds)
    }

    private QuizQuestionDto questionOf(Integer quizAggregateId, Integer questionAggregateId) {
        return quizOf(quizAggregateId).questions.find { it.questionAggregateId == questionAggregateId }
    }

    private quizOf(Integer quizAggregateId) {
        return quizService.getQuizById(quizAggregateId, unitOfWorkService.createUnitOfWork("check"))
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
