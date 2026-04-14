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
class TournamentSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UserService userService
    @Autowired ExecutionService executionService
    @Autowired TopicService topicService
    @Autowired QuestionService questionService
    @Autowired QuizService quizService
    @Autowired TournamentService tournamentService
    @Autowired UnitOfWorkService unitOfWorkService

    // --- seed helpers ---
    private CourseDto seedCourse() {
        def uow = unitOfWorkService.createUnitOfWork("tourn-seed-course")
        def req = new CreateCourseRequestDto()
        req.name = "SE-tourn-${System.nanoTime()}"
        req.type = CourseType.TECNICO
        req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("tourn-seed-user-$tag")
        def req = new CreateUserRequestDto()
        req.name = "User $tag"
        req.username = "tourn-$tag-${System.nanoTime()}"
        req.role = UserRole.STUDENT
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private ExecutionDto seedExecution(CourseDto course, UserDto... users) {
        def uow = unitOfWorkService.createUnitOfWork("tourn-seed-exec")
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
        req.acronym = "TOURN-${System.nanoTime()}"
        req.academicTerm = "2025/2026"
        req.endDate = LocalDateTime.now().plusMonths(6)
        def dto = executionService.createExecution(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private TopicDto seedTopic(CourseDto course, String name) {
        def uow = unitOfWorkService.createUnitOfWork("tourn-seed-topic-$name")
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

    private QuestionDto seedQuestion(CourseDto course, TopicDto topic) {
        def uow = unitOfWorkService.createUnitOfWork("tourn-seed-q")
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
        req.title = "TQ-${System.nanoTime()}"
        req.content = "content"
        req.creationDate = LocalDateTime.now()
        req.options = [opt]
        def dto = questionService.createQuestion(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private QuizDto seedQuiz(ExecutionDto exec, QuestionDto question) {
        def uow = unitOfWorkService.createUnitOfWork("tourn-seed-quiz")
        def execRef = new ExecutionDto()
        execRef.aggregateId = exec.aggregateId
        execRef.acronym = exec.acronym
        def qRef = new QuestionDto()
        qRef.aggregateId = question.aggregateId
        qRef.title = question.title
        def req = new CreateQuizRequestDto()
        req.execution = execRef
        req.questions = [qRef] as Set
        req.title = "TournQuiz-${System.nanoTime()}"
        req.quizType = QuizType.GENERATED
        req.creationDate = LocalDateTime.now()
        req.availableDate = LocalDateTime.now()
        req.conclusionDate = LocalDateTime.now().plusHours(2)
        req.resultsDate = LocalDateTime.now().plusDays(1)
        def dto = quizService.createQuiz(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    // --- tests ---

    def "createTournament with all projections persists correctly"() {
        given: "the full dependency chain: course -> topic -> question -> exec -> quiz -> user"
            def course = seedCourse()
            def creator = seedUser("creator")
            def participant = seedUser("participant")
            def exec = seedExecution(course, creator, participant)
            def topic = seedTopic(course, "TournTopic-${System.nanoTime()}")
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
        when:
            def uow = unitOfWorkService.createUnitOfWork("tourn-create")
            def creatorRef = new UserDto()
            creatorRef.aggregateId = creator.aggregateId
            creatorRef.name = creator.name
            creatorRef.username = creator.username
            def participantRef = new UserDto()
            participantRef.aggregateId = participant.aggregateId
            participantRef.name = participant.name
            participantRef.username = participant.username
            def execRef = new ExecutionDto()
            execRef.aggregateId = exec.aggregateId
            execRef.acronym = exec.acronym
            def topicRef = new TopicDto()
            topicRef.aggregateId = topic.aggregateId
            topicRef.name = topic.name
            def quizRef = new QuizDto()
            quizRef.aggregateId = quiz.aggregateId
            quizRef.title = quiz.title
            def req = new CreateTournamentRequestDto()
            req.creator = creatorRef
            req.participants = [participantRef] as Set
            req.execution = execRef
            req.topics = [topicRef] as Set
            req.quiz = quizRef
            req.startTime = LocalDateTime.now().plusDays(1)
            req.endTime = LocalDateTime.now().plusDays(2)
            req.numberOfQuestions = 5
            req.cancelled = false
            def tournament = tournamentService.createTournament(req, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("tourn-read")
            def found = tournamentService.getTournamentById(tournament.aggregateId, uowR)
            found != null
            found.numberOfQuestions == 5
            found.cancelled == false
    }

    def "tournament with startTime after endTime violates invariant"() {
        given:
            def course = seedCourse()
            def creator = seedUser("inv-creator")
            def exec = seedExecution(course, creator)
            def topic = seedTopic(course, "InvTopic-${System.nanoTime()}")
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
        when:
            def uow = unitOfWorkService.createUnitOfWork("tourn-inv")
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
            req.startTime = LocalDateTime.now().plusDays(2)
            req.endTime = LocalDateTime.now().plusDays(1)  // BEFORE start — violates invariant
            req.numberOfQuestions = 3
            req.cancelled = false
            tournamentService.createTournament(req, uow)
        then:
            thrown(Exception)
    }

    def "getAllTournaments returns created tournaments"() {
        given:
            def course = seedCourse()
            def creator = seedUser("list-creator")
            def exec = seedExecution(course, creator)
            def topic = seedTopic(course, "ListTopic-${System.nanoTime()}")
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
            def uow1 = unitOfWorkService.createUnitOfWork("tourn-list-create")
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
            req.numberOfQuestions = 1
            req.cancelled = false
            def tournament = tournamentService.createTournament(req, uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("tourn-list")
            def all = tournamentService.getAllTournaments(uow2)
        then:
            all.any { it.aggregateId == tournament.aggregateId }
    }

    def "updateTournament changes numberOfQuestions"() {
        given:
            def course = seedCourse()
            def creator = seedUser("upd-creator")
            def exec = seedExecution(course, creator)
            def topic = seedTopic(course, "UpdTopic-${System.nanoTime()}")
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
            def uow1 = unitOfWorkService.createUnitOfWork("tourn-upd-create")
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
            req.numberOfQuestions = 5
            req.cancelled = false
            def tournament = tournamentService.createTournament(req, uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("tourn-upd-get")
            def existing = tournamentService.getTournamentById(tournament.aggregateId, uow2)
            existing.numberOfQuestions = 10
            def uow3 = unitOfWorkService.createUnitOfWork("tourn-upd-put")
            tournamentService.updateTournament(existing, uow3)
            unitOfWorkService.commit(uow3)
        then:
            def uow4 = unitOfWorkService.createUnitOfWork("tourn-upd-read")
            def reloaded = tournamentService.getTournamentById(tournament.aggregateId, uow4)
            reloaded.numberOfQuestions == 10
    }

    def "deleteTournament succeeds"() {
        given:
            def course = seedCourse()
            def creator = seedUser("del-creator")
            def exec = seedExecution(course, creator)
            def topic = seedTopic(course, "DelTopic-${System.nanoTime()}")
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
            def uow1 = unitOfWorkService.createUnitOfWork("tourn-del-create")
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
            req.numberOfQuestions = 1
            req.cancelled = false
            def tournament = tournamentService.createTournament(req, uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("tourn-del")
            tournamentService.deleteTournament(tournament.aggregateId, uow2)
            unitOfWorkService.commit(uow2)
        then:
            noExceptionThrown()
    }
}
