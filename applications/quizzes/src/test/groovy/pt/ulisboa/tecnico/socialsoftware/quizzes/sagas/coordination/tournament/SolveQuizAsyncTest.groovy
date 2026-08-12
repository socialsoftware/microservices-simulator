package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.tournament

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.transaction.annotation.Transactional
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdRepository
import pt.ulisboa.tecnico.socialsoftware.ms.notification.EventRepository
import pt.ulisboa.tecnico.socialsoftware.ms.versioning.VersionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.QuizzesSpockTest
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.answer.aggregate.QuizAnswerRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities.ExecutionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities.TournamentFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserRepository

@SpringBootTest
class SolveQuizAsyncTest extends QuizzesSpockTest {
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
    private UserDto userCreatorDto, userDto, userDto3
    private TopicDto topicDto1, topicDto2, topicDto3
    private QuestionDto questionDto1, questionDto2, questionDto3
    private TournamentDto tournamentDto

    def setup() {
        given: 'a course execution'
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)

        and: 'three students in course execution'
        userCreatorDto = createUser(USER_NAME_1, USER_USERNAME_1, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.getAggregateId(), userCreatorDto.getAggregateId())

        userDto = createUser(USER_NAME_2, USER_USERNAME_2, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto.aggregateId)

        userDto3 = createUser(USER_NAME_3, USER_USERNAME_3, STUDENT_ROLE)
        courseExecutionFunctionalities.addStudent(courseExecutionDto.aggregateId, userDto3.aggregateId)

        and: 'three topics and questions'
        topicDto1 = createTopic(courseExecutionDto, TOPIC_NAME_1)
        topicDto2 = createTopic(courseExecutionDto, TOPIC_NAME_2)
        topicDto3 = createTopic(courseExecutionDto, TOPIC_NAME_3)

        questionDto1 = createQuestion(courseExecutionDto, new HashSet<>([topicDto1]), TITLE_1, CONTENT_1, OPTION_1, OPTION_2)
        questionDto2 = createQuestion(courseExecutionDto, new HashSet<>([topicDto2]), TITLE_2, CONTENT_2, OPTION_3, OPTION_4)
        questionDto3 = createQuestion(courseExecutionDto, new HashSet<>([topicDto3]), TITLE_3, CONTENT_3, OPTION_1, OPTION_3)

        and: 'a tournament created by first user'
        tournamentDto = createTournament(TIME_1, TIME_3, 2, userCreatorDto.getAggregateId(), courseExecutionDto.getAggregateId(), [topicDto1.getAggregateId(), topicDto2.getAggregateId()])
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
    }

    def 'solve quiz using async functionality'() {
        given: 'a participant in tournament'
        tournamentFunctionalities.addParticipant(tournamentDto.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())

        when: 'student solves quiz with async variant'
        QuizDto quizDto = tournamentFunctionalities.solveQuizAsync(tournamentDto.getAggregateId(), userDto.getAggregateId())

        then: 'quiz is returned with options sanitized for frontend'
        quizDto != null
        quizDto.aggregateId == tournamentDto.quiz.aggregateId
        quizDto.questionDtos.size() > 0
        quizDto.questionDtos.every { question -> question.optionDtos.every { !it.correct } }
    }

    def 'report async vs sync solve quiz durations'() {
        given: 'a richer tournament to make read parallelism visible'
        def benchmarkTopicIds = []
        (1..8).each { idx ->
            def topic = createTopic(courseExecutionDto, "BENCH_TOPIC_${idx}")
            benchmarkTopicIds << topic.getAggregateId()
            createQuestion(courseExecutionDto, new HashSet<>([topic]), "BENCH_TITLE_${idx}", "BENCH_CONTENT_${idx}", "BENCH_OPT_A_${idx}", "BENCH_OPT_B_${idx}")
        }

        and: 'duration collectors'
        def syncDurations = []
        def asyncDurations = []

        when: 'run sync and async solve flows in isolated tournaments'
        (1..3).each {
            def syncTournament = createTournament(TIME_1, TIME_3, 8, userCreatorDto.getAggregateId(), courseExecutionDto.getAggregateId(), benchmarkTopicIds)
            tournamentFunctionalities.addParticipant(syncTournament.getAggregateId(), courseExecutionDto.getAggregateId(), userDto.getAggregateId())
            def syncStart = System.currentTimeMillis()
            tournamentFunctionalities.solveQuiz(syncTournament.getAggregateId(), userDto.getAggregateId())
            syncDurations << (System.currentTimeMillis() - syncStart)

            def asyncTournament = createTournament(TIME_1, TIME_3, 8, userCreatorDto.getAggregateId(), courseExecutionDto.getAggregateId(), benchmarkTopicIds)
            tournamentFunctionalities.addParticipant(asyncTournament.getAggregateId(), courseExecutionDto.getAggregateId(), userDto3.getAggregateId())
            def asyncStart = System.currentTimeMillis()
            tournamentFunctionalities.solveQuizAsync(asyncTournament.getAggregateId(), userDto3.getAggregateId())
            asyncDurations << (System.currentTimeMillis() - asyncStart)
        }

        then: 'both variants finish and timings are reported'
        syncDurations.size() == 3
        asyncDurations.size() == 3
        syncDurations.every { it >= 0 }
        asyncDurations.every { it >= 0 }

        and: 'print a report-only comparison to avoid flaky threshold assertions'
        println "SolveQuiz sync durations (ms): ${syncDurations}"
        println "SolveQuiz async durations (ms): ${asyncDurations}"
        println "SolveQuiz sync median (ms): ${median(syncDurations)}"
        println "SolveQuiz async median (ms): ${median(asyncDurations)}"
        true
    }

    private static long median(List<Long> values) {
        def sorted = values.sort(false)
        return sorted[sorted.size() / 2]
    }
}
