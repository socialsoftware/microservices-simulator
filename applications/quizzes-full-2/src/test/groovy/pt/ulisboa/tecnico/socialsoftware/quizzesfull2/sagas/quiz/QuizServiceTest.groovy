package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.quiz

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest

@DataJpaTest
@Transactional
@Import(QuizServiceTest.LocalBeanConfiguration)
class QuizServiceTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999
    public static final String SECOND_QUIZ_TITLE = "Graph quiz"
    public static final String SECOND_EXECUTION_ACRONYM = "SE-02"

    def "getQuizById: reads back the persisted quiz through a fresh UnitOfWork"() {
        // Spec: plan.md §6 Quiz — fields title, creationDate, availableDate, conclusionDate,
        // resultsDate, quizType, execution
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId)

        when:
        flushAndClear()
        def result = quizService.getQuizById(quizAggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == quizAggregateId
        result.title == QUIZ_TITLE
        result.creationDate == QUIZ_CREATION_DATE
        result.availableDate == QUIZ_AVAILABLE_DATE
        result.conclusionDate == QUIZ_CONCLUSION_DATE
        result.resultsDate == QUIZ_RESULTS_DATE
        result.quizType == QUIZ_TYPE
        result.executionAggregateId == executionAggregateId
        result.executionVersion == QUIZ_EXECUTION_VERSION
        result.questions.isEmpty()
        result.version != null
    }

    def "getQuizById: unknown aggregate id is not found"() {
        // Spec: plan.md §6 Quiz — Path A (aggregateLoadAndRegisterRead)
        when:
        quizService.getQuizById(NONEXISTENT_AGGREGATE_ID, unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "getQuizzesForExecution: returns every quiz of the execution with its own state"() {
        // Spec: plan.md §6 Quiz — GetQuizzesForExecution(executionAggregateId)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def firstAggregateId = createQuiz(executionAggregateId)
        def secondAggregateId = createQuiz(executionAggregateId, SECOND_QUIZ_TITLE)

        when:
        flushAndClear()
        def result = quizService.getQuizzesForExecution(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.size() == 2
        def first = result.find { it.aggregateId == firstAggregateId }
        first.title == QUIZ_TITLE
        first.quizType == QUIZ_TYPE
        first.executionAggregateId == executionAggregateId
        def second = result.find { it.aggregateId == secondAggregateId }
        second.title == SECOND_QUIZ_TITLE
        second.availableDate == QUIZ_AVAILABLE_DATE
        second.executionAggregateId == executionAggregateId
    }

    def "getQuizzesForExecution: excludes quizzes belonging to another execution"() {
        // Spec: plan.md §6 Quiz — GetQuizzesForExecution filters on the cached executionAggregateId
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def otherExecutionAggregateId = createExecution(courseAggregateId, SECOND_EXECUTION_ACRONYM)
        def quizAggregateId = createQuiz(executionAggregateId)
        createQuiz(otherExecutionAggregateId, SECOND_QUIZ_TITLE)

        when:
        flushAndClear()
        def result = quizService.getQuizzesForExecution(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.collect { it.aggregateId } == [quizAggregateId]
    }

    def "getQuizzesForExecution: returns an empty list when the execution has no quiz"() {
        // Spec: plan.md §6 Quiz — GetQuizzesForExecution(executionAggregateId)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)

        when:
        def result = quizService.getQuizzesForExecution(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.isEmpty()
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
