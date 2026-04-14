package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.coordination.webapi.requestDtos.CreateCourseRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.eventProcessing.ExecutionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.webapi.requestDtos.CreateExecutionRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.eventProcessing.TopicEventProcessing
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.coordination.webapi.requestDtos.CreateTopicRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.topic.service.TopicService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.eventProcessing.QuestionEventProcessing
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.webapi.requestDtos.CreateQuestionRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.eventProcessing.QuizEventProcessing
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.webapi.requestDtos.CreateQuizRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.eventProcessing.AnswerEventProcessing
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.webapi.requestDtos.CreateAnswerRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.eventProcessing.TournamentEventProcessing
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.webapi.requestDtos.CreateTournamentRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService
import pt.ulisboa.tecnico.socialsoftware.answers.events.*
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.*
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState
import spock.lang.Specification
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class InterInvariantFullSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UserService userService
    @Autowired ExecutionService executionService
    @Autowired TopicService topicService
    @Autowired QuestionService questionService
    @Autowired QuizService quizService
    @Autowired AnswerService answerService
    @Autowired TournamentService tournamentService
    @Autowired UnitOfWorkService unitOfWorkService

    @Autowired ExecutionEventProcessing executionEventProcessing
    @Autowired TopicEventProcessing topicEventProcessing
    @Autowired QuestionEventProcessing questionEventProcessing
    @Autowired QuizEventProcessing quizEventProcessing
    @Autowired AnswerEventProcessing answerEventProcessing
    @Autowired TournamentEventProcessing tournamentEventProcessing

    // --- seed helpers ---
    private CourseDto seedCourse() {
        def uow = unitOfWorkService.createUnitOfWork("ii-seed-course")
        def req = new CreateCourseRequestDto()
        req.name = "II-${System.nanoTime()}"
        req.type = CourseType.TECNICO
        req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("ii-seed-user-$tag")
        def req = new CreateUserRequestDto()
        req.name = "User $tag"
        req.username = "ii-$tag-${System.nanoTime()}"
        req.role = UserRole.STUDENT
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private ExecutionDto seedExecution(CourseDto course, UserDto... users) {
        def uow = unitOfWorkService.createUnitOfWork("ii-seed-exec")
        def courseRef = new CourseDto()
        courseRef.aggregateId = course.aggregateId
        courseRef.name = course.name
        def userRefs = users.collect { u ->
            def ref = new UserDto(); ref.aggregateId = u.aggregateId; ref.name = u.name; ref.username = u.username; return ref
        } as Set
        def req = new CreateExecutionRequestDto()
        req.course = courseRef; req.users = userRefs; req.acronym = "II-${System.nanoTime()}"
        req.academicTerm = "2025/2026"; req.endDate = LocalDateTime.now().plusMonths(6)
        def dto = executionService.createExecution(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private TopicDto seedTopic(CourseDto course) {
        def uow = unitOfWorkService.createUnitOfWork("ii-seed-topic")
        def courseRef = new CourseDto(); courseRef.aggregateId = course.aggregateId; courseRef.name = course.name
        def req = new CreateTopicRequestDto(); req.course = courseRef; req.name = "IITopic-${System.nanoTime()}"
        def dto = topicService.createTopic(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private QuestionDto seedQuestion(CourseDto course, TopicDto topic) {
        def uow = unitOfWorkService.createUnitOfWork("ii-seed-q")
        def courseRef = new CourseDto(); courseRef.aggregateId = course.aggregateId; courseRef.name = course.name
        def topicRef = new TopicDto(); topicRef.aggregateId = topic.aggregateId; topicRef.name = topic.name
        def opt = new OptionDto(); opt.key = 1; opt.sequence = 1; opt.correct = true; opt.content = "Yes"
        def req = new CreateQuestionRequestDto()
        req.course = courseRef; req.topics = [topicRef] as Set; req.title = "II-Q-${System.nanoTime()}"
        req.content = "content"; req.creationDate = LocalDateTime.now(); req.options = [opt]
        def dto = questionService.createQuestion(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private QuizDto seedQuiz(ExecutionDto exec, QuestionDto question) {
        def uow = unitOfWorkService.createUnitOfWork("ii-seed-quiz")
        def execRef = new ExecutionDto(); execRef.aggregateId = exec.aggregateId; execRef.acronym = exec.acronym
        def qRef = new QuestionDto(); qRef.aggregateId = question.aggregateId; qRef.title = question.title
        def req = new CreateQuizRequestDto()
        req.execution = execRef; req.questions = [qRef] as Set; req.title = "IIQuiz-${System.nanoTime()}"
        req.quizType = QuizType.GENERATED; req.creationDate = LocalDateTime.now()
        req.availableDate = LocalDateTime.now(); req.conclusionDate = LocalDateTime.now().plusHours(2)
        req.resultsDate = LocalDateTime.now().plusDays(1)
        def dto = quizService.createQuiz(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private def seedAnswer(ExecutionDto exec, UserDto user, QuizDto quiz) {
        def uow = unitOfWorkService.createUnitOfWork("ii-seed-ans")
        def execRef = new ExecutionDto(); execRef.aggregateId = exec.aggregateId
        def userRef = new UserDto(); userRef.aggregateId = user.aggregateId; userRef.name = user.name; userRef.username = user.username
        def quizRef = new QuizDto(); quizRef.aggregateId = quiz.aggregateId
        def req = new CreateAnswerRequestDto()
        req.execution = execRef; req.user = userRef; req.quiz = quizRef; req.questions = [] as Set
        req.creationDate = LocalDateTime.now(); req.answerDate = LocalDateTime.now(); req.completed = false
        def dto = answerService.createAnswer(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private def seedTournament(CourseDto course, UserDto creator) {
        def exec = seedExecution(course, creator)
        def topic = seedTopic(course)
        def question = seedQuestion(course, topic)
        def quiz = seedQuiz(exec, question)
        def uow = unitOfWorkService.createUnitOfWork("ii-seed-tourn")
        def creatorRef = new UserDto(); creatorRef.aggregateId = creator.aggregateId; creatorRef.name = creator.name; creatorRef.username = creator.username
        def execRef = new ExecutionDto(); execRef.aggregateId = exec.aggregateId
        def topicRef = new TopicDto(); topicRef.aggregateId = topic.aggregateId; topicRef.name = topic.name
        def quizRef = new QuizDto(); quizRef.aggregateId = quiz.aggregateId
        def req = new CreateTournamentRequestDto()
        req.creator = creatorRef; req.participants = [] as Set; req.execution = execRef
        req.topics = [topicRef] as Set; req.quiz = quizRef
        req.startTime = LocalDateTime.now().plusDays(1); req.endTime = LocalDateTime.now().plusDays(2)
        req.numberOfQuestions = 3; req.cancelled = false
        def dto = tournamentService.createTournament(req, uow)
        unitOfWorkService.commit(uow)
        return [tournament: dto, exec: exec, topic: topic, question: question, quiz: quiz]
    }

    // === Execution interInvariants ===

    def "UserDeleted makes execution INACTIVE"() {
        given:
            def course = seedCourse()
            def user = seedUser("exec-udel")
            def exec = seedExecution(course, user)
        when:
            executionEventProcessing.processUserDeletedEvent(exec.aggregateId, new UserDeletedEvent(user.aggregateId))
        then:
            def uow = unitOfWorkService.createUnitOfWork("ii-exec-udel-read")
            executionService.getExecutionById(exec.aggregateId, uow).state == AggregateState.INACTIVE
    }

    // === Question interInvariants ===

    def "TopicDeleted makes question INACTIVE"() {
        given:
            def course = seedCourse()
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
        when:
            questionEventProcessing.processTopicDeletedEvent(question.aggregateId, new TopicDeletedEvent(topic.aggregateId))
        then:
            def uow = unitOfWorkService.createUnitOfWork("ii-q-tdel-read")
            questionService.getQuestionById(question.aggregateId, uow).state == AggregateState.INACTIVE
    }

    // === Quiz interInvariants ===

    def "ExecutionDeleted makes quiz INACTIVE"() {
        given:
            def course = seedCourse()
            def exec = seedExecution(course)
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
        when:
            quizEventProcessing.processExecutionDeletedEvent(quiz.aggregateId, new ExecutionDeletedEvent(exec.aggregateId))
        then:
            def uow = unitOfWorkService.createUnitOfWork("ii-quiz-edel-read")
            quizService.getQuizById(quiz.aggregateId, uow).state == AggregateState.INACTIVE
    }

    def "QuestionDeleted makes quiz INACTIVE"() {
        given:
            def course = seedCourse()
            def exec = seedExecution(course)
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
        when:
            quizEventProcessing.processQuestionDeletedEvent(quiz.aggregateId, new QuestionDeletedEvent(question.aggregateId))
        then:
            def uow = unitOfWorkService.createUnitOfWork("ii-quiz-qdel-read")
            quizService.getQuizById(quiz.aggregateId, uow).state == AggregateState.INACTIVE
    }

    // === Answer interInvariants ===

    def "QuizDeleted makes answer INACTIVE"() {
        given:
            def course = seedCourse()
            def user = seedUser("ans-qzdel")
            def exec = seedExecution(course)
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
            def answer = seedAnswer(exec, user, quiz)
        when:
            answerEventProcessing.processQuizDeletedEvent(answer.aggregateId, new QuizDeletedEvent(quiz.aggregateId))
        then:
            def uow = unitOfWorkService.createUnitOfWork("ii-ans-qzdel-read")
            answerService.getAnswerById(answer.aggregateId, uow).state == AggregateState.INACTIVE
    }

    def "QuestionDeleted makes answer INACTIVE"() {
        given:
            def course = seedCourse()
            def user = seedUser("ans-qdel")
            def exec = seedExecution(course)
            def topic = seedTopic(course)
            def question = seedQuestion(course, topic)
            def quiz = seedQuiz(exec, question)
            def answer = seedAnswer(exec, user, quiz)
        when:
            answerEventProcessing.processQuestionDeletedEvent(answer.aggregateId, new QuestionDeletedEvent(question.aggregateId))
        then:
            def uow = unitOfWorkService.createUnitOfWork("ii-ans-qdel-read")
            answerService.getAnswerById(answer.aggregateId, uow).state == AggregateState.INACTIVE
    }

    // === Tournament interInvariants ===

    def "ExecutionDeleted makes tournament INACTIVE"() {
        given:
            def course = seedCourse()
            def creator = seedUser("tourn-edel")
            def data = seedTournament(course, creator)
        when:
            tournamentEventProcessing.processExecutionDeletedEvent(data.tournament.aggregateId, new ExecutionDeletedEvent(data.exec.aggregateId))
        then:
            def uow = unitOfWorkService.createUnitOfWork("ii-tourn-edel-read")
            tournamentService.getTournamentById(data.tournament.aggregateId, uow).state == AggregateState.INACTIVE
    }

    def "UserDeleted makes tournament INACTIVE (creator)"() {
        given:
            def course = seedCourse()
            def creator = seedUser("tourn-udel")
            def data = seedTournament(course, creator)
        when:
            tournamentEventProcessing.processUserDeletedEvent(data.tournament.aggregateId, new UserDeletedEvent(creator.aggregateId))
        then:
            def uow = unitOfWorkService.createUnitOfWork("ii-tourn-udel-read")
            tournamentService.getTournamentById(data.tournament.aggregateId, uow).state == AggregateState.INACTIVE
    }

    def "TopicDeleted makes tournament INACTIVE"() {
        given:
            def course = seedCourse()
            def creator = seedUser("tourn-tdel")
            def data = seedTournament(course, creator)
        when:
            tournamentEventProcessing.processTopicDeletedEvent(data.tournament.aggregateId, new TopicDeletedEvent(data.topic.aggregateId))
        then:
            def uow = unitOfWorkService.createUnitOfWork("ii-tourn-tdel-read")
            tournamentService.getTournamentById(data.tournament.aggregateId, uow).state == AggregateState.INACTIVE
    }

    def "QuizDeleted makes tournament INACTIVE"() {
        given:
            def course = seedCourse()
            def creator = seedUser("tourn-qzdel")
            def data = seedTournament(course, creator)
        when:
            tournamentEventProcessing.processQuizDeletedEvent(data.tournament.aggregateId, new QuizDeletedEvent(data.quiz.aggregateId))
        then:
            def uow = unitOfWorkService.createUnitOfWork("ii-tourn-qzdel-read")
            tournamentService.getTournamentById(data.tournament.aggregateId, uow).state == AggregateState.INACTIVE
    }
}
