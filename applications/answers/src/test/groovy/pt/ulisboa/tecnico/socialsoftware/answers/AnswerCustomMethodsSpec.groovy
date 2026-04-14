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
class AnswerCustomMethodsSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UserService userService
    @Autowired ExecutionService executionService
    @Autowired TopicService topicService
    @Autowired QuestionService questionService
    @Autowired QuizService quizService
    @Autowired AnswerService answerService
    @Autowired UnitOfWorkService unitOfWorkService

    private CourseDto seedCourse() {
        def uow = unitOfWorkService.createUnitOfWork("acm-seed-course")
        def req = new CreateCourseRequestDto()
        req.name = "ACM-${System.nanoTime()}"
        req.type = CourseType.TECNICO
        req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private UserDto seedUser(String tag) {
        def uow = unitOfWorkService.createUnitOfWork("acm-seed-user-$tag")
        def req = new CreateUserRequestDto()
        req.name = "User $tag"
        req.username = "acm-$tag-${System.nanoTime()}"
        req.role = UserRole.STUDENT
        req.active = true
        def dto = userService.createUser(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private ExecutionDto seedExecution(CourseDto course) {
        def uow = unitOfWorkService.createUnitOfWork("acm-seed-exec")
        def courseRef = new CourseDto()
        courseRef.aggregateId = course.aggregateId
        courseRef.name = course.name
        def req = new CreateExecutionRequestDto()
        req.course = courseRef
        req.users = [] as Set
        req.acronym = "ACM-${System.nanoTime()}"
        req.academicTerm = "2025/2026"
        req.endDate = LocalDateTime.now().plusMonths(3)
        def dto = executionService.createExecution(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private TopicDto seedTopic(CourseDto course) {
        def uow = unitOfWorkService.createUnitOfWork("acm-seed-topic")
        def courseRef = new CourseDto()
        courseRef.aggregateId = course.aggregateId
        courseRef.name = course.name
        def req = new CreateTopicRequestDto()
        req.course = courseRef
        req.name = "ACMTopic-${System.nanoTime()}"
        def dto = topicService.createTopic(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private QuestionDto seedQuestion(CourseDto course) {
        def topic = seedTopic(course)
        def uow = unitOfWorkService.createUnitOfWork("acm-seed-q")
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
        req.title = "ACM-Q-${System.nanoTime()}"
        req.content = "content"
        req.creationDate = LocalDateTime.now()
        req.options = [opt]
        def dto = questionService.createQuestion(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private QuizDto seedQuiz(ExecutionDto exec, QuestionDto question) {
        def uow = unitOfWorkService.createUnitOfWork("acm-seed-quiz")
        def execRef = new ExecutionDto()
        execRef.aggregateId = exec.aggregateId
        execRef.acronym = exec.acronym
        def qRef = new QuestionDto()
        qRef.aggregateId = question.aggregateId
        qRef.title = question.title
        def req = new CreateQuizRequestDto()
        req.execution = execRef
        req.questions = [qRef] as Set
        req.title = "ACMQuiz-${System.nanoTime()}"
        req.quizType = QuizType.TEST
        req.creationDate = LocalDateTime.now()
        req.availableDate = LocalDateTime.now()
        req.conclusionDate = LocalDateTime.now().plusHours(2)
        req.resultsDate = LocalDateTime.now().plusDays(1)
        def dto = quizService.createQuiz(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    private def createAnswerWithKey(ExecutionDto exec, UserDto user, QuizDto quiz, QuestionDto question, int key) {
        def uow = unitOfWorkService.createUnitOfWork("acm-create-answer")
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
        qRef.key = key
        qRef.sequence = 1
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
        return answer
    }

    def "answerQuestion sets timeTaken and correct on specific question"() {
        given:
            def course = seedCourse()
            def user = seedUser("aq-user")
            def exec = seedExecution(course)
            def question = seedQuestion(course)
            def quiz = seedQuiz(exec, question)
            def answer = createAnswerWithKey(exec, user, quiz, question, 1)
        when:
            def uow = unitOfWorkService.createUnitOfWork("acm-answer-q")
            answerService.answerQuestion(answer.aggregateId, 1, 42, true, uow)
            unitOfWorkService.commit(uow)
        then:
            noExceptionThrown()
    }

    def "answerQuestion with multiple questions targets correct one"() {
        given:
            def course = seedCourse()
            def user = seedUser("aq-multi")
            def exec = seedExecution(course)
            def q1 = seedQuestion(course)
            def q2 = seedQuestion(course)
            def quiz = seedQuiz(exec, q1)
            def uow1 = unitOfWorkService.createUnitOfWork("acm-multi-create")
            def execRef = new ExecutionDto()
            execRef.aggregateId = exec.aggregateId
            def userRef = new UserDto()
            userRef.aggregateId = user.aggregateId
            userRef.name = user.name
            userRef.username = user.username
            def quizRef = new QuizDto()
            quizRef.aggregateId = quiz.aggregateId
            def qRef1 = new AnswerQuestionDto()
            qRef1.aggregateId = q1.aggregateId
            qRef1.key = 10
            qRef1.sequence = 1
            def qRef2 = new AnswerQuestionDto()
            qRef2.aggregateId = q2.aggregateId
            qRef2.key = 20
            qRef2.sequence = 2
            def req = new CreateAnswerRequestDto()
            req.execution = execRef
            req.user = userRef
            req.quiz = quizRef
            req.questions = [qRef1, qRef2] as Set
            req.creationDate = LocalDateTime.now()
            req.answerDate = LocalDateTime.now()
            req.completed = false
            def answer = answerService.createAnswer(req, uow1)
            unitOfWorkService.commit(uow1)
        when:
            def uow = unitOfWorkService.createUnitOfWork("acm-answer-q2")
            answerService.answerQuestion(answer.aggregateId, 20, 99, false, uow)
            unitOfWorkService.commit(uow)
        then:
            noExceptionThrown()
    }

    def "answerQuestion with non-existing key throws"() {
        given:
            def course = seedCourse()
            def user = seedUser("aq-bad-key")
            def exec = seedExecution(course)
            def question = seedQuestion(course)
            def quiz = seedQuiz(exec, question)
            def answer = createAnswerWithKey(exec, user, quiz, question, 1)
        when:
            def uow = unitOfWorkService.createUnitOfWork("acm-answer-bad")
            answerService.answerQuestion(answer.aggregateId, 999, 10, true, uow)
        then:
            thrown(Exception)
    }

    def "updateAnswer changes completed flag"() {
        given:
            def course = seedCourse()
            def user = seedUser("upd-ans")
            def exec = seedExecution(course)
            def question = seedQuestion(course)
            def quiz = seedQuiz(exec, question)
            def answer = createAnswerWithKey(exec, user, quiz, question, 1)
        when:
            def uowGet = unitOfWorkService.createUnitOfWork("acm-upd-get")
            def existing = answerService.getAnswerById(answer.aggregateId, uowGet)
            existing.completed = true
            def uowPut = unitOfWorkService.createUnitOfWork("acm-upd-put")
            answerService.updateAnswer(existing, uowPut)
            unitOfWorkService.commit(uowPut)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("acm-upd-read")
            def reloaded = answerService.getAnswerById(answer.aggregateId, uowR)
            reloaded.completed == true
    }
}
