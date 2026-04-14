package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.webapi.requestDtos.CreateCourseRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.webapi.requestDtos.CreateExecutionRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.webapi.requestDtos.CreateTopicRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.eventProcessing.AnswerEventProcessing
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.webapi.requestDtos.CreateAnswerRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.eventProcessing.QuestionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.webapi.requestDtos.CreateQuestionRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.eventProcessing.QuizEventProcessing
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.webapi.requestDtos.CreateQuizRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.eventProcessing.TournamentEventProcessing
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.webapi.requestDtos.CreateTournamentRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.answers.events.*
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.*
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class ProjectionSyncFullSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UserService userService
    @Autowired ExecutionService executionService
    @Autowired TopicService topicService
    @Autowired QuestionService questionService
    @Autowired QuizService quizService
    @Autowired TournamentService tournamentService
    @Autowired UnitOfWorkService unitOfWorkService

    @Autowired AnswerService answerService
    @Autowired AnswerEventProcessing answerEventProcessing
    @Autowired QuestionEventProcessing questionEventProcessing
    @Autowired QuizEventProcessing quizEventProcessing
    @Autowired TournamentEventProcessing tournamentEventProcessing

    // --- seed helpers ---
    private CourseDto seedCourse() {
        def uow = unitOfWorkService.createUnitOfWork("ps-seed-course")
        def req = new CreateCourseRequestDto()
        req.name = "PS-${System.nanoTime()}"; req.type = CourseType.TECNICO; req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("ps-seed-user-$tag")
        def req = new CreateUserRequestDto()
        req.name = "User $tag"; req.username = "ps-$tag-${System.nanoTime()}"; req.role = UserRole.STUDENT; req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private ExecutionDto seedExecution(CourseDto course, UserDto... users) {
        def uow = unitOfWorkService.createUnitOfWork("ps-seed-exec")
        def courseRef = new CourseDto(); courseRef.aggregateId = course.aggregateId; courseRef.name = course.name
        def userRefs = users.collect { u ->
            def ref = new UserDto(); ref.aggregateId = u.aggregateId; ref.name = u.name; ref.username = u.username; return ref
        } as Set
        def req = new CreateExecutionRequestDto()
        req.course = courseRef; req.users = userRefs; req.acronym = "PS-${System.nanoTime()}"
        req.academicTerm = "2025/2026"; req.endDate = LocalDateTime.now().plusMonths(6)
        def dto = executionService.createExecution(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private TopicDto seedTopic(CourseDto course) {
        def uow = unitOfWorkService.createUnitOfWork("ps-seed-topic")
        def courseRef = new CourseDto(); courseRef.aggregateId = course.aggregateId; courseRef.name = course.name
        def req = new CreateTopicRequestDto(); req.course = courseRef; req.name = "PSTopic-${System.nanoTime()}"
        def dto = topicService.createTopic(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private QuestionDto seedQuestion(CourseDto course, TopicDto topic) {
        def uow = unitOfWorkService.createUnitOfWork("ps-seed-q")
        def courseRef = new CourseDto(); courseRef.aggregateId = course.aggregateId; courseRef.name = course.name
        def topicRef = new TopicDto(); topicRef.aggregateId = topic.aggregateId; topicRef.name = topic.name
        def opt = new OptionDto(); opt.key = 1; opt.sequence = 1; opt.correct = true; opt.content = "Yes"
        def req = new CreateQuestionRequestDto()
        req.course = courseRef; req.topics = [topicRef] as Set; req.title = "PS-Q-${System.nanoTime()}"
        req.content = "content"; req.creationDate = LocalDateTime.now(); req.options = [opt]
        def dto = questionService.createQuestion(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private QuizDto seedQuiz(ExecutionDto exec, QuestionDto question) {
        def uow = unitOfWorkService.createUnitOfWork("ps-seed-quiz")
        def execRef = new ExecutionDto(); execRef.aggregateId = exec.aggregateId; execRef.acronym = exec.acronym
        def qRef = new QuestionDto(); qRef.aggregateId = question.aggregateId; qRef.title = question.title
        def req = new CreateQuizRequestDto()
        req.execution = execRef; req.questions = [qRef] as Set; req.title = "PSQuiz-${System.nanoTime()}"
        req.quizType = QuizType.GENERATED; req.creationDate = LocalDateTime.now()
        req.availableDate = LocalDateTime.now(); req.conclusionDate = LocalDateTime.now().plusHours(2)
        req.resultsDate = LocalDateTime.now().plusDays(1)
        def dto = quizService.createQuiz(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    // === TopicUpdated -> Question projection sync ===

    def "TopicUpdated event processes without error on Question"() {
        given:
            def course = seedCourse()
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
            def event = new TopicUpdatedEvent(topic.aggregateId, "NewTopicName")
            event.setPublisherAggregateVersion(1)
        when:
            questionEventProcessing.processTopicUpdatedEvent(question.aggregateId, event)
        then:
            noExceptionThrown()
    }

    // === ExecutionUpdated -> Quiz projection sync ===

    def "ExecutionUpdated event processes without error on Quiz"() {
        given:
            def course = seedCourse()
            def exec = seedExecution(course)
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
            def event = new ExecutionUpdatedEvent(exec.aggregateId, "NEW-ACRONYM", "2026/2027", LocalDateTime.now().plusMonths(12))
            event.setPublisherAggregateVersion(1)
        when:
            quizEventProcessing.processExecutionUpdatedEvent(quiz.aggregateId, event)
        then:
            noExceptionThrown()
    }

    // === TopicUpdated -> Quiz projection sync ===

    def "TopicUpdated event processes without error on Quiz"() {
        given:
            def course = seedCourse()
            def exec = seedExecution(course)
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
            def event = new TopicUpdatedEvent(topic.aggregateId, "UpdatedTopic")
            event.setPublisherAggregateVersion(1)
        when:
            quizEventProcessing.processTopicUpdatedEvent(quiz.aggregateId, event)
        then:
            noExceptionThrown()
    }

    // === TopicDeleted -> Quiz (plain subscribe, not interInvariant) ===

    def "TopicDeleted plain subscribe event processes without error on Quiz"() {
        given:
            def course = seedCourse()
            def exec = seedExecution(course)
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
            def event = new TopicDeletedEvent(topic.aggregateId)
            event.setPublisherAggregateVersion(1)
        when:
            quizEventProcessing.processTopicDeletedEvent(quiz.aggregateId, event)
        then:
            noExceptionThrown()
    }

    // === ExecutionUpdated -> Tournament projection sync ===

    def "ExecutionUpdated event processes without error on Tournament"() {
        given:
            def course = seedCourse()
            def creator = seedUser("tourn-eup")
            def exec = seedExecution(course, creator)
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
            def uow = unitOfWorkService.createUnitOfWork("ps-tourn-create")
            def creatorRef = new UserDto(); creatorRef.aggregateId = creator.aggregateId; creatorRef.name = creator.name; creatorRef.username = creator.username
            def execRef = new ExecutionDto(); execRef.aggregateId = exec.aggregateId
            def topicRef = new TopicDto(); topicRef.aggregateId = topic.aggregateId; topicRef.name = topic.name
            def quizRef = new QuizDto(); quizRef.aggregateId = quiz.aggregateId
            def req = new CreateTournamentRequestDto()
            req.creator = creatorRef; req.participants = [] as Set; req.execution = execRef
            req.topics = [topicRef] as Set; req.quiz = quizRef
            req.startTime = LocalDateTime.now().plusDays(1); req.endTime = LocalDateTime.now().plusDays(2)
            req.numberOfQuestions = 3; req.cancelled = false
            def tournament = tournamentService.createTournament(req, uow)
            unitOfWorkService.commit(uow)
            def event = new ExecutionUpdatedEvent(exec.aggregateId, "UPD-ACR", "2026/2027", LocalDateTime.now().plusMonths(12))
            event.setPublisherAggregateVersion(1)
        when:
            tournamentEventProcessing.processExecutionUpdatedEvent(tournament.aggregateId, event)
        then:
            noExceptionThrown()
    }

    // === TopicUpdated -> Tournament projection sync ===

    def "TopicUpdated event processes without error on Tournament"() {
        given:
            def course = seedCourse()
            def creator = seedUser("tourn-tup")
            def exec = seedExecution(course, creator)
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
            def uow = unitOfWorkService.createUnitOfWork("ps-tourn-tup")
            def creatorRef = new UserDto(); creatorRef.aggregateId = creator.aggregateId; creatorRef.name = creator.name; creatorRef.username = creator.username
            def execRef = new ExecutionDto(); execRef.aggregateId = exec.aggregateId
            def topicRef = new TopicDto(); topicRef.aggregateId = topic.aggregateId; topicRef.name = topic.name
            def quizRef = new QuizDto(); quizRef.aggregateId = quiz.aggregateId
            def req = new CreateTournamentRequestDto()
            req.creator = creatorRef; req.participants = [] as Set; req.execution = execRef
            req.topics = [topicRef] as Set; req.quiz = quizRef
            req.startTime = LocalDateTime.now().plusDays(1); req.endTime = LocalDateTime.now().plusDays(2)
            req.numberOfQuestions = 3; req.cancelled = false
            def tournament = tournamentService.createTournament(req, uow)
            unitOfWorkService.commit(uow)
            def event = new TopicUpdatedEvent(topic.aggregateId, "NewTopicName")
            event.setPublisherAggregateVersion(1)
        when:
            tournamentEventProcessing.processTopicUpdatedEvent(tournament.aggregateId, event)
        then:
            noExceptionThrown()
    }

    // === QuizUpdated -> Tournament projection sync ===

    def "QuizUpdated event processes without error on Tournament"() {
        given:
            def course = seedCourse()
            def creator = seedUser("tourn-qup")
            def exec = seedExecution(course, creator)
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
            def uow = unitOfWorkService.createUnitOfWork("ps-tourn-qup")
            def creatorRef = new UserDto(); creatorRef.aggregateId = creator.aggregateId; creatorRef.name = creator.name; creatorRef.username = creator.username
            def execRef = new ExecutionDto(); execRef.aggregateId = exec.aggregateId
            def topicRef = new TopicDto(); topicRef.aggregateId = topic.aggregateId; topicRef.name = topic.name
            def quizRef = new QuizDto(); quizRef.aggregateId = quiz.aggregateId
            def req = new CreateTournamentRequestDto()
            req.creator = creatorRef; req.participants = [] as Set; req.execution = execRef
            req.topics = [topicRef] as Set; req.quiz = quizRef
            req.startTime = LocalDateTime.now().plusDays(1); req.endTime = LocalDateTime.now().plusDays(2)
            req.numberOfQuestions = 3; req.cancelled = false
            def tournament = tournamentService.createTournament(req, uow)
            unitOfWorkService.commit(uow)
            def event = new QuizUpdatedEvent(quiz.aggregateId, "NewTitle", LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now().plusHours(2), LocalDateTime.now().plusDays(1))
            event.setPublisherAggregateVersion(1)
        when:
            tournamentEventProcessing.processQuizUpdatedEvent(tournament.aggregateId, event)
        then:
            noExceptionThrown()
    }

    // === ExecutionUserUpdated -> Answer projection sync ===

    def "ExecutionUserUpdated event processes without error on Answer"() {
        given:
            def course = seedCourse()
            def user = seedUser("ans-euup")
            def exec = seedExecution(course)
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
            def uowA = unitOfWorkService.createUnitOfWork("ps-ans-create")
            def execRef = new ExecutionDto(); execRef.aggregateId = exec.aggregateId
            def userRef = new UserDto(); userRef.aggregateId = user.aggregateId; userRef.name = user.name; userRef.username = user.username
            def quizRef = new QuizDto(); quizRef.aggregateId = quiz.aggregateId
            def ansReq = new CreateAnswerRequestDto()
            ansReq.execution = execRef; ansReq.user = userRef; ansReq.quiz = quizRef; ansReq.questions = [] as Set
            ansReq.creationDate = LocalDateTime.now(); ansReq.answerDate = LocalDateTime.now(); ansReq.completed = false
            def answer = answerService.createAnswer(ansReq, uowA)
            unitOfWorkService.commit(uowA)
            def event = new ExecutionUserUpdatedEvent(exec.aggregateId, user.aggregateId, 1, "NewName", "newuser", true)
            event.setPublisherAggregateVersion(1)
        when:
            answerEventProcessing.processExecutionUserUpdatedEvent(answer.aggregateId, event)
        then:
            noExceptionThrown()
    }

    // === QuestionUpdated -> Answer projection sync ===

    def "QuestionUpdated event processes without error on Answer"() {
        given:
            def course = seedCourse()
            def user = seedUser("ans-qup")
            def exec = seedExecution(course)
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
            def uowA = unitOfWorkService.createUnitOfWork("ps-ans-qup-create")
            def execRef = new ExecutionDto(); execRef.aggregateId = exec.aggregateId
            def userRef = new UserDto(); userRef.aggregateId = user.aggregateId; userRef.name = user.name; userRef.username = user.username
            def quizRef = new QuizDto(); quizRef.aggregateId = quiz.aggregateId
            def ansReq = new CreateAnswerRequestDto()
            ansReq.execution = execRef; ansReq.user = userRef; ansReq.quiz = quizRef; ansReq.questions = [] as Set
            ansReq.creationDate = LocalDateTime.now(); ansReq.answerDate = LocalDateTime.now(); ansReq.completed = false
            def answer = answerService.createAnswer(ansReq, uowA)
            unitOfWorkService.commit(uowA)
            def event = new QuestionUpdatedEvent(question.aggregateId, "NewTitle", "NewContent", LocalDateTime.now())
            event.setPublisherAggregateVersion(1)
        when:
            answerEventProcessing.processQuestionUpdatedEvent(answer.aggregateId, event)
        then:
            noExceptionThrown()
    }

    // === ExecutionUserUpdated -> Tournament projection sync ===

    def "ExecutionUserUpdated event processes without error on Tournament"() {
        given:
            def course = seedCourse()
            def creator = seedUser("tourn-euup")
            def exec = seedExecution(course, creator)
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
            def uow = unitOfWorkService.createUnitOfWork("ps-tourn-euup")
            def creatorRef = new UserDto(); creatorRef.aggregateId = creator.aggregateId; creatorRef.name = creator.name; creatorRef.username = creator.username
            def execRef = new ExecutionDto(); execRef.aggregateId = exec.aggregateId
            def topicRef = new TopicDto(); topicRef.aggregateId = topic.aggregateId; topicRef.name = topic.name
            def quizRef = new QuizDto(); quizRef.aggregateId = quiz.aggregateId
            def req = new CreateTournamentRequestDto()
            req.creator = creatorRef; req.participants = [] as Set; req.execution = execRef
            req.topics = [topicRef] as Set; req.quiz = quizRef
            req.startTime = LocalDateTime.now().plusDays(1); req.endTime = LocalDateTime.now().plusDays(2)
            req.numberOfQuestions = 3; req.cancelled = false
            def tournament = tournamentService.createTournament(req, uow)
            unitOfWorkService.commit(uow)
            def event = new ExecutionUserUpdatedEvent(exec.aggregateId, creator.aggregateId, 1, "UpdatedCreator", "updateduser", true)
            event.setPublisherAggregateVersion(1)
        when:
            tournamentEventProcessing.processExecutionUserUpdatedEvent(tournament.aggregateId, event)
        then:
            noExceptionThrown()
    }
}
