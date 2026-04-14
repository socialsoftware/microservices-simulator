package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.webapi.requestDtos.CreateCourseRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.webapi.requestDtos.CreateExecutionRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.webapi.requestDtos.CreateQuestionRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.webapi.requestDtos.CreateQuizRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.webapi.requestDtos.CreateTopicRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.webapi.requestDtos.CreateTournamentRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.*
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class QueryMethodsSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UserService userService
    @Autowired ExecutionService executionService
    @Autowired TopicService topicService
    @Autowired QuestionService questionService
    @Autowired QuizService quizService
    @Autowired TournamentService tournamentService
    @Autowired UnitOfWorkService unitOfWorkService

    // --- seed helpers ---
    private CourseDto seedCourse(String name) {
        def uow = unitOfWorkService.createUnitOfWork("qm-seed-course")
        def req = new CreateCourseRequestDto()
        req.name = name
        req.type = CourseType.TECNICO
        req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private UserDto seedUser(String tag, UserRole role) {
        def uow = unitOfWorkService.createUnitOfWork("qm-seed-user-$tag")
        def req = new CreateUserRequestDto()
        req.name = "User $tag"
        req.username = "qm-$tag-${System.nanoTime()}"
        req.role = role
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private ExecutionDto seedExecution(CourseDto course, UserDto... users) {
        def uow = unitOfWorkService.createUnitOfWork("qm-seed-exec")
        def courseRef = new CourseDto()
        courseRef.aggregateId = course.aggregateId
        courseRef.name = course.name
        def userRefs = users.collect { u ->
            def ref = new UserDto()
            ref.aggregateId = u.aggregateId
            ref.name = u.name
            ref.username = u.username
            return ref
        } as Set
        def req = new CreateExecutionRequestDto()
        req.course = courseRef
        req.users = userRefs
        req.acronym = "QM-${System.nanoTime()}"
        req.academicTerm = "2025/2026"
        req.endDate = LocalDateTime.now().plusMonths(6)
        def dto = executionService.createExecution(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private TopicDto seedTopic(CourseDto course, String name) {
        def uow = unitOfWorkService.createUnitOfWork("qm-seed-topic")
        def courseRef = new CourseDto()
        courseRef.aggregateId = course.aggregateId
        courseRef.name = course.name
        def req = new CreateTopicRequestDto()
        req.course = courseRef
        req.name = name
        def dto = topicService.createTopic(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private QuestionDto seedQuestion(CourseDto course, TopicDto topic, String title) {
        def uow = unitOfWorkService.createUnitOfWork("qm-seed-q")
        def courseRef = new CourseDto()
        courseRef.aggregateId = course.aggregateId
        courseRef.name = course.name
        def topicRef = new TopicDto()
        topicRef.aggregateId = topic.aggregateId
        topicRef.name = topic.name
        def opt = new OptionDto()
        opt.key = 1; opt.sequence = 1; opt.correct = true; opt.content = "Yes"
        def req = new CreateQuestionRequestDto()
        req.course = courseRef
        req.topics = [topicRef] as Set
        req.title = title
        req.content = "content"
        req.creationDate = LocalDateTime.now()
        req.options = [opt]
        def dto = questionService.createQuestion(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private QuizDto seedQuiz(ExecutionDto exec, QuestionDto question) {
        def uow = unitOfWorkService.createUnitOfWork("qm-seed-quiz")
        def execRef = new ExecutionDto()
        execRef.aggregateId = exec.aggregateId
        execRef.acronym = exec.acronym
        def qRef = new QuestionDto()
        qRef.aggregateId = question.aggregateId
        qRef.title = question.title
        def req = new CreateQuizRequestDto()
        req.execution = execRef
        req.questions = [qRef] as Set
        req.title = "QMQuiz-${System.nanoTime()}"
        req.quizType = QuizType.GENERATED
        req.creationDate = LocalDateTime.now()
        req.availableDate = LocalDateTime.now()
        req.conclusionDate = LocalDateTime.now().plusHours(2)
        req.resultsDate = LocalDateTime.now().plusDays(1)
        def dto = quizService.createQuiz(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    // --- findQuestionsByTitle ---

    def "findQuestionsByTitle returns matching questions"() {
        given:
            def course = seedCourse("QMCourse-${System.nanoTime()}")
            def topic = seedTopic(course, "QMTopic-${System.nanoTime()}")
            def uniquePrefix = "UNIQUE-${System.nanoTime()}"
            def q1 = seedQuestion(course, topic, "${uniquePrefix}-Alpha")
            def q2 = seedQuestion(course, topic, "${uniquePrefix}-Beta")
            seedQuestion(course, topic, "Unrelated-${System.nanoTime()}")
        when:
            def uow = unitOfWorkService.createUnitOfWork("qm-find-q")
            def results = questionService.findQuestionsByTitle("${uniquePrefix}%", uow)
        then:
            results.size() >= 2
            results.any { it.aggregateId == q1.aggregateId }
            results.any { it.aggregateId == q2.aggregateId }
    }

    def "findQuestionsByTitle returns empty for no match"() {
        when:
            def uow = unitOfWorkService.createUnitOfWork("qm-find-q-none")
            def results = questionService.findQuestionsByTitle("ZZZNOMATCH-${System.nanoTime()}%", uow)
        then:
            results.isEmpty()
    }

    // --- getAllNonDeletedExecutions ---

    def "getAllNonDeletedExecutions returns active executions"() {
        given:
            def course = seedCourse("QMExecCourse-${System.nanoTime()}")
            def exec = seedExecution(course)
        when:
            def uow = unitOfWorkService.createUnitOfWork("qm-all-exec")
            def results = executionService.getAllNonDeletedExecutions(uow)
        then:
            results.any { it.aggregateId == exec.aggregateId }
    }

    def "getAllNonDeletedExecutions excludes deleted executions"() {
        given:
            def course = seedCourse("QMDelExecCourse-${System.nanoTime()}")
            def exec = seedExecution(course)
            def uowDel = unitOfWorkService.createUnitOfWork("qm-del-exec")
            executionService.deleteExecution(exec.aggregateId, uowDel)
            unitOfWorkService.commit(uowDel)
        when:
            def uow = unitOfWorkService.createUnitOfWork("qm-all-exec-after-del")
            def results = executionService.getAllNonDeletedExecutions(uow)
        then:
            !results.any { it.aggregateId == exec.aggregateId }
    }

    // --- getActiveTournaments ---

    def "getActiveTournaments returns non-cancelled tournaments"() {
        given:
            def course = seedCourse("QMTournCourse-${System.nanoTime()}")
            def creator = seedUser("qm-creator", UserRole.STUDENT)
            def exec = seedExecution(course, creator)
            def topic = seedTopic(course, "QMTournTopic-${System.nanoTime()}")
            def question = seedQuestion(course, topic, "QMTournQ-${System.nanoTime()}")
            def quiz = seedQuiz(exec, question)

            def uow1 = unitOfWorkService.createUnitOfWork("qm-tourn-create")
            def creatorRef = new UserDto()
            creatorRef.aggregateId = creator.aggregateId
            creatorRef.name = creator.name
            creatorRef.username = creator.username
            def execRef = new ExecutionDto()
            execRef.aggregateId = exec.aggregateId
            def topicRef = new TopicDto()
            topicRef.aggregateId = topic.aggregateId
            topicRef.name = topic.name
            def quizRef = new QuizDto()
            quizRef.aggregateId = quiz.aggregateId
            def req = new CreateTournamentRequestDto()
            req.creator = creatorRef
            req.participants = [] as Set
            req.execution = execRef
            req.topics = [topicRef] as Set
            req.quiz = quizRef
            req.startTime = LocalDateTime.now().plusDays(1)
            req.endTime = LocalDateTime.now().plusDays(2)
            req.numberOfQuestions = 3
            req.cancelled = false
            def tournament = tournamentService.createTournament(req, uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow = unitOfWorkService.createUnitOfWork("qm-active-tourn")
            def results = tournamentService.getActiveTournaments(uow)
        then:
            results.any { it.aggregateId == tournament.aggregateId }
    }
}
