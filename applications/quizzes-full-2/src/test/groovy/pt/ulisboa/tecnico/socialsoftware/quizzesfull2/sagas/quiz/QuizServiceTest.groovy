package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.quiz

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestionDto

@DataJpaTest
@Transactional
@Import(QuizServiceTest.LocalBeanConfiguration)
class QuizServiceTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999
    public static final String SECOND_QUIZ_TITLE = "Graph quiz"
    public static final String SECOND_EXECUTION_ACRONYM = "SE-02"
    public static final String UPDATED_QUIZ_TITLE = "Sorting and searching quiz"
    public static final String SECOND_QUESTION_TITLE = "Graph traversal"

    @Autowired
    EventService eventService

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
        result.creationDate != null
        result.availableDate == QUIZ_AVAILABLE_DATE
        result.conclusionDate == QUIZ_CONCLUSION_DATE
        result.resultsDate == QUIZ_RESULTS_DATE
        result.quizType == QUIZ_TYPE
        result.executionAggregateId == executionAggregateId
        result.executionVersion != null
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

    def "createQuiz: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §6 Quiz — CreateQuiz(executionAggregateId, title, availableDate, conclusionDate,
        // resultsDate, quizType, questionAggregateIds)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def questionAggregateId = createQuestion(courseAggregateId)
        def executionDto = executionDtoOf(executionAggregateId)
        def questions = [quizQuestionDtoOf(questionAggregateId)]

        when:
        def result = quizService.createQuiz(quizDtoFor(executionAggregateId), executionDto, questions,
                unitOfWorkService.createUnitOfWork("createQuiz"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        result.aggregateId != null
        flushAndClear()
        def readBack = quizService.getQuizById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.title == QUIZ_TITLE
        readBack.creationDate != null
        readBack.availableDate == QUIZ_AVAILABLE_DATE
        readBack.conclusionDate == QUIZ_CONCLUSION_DATE
        readBack.resultsDate == QUIZ_RESULTS_DATE
        readBack.quizType == QUIZ_TYPE
        readBack.executionAggregateId == executionAggregateId
        readBack.executionVersion == executionDto.version
        readBack.questions.size() == 1
        readBack.questions[0].questionAggregateId == questionAggregateId
        readBack.questions[0].title == QUESTION_TITLE
        readBack.questions[0].content == QUESTION_CONTENT
    }

    def "updateQuiz: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §6 Quiz — UpdateQuiz(quizAggregateId, title, availableDate, conclusionDate,
        // resultsDate, questionAggregateIds)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def questionAggregateId = createQuestion(courseAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId)

        when:
        quizService.updateQuiz(quizAggregateId, UPDATED_QUIZ_TITLE, QUIZ_AVAILABLE_DATE.plusDays(1),
                QUIZ_CONCLUSION_DATE.plusDays(1), QUIZ_RESULTS_DATE.plusDays(1),
                [quizQuestionDtoOf(questionAggregateId)],
                unitOfWorkService.createUnitOfWork("updateQuiz"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = quizService.getQuizById(quizAggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.title == UPDATED_QUIZ_TITLE
        readBack.availableDate == QUIZ_AVAILABLE_DATE.plusDays(1)
        readBack.conclusionDate == QUIZ_CONCLUSION_DATE.plusDays(1)
        readBack.resultsDate == QUIZ_RESULTS_DATE.plusDays(1)
        readBack.executionAggregateId == executionAggregateId
        readBack.questions.size() == 1
        readBack.questions[0].questionAggregateId == questionAggregateId
        readBack.questions[0].title == QUESTION_TITLE
    }

    def "updateQuiz: replaces the previous question set"() {
        // Spec: plan.md §6 Quiz — UpdateQuiz(quizAggregateId, ..., questionAggregateIds)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def firstQuestionAggregateId = createQuestion(courseAggregateId)
        def secondQuestionAggregateId = createQuestion(courseAggregateId, SECOND_QUESTION_TITLE)
        def quizAggregateId = createQuiz(executionAggregateId)
        quizService.updateQuiz(quizAggregateId, QUIZ_TITLE, QUIZ_AVAILABLE_DATE, QUIZ_CONCLUSION_DATE,
                QUIZ_RESULTS_DATE, [quizQuestionDtoOf(firstQuestionAggregateId)],
                unitOfWorkService.createUnitOfWork("updateQuiz"))

        when:
        quizService.updateQuiz(quizAggregateId, QUIZ_TITLE, QUIZ_AVAILABLE_DATE, QUIZ_CONCLUSION_DATE,
                QUIZ_RESULTS_DATE, [quizQuestionDtoOf(secondQuestionAggregateId)],
                unitOfWorkService.createUnitOfWork("updateQuiz"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = quizService.getQuizById(quizAggregateId, unitOfWorkService.createUnitOfWork("check"))
        readBack.questions.collect { it.questionAggregateId } == [secondQuestionAggregateId]
    }

    def "updateQuiz: unknown aggregate id is not found"() {
        // Spec: plan.md §6 Quiz — UpdateQuiz(quizAggregateId, ...); Path A (aggregateLoadAndRegisterRead)
        when:
        quizService.updateQuiz(NONEXISTENT_AGGREGATE_ID, UPDATED_QUIZ_TITLE, QUIZ_AVAILABLE_DATE,
                QUIZ_CONCLUSION_DATE, QUIZ_RESULTS_DATE, [],
                unitOfWorkService.createUnitOfWork("updateQuiz"))

        then:
        thrown(SimulatorException)
    }

    def "deleteQuiz: the deleted quiz no longer resolves through a fresh UnitOfWork"() {
        // Spec: plan.md §8 Tournament — CreateTournament's createQuizStep compensation
        // (DeleteQuizCommand); a DELETED aggregate is not loadable
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId)

        when:
        quizService.deleteQuiz(quizAggregateId, unitOfWorkService.createUnitOfWork("deleteQuiz"))
        flushAndClear()
        quizService.getQuizById(quizAggregateId, unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    // InvalidateQuizEvent is the only event Quiz publishes, and its publication site is the
    // DeleteQuestionEvent handler chain written in 2.6.d (plan.md §6 Quiz). Its payload-asserting
    // case therefore belongs to that session; 2.6.c owns the class-scoped negative case below.
    def "createQuiz publishes no event"() {
        // Spec: plan.md §6 Quiz — events published: InvalidateQuizEvent only
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def executionDto = executionDtoOf(executionAggregateId)
        def countBefore = eventService.getAllEvents().size()

        when:
        quizService.createQuiz(quizDtoFor(executionAggregateId), executionDto, [],
                unitOfWorkService.createUnitOfWork("createQuiz"))

        then:
        eventService.getAllEvents().size() == countBefore
    }

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

    private ExecutionDto executionDtoOf(Integer executionAggregateId) {
        return executionService.getExecutionById(executionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
    }

    // The saga assembles this from the Question the create/update step fetched; a direct service test
    // does the same fetch itself.
    private QuizQuestionDto quizQuestionDtoOf(Integer questionAggregateId) {
        def questionDto = questionService.getQuestionById(questionAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        return new QuizQuestionDto(questionDto.aggregateId, questionDto.version, questionDto.title,
                questionDto.content)
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
