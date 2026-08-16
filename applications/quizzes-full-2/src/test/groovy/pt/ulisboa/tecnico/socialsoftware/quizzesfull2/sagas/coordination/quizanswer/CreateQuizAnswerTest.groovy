package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.quizanswer

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception

@DataJpaTest
@Transactional
@Import(CreateQuizAnswerTest.LocalBeanConfiguration)
class CreateQuizAnswerTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999
    public static final String SECOND_EXECUTION_ACRONYM = "SE-02"

    def "createQuizAnswer: success"() {
        // Spec: plan.md §7 QuizAnswer — CreateQuizAnswer(quizAggregateId, userAggregateId,
        // executionAggregateId)
        given: 'a quiz with one question, and a student of its execution'
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def questionAggregateId = createQuestionWithOptions(courseAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId, QUIZ_TITLE, QUIZ_AVAILABLE_DATE,
                QUIZ_CONCLUSION_DATE, QUIZ_RESULTS_DATE, QUIZ_TYPE, [questionAggregateId])

        when:
        def result = quizAnswerFunctionalities.createQuizAnswer(quizAggregateId, userAggregateId,
                executionAggregateId)

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.aggregateId != null
        result.quizAggregateId == quizAggregateId
        result.userAggregateId == userAggregateId
        result.userName == USER_NAME
        result.executionAggregateId == executionAggregateId
        !result.completed
        result.questionAnswers.collect { it.questionAggregateId } == [questionAggregateId]
        result.questionAnswers[0].correctOptionKey == QUESTION_ANSWER_CORRECT_OPTION_KEY
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createQuizAnswer: COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION violation"() {
        // Spec: plan.md §7 QuizAnswer — rule COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION; the quiz's
        // execution id reaches the guard through the getQuizStep data-assembly step
        given:
        def courseAggregateId = createCourse()
        def quizExecutionAggregateId = createExecution(courseAggregateId)
        def otherExecutionAggregateId = createExecution(courseAggregateId, SECOND_EXECUTION_ACRONYM)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(quizExecutionAggregateId)

        when:
        quizAnswerFunctionalities.createQuizAnswer(quizAggregateId, userAggregateId,
                otherExecutionAggregateId)

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION
    }

    def "createQuizAnswer: aborts when the quiz prerequisite does not exist"() {
        // Spec: plan.md §7 QuizAnswer — cross-aggregate prerequisite (P4a): getQuizStep fetches the quiz
        // and throws if it does not exist, so no explicit guard is written
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()

        when:
        quizAnswerFunctionalities.createQuizAnswer(NONEXISTENT_AGGREGATE_ID, userAggregateId,
                executionAggregateId)

        then:
        thrown(SimulatorException)
    }

    def "createQuizAnswer: aborts when the student prerequisite does not exist"() {
        // Spec: plan.md §7 QuizAnswer — cross-aggregate prerequisite (P4a): getUserStep seeds the
        // QuizAnswerStudent snapshot and throws if the user does not exist
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId)

        when:
        quizAnswerFunctionalities.createQuizAnswer(quizAggregateId, NONEXISTENT_AGGREGATE_ID,
                executionAggregateId)

        then:
        thrown(SimulatorException)
    }

    def "createQuizAnswer: aborts when the execution prerequisite does not exist"() {
        // Spec: plan.md §7 QuizAnswer — cross-aggregate prerequisite (P4a): getExecutionStep seeds the
        // QuizAnswerExecution snapshot and throws if the execution does not exist
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)

        when:
        quizAnswerFunctionalities.createQuizAnswer(quizAggregateId, userAggregateId,
                NONEXISTENT_AGGREGATE_ID)

        then:
        thrown(SimulatorException)
    }

    // Semantic-lock acquisition: CreateQuizAnswerFunctionalitySagas has no setSemanticLock step — the
    // create step brings the aggregate into existence (sagas.md § Create Functionality Sagas), so there
    // is no prior state to lock, and the four data-assembly steps are plain upstream reads.

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
