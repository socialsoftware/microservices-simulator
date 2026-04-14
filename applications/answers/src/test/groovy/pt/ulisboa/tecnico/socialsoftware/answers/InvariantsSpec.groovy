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
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.*
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService
import spock.lang.Specification
import java.time.LocalDateTime

@SpringBootTest
@ActiveProfiles(["test", "sagas", "local"])
class InvariantsSpec extends Specification {

    @Autowired CourseService courseService
    @Autowired UserService userService
    @Autowired ExecutionService executionService
    @Autowired TopicService topicService
    @Autowired QuestionService questionService
    @Autowired QuizService quizService
    @Autowired UnitOfWorkService unitOfWorkService

    private CourseDto seedCourse() {
        def uow = unitOfWorkService.createUnitOfWork("inv-seed-course")
        def req = new CreateCourseRequestDto()
        req.name = "INV-${System.nanoTime()}"
        req.type = CourseType.TECNICO
        req.creationDate = LocalDateTime.now()
        def dto = courseService.createCourse(req, uow)
        unitOfWorkService.commit(uow)
        return dto
    }

    // --- Execution invariants ---

    def "execution with blank academicTerm violates invariant"() {
        given:
            def course = seedCourse()
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
        when:
            def uow = unitOfWorkService.createUnitOfWork("inv-exec-term")
            def req = new CreateExecutionRequestDto()
            req.course = courseRef
            req.users = [] as Set
            req.acronym = "INV-${System.nanoTime()}"
            req.academicTerm = ""
            req.endDate = LocalDateTime.now().plusMonths(3)
            executionService.createExecution(req, uow)
        then:
            thrown(Exception)
    }

    def "execution with blank acronym violates invariant"() {
        given:
            def course = seedCourse()
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
        when:
            def uow = unitOfWorkService.createUnitOfWork("inv-exec-acr")
            def req = new CreateExecutionRequestDto()
            req.course = courseRef
            req.users = [] as Set
            req.acronym = ""
            req.academicTerm = "2025/2026"
            req.endDate = LocalDateTime.now().plusMonths(3)
            executionService.createExecution(req, uow)
        then:
            thrown(Exception)
    }

    // --- Topic invariants ---

    def "topic with blank name violates invariant"() {
        given:
            def course = seedCourse()
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
        when:
            def uow = unitOfWorkService.createUnitOfWork("inv-topic-name")
            def req = new CreateTopicRequestDto()
            req.course = courseRef
            req.name = ""
            topicService.createTopic(req, uow)
        then:
            thrown(Exception)
    }

    // --- Question invariants ---

    def "question with blank content violates invariant"() {
        given:
            def course = seedCourse()
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
        when:
            def uow = unitOfWorkService.createUnitOfWork("inv-q-content")
            def opt = new OptionDto()
            opt.key = 1; opt.sequence = 1; opt.correct = true; opt.content = "Yes"
            def req = new CreateQuestionRequestDto()
            req.course = courseRef
            req.topics = [] as Set
            req.title = "Title-${System.nanoTime()}"
            req.content = ""
            req.creationDate = LocalDateTime.now()
            req.options = [opt]
            questionService.createQuestion(req, uow)
        then:
            thrown(Exception)
    }

    def "question with blank title violates invariant"() {
        given:
            def course = seedCourse()
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
        when:
            def uow = unitOfWorkService.createUnitOfWork("inv-q-title")
            def opt = new OptionDto()
            opt.key = 1; opt.sequence = 1; opt.correct = true; opt.content = "Yes"
            def req = new CreateQuestionRequestDto()
            req.course = courseRef
            req.topics = [] as Set
            req.title = ""
            req.content = "Some content"
            req.creationDate = LocalDateTime.now()
            req.options = [opt]
            questionService.createQuestion(req, uow)
        then:
            thrown(Exception)
    }

    // --- CRUD update coverage for aggregates missing update tests ---

    def "updateTopic changes name"() {
        given:
            def course = seedCourse()
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def uowC = unitOfWorkService.createUnitOfWork("inv-topic-create")
            def topicReq = new CreateTopicRequestDto()
            topicReq.course = courseRef
            topicReq.name = "OrigTopic-${System.nanoTime()}"
            def topic = topicService.createTopic(topicReq, uowC)
            unitOfWorkService.commit(uowC)
        when:
            def uowGet = unitOfWorkService.createUnitOfWork("inv-topic-upd-get")
            def existing = topicService.getTopicById(topic.aggregateId, uowGet)
            existing.name = "RenamedTopic"
            def uowPut = unitOfWorkService.createUnitOfWork("inv-topic-upd-put")
            topicService.updateTopic(existing, uowPut)
            unitOfWorkService.commit(uowPut)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("inv-topic-upd-read")
            def reloaded = topicService.getTopicById(topic.aggregateId, uowR)
            reloaded.name == "RenamedTopic"
    }

