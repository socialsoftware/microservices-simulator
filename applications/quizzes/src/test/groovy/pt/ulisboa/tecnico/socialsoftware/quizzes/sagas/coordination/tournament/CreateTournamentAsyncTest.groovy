package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdRepository
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventRepository
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.VersionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserRepository

@SpringBootTest
class CreateTournamentAsyncTest extends QuizzesSpockTest {
    @Autowired
    private ExecutionFunctionalities courseExecutionFunctionalities

    @Autowired
    private TournamentFunctionalities tournamentFunctionalities

    @Autowired
    private CourseRepository courseRepository
    @Autowired
    private CourseExecutionRepository courseExecutionRepository
    @Autowired
    private UserRepository userRepository
    @Autowired
    private TopicRepository topicRepository
    @Autowired
    private QuestionRepository questionRepository
    @Autowired
    private TournamentRepository tournamentRepository
    @Autowired
    private QuizRepository quizRepository
    @Autowired
    private QuizAnswerRepository quizAnswerRepository
    @Autowired
    private EventRepository eventRepository
    @Autowired
    private AggregateIdRepository aggregateIdRepository
    @Autowired
    private VersionRepository versionRepository

    private CourseExecutionDto courseExecutionDto
    private UserDto userCreatorDto, userDto
    private TopicDto topicDto1, topicDto2, topicDto3
    private QuestionDto questionDto1, questionDto2, questionDto3

    def setup() {
        given: 'load behavior scripts for controlled delays'
        loadBehaviorScripts()

        given: 'a course execution'
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)

        and: 'two students in course execution'
        userCreatorDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())

        userDto = createUser(USER_NAME_2, USER_USERNAME_2, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto.aggregateId)

        and: 'three topics and questions'
        topicDto1 = createTopic(courseExecutionDto, TOPIC_NAME_1)
        topicDto2 = createTopic(courseExecutionDto, TOPIC_NAME_2)
        topicDto3 = createTopic(courseExecutionDto, TOPIC_NAME_3)

        questionDto1 = createQuestion(courseExecutionDto, new HashSet<>([topicDto1]), TITLE_1, CONTENT_1, OPTION_1, OPTION_2)
        questionDto2 = createQuestion(courseExecutionDto, new HashSet<>([topicDto2]), TITLE_2, CONTENT_2, OPTION_3, OPTION_4)
        questionDto3 = createQuestion(courseExecutionDto, new HashSet<>([topicDto3]), TITLE_3, CONTENT_3, OPTION_1, OPTION_3)
    }

    @Transactional
    def cleanup() {
        tournamentRepository.deleteAllInBatch()
        quizAnswerRepository.deleteAllInBatch()
        quizRepository.deleteAllInBatch()
        questionRepository.deleteAllInBatch()
        topicRepository.deleteAllInBatch()
        courseExecutionRepository.deleteAllInBatch()
        courseRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
        eventRepository.deleteAllInBatch()
        aggregateIdRepository.deleteAllInBatch()
        versionRepository.deleteAllInBatch()
        impairmentService.cleanUpCounter()
        impairmentService.cleanDirectory()
    }

    def 'create tournament using async functionality'() {
        when:
        def result = tournamentFunctionalities.createTournamentAsync(
                userCreatorDto.getAggregateId(),
                courseExecutionDto.getAggregateId(),
                [topicDto1.getAggregateId(), topicDto2.getAggregateId()],
                new TournamentDto(
                        startTime: DateHandler.toISOString(TIME_1),
                        endTime: DateHandler.toISOString(TIME_3),
                        numberOfQuestions: 2
                )
        )

        then:
        result != null
        result.numberOfQuestions == 2
        result.topics*.aggregateId.containsAll([topicDto1.getAggregateId(), topicDto2.getAggregateId()])
        result.quiz != null
    }

    def 'report async vs sync create tournament durations'() {
        given: 'a richer topic/question set to amplify read latency effects'
        def benchmarkTopicIds = []
        (1..8).each { idx ->
            def topic = createTopic(courseExecutionDto, "CREATE_BENCH_TOPIC_${idx}")
            benchmarkTopicIds << topic.getAggregateId()
            createQuestion(courseExecutionDto, new HashSet<>([topic]), "CREATE_BENCH_TITLE_${idx}", "CREATE_BENCH_CONTENT_${idx}", "CREATE_BENCH_OPT_A_${idx}", "CREATE_BENCH_OPT_B_${idx}")
        }

        and: 'duration collectors'
        def syncDurations = []
        def asyncDurations = []

        when: 'run sync and async create flows with disjoint time windows'
        (1..3).each { idx ->
            def syncStartTime = TIME_1.plusDays(1).plusHours(idx * 4L)
            def syncEndTime = syncStartTime.plusHours(1)
            def syncStart = System.currentTimeMillis()
            createTournament(syncStartTime, syncEndTime, 8, userCreatorDto.getAggregateId(), courseExecutionDto.getAggregateId(), benchmarkTopicIds)
            syncDurations << (System.currentTimeMillis() - syncStart)

            def asyncStartTime = TIME_1.plusDays(1).plusHours(idx * 4L + 2)
            def asyncEndTime = asyncStartTime.plusHours(1)
            def asyncStart = System.currentTimeMillis()
            tournamentFunctionalities.createTournamentAsync(
                    userCreatorDto.getAggregateId(),
                    courseExecutionDto.getAggregateId(),
                    benchmarkTopicIds,
                    new TournamentDto(
                            startTime: DateHandler.toISOString(asyncStartTime),
                            endTime: DateHandler.toISOString(asyncEndTime),
                            numberOfQuestions: 8
                    )
            )
            asyncDurations << (System.currentTimeMillis() - asyncStart)
        }

        then: 'both variants complete and timings are reported'
        syncDurations.size() == 3
        asyncDurations.size() == 3
        syncDurations.every { it >= 0 }
        asyncDurations.every { it >= 0 }

        and: 'report-only comparison to avoid flaky speed assertions'
        println "CreateTournament sync durations (ms): ${syncDurations}"
        println "CreateTournament async durations (ms): ${asyncDurations}"
        println "CreateTournament sync median (ms): ${median(syncDurations)}"
        println "CreateTournament async median (ms): ${median(asyncDurations)}"
        true
    }

    private static long median(List<Long> values) {
        def sorted = values.sort(false)
        return sorted[sorted.size() / 2]
    }
}