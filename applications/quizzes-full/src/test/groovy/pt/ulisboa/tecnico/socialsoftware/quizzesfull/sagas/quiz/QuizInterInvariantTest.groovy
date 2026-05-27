package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.quiz

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.Quiz
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.notification.handling.QuizEventHandling
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.InterInvariantTestBase

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class QuizInterInvariantTest extends InterInvariantTestBase {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    QuizEventHandling quizEventHandling

    def setup() {
        buildFixture([Stage.QUIZ] as Set)
    }

    // ─── QUESTION_EXISTS — UpdateQuestionEvent ───────────────────────────────

    def "quiz updates cachedQuestion on UpdateQuestionEvent"() {
        when: 'question is updated, publishing UpdateQuestionEvent'
        questionFunctionalities.updateQuestion(questionId, "Updated Title", "Updated Content", [topicId])

        and: 'quiz polls for update question events'
        quizEventHandling.handleUpdateQuestionEvents()

        then: 'cached question fields in quiz are updated'
        def updatedQuiz = loadForCheck(quizId, Quiz)
        updatedQuiz.questions.any { it.questionAggregateId == questionId && it.title == "Updated Title" && it.content == "Updated Content" }
    }

    def "quiz ignores UpdateQuestionEvent for unrelated question"() {
        given:
        def question2 = createQuestion(courseId, [topicId], "Q2 Title", "Q2 Content")

        when: 'unrelated question is updated'
        questionFunctionalities.updateQuestion(question2.aggregateId, "Q2 Updated", "Q2 Updated Content", [topicId])

        and: 'quiz polls for update question events'
        quizEventHandling.handleUpdateQuestionEvents()

        then: 'cached question1 fields in quiz are unchanged'
        def unchanged = loadForCheck(quizId, Quiz)
        unchanged.questions.any { it.questionAggregateId == questionId && it.title == "Q1 Title" && it.content == "Q1 Content" }
    }

    // ─── QUESTION_EXISTS — DeleteQuestionEvent ───────────────────────────────

    def "quiz invalidates self on DeleteQuestionEvent"() {
        when: 'question is deleted, publishing DeleteQuestionEvent'
        questionFunctionalities.deleteQuestion(questionId)

        and: 'quiz polls for delete question events'
        quizEventHandling.handleDeleteQuestionEvents()

        and: 'loading the quiz is attempted'
        loadForCheck(quizId, Quiz)

        then: 'quiz is no longer loadable (deleted)'
        thrown(SimulatorException)
    }

    def "quiz ignores DeleteQuestionEvent for unrelated question"() {
        given:
        def question2 = createQuestion(courseId, [topicId], "Q2 Title", "Q2 Content")

        when: 'unrelated question is deleted'
        questionFunctionalities.deleteQuestion(question2.aggregateId)

        and: 'quiz polls for delete question events'
        quizEventHandling.handleDeleteQuestionEvents()

        then: 'quiz is still active'
        def unchanged = loadForCheck(quizId, Quiz)
        unchanged.state == AggregateState.ACTIVE
    }

    // ─── COURSE_EXECUTION_EXISTS — DeleteCourseExecutionEvent ─────────────────

    def "quiz invalidates self on DeleteCourseExecutionEvent"() {
        when: 'execution is deleted, publishing DeleteCourseExecutionEvent'
        executionFunctionalities.deleteExecution(executionId)

        and: 'quiz polls for delete course execution events'
        quizEventHandling.handleDeleteCourseExecutionEvents()

        and: 'loading the quiz is attempted'
        loadForCheck(quizId, Quiz)

        then: 'quiz is no longer loadable (deleted)'
        thrown(SimulatorException)
    }

    def "quiz ignores DeleteCourseExecutionEvent for unrelated execution"() {
        given:
        def execution2 = createExecution(courseId, "SE-2027", "2027/2028")

        when: 'unrelated execution is deleted'
        executionFunctionalities.deleteExecution(execution2.aggregateId)

        and: 'quiz polls for delete course execution events'
        quizEventHandling.handleDeleteCourseExecutionEvents()

        then: 'quiz is still active'
        def unchanged = loadForCheck(quizId, Quiz)
        unchanged.state == AggregateState.ACTIVE
    }
}
