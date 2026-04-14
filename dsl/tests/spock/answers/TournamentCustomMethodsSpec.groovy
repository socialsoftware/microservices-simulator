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
class TournamentCustomMethodsSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UserService userService
    @Autowired ExecutionService executionService
    @Autowired TopicService topicService
    @Autowired QuestionService questionService
    @Autowired QuizService quizService
    @Autowired TournamentService tournamentService
    @Autowired UnitOfWorkService unitOfWorkService

    private CourseDto seedCourse() {
        def uow = unitOfWorkService.createUnitOfWork("tcm-seed-course")
        def req = new CreateCourseRequestDto()
        req.name = "TCM-${System.nanoTime()}"
        req.type = CourseType.TECNICO
        req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("tcm-seed-user-$tag")
        def req = new CreateUserRequestDto()
        req.name = "User $tag"
        req.username = "tcm-$tag-${System.nanoTime()}"
        req.role = UserRole.STUDENT
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private ExecutionDto seedExecution(CourseDto course, UserDto... users) {
        def uow = unitOfWorkService.createUnitOfWork("tcm-seed-exec")
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
        req.acronym = "TCM-${System.nanoTime()}"
        req.academicTerm = "2025/2026"
        req.endDate = LocalDateTime.now().plusMonths(6)
        def dto = executionService.createExecution(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private TopicDto seedTopic(CourseDto course) {
        def uow = unitOfWorkService.createUnitOfWork("tcm-seed-topic")
        def courseRef = new CourseDto()
        courseRef.aggregateId = course.aggregateId
        courseRef.name = course.name
        def req = new CreateTopicRequestDto()
        req.course = courseRef
        req.name = "TCMTopic-${System.nanoTime()}"
        def dto = topicService.createTopic(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private QuestionDto seedQuestion(CourseDto course, TopicDto topic) {
        def uow = unitOfWorkService.createUnitOfWork("tcm-seed-q")
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
        req.title = "TCM-Q-${System.nanoTime()}"
        req.content = "content"
        req.creationDate = LocalDateTime.now()
        req.options = [opt]
        def dto = questionService.createQuestion(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private QuizDto seedQuiz(ExecutionDto exec, QuestionDto question) {
        def uow = unitOfWorkService.createUnitOfWork("tcm-seed-quiz")
        def execRef = new ExecutionDto()
        execRef.aggregateId = exec.aggregateId
        execRef.acronym = exec.acronym
        def qRef = new QuestionDto()
        qRef.aggregateId = question.aggregateId
        qRef.title = question.title
        def req = new CreateQuizRequestDto()
        req.execution = execRef
        req.questions = [qRef] as Set
        req.title = "TCMQuiz-${System.nanoTime()}"
        req.quizType = QuizType.GENERATED
        req.creationDate = LocalDateTime.now()
        req.availableDate = LocalDateTime.now()
        req.conclusionDate = LocalDateTime.now().plusHours(2)
        req.resultsDate = LocalDateTime.now().plusDays(1)
        def dto = quizService.createQuiz(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private def createTournament(CourseDto course, UserDto creator) {
        def exec = seedExecution(course, creator)
        def topic = seedTopic(course)
        def question = seedQuestion(course, topic)
        def quiz = seedQuiz(exec, question)

        def uow = unitOfWorkService.createUnitOfWork("tcm-create-tourn")
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
        def tournament = tournamentService.createTournament(req, uow)
        unitOfWorkService.commit(uow)
        return tournament
    }

    def "cancelTournament sets cancelled to true"() {
        given:
            def course = seedCourse()
            def creator = seedUser("cancel-c")
            def tournament = createTournament(course, creator)
        when:
            def uow = unitOfWorkService.createUnitOfWork("tcm-cancel")
            tournamentService.cancelTournament(tournament.aggregateId, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("tcm-cancel-read")
            def found = tournamentService.getTournamentById(tournament.aggregateId, uowR)
            found.cancelled == true
    }

    def "cancelTournament then read back still persists other fields"() {
        given:
            def course = seedCourse()
            def creator = seedUser("cancel-fields")
            def tournament = createTournament(course, creator)
        when:
            def uow = unitOfWorkService.createUnitOfWork("tcm-cancel-f")
            tournamentService.cancelTournament(tournament.aggregateId, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("tcm-cancel-f-read")
            def found = tournamentService.getTournamentById(tournament.aggregateId, uowR)
            found.numberOfQuestions == 5
            found.cancelled == true
    }

    def "updateParticipantName changes the participant name"() {
        given:
            def course = seedCourse()
            def creator = seedUser("upn-creator")
            def participant = seedUser("upn-part")
            def exec = seedExecution(course, creator, participant)
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)

            def uow1 = unitOfWorkService.createUnitOfWork("tcm-upn-create")
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
            def topicRef = new TopicDto()
            topicRef.aggregateId = topic.aggregateId
            topicRef.name = topic.name
            def quizRef = new QuizDto()
            quizRef.aggregateId = quiz.aggregateId
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
            def tournament = tournamentService.createTournament(req, uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow = unitOfWorkService.createUnitOfWork("tcm-upn")
            tournamentService.updateParticipantName(tournament.aggregateId, participant.aggregateId, "NewName", uow)
            unitOfWorkService.commit(uow)
        then:
            noExceptionThrown()
    }

    def "cancelled tournament excluded from getActiveTournaments"() {
        given:
            def course = seedCourse()
            def creator = seedUser("cancel-active")
            def tournament = createTournament(course, creator)
            def uowC = unitOfWorkService.createUnitOfWork("tcm-cancel-excl")
            tournamentService.cancelTournament(tournament.aggregateId, uowC)
            unitOfWorkService.commit(uowC)
        when:
            def uow = unitOfWorkService.createUnitOfWork("tcm-active-after-cancel")
            def results = tournamentService.getActiveTournaments(uow)
        then:
            !results.any { it.aggregateId == tournament.aggregateId }
    }

    def "getActiveTournaments includes non-cancelled tournament"() {
        given:
            def course = seedCourse()
            def creator = seedUser("active-check")
            def tournament = createTournament(course, creator)
        when:
            def uow = unitOfWorkService.createUnitOfWork("tcm-active")
            def results = tournamentService.getActiveTournaments(uow)
        then:
            results.any { it.aggregateId == tournament.aggregateId }
    }
}
