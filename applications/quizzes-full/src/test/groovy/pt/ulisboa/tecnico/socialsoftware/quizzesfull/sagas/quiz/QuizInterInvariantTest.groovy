package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.quiz

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.Quiz
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.notification.handling.QuizEventHandling

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class QuizInterInvariantTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    QuizEventHandling quizEventHandling

    // ─── QUESTION_EXISTS — UpdateQuestionEvent ───────────────────────────────

    def "quiz reflects UpdateQuestionEvent"() {
        given:
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        createExecution(course.aggregateId, "SE-2025", "2025/2026")
        def topic = createTopic(course.aggregateId, "Topic A")
        def question = createQuestion(course.aggregateId, [topic.aggregateId], "Original Title", "Original Content")
        def execution = createExecution(course.aggregateId, "SE-2026", "2026/2027")
        def quiz = createQuiz(execution.aggregateId, [question.aggregateId])

        when: 'question is updated, publishing UpdateQuestionEvent'
        questionFunctionalities.updateQuestion(question.aggregateId, "Updated Title", "Updated Content", [topic.aggregateId])

        and: 'quiz polls for update question events'
        quizEventHandling.handleUpdateQuestionEvents()

        then: 'cached question fields in quiz are updated'
        def updatedQuiz = loadForCheck(quiz.aggregateId, Quiz)
        updatedQuiz.questions.any { it.questionAggregateId == question.aggregateId && it.title == "Updated Title" && it.content == "Updated Content" }
    }

    def "quiz ignores UpdateQuestionEvent for unrelated question"() {
        given:
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        createExecution(course.aggregateId, "SE-2025", "2025/2026")
        def topic = createTopic(course.aggregateId, "Topic A")
        def question1 = createQuestion(course.aggregateId, [topic.aggregateId], "Q1 Title", "Q1 Content")
        def question2 = createQuestion(course.aggregateId, [topic.aggregateId], "Q2 Title", "Q2 Content")
        def execution = createExecution(course.aggregateId, "SE-2026", "2026/2027")
        def quiz = createQuiz(execution.aggregateId, [question1.aggregateId])

        when: 'unrelated question is updated'
        questionFunctionalities.updateQuestion(question2.aggregateId, "Q2 Updated", "Q2 Updated Content", [topic.aggregateId])

        and: 'quiz polls for update question events'
        quizEventHandling.handleUpdateQuestionEvents()

        then: 'cached question1 fields in quiz are unchanged'
        def unchanged = loadForCheck(quiz.aggregateId, Quiz)
        unchanged.questions.any { it.questionAggregateId == question1.aggregateId && it.title == "Q1 Title" && it.content == "Q1 Content" }
    }

    // ─── QUESTION_EXISTS — DeleteQuestionEvent ───────────────────────────────

    def "quiz is invalidated on DeleteQuestionEvent"() {
        given:
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        createExecution(course.aggregateId, "SE-2025", "2025/2026")
        def topic = createTopic(course.aggregateId, "Topic A")
        def question = createQuestion(course.aggregateId, [topic.aggregateId], "Q1 Title", "Q1 Content")
        def execution = createExecution(course.aggregateId, "SE-2026", "2026/2027")
        def quiz = createQuiz(execution.aggregateId, [question.aggregateId])

        when: 'question is deleted, publishing DeleteQuestionEvent'
        questionFunctionalities.deleteQuestion(question.aggregateId)

        and: 'quiz polls for delete question events'
        quizEventHandling.handleDeleteQuestionEvents()

        and: 'loading the quiz is attempted'
        loadForCheck(quiz.aggregateId, Quiz)

        then: 'quiz is no longer loadable (deleted)'
        thrown(SimulatorException)
    }

    def "quiz ignores DeleteQuestionEvent for unrelated question"() {
        given:
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        createExecution(course.aggregateId, "SE-2025", "2025/2026")
        def topic = createTopic(course.aggregateId, "Topic A")
        def question1 = createQuestion(course.aggregateId, [topic.aggregateId], "Q1 Title", "Q1 Content")
        def question2 = createQuestion(course.aggregateId, [topic.aggregateId], "Q2 Title", "Q2 Content")
        def execution = createExecution(course.aggregateId, "SE-2026", "2026/2027")
        def quiz = createQuiz(execution.aggregateId, [question1.aggregateId])

        when: 'unrelated question is deleted'
        questionFunctionalities.deleteQuestion(question2.aggregateId)

        and: 'quiz polls for delete question events'
        quizEventHandling.handleDeleteQuestionEvents()

        then: 'quiz is still active'
        def unchanged = loadForCheck(quiz.aggregateId, Quiz)
        unchanged.state == AggregateState.ACTIVE
    }

    // ─── COURSE_EXECUTION_EXISTS — DeleteCourseExecutionEvent ─────────────────

    def "quiz is invalidated on DeleteCourseExecutionEvent"() {
        given:
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        createExecution(course.aggregateId, "SE-2025", "2025/2026")
        def topic = createTopic(course.aggregateId, "Topic A")
        def question = createQuestion(course.aggregateId, [topic.aggregateId], "Q1 Title", "Q1 Content")
        def execution = createExecution(course.aggregateId, "SE-2026", "2026/2027")
        def quiz = createQuiz(execution.aggregateId, [question.aggregateId])

        when: 'execution is deleted, publishing DeleteCourseExecutionEvent'
        executionFunctionalities.deleteExecution(execution.aggregateId)

        and: 'quiz polls for delete course execution events'
        quizEventHandling.handleDeleteCourseExecutionEvents()

        and: 'loading the quiz is attempted'
        loadForCheck(quiz.aggregateId, Quiz)

        then: 'quiz is no longer loadable (deleted)'
        thrown(SimulatorException)
    }

    def "quiz ignores DeleteCourseExecutionEvent for unrelated execution"() {
        given:
        def course = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        createExecution(course.aggregateId, "SE-2025", "2025/2026")
        def topic = createTopic(course.aggregateId, "Topic A")
        def question = createQuestion(course.aggregateId, [topic.aggregateId], "Q1 Title", "Q1 Content")
        def execution1 = createExecution(course.aggregateId, "SE-2026", "2026/2027")
        def execution2 = createExecution(course.aggregateId, "SE-2027", "2027/2028")
        def quiz = createQuiz(execution1.aggregateId, [question.aggregateId])

        when: 'unrelated execution is deleted'
        executionFunctionalities.deleteExecution(execution2.aggregateId)

        and: 'quiz polls for delete course execution events'
        quizEventHandling.handleDeleteCourseExecutionEvents()

        then: 'quiz is still active'
        def unchanged = loadForCheck(quiz.aggregateId, Quiz)
        unchanged.state == AggregateState.ACTIVE
    }
}
