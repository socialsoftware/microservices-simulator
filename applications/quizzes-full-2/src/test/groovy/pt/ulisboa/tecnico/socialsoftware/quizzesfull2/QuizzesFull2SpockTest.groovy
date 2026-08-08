package pt.ulisboa.tecnico.socialsoftware.quizzesfull2

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.beans.factory.annotation.Autowired
import pt.ulisboa.tecnico.socialsoftware.SpockTest
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService
import pt.ulisboa.tecnico.socialsoftware.ms.impairment.ImpairmentService
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler

// Domain imports (DTOs, functionalities, services) are added here as aggregates are implemented in Phase 2.
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.coordination.functionalities.CourseFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.coordination.functionalities.TopicFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionStudent
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.sagas.SagaExecution
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.service.ExecutionService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.OptionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.coordination.functionalities.QuestionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizType
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.coordination.functionalities.QuizFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.aggregate.sagas.SagaQuizAnswer
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.coordination.functionalities.QuizAnswerFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quizanswer.service.QuizAnswerService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentTopic
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.sagas.SagaTournament
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.Role
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.coordination.functionalities.UserFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.service.UserService

import java.time.LocalDateTime

class QuizzesFull2SpockTest extends SpockTest {

    public static final String mavenBaseDir = System.getProperty("maven.basedir", new File(".").absolutePath)

    // Domain constants are added here as aggregates are implemented in Phase 2.
    public static final Integer COURSE_AGGREGATE_ID = 1
    public static final Integer COURSE_AGGREGATE_ID_2 = 6
    public static final String COURSE_NAME = "Software Engineering"
    public static final CourseType COURSE_TYPE = CourseType.TECNICO

    public static final Integer USER_AGGREGATE_ID = 2
    public static final String USER_NAME = "Alice Smith"
    public static final String USER_USERNAME = "alice"
    public static final Role USER_ROLE = Role.STUDENT

    public static final Integer TOPIC_AGGREGATE_ID = 3
    public static final String TOPIC_NAME = "Algorithms"

    public static final Integer EXECUTION_AGGREGATE_ID = 4
    public static final String EXECUTION_ACRONYM = "SE-01"
    public static final String EXECUTION_ACADEMIC_TERM = "2025/2026"
    public static final LocalDateTime EXECUTION_END_DATE = LocalDateTime.of(2026, 7, 31, 23, 59)

    public static final Integer EXECUTION_STUDENT_USER_AGGREGATE_ID = 20
    public static final Integer EXECUTION_STUDENT_USER_AGGREGATE_ID_2 = 21
    public static final String EXECUTION_STUDENT_USER_NAME = "Bob Jones"
    public static final String EXECUTION_STUDENT_USER_USERNAME = "bob"
    public static final Long EXECUTION_STUDENT_USER_VERSION = 1L

    public static final Integer QUESTION_AGGREGATE_ID = 5
    public static final String QUESTION_TITLE = "Sorting complexity"
    public static final String QUESTION_CONTENT = "What is the worst-case complexity of merge sort?"
    public static final LocalDateTime QUESTION_CREATION_DATE = LocalDateTime.of(2025, 9, 1, 10, 0)

    public static final Integer QUIZ_AGGREGATE_ID = 7
    public static final String QUIZ_TITLE = "Sorting quiz"
    public static final QuizType QUIZ_TYPE = QuizType.TEST
    // CreateQuiz stamps creationDate from DateHandler.now() and QUIZ_DATE_ORDERING requires it to
    // precede availableDate, so the three caller-supplied dates are pinned relative to that same
    // clock. A fixed absolute instant would put every fixture quiz in the past and fail the invariant.
    public static final LocalDateTime QUIZ_CREATION_DATE = DateHandler.now().plusDays(1)
    public static final LocalDateTime QUIZ_AVAILABLE_DATE = DateHandler.now().plusDays(10)
    public static final LocalDateTime QUIZ_CONCLUSION_DATE = DateHandler.now().plusDays(10).plusHours(2)
    public static final LocalDateTime QUIZ_RESULTS_DATE = DateHandler.now().plusDays(11)
    public static final Long QUIZ_EXECUTION_VERSION = 1L

    public static final Integer QUIZ_QUESTION_AGGREGATE_ID = 40
    public static final Integer QUIZ_QUESTION_AGGREGATE_ID_2 = 41
    public static final Long QUIZ_QUESTION_VERSION = 1L
    public static final String QUIZ_QUESTION_TITLE = "Sorting complexity"
    public static final String QUIZ_QUESTION_CONTENT = "What is the worst-case complexity of merge sort?"

