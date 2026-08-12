package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.question

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
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.aggregate.QuestionRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.question.coordination.functionalities.QuestionFunctionalities
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserRepository

@SpringBootTest
class UpdateQuestionTopicsAsyncTest extends QuizzesSpockTest {
    @Autowired
    private QuestionFunctionalities questionFunctionalities

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
    private TopicDto topicDto1, topicDto2, topicDto3
    private QuestionDto questionDto

    def setup() {
        given: 'load behavior scripts for controlled delays'
        loadBehaviorScripts()

        given: 'a course execution and topics'
        courseExecutionDto = createCourseExecution(COURSE_EXECUTION_NAME, COURSE_EXECUTION_TYPE, COURSE_EXECUTION_ACRONYM, COURSE_EXECUTION_ACADEMIC_TERM, TIME_4)

        topicDto1 = createTopic(courseExecutionDto, TOPIC_NAME_1)
        topicDto2 = createTopic(courseExecutionDto, TOPIC_NAME_2)
        topicDto3 = createTopic(courseExecutionDto, TOPIC_NAME_3)

        and: 'a question initially attached to one topic'
        questionDto = createQuestion(courseExecutionDto, new HashSet<>([topicDto1]), TITLE_1, CONTENT_1, OPTION_1, OPTION_2)
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

    def 'update question topics using async functionality'() {
        when:
        questionFunctionalities.updateQuestionTopicsAsync(questionDto.getAggregateId(), [topicDto2.getAggregateId(), topicDto3.getAggregateId()])
        def updatedQuestion = questionFunctionalities.findQuestionByAggregateId(questionDto.getAggregateId())

        then:
        updatedQuestion != null
        updatedQuestion.topicDto*.aggregateId as Set == [topicDto2.getAggregateId(), topicDto3.getAggregateId()] as Set
    }

    def 'report async vs sync update question topics durations'() {
        given: 'a larger topic set to exercise async topic reads'
        def benchmarkTopicIds = []
        (1..10).each { idx ->
            def topic = createTopic(courseExecutionDto, "QUESTION_TOPIC_BENCH_${idx}")
            benchmarkTopicIds << topic.getAggregateId()
        }

        and: 'duration collectors'
        def syncDurations = []
        def asyncDurations = []

        when: 'run sync and async updates on fresh questions'
        (1..3).each { idx ->
            def syncQuestion = createQuestion(courseExecutionDto, new HashSet<>([topicDto1]), "SYNC_Q_TITLE_${idx}", "SYNC_Q_CONTENT_${idx}", "SYNC_Q_OPT_A_${idx}", "SYNC_Q_OPT_B_${idx}")
            def syncStart = System.currentTimeMillis()
            questionFunctionalities.updateQuestionTopics(syncQuestion.getAggregateId(), benchmarkTopicIds)
            syncDurations << (System.currentTimeMillis() - syncStart)

            def asyncQuestion = createQuestion(courseExecutionDto, new HashSet<>([topicDto1]), "ASYNC_Q_TITLE_${idx}", "ASYNC_Q_CONTENT_${idx}", "ASYNC_Q_OPT_A_${idx}", "ASYNC_Q_OPT_B_${idx}")
            def asyncStart = System.currentTimeMillis()
            questionFunctionalities.updateQuestionTopicsAsync(asyncQuestion.getAggregateId(), benchmarkTopicIds)
            asyncDurations << (System.currentTimeMillis() - asyncStart)
        }

        then: 'both variants complete and timings are reported'
        syncDurations.size() == 3
        asyncDurations.size() == 3
        syncDurations.every { it >= 0 }
        asyncDurations.every { it >= 0 }

        and: 'report-only comparison to avoid flaky speed assertions'
        println "UpdateQuestionTopics sync durations (ms): ${syncDurations}"
        println "UpdateQuestionTopics async durations (ms): ${asyncDurations}"
        println "UpdateQuestionTopics sync median (ms): ${median(syncDurations)}"
        println "UpdateQuestionTopics async median (ms): ${median(asyncDurations)}"
        true
    }

    private static long median(List<Long> values) {
        def sorted = values.sort(false)
        return sorted[sorted.size() / 2]
    }
}