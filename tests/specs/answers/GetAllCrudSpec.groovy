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
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.coordination.webapi.requestDtos.CreateQuestionRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.question.service.QuestionService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.coordination.webapi.requestDtos.CreateQuizRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.quiz.service.QuizService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.coordination.webapi.requestDtos.CreateAnswerRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.answer.service.AnswerService
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.*
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class GetAllCrudSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UserService userService
    @Autowired ExecutionService executionService
    @Autowired TopicService topicService
    @Autowired QuestionService questionService
    @Autowired QuizService quizService
    @Autowired AnswerService answerService
    @Autowired UnitOfWorkService unitOfWorkService

    private CourseDto seedCourse() {
        def uow = unitOfWorkService.createUnitOfWork("ga-seed-course")
        def req = new CreateCourseRequestDto()
        req.name = "GA-${System.nanoTime()}"; req.type = CourseType.TECNICO; req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("ga-seed-user-$tag")
        def req = new CreateUserRequestDto()
        req.name = "User $tag"; req.username = "ga-$tag-${System.nanoTime()}"; req.role = UserRole.STUDENT; req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    def "getAllCourses returns created courses"() {
        given:
            def c = seedCourse()
        when:
            def uow = unitOfWorkService.createUnitOfWork("ga-courses")
            def all = courseService.getAllCourses(uow)
        then:
            all.any { it.aggregateId == c.aggregateId }
    }

    def "getAllTopics returns created topics"() {
        given:
            def course = seedCourse()
            def uowT = unitOfWorkService.createUnitOfWork("ga-topic-create")
            def courseRef = new CourseDto(); courseRef.aggregateId = course.aggregateId; courseRef.name = course.name
            def req = new CreateTopicRequestDto(); req.course = courseRef; req.name = "GATopic-${System.nanoTime()}"
            def topic = topicService.createTopic(req, uowT)
            unitOfWorkService.commit(uowT)
        when:
            def uow = unitOfWorkService.createUnitOfWork("ga-topics")
            def all = topicService.getAllTopics(uow)
        then:
            all.any { it.aggregateId == topic.aggregateId }
    }

    def "getAllExecutions returns created executions"() {
        given:
            def course = seedCourse()
            def uowE = unitOfWorkService.createUnitOfWork("ga-exec-create")
            def courseRef = new CourseDto(); courseRef.aggregateId = course.aggregateId; courseRef.name = course.name
            def req = new CreateExecutionRequestDto()
            req.course = courseRef; req.users = [] as Set; req.acronym = "GA-${System.nanoTime()}"
            req.academicTerm = "2025/2026"; req.endDate = LocalDateTime.now().plusMonths(3)
            def exec = executionService.createExecution(req, uowE)
            unitOfWorkService.commit(uowE)
        when:
            def uow = unitOfWorkService.createUnitOfWork("ga-executions")
            def all = executionService.getAllExecutions(uow)
        then:
            all.any { it.aggregateId == exec.aggregateId }
    }

    def "getAllQuestions returns created questions"() {
        given:
            def course = seedCourse()
            def uowQ = unitOfWorkService.createUnitOfWork("ga-q-create")
            def courseRef = new CourseDto(); courseRef.aggregateId = course.aggregateId; courseRef.name = course.name
            def opt = new OptionDto(); opt.key = 1; opt.sequence = 1; opt.correct = true; opt.content = "Yes"
            def req = new CreateQuestionRequestDto()
            req.course = courseRef; req.topics = [] as Set; req.title = "GA-Q-${System.nanoTime()}"
            req.content = "content"; req.creationDate = LocalDateTime.now(); req.options = [opt]
            def question = questionService.createQuestion(req, uowQ)
            unitOfWorkService.commit(uowQ)
        when:
            def uow = unitOfWorkService.createUnitOfWork("ga-questions")
            def all = questionService.getAllQuestions(uow)
        then:
            all.any { it.aggregateId == question.aggregateId }
    }

    def "getAllQuizzes returns created quizzes"() {
        given:
            def course = seedCourse()
            def uowE = unitOfWorkService.createUnitOfWork("ga-quiz-exec")
            def courseRef = new CourseDto(); courseRef.aggregateId = course.aggregateId; courseRef.name = course.name
            def execReq = new CreateExecutionRequestDto()
            execReq.course = courseRef; execReq.users = [] as Set; execReq.acronym = "GAQ-${System.nanoTime()}"
            execReq.academicTerm = "2025/2026"; execReq.endDate = LocalDateTime.now().plusMonths(3)
            def exec = executionService.createExecution(execReq, uowE)
            unitOfWorkService.commit(uowE)
            def uowQ = unitOfWorkService.createUnitOfWork("ga-quiz-create")
            def execRef = new ExecutionDto(); execRef.aggregateId = exec.aggregateId; execRef.acronym = exec.acronym
            def req = new CreateQuizRequestDto()
            req.execution = execRef; req.questions = [] as Set; req.title = "GAQuiz-${System.nanoTime()}"
            req.quizType = QuizType.GENERATED; req.creationDate = LocalDateTime.now()
            req.availableDate = LocalDateTime.now(); req.conclusionDate = LocalDateTime.now().plusHours(2)
            req.resultsDate = LocalDateTime.now().plusDays(1)
            def quiz = quizService.createQuiz(req, uowQ)
            unitOfWorkService.commit(uowQ)
        when:
            def uow = unitOfWorkService.createUnitOfWork("ga-quizzes")
            def all = quizService.getAllQuizs(uow)
        then:
            all.any { it.aggregateId == quiz.aggregateId }
    }

    def "getAllAnswers returns created answers"() {
        given:
            def course = seedCourse()
            def user = seedUser("ga-ans")
            def uowE = unitOfWorkService.createUnitOfWork("ga-ans-exec")
            def courseRef = new CourseDto(); courseRef.aggregateId = course.aggregateId; courseRef.name = course.name
            def execReq = new CreateExecutionRequestDto()
            execReq.course = courseRef; execReq.users = [] as Set; execReq.acronym = "GAA-${System.nanoTime()}"
            execReq.academicTerm = "2025/2026"; execReq.endDate = LocalDateTime.now().plusMonths(3)
            def exec = executionService.createExecution(execReq, uowE)
            unitOfWorkService.commit(uowE)
            def uowQz = unitOfWorkService.createUnitOfWork("ga-ans-quiz")
            def execRef = new ExecutionDto(); execRef.aggregateId = exec.aggregateId; execRef.acronym = exec.acronym
            def quizReq = new CreateQuizRequestDto()
            quizReq.execution = execRef; quizReq.questions = [] as Set; quizReq.title = "GAAQuiz-${System.nanoTime()}"
            quizReq.quizType = QuizType.TEST; quizReq.creationDate = LocalDateTime.now()
            quizReq.availableDate = LocalDateTime.now(); quizReq.conclusionDate = LocalDateTime.now().plusHours(2)
            quizReq.resultsDate = LocalDateTime.now().plusDays(1)
            def quiz = quizService.createQuiz(quizReq, uowQz)
            unitOfWorkService.commit(uowQz)
            def uowA = unitOfWorkService.createUnitOfWork("ga-ans-create")
            def execRef2 = new ExecutionDto(); execRef2.aggregateId = exec.aggregateId
            def userRef = new UserDto(); userRef.aggregateId = user.aggregateId; userRef.name = user.name; userRef.username = user.username
            def quizRef = new QuizDto(); quizRef.aggregateId = quiz.aggregateId
            def ansReq = new CreateAnswerRequestDto()
            ansReq.execution = execRef2; ansReq.user = userRef; ansReq.quiz = quizRef; ansReq.questions = [] as Set
            ansReq.creationDate = LocalDateTime.now(); ansReq.answerDate = LocalDateTime.now(); ansReq.completed = false
            def answer = answerService.createAnswer(ansReq, uowA)
            unitOfWorkService.commit(uowA)
        when:
            def uow = unitOfWorkService.createUnitOfWork("ga-answers")
            def all = answerService.getAllAnswers(uow)
        then:
            all.any { it.aggregateId == answer.aggregateId }
    }
}
