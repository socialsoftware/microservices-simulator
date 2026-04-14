package pt.ulisboa.tecnico.socialsoftware.answers

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.webapi.requestDtos.CreateAnswerRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService
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
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.*
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class AnswerSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UserService userService
    @Autowired ExecutionService executionService
    @Autowired TopicService topicService
    @Autowired QuestionService questionService
    @Autowired QuizService quizService
    @Autowired AnswerService answerService
    @Autowired UnitOfWorkService unitOfWorkService

    // --- seed helpers ---
    private CourseDto seedCourse() {
        def uow = unitOfWorkService.createUnitOfWork("ans-seed-course")
        def req = new CreateCourseRequestDto()
        req.name = "SE-ans-${System.nanoTime()}"
        req.type = CourseType.TECNICO
        req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("ans-seed-user-$tag")
        def req = new CreateUserRequestDto()
        req.name = "User $tag"
        req.username = "ans-$tag-${System.nanoTime()}"
        req.role = UserRole.STUDENT
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private ExecutionDto seedExecution(CourseDto course) {
        def uow = unitOfWorkService.createUnitOfWork("ans-seed-exec")
        def courseRef = new CourseDto()
        courseRef.aggregateId = course.aggregateId
        courseRef.name = course.name
        def req = new CreateExecutionRequestDto()
        req.course = courseRef
        req.users = [] as Set
        req.acronym = "ANS-${System.nanoTime()}"
        req.academicTerm = "2025/2026"
        req.endDate = LocalDateTime.now().plusMonths(3)
        def dto = executionService.createExecution(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private QuizDto seedQuiz(CourseDto course, ExecutionDto exec, QuestionDto question) {
        def uow = unitOfWorkService.createUnitOfWork("ans-seed-quiz")
        def execRef = new ExecutionDto()
        execRef.aggregateId = exec.aggregateId
        execRef.acronym = exec.acronym
        def qRef = new QuestionDto()
        qRef.aggregateId = question.aggregateId
        qRef.title = question.title
        def req = new CreateQuizRequestDto()
        req.execution = execRef
        req.questions = [qRef] as Set
        req.title = "Quiz-${System.nanoTime()}"
        req.quizType = QuizType.TEST
        req.creationDate = LocalDateTime.now()
        req.availableDate = LocalDateTime.now()
        req.conclusionDate = LocalDateTime.now().plusHours(2)
        req.resultsDate = LocalDateTime.now().plusDays(1)
        def dto = quizService.createQuiz(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private QuestionDto seedQuestion(CourseDto course) {
        def topic = seedTopic(course, "t-${System.nanoTime()}")
        def uow = unitOfWorkService.createUnitOfWork("ans-seed-q")
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
        req.title = "Q-${System.nanoTime()}"
        req.content = "content"
        req.creationDate = LocalDateTime.now()
        req.options = [opt]
        def dto = questionService.createQuestion(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private TopicDto seedTopic(CourseDto course, String name) {
        def uow = unitOfWorkService.createUnitOfWork("ans-seed-topic-$name")
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

    // --- tests ---

    def "createAnswer with execution, user, quiz, and questions persists correctly"() {
        given:
            def course = seedCourse()
            def user = seedUser("answerer")
            def exec = seedExecution(course)
            def question = seedQuestion(course)
            def quiz = seedQuiz(course, exec, question)
        when:
            def uow = unitOfWorkService.createUnitOfWork("ans-create")
            def execRef = new ExecutionDto()
            execRef.aggregateId = exec.aggregateId
            def userRef = new UserDto()
            userRef.aggregateId = user.aggregateId
            userRef.name = user.name
            userRef.username = user.username
            def quizRef = new QuizDto()
            quizRef.aggregateId = quiz.aggregateId
            quizRef.title = quiz.title
            def qRef = new AnswerQuestionDto()
            qRef.aggregateId = question.aggregateId
            def req = new CreateAnswerRequestDto()
            req.execution = execRef
            req.user = userRef
            req.quiz = quizRef
            req.questions = [qRef] as Set
            req.creationDate = LocalDateTime.now()
            req.answerDate = LocalDateTime.now()
            req.completed = false
            def answer = answerService.createAnswer(req, uow)
            unitOfWorkService.commit(uow)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("ans-read")
            def found = answerService.getAnswerById(answer.aggregateId, uowR)
            found != null
            found.completed == false
    }

    def "deleteAnswer with empty questions succeeds"() {
        given:
            def course = seedCourse()
            def user = seedUser("del-ans")
            def exec = seedExecution(course)
            def question = seedQuestion(course)
            def quiz = seedQuiz(course, exec, question)
            def uow1 = unitOfWorkService.createUnitOfWork("ans-del-create")
            def execRef = new ExecutionDto()
            execRef.aggregateId = exec.aggregateId
            def userRef = new UserDto()
            userRef.aggregateId = user.aggregateId
            userRef.name = user.name
            userRef.username = user.username
            def quizRef = new QuizDto()
            quizRef.aggregateId = quiz.aggregateId
            def req = new CreateAnswerRequestDto()
            req.execution = execRef
            req.user = userRef
            req.quiz = quizRef
            req.questions = [] as Set
            req.creationDate = LocalDateTime.now()
            req.answerDate = LocalDateTime.now()
            req.completed = false
            def answer = answerService.createAnswer(req, uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow2 = unitOfWorkService.createUnitOfWork("ans-del")
            answerService.deleteAnswer(answer.aggregateId, uow2)
            unitOfWorkService.commit(uow2)
        then:
            noExceptionThrown()
    }
}