    def "updateQuestion changes title"() {
        given:
            def course = seedCourse()
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def uowQ = unitOfWorkService.createUnitOfWork("inv-q-create")
            def opt = new OptionDto()
            opt.key = 1; opt.sequence = 1; opt.correct = true; opt.content = "Yes"
            def qReq = new CreateQuestionRequestDto()
            qReq.course = courseRef
            qReq.topics = [] as Set
            qReq.title = "OrigQ-${System.nanoTime()}"
            qReq.content = "content"
            qReq.creationDate = LocalDateTime.now()
            qReq.options = [opt]
            def question = questionService.createQuestion(qReq, uowQ)
            unitOfWorkService.commit(uowQ)
        when:
            def uowGet = unitOfWorkService.createUnitOfWork("inv-q-upd-get")
            def existing = questionService.getQuestionById(question.aggregateId, uowGet)
            existing.title = "RenamedQ"
            def uowPut = unitOfWorkService.createUnitOfWork("inv-q-upd-put")
            questionService.updateQuestion(existing, uowPut)
            unitOfWorkService.commit(uowPut)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("inv-q-upd-read")
            def reloaded = questionService.getQuestionById(question.aggregateId, uowR)
            reloaded.title == "RenamedQ"
    }

    def "updateExecution changes acronym"() {
        given:
            def course = seedCourse()
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def uowE = unitOfWorkService.createUnitOfWork("inv-exec-create")
            def execReq = new CreateExecutionRequestDto()
            execReq.course = courseRef
            execReq.users = [] as Set
            execReq.acronym = "ORIG-${System.nanoTime()}"
            execReq.academicTerm = "2025/2026"
            execReq.endDate = LocalDateTime.now().plusMonths(3)
            def exec = executionService.createExecution(execReq, uowE)
            unitOfWorkService.commit(uowE)
        when:
            def uowGet = unitOfWorkService.createUnitOfWork("inv-exec-upd-get")
            def existing = executionService.getExecutionById(exec.aggregateId, uowGet)
            existing.acronym = "RENAMED"
            def uowPut = unitOfWorkService.createUnitOfWork("inv-exec-upd-put")
            executionService.updateExecution(existing, uowPut)
            unitOfWorkService.commit(uowPut)
        then:
            def uowR = unitOfWorkService.createUnitOfWork("inv-exec-upd-read")
            def reloaded = executionService.getExecutionById(exec.aggregateId, uowR)
            reloaded.acronym == "RENAMED"
    }

    // --- User invariants ---

    def "user with null role violates invariant"() {
        given:
            def uow = unitOfWorkService.createUnitOfWork("inv-user-role")
            def req = new CreateUserRequestDto()
            req.name = "Test"
            req.username = "role-test-${System.nanoTime()}"
            req.role = null
            req.active = true
        when:
            userService.createUser(req, uow)
        then:
            thrown(Exception)
    }

    // --- Quiz invariants ---

    def "quiz with dates out of order violates invariant"() {
        given:
            def course = seedCourse()
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            // create an execution for the quiz
            def uowE = unitOfWorkService.createUnitOfWork("inv-quiz-exec")
            def execReq = new CreateExecutionRequestDto()
            execReq.course = courseRef
            execReq.users = [] as Set
            execReq.acronym = "QINV-${System.nanoTime()}"
            execReq.academicTerm = "2025/2026"
            execReq.endDate = LocalDateTime.now().plusMonths(3)
            def exec = executionService.createExecution(execReq, uowE)
            unitOfWorkService.commit(uowE)
            def execRef = new ExecutionDto()
            execRef.aggregateId = exec.aggregateId
            execRef.acronym = exec.acronym
        when:
            def uow = unitOfWorkService.createUnitOfWork("inv-quiz-dates")
            def req = new CreateQuizRequestDto()
            req.execution = execRef
            req.questions = [] as Set
            req.title = "BadDates-${System.nanoTime()}"
            req.quizType = QuizType.GENERATED
            req.creationDate = LocalDateTime.now()
            req.availableDate = LocalDateTime.now().plusDays(3)
            req.conclusionDate = LocalDateTime.now().plusDays(1)  // BEFORE available
            req.resultsDate = LocalDateTime.now().plusDays(5)
            quizService.createQuiz(req, uow)
        then:
            thrown(Exception)
    }

    def "quiz with blank title violates invariant"() {
        given:
            def course = seedCourse()
            def courseRef = new CourseDto()
            courseRef.aggregateId = course.aggregateId
            courseRef.name = course.name
            def uowE = unitOfWorkService.createUnitOfWork("inv-quiz-exec2")
            def execReq = new CreateExecutionRequestDto()
            execReq.course = courseRef
            execReq.users = [] as Set
            execReq.acronym = "QTINV-${System.nanoTime()}"
            execReq.academicTerm = "2025/2026"
            execReq.endDate = LocalDateTime.now().plusMonths(3)
            def exec = executionService.createExecution(execReq, uowE)
            unitOfWorkService.commit(uowE)
            def execRef = new ExecutionDto()
            execRef.aggregateId = exec.aggregateId
            execRef.acronym = exec.acronym
        when:
            def uow = unitOfWorkService.createUnitOfWork("inv-quiz-title")
            def req = new CreateQuizRequestDto()
            req.execution = execRef
            req.questions = [] as Set
            req.title = ""
            req.quizType = QuizType.GENERATED
            req.creationDate = LocalDateTime.now()
            req.availableDate = LocalDateTime.now()
            req.conclusionDate = LocalDateTime.now().plusHours(2)
            req.resultsDate = LocalDateTime.now().plusDays(1)
            quizService.createQuiz(req, uow)
        then:
            thrown(Exception)
    }
}
