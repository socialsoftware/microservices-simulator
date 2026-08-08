package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.sagas.quizanswer

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.BeanConfigurationSagas
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.QuizzesFull2SpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.events.QuizAnswerQuestionAnswerEvent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.QuestionAnswerDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto

@DataJpaTest
@Transactional
@Import(QuizAnswerServiceTest.LocalBeanConfiguration)
class QuizAnswerServiceTest extends QuizzesFull2SpockTest {

    public static final Integer NONEXISTENT_AGGREGATE_ID = 999999
    public static final Integer NONEXISTENT_QUESTION_AGGREGATE_ID = 999998
    public static final String SECOND_QUIZ_TITLE = "Graph quiz"
    public static final String SECOND_USER_NAME = "Bob Jones"
    public static final String SECOND_USER_USERNAME = "bob"
    public static final String SECOND_EXECUTION_ACRONYM = "SE-02"

    @Autowired
    EventService eventService

    def "getQuizAnswerById: reads back the persisted quiz answer through a fresh UnitOfWork"() {
        // Spec: plan.md §7 QuizAnswer — GetQuizAnswerById(quizAnswerAggregateId); fields creationDate,
        // answerDate, completed and the quiz / student / execution snapshots
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)
        def quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)

        when:
        flushAndClear()
        def result = quizAnswerService.getQuizAnswerById(quizAnswerAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == quizAnswerAggregateId
        result.creationDate != null
        result.answerDate != null
        !result.completed
        result.quizAggregateId == quizAggregateId
        result.quizVersion != null
        result.userAggregateId == userAggregateId
        result.userName == USER_NAME
        result.userVersion != null
        result.executionAggregateId == executionAggregateId
        result.executionVersion != null
        result.questionAnswers.isEmpty()
        result.version != null
    }

    def "getQuizAnswerById: unknown aggregate id is not found"() {
        // Spec: plan.md §7 QuizAnswer — Path A (aggregateLoadAndRegisterRead)
        when:
        quizAnswerService.getQuizAnswerById(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        thrown(SimulatorException)
    }

    def "getQuizAnswerForStudentAndQuiz: returns the quiz answer of that student for that quiz"() {
        // Spec: plan.md §7 QuizAnswer — GetQuizAnswerForStudentAndQuiz(userAggregateId, quizAggregateId)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)
        def quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)

        when:
        flushAndClear()
        def result = quizAnswerService.getQuizAnswerForStudentAndQuiz(userAggregateId, quizAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == quizAnswerAggregateId
        result.quizAggregateId == quizAggregateId
        result.userAggregateId == userAggregateId
        result.userName == USER_NAME
        result.executionAggregateId == executionAggregateId
        !result.completed
    }

    def "getQuizAnswerForStudentAndQuiz: ignores the same student's answer to another quiz"() {
        // Spec: plan.md §7 QuizAnswer — the lookup is keyed on the (student, quiz) pair
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)
        def otherQuizAggregateId = createQuiz(executionAggregateId, SECOND_QUIZ_TITLE)
        createQuizAnswer(otherQuizAggregateId, userAggregateId, executionAggregateId)
        def quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)

        when:
        flushAndClear()
        def result = quizAnswerService.getQuizAnswerForStudentAndQuiz(userAggregateId, quizAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == quizAnswerAggregateId
    }

    def "getQuizAnswerForStudentAndQuiz: ignores another student's answer to the same quiz"() {
        // Spec: plan.md §7 QuizAnswer — the lookup is keyed on the (student, quiz) pair
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def otherUserAggregateId = createActiveUser(SECOND_USER_NAME, SECOND_USER_USERNAME)
        def quizAggregateId = createQuiz(executionAggregateId)
        createQuizAnswer(quizAggregateId, otherUserAggregateId, executionAggregateId)
        def quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)

        when:
        flushAndClear()
        def result = quizAnswerService.getQuizAnswerForStudentAndQuiz(userAggregateId, quizAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        result.aggregateId == quizAnswerAggregateId
        result.userName == USER_NAME
    }

    def "getQuizAnswerForStudentAndQuiz: the student has not answered the quiz"() {
        // Spec: plan.md §7 QuizAnswer — Path B (custom-repository lookup returning Optional)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)

        when:
        quizAnswerService.getQuizAnswerForStudentAndQuiz(userAggregateId, quizAggregateId,
                unitOfWorkService.createUnitOfWork("check"))

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.QUIZ_ANSWER_NOT_FOUND
    }

    def "createQuizAnswer: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §7 QuizAnswer — CreateQuizAnswer(quizAggregateId, userAggregateId,
        // executionAggregateId); seeds one QuestionAnswer per quiz question with its correctOptionKey
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def questionAggregateId = createQuestionWithOptions(courseAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId, QUIZ_TITLE, QUIZ_AVAILABLE_DATE,
                QUIZ_CONCLUSION_DATE, QUIZ_RESULTS_DATE, QUIZ_TYPE, [questionAggregateId])
        def quizDto = quizDtoOf(quizAggregateId)
        def userDto = userDtoOf(userAggregateId)
        def executionDto = executionDtoOf(executionAggregateId)

        when:
        def result = quizAnswerService.createQuizAnswer(quizDto, userDto, executionDto,
                questionAnswerDtosOf(quizDto), unitOfWorkService.createUnitOfWork("createQuizAnswer"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        result.aggregateId != null
        flushAndClear()
        def readBack = quizAnswerService.getQuizAnswerById(result.aggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.creationDate != null
        readBack.answerDate != null
        !readBack.completed
        readBack.quizAggregateId == quizAggregateId
        readBack.quizVersion == quizDto.version
        readBack.userAggregateId == userAggregateId
        readBack.userName == USER_NAME
        readBack.userVersion == userDto.version
        readBack.executionAggregateId == executionAggregateId
        readBack.executionVersion == executionDto.version
        readBack.questionAnswers.size() == 1
        readBack.questionAnswers[0].questionAggregateId == questionAggregateId
        readBack.questionAnswers[0].correctOptionKey == QUESTION_ANSWER_CORRECT_OPTION_KEY
        readBack.questionAnswers[0].optionKey == null
        readBack.questionAnswers[0].correct == null
    }

    def "createQuizAnswer: UNIQUE_QUIZ_ANSWER_PER_STUDENT violation"() {
        // Spec: plan.md §7 QuizAnswer — rule UNIQUE_QUIZ_ANSWER_PER_STUDENT (P3 own-table uniqueness)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)
        createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)

        when:
        quizAnswerService.createQuizAnswer(quizDtoOf(quizAggregateId), userDtoOf(userAggregateId),
                executionDtoOf(executionAggregateId), [],
                unitOfWorkService.createUnitOfWork("createQuizAnswer"))

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.UNIQUE_QUIZ_ANSWER_PER_STUDENT
    }

    def "createQuizAnswer: COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION violation"() {
        // Spec: plan.md §7 QuizAnswer — rule COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION (P3 DTO check)
        given:
        def courseAggregateId = createCourse()
        def quizExecutionAggregateId = createExecution(courseAggregateId)
        def otherExecutionAggregateId = createExecution(courseAggregateId, SECOND_EXECUTION_ACRONYM)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(quizExecutionAggregateId)

        when:
        quizAnswerService.createQuizAnswer(quizDtoOf(quizAggregateId), userDtoOf(userAggregateId),
                executionDtoOf(otherExecutionAggregateId), [],
                unitOfWorkService.createUnitOfWork("createQuizAnswer"))

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.COURSE_EXECUTION_SAME_QUIZ_COURSE_EXECUTION
    }

    def "answerQuestion: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §7 QuizAnswer — AnswerQuestion(quizAnswerAggregateId, questionAggregateId,
        // optionSequenceChoice, optionKey, timeTaken)
        given:
        def fixture = answerableQuizAnswer()

        when:
        quizAnswerService.answerQuestion(fixture.quizAnswerAggregateId, fixture.questionAggregateId,
                QUESTION_ANSWER_OPTION_SEQUENCE_CHOICE, QUESTION_ANSWER_CORRECT_OPTION_KEY,
                QUESTION_ANSWER_TIME_TAKEN, unitOfWorkService.createUnitOfWork("answerQuestion"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = quizAnswerService.getQuizAnswerById(fixture.quizAnswerAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.questionAnswers.size() == 1
        readBack.questionAnswers[0].questionAggregateId == fixture.questionAggregateId
        readBack.questionAnswers[0].optionSequenceChoice == QUESTION_ANSWER_OPTION_SEQUENCE_CHOICE
        readBack.questionAnswers[0].optionKey == QUESTION_ANSWER_CORRECT_OPTION_KEY
        readBack.questionAnswers[0].timeTaken == QUESTION_ANSWER_TIME_TAKEN
        readBack.questionAnswers[0].correct
    }

    def "answerQuestion: a non-correct option key records an incorrect answer"() {
        // Spec: plan.md §7 QuizAnswer — rule ANSWER_MATCHES_CORRECT_OPTION decides correctness from the
        // cached correctOptionKey
        given:
        def fixture = answerableQuizAnswer()

        when:
        quizAnswerService.answerQuestion(fixture.quizAnswerAggregateId, fixture.questionAggregateId,
                QUESTION_ANSWER_OPTION_SEQUENCE_CHOICE, QUESTION_ANSWER_WRONG_OPTION_KEY,
                QUESTION_ANSWER_TIME_TAKEN, unitOfWorkService.createUnitOfWork("answerQuestion"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = quizAnswerService.getQuizAnswerById(fixture.quizAnswerAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.questionAnswers[0].optionKey == QUESTION_ANSWER_WRONG_OPTION_KEY
        !readBack.questionAnswers[0].correct
    }

    def "answerQuestion: QUESTION_NOT_IN_QUIZ_ANSWER violation"() {
        // Spec: plan.md §7 QuizAnswer — CreateQuizAnswer seeds one QuestionAnswer per quiz question, so a
        // question outside that set has nothing to answer. Guard added in 2.7.c; see the session retro.
        given:
        def fixture = answerableQuizAnswer()

        when:
        quizAnswerService.answerQuestion(fixture.quizAnswerAggregateId, NONEXISTENT_QUESTION_AGGREGATE_ID,
                QUESTION_ANSWER_OPTION_SEQUENCE_CHOICE, QUESTION_ANSWER_CORRECT_OPTION_KEY,
                QUESTION_ANSWER_TIME_TAKEN, unitOfWorkService.createUnitOfWork("answerQuestion"))

        then:
        def ex = thrown(QuizzesFull2Exception)
        ex.message == QuizzesFull2ErrorMessage.QUESTION_NOT_IN_QUIZ_ANSWER
    }

    def "answerQuestion: unknown aggregate id is not found"() {
        // Spec: plan.md §7 QuizAnswer — Path A (aggregateLoadAndRegisterRead)
        when:
        quizAnswerService.answerQuestion(NONEXISTENT_AGGREGATE_ID, QUESTION_ANSWER_QUESTION_AGGREGATE_ID,
                QUESTION_ANSWER_OPTION_SEQUENCE_CHOICE, QUESTION_ANSWER_CORRECT_OPTION_KEY,
                QUESTION_ANSWER_TIME_TAKEN, unitOfWorkService.createUnitOfWork("answerQuestion"))

        then:
        thrown(SimulatorException)
    }

    def "concludeQuiz: persisted and readable through a fresh UnitOfWork"() {
        // Spec: plan.md §7 QuizAnswer — ConcludeQuiz(quizAnswerAggregateId)
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)
        def quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)

        when:
        quizAnswerService.concludeQuiz(quizAnswerAggregateId,
                unitOfWorkService.createUnitOfWork("concludeQuiz"))

        then: 'read back off a cleared persistence context, through a fresh UnitOfWork'
        flushAndClear()
        def readBack = quizAnswerService.getQuizAnswerById(quizAnswerAggregateId,
                unitOfWorkService.createUnitOfWork("check"))
        readBack.completed
    }

    def "concludeQuiz: unknown aggregate id is not found"() {
        // Spec: plan.md §7 QuizAnswer — Path A (aggregateLoadAndRegisterRead)
        when:
        quizAnswerService.concludeQuiz(NONEXISTENT_AGGREGATE_ID,
                unitOfWorkService.createUnitOfWork("concludeQuiz"))

        then:
        thrown(SimulatorException)
    }

    def "answerQuestion publishes QuizAnswerQuestionAnswerEvent with correct payload"() {
        // Spec: plan.md §7 QuizAnswer — events published: QuizAnswerQuestionAnswerEvent
        given:
        def fixture = answerableQuizAnswer()

        when:
        quizAnswerService.answerQuestion(fixture.quizAnswerAggregateId, fixture.questionAggregateId,
                QUESTION_ANSWER_OPTION_SEQUENCE_CHOICE, QUESTION_ANSWER_CORRECT_OPTION_KEY,
                QUESTION_ANSWER_TIME_TAKEN, unitOfWorkService.createUnitOfWork("answerQuestion"))

        then:
        def events = eventService.getAllEvents().findAll { it instanceof QuizAnswerQuestionAnswerEvent }
        events.size() == 1
        def event = events[0] as QuizAnswerQuestionAnswerEvent
        event.publisherAggregateId == fixture.quizAnswerAggregateId
        event.quizAnswerAggregateId == fixture.quizAnswerAggregateId
        event.questionAggregateId == fixture.questionAggregateId
        event.quizAggregateId == fixture.quizAggregateId
        event.studentAggregateId == fixture.userAggregateId
        event.correct
        event.answerTime != null
    }

    def "concludeQuiz publishes no event"() {
        // Spec: plan.md §7 QuizAnswer — events published: QuizAnswerQuestionAnswerEvent only
        given:
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def quizAggregateId = createQuiz(executionAggregateId)
        def quizAnswerAggregateId = createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId)
        def countBefore = eventService.getAllEvents().size()

        when:
        quizAnswerService.concludeQuiz(quizAnswerAggregateId,
                unitOfWorkService.createUnitOfWork("concludeQuiz"))

        then:
        eventService.getAllEvents().size() == countBefore
    }

    private Map answerableQuizAnswer() {
        def courseAggregateId = createCourse()
        def executionAggregateId = createExecution(courseAggregateId)
        def userAggregateId = createActiveUser()
        def questionAggregateId = createQuestionWithOptions(courseAggregateId)
        def quizAggregateId = createQuiz(executionAggregateId, QUIZ_TITLE, QUIZ_AVAILABLE_DATE,
                QUIZ_CONCLUSION_DATE, QUIZ_RESULTS_DATE, QUIZ_TYPE, [questionAggregateId])
        return [quizAnswerAggregateId: createQuizAnswer(quizAggregateId, userAggregateId, executionAggregateId),
                questionAggregateId  : questionAggregateId,
                quizAggregateId      : quizAggregateId,
                userAggregateId      : userAggregateId]
    }

    private QuizDto quizDtoOf(Integer quizAggregateId) {
        return quizService.getQuizById(quizAggregateId, unitOfWorkService.createUnitOfWork("fixture"))
    }

    private UserDto userDtoOf(Integer userAggregateId) {
        return userService.getUserById(userAggregateId, unitOfWorkService.createUnitOfWork("fixture"))
    }

    private ExecutionDto executionDtoOf(Integer executionAggregateId) {
        return executionService.getExecutionById(executionAggregateId,
                unitOfWorkService.createUnitOfWork("fixture"))
    }

    // Mirrors what CreateQuizAnswerFunctionalitySagas assembles, since a direct service call receives the
    // seeds rather than fetching them.
    private List<QuestionAnswerDto> questionAnswerDtosOf(QuizDto quizDto) {
        return quizDto.questions.collect { quizQuestion ->
            def questionDto = questionService.getQuestionById(quizQuestion.questionAggregateId,
                    unitOfWorkService.createUnitOfWork("fixture"))
            def questionAnswerDto = new QuestionAnswerDto()
            questionAnswerDto.setQuestionAggregateId(questionDto.aggregateId)
            questionAnswerDto.setQuestionVersion(questionDto.version)
            questionAnswerDto.setCorrectOptionKey(
                    questionDto.options.find { it.isCorrect() }?.getOptionKey())
            return questionAnswerDto
        }
    }

    @TestConfiguration
    static class LocalBeanConfiguration extends BeanConfigurationSagas {}
}
