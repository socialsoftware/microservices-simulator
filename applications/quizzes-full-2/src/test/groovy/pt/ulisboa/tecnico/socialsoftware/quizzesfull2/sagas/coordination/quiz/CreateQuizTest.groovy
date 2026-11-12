package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.coordination.quiz

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto

@DataJpaTest
@Transactional
@Import(CreateQuizTest.LocalBeanConfiguration)
class CreateQuizTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_EXECUTION_AGGREGATE_ID = 999999
    public static final Integer NONEXISTENT_QUESTION_AGGREGATE_ID = 999998

    def "createQuiz: success"() {
        // Spec: plan.md §6 Quiz — CreateQuiz(executionAggregateId, title, availableDate, conclusionDate,
        // resultsDate, quizType, questionAggregateIds)
        given: 'an execution and a question of its course'
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def questionAggregateId = createQuestion(courseAggregateId)

        when:
        def result = quizFunctionalities.createQuiz(quizDtoFor(executionAggregateId), [questionAggregateId])

        then: 'orchestration outcome only — persistence is asserted in T2'
        result.aggregateId != null
        result.title == QUIZ_TITLE
        result.quizType == QUIZ_TYPE
        result.executionAggregateId == executionAggregateId
        result.questions.collect { it.questionAggregateId } == [questionAggregateId]
        result.questions[0].title == QUESTION_TITLE
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createQuiz: aborts when the execution prerequisite does not exist"() {
        // Spec: plan.md §6 Quiz — cross-aggregate prerequisite (P4a): getExecutionStep fetches the
        // execution and throws if it does not exist, so no explicit guard is written
        when:
        quizFunctionalities.createQuiz(quizDtoFor(NONEXISTENT_EXECUTION_AGGREGATE_ID), [])

        then:
        thrown(SimulatorException)
    }

    def "createQuiz: aborts when a question prerequisite does not exist"() {
        // Spec: plan.md §6 Quiz — cross-aggregate prerequisite (P4a): getQuestionsStep fetches each
        // question and throws if one does not exist, so no explicit guard is written
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)

        when:
        quizFunctionalities.createQuiz(quizDtoFor(executionAggregateId), [NONEXISTENT_QUESTION_AGGREGATE_ID])

        then:
        thrown(SimulatorException)
    }

    // Semantic-lock acquisition: CreateQuizFunctionalitySagas has no setSemanticLock step — the create
    // step brings the aggregate into existence (sagas.md § Create Functionality Sagas), so there is no
    // prior state to lock, and getExecutionStep / getQuestionsStep are plain upstream reads.

    private QuizDto quizDtoFor(Integer executionAggregateId) {
        def quizDto = new QuizDto()
        quizDto.setExecutionAggregateId(executionAggregateId)
        quizDto.setTitle(QUIZ_TITLE)
        quizDto.setAvailableDate(QUIZ_AVAILABLE_DATE)
        quizDto.setConclusionDate(QUIZ_CONCLUSION_DATE)
        quizDto.setResultsDate(QUIZ_RESULTS_DATE)
        quizDto.setQuizType(QUIZ_TYPE)
        return quizDto
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
