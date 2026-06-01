package pt.ulisboa.tecnico.socialsoftware.quizzesfull.sagas.coordination.quiz

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.QuizzesFullSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.states.ExecutionSagaState
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.aggregate.QuizDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.quiz.coordination.sagas.CreateQuizFunctionalitySagas
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.QUIZ_DATE_ORDERING

import java.time.LocalDateTime

@DataJpaTest
@Transactional
@Import(LocalBeanConfiguration)
class CreateQuizTest extends QuizzesFullSpockTest {

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}

    @Autowired
    CommandGateway commandGateway

    def courseDto
    def executionDto
    def questionDto

    def setup() {
        courseDto = createCourse(COURSE_NAME_1, COURSE_TYPE_TECNICO)
        executionDto = createExecution(courseDto.aggregateId, "SE-2025", "2025/2026")
        questionDto = createQuestion(courseDto.aggregateId, [], "Quiz Question", "Describe sorting.")
    }

    def "createQuiz: success with questions"() {
        given:
        def availableDate = LocalDateTime.now().plusDays(1)
        def conclusionDate = LocalDateTime.now().plusDays(2)
        def resultsDate = LocalDateTime.now().plusDays(3)

        // Spec: Quiz.{title,executionId,questionIds} = input; dates as supplied;
        //       SagaState after commit == NOT_IN_SAGA.
        // Source: plan.md §2.6 Quiz / createQuiz.
        when:
        QuizDto result = quizFunctionalities.createQuiz("Test Quiz", availableDate, conclusionDate,
                resultsDate, "GENERATED", executionDto.aggregateId, [questionDto.aggregateId])

        then:
        result.aggregateId != null
        result.title == "Test Quiz"
        result.executionId == executionDto.aggregateId
        result.questionIds.contains(questionDto.aggregateId)
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createQuiz: success with no questions"() {
        given:
        def availableDate = LocalDateTime.now().plusDays(1)
        def conclusionDate = LocalDateTime.now().plusDays(2)
        def resultsDate = LocalDateTime.now().plusDays(3)

        // Spec: Quiz.{title,executionId,questionIds} = input; dates as supplied;
        //       SagaState after commit == NOT_IN_SAGA.
        // Source: plan.md §2.6 Quiz / createQuiz.
        when:
        QuizDto result = quizFunctionalities.createQuiz("Empty Quiz", availableDate, conclusionDate,
                resultsDate, "GENERATED", executionDto.aggregateId, [])

        then:
        result.aggregateId != null
        result.questionIds.isEmpty()
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createQuiz: violates QUIZ_DATE_ORDERING — availableDate after conclusionDate"() {
        given:
        def availableDate = LocalDateTime.now().plusDays(5)
        def conclusionDate = LocalDateTime.now().plusDays(2)
        def resultsDate = LocalDateTime.now().plusDays(6)

        when:
        quizFunctionalities.createQuiz("Bad Quiz", availableDate, conclusionDate, resultsDate,
                "GENERATED", executionDto.aggregateId, [questionDto.aggregateId])

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == QUIZ_DATE_ORDERING
    }

    // ─── Boundary Value Analysis: QUIZ_DATE_ORDERING ───────────────────────────
    // Spec: plan.md §2.6 Quiz — QUIZ_DATE_ORDERING (available < conclusion <= results).
    // The case above is an equivalence-partitioning representative (available far past conclusion).
    // The pair below straddles the exact boundary of each comparison link, including the `<=` link
    // where equality is valid — the classic `<` vs `<=` defect. See docs/concepts/testing.md
    // § Choosing Input Values. Dates are explicit args, so the saga path pins the on-point exactly.

    def "createQuiz: QUIZ_DATE_ORDERING boundary — availableDate == conclusionDate violates"() {
        given: 'available and conclusion at the same instant (available < conclusion is strict)'
        def availableDate = LocalDateTime.now().plusDays(2)
        def conclusionDate = availableDate
        def resultsDate = LocalDateTime.now().plusDays(3)

        when:
        quizFunctionalities.createQuiz("Edge Quiz", availableDate, conclusionDate, resultsDate,
                "GENERATED", executionDto.aggregateId, [questionDto.aggregateId])

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == QUIZ_DATE_ORDERING
    }

    def "createQuiz: QUIZ_DATE_ORDERING boundary — conclusionDate == resultsDate is allowed"() {
        given: 'conclusion equal to results (conclusion <= results is inclusive)'
        def availableDate = LocalDateTime.now().plusDays(1)
        def conclusionDate = LocalDateTime.now().plusDays(2)
        def resultsDate = conclusionDate

        when:
        QuizDto result = quizFunctionalities.createQuiz("Edge Quiz", availableDate, conclusionDate,
                resultsDate, "GENERATED", executionDto.aggregateId, [questionDto.aggregateId])

        then:
        result.aggregateId != null
        sagaStateOf(result.aggregateId) == GenericSagaState.NOT_IN_SAGA
    }

    def "createQuiz: QUIZ_DATE_ORDERING boundary — resultsDate one nanosecond before conclusionDate violates"() {
        given: 'results just before conclusion (off-point of conclusion <= results)'
        def availableDate = LocalDateTime.now().plusDays(1)
        def conclusionDate = LocalDateTime.now().plusDays(2)
        def resultsDate = conclusionDate.minusNanos(1)

        when:
        quizFunctionalities.createQuiz("Edge Quiz", availableDate, conclusionDate, resultsDate,
                "GENERATED", executionDto.aggregateId, [questionDto.aggregateId])

        then:
        def ex = thrown(QuizzesFullException)
        ex.message == QUIZ_DATE_ORDERING
    }

    def "createQuiz: getExecutionStep acquires READ_EXECUTION semantic lock"() {
        given: 'a createQuiz workflow pauses after getExecutionStep has acquired READ_EXECUTION lock'
        def availableDate = LocalDateTime.now().plusDays(1)
        def conclusionDate = LocalDateTime.now().plusDays(2)
        def resultsDate = LocalDateTime.now().plusDays(3)
        def uow = unitOfWorkService.createUnitOfWork("createQuiz")
        def func = new CreateQuizFunctionalitySagas(
                unitOfWorkService, "Test Quiz", availableDate, conclusionDate, resultsDate,
                "GENERATED", executionDto.aggregateId, [questionDto.aggregateId], uow, commandGateway)
        func.executeUntilStep("getExecutionStep", uow)

        expect: 'execution saga state is READ_EXECUTION'
        sagaStateOf(executionDto.aggregateId) == ExecutionSagaState.READ_EXECUTION

        when: 'workflow resumes and completes'
        func.resumeWorkflow(uow)

        then:
        noExceptionThrown()

        and: 'quiz was created'
        func.getCreatedQuizDto().aggregateId != null
        func.getCreatedQuizDto().executionId == executionDto.aggregateId
    }
}