    public static final Integer QUIZ_ANSWER_AGGREGATE_ID = 8
    public static final LocalDateTime QUIZ_ANSWER_CREATION_DATE = LocalDateTime.of(2025, 10, 1, 9, 0)
    public static final LocalDateTime QUIZ_ANSWER_ANSWER_DATE = LocalDateTime.of(2025, 10, 1, 9, 5)
    public static final Long QUIZ_ANSWER_QUIZ_VERSION = 1L
    public static final Long QUIZ_ANSWER_USER_VERSION = 1L
    public static final Long QUIZ_ANSWER_EXECUTION_VERSION = 1L

    public static final Integer QUESTION_ANSWER_QUESTION_AGGREGATE_ID = 50
    public static final Integer QUESTION_ANSWER_QUESTION_AGGREGATE_ID_2 = 51
    public static final Long QUESTION_ANSWER_QUESTION_VERSION = 1L
    public static final Integer QUESTION_ANSWER_CORRECT_OPTION_KEY = 1
    public static final Integer QUESTION_ANSWER_WRONG_OPTION_KEY = 2
    public static final Integer QUESTION_ANSWER_OPTION_SEQUENCE_CHOICE = 0
    public static final Integer QUESTION_ANSWER_TIME_TAKEN = 30

    public static final Integer TOURNAMENT_AGGREGATE_ID = 9
    // CreateTournament stamps lastModifiedTime from DateHandler.now() and TOURNAMENT_FINAL_AFTER_START
    // compares that stamp against startTime, so the tournament instants are pinned relative to the same
    // clock. A fixed absolute instant would put every fixture tournament in the past.
    public static final LocalDateTime TOURNAMENT_START_TIME = DateHandler.now().plusDays(10)
    public static final LocalDateTime TOURNAMENT_END_TIME = DateHandler.now().plusDays(10).plusHours(2)
    public static final Integer TOURNAMENT_NUMBER_OF_QUESTIONS = 5
    public static final Long TOURNAMENT_EXECUTION_VERSION = 1L
    public static final Long TOURNAMENT_QUIZ_VERSION = 1L

    public static final Integer TOURNAMENT_CREATOR_AGGREGATE_ID = 60
    public static final String TOURNAMENT_CREATOR_NAME = "Carol White"
    public static final String TOURNAMENT_CREATOR_USERNAME = "carol"
    public static final Long TOURNAMENT_CREATOR_VERSION = 1L

    public static final Integer TOURNAMENT_PARTICIPANT_AGGREGATE_ID = 61
    public static final Integer TOURNAMENT_PARTICIPANT_AGGREGATE_ID_2 = 62
    public static final String TOURNAMENT_PARTICIPANT_NAME = "Dave Black"
    public static final String TOURNAMENT_PARTICIPANT_USERNAME = "dave"
    public static final Long TOURNAMENT_PARTICIPANT_VERSION = 1L
    public static final LocalDateTime TOURNAMENT_ENROLL_TIME = DateHandler.now().plusDays(1)

    public static final Integer TOURNAMENT_TOPIC_AGGREGATE_ID = 70
    public static final Integer TOURNAMENT_TOPIC_AGGREGATE_ID_2 = 71
    public static final String TOURNAMENT_TOPIC_NAME = "Algorithms"
    public static final Long TOURNAMENT_TOPIC_VERSION = 1L

    public static final Integer TOURNAMENT_QUIZ_ANSWER_AGGREGATE_ID = 80
    public static final Long TOURNAMENT_QUIZ_ANSWER_VERSION = 1L

    public static final Integer QUESTION_TOPIC_AGGREGATE_ID = 30
    public static final String QUESTION_TOPIC_NAME = "Algorithms"
    public static final Long QUESTION_TOPIC_VERSION = 1L

    @Autowired
    public ImpairmentService impairmentService
    @Autowired(required = false)
    protected SagaUnitOfWorkService unitOfWorkService
    @Autowired(required = false)
    protected AggregateIdGeneratorService aggregateIdGeneratorService
    @PersistenceContext
    protected EntityManager entityManager

    // Domain @Autowired fields are added here as aggregates are implemented in Phase 2.
    @Autowired(required = false)
    protected CourseService courseService
    @Autowired(required = false)
    protected CourseFunctionalities courseFunctionalities
    @Autowired(required = false)
    protected UserService userService
    @Autowired(required = false)
    protected UserFunctionalities userFunctionalities
    @Autowired(required = false)
    protected TopicService topicService
    @Autowired(required = false)
    protected TopicFunctionalities topicFunctionalities
    @Autowired(required = false)
    protected ExecutionService executionService
    @Autowired(required = false)
    protected ExecutionFunctionalities executionFunctionalities
    @Autowired(required = false)
    protected QuestionService questionService
    @Autowired(required = false)
    protected QuestionFunctionalities questionFunctionalities
    @Autowired(required = false)
    protected QuizService quizService
    @Autowired(required = false)
    protected QuizFunctionalities quizFunctionalities
    @Autowired(required = false)
    protected QuizAnswerService quizAnswerService
    @Autowired(required = false)
    protected QuizAnswerFunctionalities quizAnswerFunctionalities
    @Autowired(required = false)
    protected TournamentService tournamentService
    @Autowired(required = false)
    protected TournamentFunctionalities tournamentFunctionalities

    def loadBehaviorScripts() {
        def mavenBaseDir = System.getProperty("maven.basedir", new File(".").absolutePath)
        def scriptDir = "groovy/" + this.class.simpleName
        impairmentService.LoadDir(mavenBaseDir, scriptDir)
    }

    SagaState sagaStateOf(Integer aggregateId) {
        def uow = unitOfWorkService.createUnitOfWork("TEST")
        def agg = (SagaAggregate) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, uow)
        return agg.getSagaState()
    }

    protected <T> T loadForCheck(Integer aggregateId, Class<T> type) {
        def uow = unitOfWorkService.createUnitOfWork("check")
        return type.cast(unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, uow))
    }

    // Forces the next read to go through Hibernate's instantiation path. Under @DataJpaTest every
    // UnitOfWork in a test shares one persistence context, so a "fresh UnitOfWork" read-back returns
    // the managed write instance and never exercises the load path - final fields set reflectively
    // on load, @Convert converters and lazy associations all go unproven without this.
    protected void flushAndClear() {
        entityManager.flush()
        entityManager.clear()
    }

    // Domain create* helpers are added below as aggregates are implemented in Phase 2.

    Integer createCourse(String name = COURSE_NAME, CourseType type = COURSE_TYPE) {
        def courseDto = new CourseDto()
        courseDto.setName(name)
        courseDto.setType(type)
        return courseFunctionalities.createCourse(courseDto).aggregateId
    }

    Integer createUser(String name = USER_NAME, String username = USER_USERNAME, Role role = USER_ROLE) {
        def userDto = new UserDto()
        userDto.setName(name)
        userDto.setUsername(username)
        userDto.setRole(role)
        return userFunctionalities.createUser(userDto).aggregateId
    }

    Integer createTopic(Integer courseAggregateId, String name = TOPIC_NAME) {
        def topicDto = new TopicDto()
        topicDto.setName(name)
        topicDto.setCourseAggregateId(courseAggregateId)
        return topicFunctionalities.createTopic(topicDto).aggregateId
    }

    Integer createExecution(Integer courseAggregateId, String acronym = EXECUTION_ACRONYM,
                            String academicTerm = EXECUTION_ACADEMIC_TERM,
                            LocalDateTime endDate = EXECUTION_END_DATE) {
        def executionDto = new ExecutionDto()
        executionDto.setCourseAggregateId(courseAggregateId)
        executionDto.setAcronym(acronym)
        executionDto.setAcademicTerm(academicTerm)
        executionDto.setEndDate(endDate)
        return executionFunctionalities.createExecution(executionDto).aggregateId
    }

    void enrollStudentInExecution(Integer executionAggregateId, Integer userAggregateId) {
        executionFunctionalities.enrollStudentInExecution(executionAggregateId, userAggregateId)
    }

    // The enroll functionality fetches the user and rejects an inactive one (INACTIVE_USER), so a
    // student fixture is a created user plus an activation.
    Integer createActiveUser(String name = USER_NAME, String username = USER_USERNAME, Role role = USER_ROLE) {
        def userAggregateId = createUser(name, username, role)
        userFunctionalities.activateUser(userAggregateId)
        return userAggregateId
    }

    // Minimal valid question: no options, and no topics unless the caller asks for them.
    // creationDate is not a CreateQuestion parameter - the 2.5.c functionality stamps it - so it
    // stays out of the signature.
    Integer createQuestion(Integer courseAggregateId, String title = QUESTION_TITLE,
                           String content = QUESTION_CONTENT, List<Integer> topicAggregateIds = []) {
        def questionDto = new QuestionDto()
        questionDto.setCourseAggregateId(courseAggregateId)
        questionDto.setTitle(title)
        questionDto.setContent(content)
        return questionFunctionalities.createQuestion(questionDto, topicAggregateIds).aggregateId
    }

    // CreateQuizAnswer caches each question's correct option key on the seeded QuestionAnswer, so any
    // fixture exercising AnswerQuestion needs a question that actually has one.
    Integer createQuestionWithOptions(Integer courseAggregateId, String title = QUESTION_TITLE,
                                      String content = QUESTION_CONTENT) {
        def questionDto = new QuestionDto()
        questionDto.setCourseAggregateId(courseAggregateId)
        questionDto.setTitle(title)
        questionDto.setContent(content)
        questionDto.setOptions([
                optionDto(0, QUESTION_ANSWER_CORRECT_OPTION_KEY, true),
                optionDto(1, QUESTION_ANSWER_WRONG_OPTION_KEY, false)
        ])
        return questionFunctionalities.createQuestion(questionDto, []).aggregateId
    }

    private static OptionDto optionDto(Integer sequence, Integer optionKey, Boolean correct) {
        def option = new OptionDto()
        option.setSequence(sequence)
        option.setOptionKey(optionKey)
        option.setContent("Option " + optionKey)
        option.setCorrect(correct)
        return option
    }

    // Minimal valid quiz: no questions unless the caller asks for them. creationDate is not a
    // CreateQuiz parameter - the 2.6.c functionality stamps it - so it stays out of the signature, as
    // does the execution version the create saga reads off the fetched execution.
    Integer createQuiz(Integer executionAggregateId, String title = QUIZ_TITLE,
                       LocalDateTime availableDate = QUIZ_AVAILABLE_DATE,
                       LocalDateTime conclusionDate = QUIZ_CONCLUSION_DATE,
                       LocalDateTime resultsDate = QUIZ_RESULTS_DATE,
                       QuizType quizType = QUIZ_TYPE,
                       List<Integer> questionAggregateIds = []) {
        def quizDto = new QuizDto()
        quizDto.setExecutionAggregateId(executionAggregateId)
        quizDto.setTitle(title)
        quizDto.setAvailableDate(availableDate)
        quizDto.setConclusionDate(conclusionDate)
        quizDto.setResultsDate(resultsDate)
        quizDto.setQuizType(quizType)
        return quizFunctionalities.createQuiz(quizDto, questionAggregateIds).aggregateId
    }

    // One QuestionAnswer is seeded per question of the quiz, so a quiz created with no questions yields
    // an empty question-answer list. creationDate and answerDate are not CreateQuizAnswer parameters -
    // the functionality stamps them - so they stay out of the signature.
    Integer createQuizAnswer(Integer quizAggregateId, Integer userAggregateId, Integer executionAggregateId) {
        return quizAnswerFunctionalities.createQuizAnswer(quizAggregateId, userAggregateId,
                executionAggregateId).aggregateId
    }

    // Built directly on the aggregate: CreateTournament does not exist until 2.8.c. The execution,
    // creator and topic snapshots are read off the real upstream aggregates rather than filled from
    // constants, so the DTO fields these tests assert are the same values the 2.8.c create saga will
    // seed and the assertions survive the swap. The caller must have enrolled the creator in the
    // execution — CREATOR_COURSE_EXECUTION is checked by the 2.8.c functionality.
    Integer createTournament(Integer executionAggregateId, Integer creatorAggregateId,
                             LocalDateTime startTime = TOURNAMENT_START_TIME,
                             LocalDateTime endTime = TOURNAMENT_END_TIME,
                             Integer numberOfQuestions = TOURNAMENT_NUMBER_OF_QUESTIONS,
                             List<Integer> topicAggregateIds = []) {
        def executionDto = executionFunctionalities.getExecutionById(executionAggregateId)
        def creatorDto = userFunctionalities.getUserById(creatorAggregateId)
        // The Quiz reference is a placeholder: 2.8.c creates the Quiz inside the same saga and passes
        // its real id and version. No P1 rule reads it, so no read-side assertion depends on it.
        def tournament = new SagaTournament(aggregateIdGeneratorService.getNewAggregateId(),
                executionAggregateId, executionDto.version, executionDto.courseAggregateId,
                creatorAggregateId, creatorDto.name, creatorDto.username, creatorDto.version,
                QUIZ_AGGREGATE_ID, TOURNAMENT_QUIZ_VERSION,
                startTime, endTime, numberOfQuestions)

        if (!topicAggregateIds.isEmpty()) {
            def topicsOfCourse = topicFunctionalities.getTopicsByCourse(executionDto.courseAggregateId)
            topicAggregateIds.each { topicAggregateId ->
                def topicDto = topicsOfCourse.find { it.aggregateId == topicAggregateId }
                tournament.addTopic(new TournamentTopic(topicDto.aggregateId, topicDto.name,
                        topicDto.version, topicDto.courseAggregateId))
            }
        }

        unitOfWorkService.registerChanged(tournament, unitOfWorkService.createUnitOfWork("fixture"))
        return tournament.getAggregateId()
    }

    // Sibling of createTournament, for the same reason: the open/closed reads of 2.8.b partition on
    // the cancelled flag, which only CancelTournament (2.8.c) can raise.
    void cancelTournament(Integer tournamentAggregateId) {
        def unitOfWork = unitOfWorkService.createUnitOfWork("fixture")
        def tournament = new SagaTournament((SagaTournament) unitOfWorkService.aggregateLoadAndRegisterRead(
                tournamentAggregateId, unitOfWork))
        tournament.setCancelled(true)
        unitOfWorkService.registerChanged(tournament, unitOfWork)
    }
}
